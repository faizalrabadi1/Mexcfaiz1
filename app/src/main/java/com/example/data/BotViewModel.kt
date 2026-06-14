package com.example.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

data class TickerState(
    val symbol: String,
    val currentPrice: Double,
    val priceChange24h: Double,
    val rsi: Double?,
    val emaShort: Double?,
    val emaLong: Double?,
    val signal: String, // "BUY LONG", "SELL SHORT", "NONE"
    val priceHistory: List<Double>
)

class BotViewModel(private val repository: BotRepository) : ViewModel() {

    // Expose flows from Repository
    val configuration: StateFlow<BotConfiguration> = repository.configuration
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BotConfiguration()
        )

    val allTrades: StateFlow<List<SimulatedTrade>> = repository.allTrades
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val openTrades: StateFlow<List<SimulatedTrade>> = repository.openTrades
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentLogs: StateFlow<List<TradingLog>> = repository.recentLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Tickers State flow for active visualization
    private val _tickers = MutableStateFlow<List<TickerState>>(emptyList())
    val tickers: StateFlow<List<TickerState>> = _tickers.asStateFlow()

    // Simulation parameters
    private var volatilityFactor = 1.0 // Multiplier for price action
    private val basePrices = mapOf(
        "BTC/USDT:USDT" to 67500.0,
        "ETH/USDT:USDT" to 3450.0,
        "SOL/USDT:USDT" to 148.0,
        "XRP/USDT:USDT" to 0.49,
        "ADA/USDT:USDT" to 0.38,
        "DOGE/USDT:USDT" to 0.125
    )

    init {
        // Initialize ticker histories with pre-computed walk data so indicators work instantly
        val initialTickers = basePrices.map { (symbol, basePrice) ->
            val history = mutableListOf<Double>()
            var prevPrice = basePrice * 0.95 // Start slightly lower and drift up
            repeat(40) {
                // Add tiny walk with random trend
                val change = (Random.nextDouble() - 0.48) * 0.003 * prevPrice
                val nextPrice = prevPrice + change
                history.add(nextPrice)
                prevPrice = nextPrice
            }
            
            // Calculate indicators
            val rsi = calculateRsi(history)
            val config = BotConfiguration() // temporary defaults
            val emaShort = calculateEma(history, config.emaShortPeriod)
            val emaLong = calculateEma(history, config.emaLongPeriod)
            val signal = determineSignal(rsi, emaShort, emaLong, config)

            TickerState(
                symbol = symbol,
                currentPrice = history.last(),
                priceChange24h = ((history.last() - history.first()) / history.first()) * 100.0,
                rsi = rsi,
                emaShort = emaShort,
                emaLong = emaLong,
                signal = signal,
                priceHistory = history
            )
        }
        _tickers.value = initialTickers

        // Start background simulator
        startSimulationLoop()
    }

    private fun startSimulationLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1500) // Update simulation every 1.5 seconds for snappy updates
                val config = repository.getConfigurationDirect()
                val currentTickers = _tickers.value.toMutableList()
                val openTradesList = repository.getOpenTradesDirect()

                for (i in currentTickers.indices) {
                    val ticker = currentTickers[i]
                    val history = ticker.priceHistory.toMutableList()
                    
                    // Generate new tick price
                    val rand = Random.nextDouble()
                    // Random noise + tiny random walk bias
                    val changePct = (rand - 0.5) * 0.002 * volatilityFactor
                    val lastPrice = ticker.currentPrice
                    val newPrice = lastPrice * (1.0 + changePct)

                    history.removeAt(0)
                    history.add(newPrice)

                    // Recompute indicators
                    val rsi = calculateRsi(history, config.rsiPeriod)
                    val emaShort = calculateEma(history, config.emaShortPeriod)
                    val emaLong = calculateEma(history, config.emaLongPeriod)
                    val signal = determineSignal(rsi, emaShort, emaLong, config)

                    // Update ticker instance in list
                    val firstHistoricalClose = history.first()
                    currentTickers[i] = ticker.copy(
                        currentPrice = newPrice,
                        priceChange24h = ((newPrice - firstHistoricalClose) / firstHistoricalClose) * 100.0,
                        rsi = rsi,
                        emaShort = emaShort,
                        emaLong = emaLong,
                        signal = signal,
                        priceHistory = history
                    )

                    // Check if simulation is running
                    if (config.isActive) {
                        // 1. Process SL/TP for open positions on this coin
                        val coinTrade = openTradesList.find { it.symbol == ticker.symbol }
                        if (coinTrade != null) {
                            val sideFactor = if (coinTrade.side == "LONG") 1.0 else -1.0
                            val currentProfitPct = sideFactor * ((newPrice - coinTrade.entryPrice) / coinTrade.entryPrice) * 100.0 * coinTrade.leverage

                            if (coinTrade.side == "LONG") {
                                if (newPrice >= coinTrade.takeProfitPrice) {
                                    repository.closeTrade(coinTrade.id, coinTrade.takeProfitPrice, "UUID_TP_CLOSED_OK")
                                    repository.closeTrade(coinTrade.id, coinTrade.takeProfitPrice, "CLOSED_TP")
                                } else if (newPrice <= coinTrade.stopLossPrice) {
                                    repository.closeTrade(coinTrade.id, coinTrade.stopLossPrice, "CLOSED_SL")
                                } else if (emaShort != null && emaLong != null && emaShort < emaLong) {
                                    // Tech exit: counter cross
                                    repository.closeTrade(coinTrade.id, newPrice, "CLOSED_MANUAL")
                                }
                            } else { // SHORT
                                if (newPrice <= coinTrade.takeProfitPrice) {
                                    repository.closeTrade(coinTrade.id, coinTrade.takeProfitPrice, "CLOSED_TP")
                                } else if (newPrice >= coinTrade.stopLossPrice) {
                                    repository.closeTrade(coinTrade.id, coinTrade.stopLossPrice, "CLOSED_SL")
                                } else if (emaShort != null && emaLong != null && emaShort > emaLong) {
                                    // Tech exit: counter cross
                                    repository.closeTrade(coinTrade.id, newPrice, "CLOSED_MANUAL")
                                }
                            }
                        } else {
                            // 2. Try to trigger a new trade!
                            // Only open a trade if this symbol is active in config (Csv string)
                            val isActiveSymbol = config.symbolsCsv.split(",").map { it.trim() }.contains(ticker.symbol)
                            if (isActiveSymbol) {
                                if (signal == "BUY LONG") {
                                    // Open LONG Position
                                    val allocatedMargin = config.accountBalance * (config.positionSizePercent / 100).coerceIn(0.01, 100.0)
                                    val positionValUsdt = allocatedMargin * config.leverage
                                    val qty = positionValUsdt / newPrice
                                    
                                    repository.openTrade(
                                        symbol = ticker.symbol,
                                        side = "LONG",
                                        entryPrice = newPrice,
                                        quantity = qty,
                                        leverage = config.leverage,
                                        takeProfitPct = 1.5, // Standard fast target
                                        stopLossPct = 0.8    // Tight protective wall
                                    )
                                } else if (signal == "SELL SHORT") {
                                    // Open SHORT Position
                                    val allocatedMargin = config.accountBalance * (config.positionSizePercent / 100).coerceIn(0.01, 100.0)
                                    val positionValUsdt = allocatedMargin * config.leverage
                                    val qty = positionValUsdt / newPrice
                                    
                                    repository.openTrade(
                                        symbol = ticker.symbol,
                                        side = "SHORT",
                                        entryPrice = newPrice,
                                        quantity = qty,
                                        leverage = config.leverage,
                                        takeProfitPct = 1.5,
                                        stopLossPct = 0.8
                                    )
                                }
                            }
                        }
                    }
                }
                
                _tickers.value = currentTickers
            }
        }
    }

    private fun determineSignal(rsi: Double?, emaShort: Double?, emaLong: Double?, config: BotConfiguration): String {
        if (rsi == null || emaShort == null || emaLong == null) return "NONE"

        // Gold cross + RSI oversold -> BUY LONG
        if (rsi < config.rsiOversold && emaShort > emaLong) {
            return "BUY LONG"
        }
        // Death cross + RSI overbought -> SELL SHORT
        if (rsi > config.rsiOverbought && emaShort < emaLong) {
            return "SELL SHORT"
        }
        return "NONE"
    }

    // Interactive Trigger spikes for testing actions
    fun triggerMarketSpike(symbol: String, isUpward: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            val currentList = _tickers.value.toMutableList()
            val index = currentList.indexOfFirst { it.symbol == symbol }
            if (index == -1) return@launch
            
            val ticker = currentList[index]
            val spikeFactor = if (isUpward) 1.05 else 0.95 // 5% massive spike
            val spikedPrice = ticker.currentPrice * spikeFactor
            
            val newHistory = ticker.priceHistory.toMutableList()
            // Shift history by replacing last few ticks to simulate a swift, steep shift
            newHistory.removeAt(newHistory.size - 1)
            newHistory.add(spikedPrice)

            // Re-calculate
            val config = repository.getConfigurationDirect()
            val rsi = calculateRsi(newHistory, config.rsiPeriod)
            val emaShort = calculateEma(newHistory, config.emaShortPeriod)
            val emaLong = calculateEma(newHistory, config.emaLongPeriod)
            val signal = determineSignal(rsi, emaShort, emaLong, config)

            currentList[index] = ticker.copy(
                currentPrice = spikedPrice,
                rsi = rsi,
                emaShort = emaShort,
                emaLong = emaLong,
                signal = signal,
                priceHistory = newHistory
            )
            _tickers.value = currentList

            repository.insertLog(
                level = if (isUpward) "INFO" else "WARNING",
                message = "محاكاة هزة سعرية خاطفة (${if (isUpward) "صعود" else "هبوط"}) على ${symbol}. السعر الجديد: $spikedPrice",
                symbol = symbol
            )
        }
    }

    // Configuration Commands
    fun toggleBotActive() {
        viewModelScope.launch {
            val config = repository.getConfigurationDirect()
            val nextState = !config.isActive
            repository.saveConfiguration(config.copy(isActive = nextState))
            repository.insertLog(
                level = "INFO",
                message = if (nextState) "تم بدء تشغيل روبوت السكالبينغ بنجاح 🟢 يراقب الآن ${config.symbolsCsv}" 
                          else "تم إيقاف روبوت السكالبينغ مؤقتاً 🔴"
            )
        }
    }

    fun updateConfig(
        leverage: Int,
        marginMode: String,
        positionSizePercent: Double,
        rsiOverbought: Double,
        rsiOversold: Double,
        emaShortPeriod: Int,
        emaLongPeriod: Int,
        symbols: List<String>
    ) {
        viewModelScope.launch {
            val config = repository.getConfigurationDirect()
            val updated = config.copy(
                leverage = leverage,
                marginMode = marginMode,
                positionSizePercent = positionSizePercent,
                rsiOverbought = rsiOverbought,
                rsiOversold = rsiOversold,
                emaShortPeriod = emaShortPeriod,
                emaLongPeriod = emaLongPeriod,
                symbolsCsv = symbols.joinToString(",")
            )
            repository.saveConfiguration(updated)
            repository.insertLog("INFO", "تم تعديل بارامترات التداول والرافعة المالية إلى ${leverage}x ومزامنتها بنجاح.")
        }
    }

    fun updateKeys(apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            val config = repository.getConfigurationDirect()
            repository.saveConfiguration(config.copy(apiKey = apiKey, apiSecret = apiSecret))
            repository.insertLog("INFO", "تم حفظ مفاتيح منصة MEXC بشكل آمن محلياً.")
        }
    }

    fun manualOpenPosition(symbol: String, side: String) {
        viewModelScope.launch {
            val config = repository.getConfigurationDirect()
            val ticker = _tickers.value.find { it.symbol == symbol } ?: return@launch
            
            // Check if position already open
            val openTrades = repository.getOpenTradesForSymbol(symbol)
            if (openTrades.isNotEmpty()) {
                repository.insertLog("WARNING", "فشل فتح مركز يدوي: يوجد مركز مفتوح بالفعل لـ $symbol", symbol)
                return@launch
            }

            val allocatedMargin = config.accountBalance * (config.positionSizePercent / 100).coerceIn(0.01, 100.0)
            val positionValUsdt = allocatedMargin * config.leverage
            val qty = positionValUsdt / ticker.currentPrice

            repository.openTrade(
                symbol = symbol,
                side = side,
                entryPrice = ticker.currentPrice,
                quantity = qty,
                leverage = config.leverage,
                takeProfitPct = 1.5,
                stopLossPct = 0.8
            )
        }
    }

    fun manualClosePosition(tradeId: Int, currentPrice: Double) {
        viewModelScope.launch {
            repository.closeTrade(tradeId, currentPrice, "CLOSED_MANUAL")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllTrades()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    private fun calculateEma(prices: List<Double>, period: Int): Double? {
        if (prices.size < period) return null
        val alpha = 2.0 / (period + 1.0)
        var ema = prices[0]
        for (i in 1 until prices.size) {
            ema = prices[i] * alpha + ema * (1.0 - alpha)
        }
        return ema
    }

    private fun calculateRsi(prices: List<Double>, period: Int = 14): Double? {
        if (prices.size < period + 1) return null
        val deltas = mutableListOf<Double>()
        for (i in 1 until prices.size) {
            deltas.add(prices[i] - prices[i - 1])
        }

        var avgGain = 0.0
        var avgLoss = 0.0

        for (i in 0 until period) {
            val delta = deltas[i]
            if (delta > 0) {
                avgGain += delta
            } else {
                avgLoss += -delta
            }
        }
        avgGain /= period
        avgLoss /= period

        for (i in period until deltas.size) {
            val delta = deltas[i]
            val gain = if (delta > 0) delta else 0.0
            val loss = if (delta < 0) -delta else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) {
            return if (avgGain > 0.0) 100.0 else 50.0
        }
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }
}
