package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BotRepository(private val botDao: BotDao) {

    // Configuration Flow
    val configuration: Flow<BotConfiguration> = botDao.getConfiguration()
        .map { it ?: BotConfiguration() }

    suspend fun getConfigurationDirect(): BotConfiguration {
        return botDao.getConfigurationDirect() ?: BotConfiguration()
    }

    suspend fun saveConfiguration(config: BotConfiguration) {
        botDao.saveConfiguration(config)
    }

    // Trades
    val allTrades: Flow<List<SimulatedTrade>> = botDao.getAllTrades()
    val openTrades: Flow<List<SimulatedTrade>> = botDao.getOpenTradesFlow()

    suspend fun getOpenTradesDirect(): List<SimulatedTrade> {
        return botDao.getOpenTradesDirect()
    }

    suspend fun getOpenTradesForSymbol(symbol: String): List<SimulatedTrade> {
        return botDao.getOpenTradesForSymbol(symbol)
    }

    suspend fun openTrade(
        symbol: String,
        side: String,
        entryPrice: Double,
        quantity: Double,
        leverage: Int,
        takeProfitPct: Double,
        stopLossPct: Double
    ): Long {
        // Calculate TP and SL prices
        // For LONG: TP = Entry * (1 + TP_PCT / Leverage), SL = Entry * (1 - SL_PCT / Leverage)
        // For SHORT: TP = Entry * (1 - TP_PCT / Leverage), SL = Entry * (1 + SL_PCT / Leverage)
        // Note: TP_PCT and SL_PCT are input as percentages (e.g. 1.5 for 1.5%)
        val factor = 100.0 * leverage
        val takeProfitPrice = if (side == "LONG") {
            entryPrice * (1.0 + (takeProfitPct / factor))
        } else {
            entryPrice * (1.0 - (takeProfitPct / factor))
        }

        val stopLossPrice = if (side == "LONG") {
            entryPrice * (1.0 - (stopLossPct / factor))
        } else {
            entryPrice * (1.0 + (stopLossPct / factor))
        }

        val trade = SimulatedTrade(
            symbol = symbol,
            side = side,
            entryPrice = entryPrice,
            quantity = quantity,
            leverage = leverage,
            takeProfitPrice = takeProfitPrice,
            stopLossPrice = stopLossPrice,
            status = "OPEN"
        )
        val id = botDao.insertTrade(trade)
        
        insertLog(
            TradingLog(
                symbol = symbol,
                level = "TRIGGER",
                message = "فتح مركز $side: بسعر $entryPrice، الكمية: $quantity، جني الأرباح: %.4f، وقف الخسارة: %.4f".format(takeProfitPrice, stopLossPrice)
            )
        )
        return id
    }

    suspend fun closeTrade(tradeId: Int, exitPrice: Double, reason: String) {
        val openTrades = getOpenTradesDirect()
        val trade = openTrades.find { it.id == tradeId } ?: return

        // Calculate actual realized PnL
        // PnL = SideFactor * (Exit - Entry) * Qty * Leverage
        val sideFactor = if (trade.side == "LONG") 1.0 else -1.0
        val priceDiffPct = (exitPrice - trade.entryPrice) / trade.entryPrice
        val rawPnl = sideFactor * priceDiffPct * trade.entryPrice * trade.quantity * trade.leverage
        
        // Let's make it simpler and clear:
        // Value = Size * Price. Position Value in USDT = quantity * entryPrice.
        // Pnl = sideFactor * (exitPrice - entryPrice) * quantity * leverage? No, if quantity is in base asset (like count of BTC), position size in USDT is quantity * entryPrice.
        // Profit/loss in USDT is sideFactor * (exitPrice - entryPrice) * quantity.
        // Leverage is already accounted for in position size (we bought more quantity for the same margin).
        // So PnL = sideFactor * (exitPrice - entryPrice) * quantity.
        val realizedPnl = sideFactor * (exitPrice - trade.entryPrice) * trade.quantity

        val updatedTrade = trade.copy(
            exitPrice = exitPrice,
            status = reason,
            pnl = realizedPnl,
            exitTimestamp = System.currentTimeMillis()
        )
        
        botDao.updateTrade(updatedTrade)

        // Update active configuration balance
        val config = getConfigurationDirect()
        val newBalance = config.accountBalance + realizedPnl
        saveConfiguration(config.copy(accountBalance = newBalance))

        val statusArabic = when (reason) {
            "CLOSED_TP" -> "ضرب جني الأرباح (TP) ✅"
            "CLOSED_SL" -> "ضرب وقف الخسارة (SL) 🛑"
            else -> "إغلاق يدوي ⚠️"
        }

        insertLog(
            TradingLog(
                symbol = trade.symbol,
                level = "INFO",
                message = "إغلاق مركز ${trade.side}: $statusArabic بسعر $exitPrice. الأرباح/الخسائر الناتجة: %.2f USDT".format(realizedPnl)
            )
        )
    }

    suspend fun clearAllTrades() {
        botDao.clearAllTrades()
        insertLog(TradingLog(level = "WARNING", message = "تم تصفية سجل الصفقات بالكامل بنجاح."))
    }

    // Logs Flow
    val recentLogs: Flow<List<TradingLog>> = botDao.getRecentLogs()

    suspend fun insertLog(log: TradingLog) {
        botDao.insertLog(log)
    }

    suspend fun insertLog(level: String, message: String, symbol: String? = null) {
        botDao.insertLog(TradingLog(level = level, message = message, symbol = symbol))
    }

    suspend fun clearLogs() {
        botDao.clearLogs()
        insertLog(TradingLog(level = "INFO", message = "تم مسح سجل العمليات."))
    }
}
