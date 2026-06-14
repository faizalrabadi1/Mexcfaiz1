package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bot_configurations")
data class BotConfiguration(
    @PrimaryKey val id: Int = 1,
    val apiKey: String = "",
    val apiSecret: String = "",
    val leverage: Int = 10,
    val marginMode: String = "ISOLATED", // ISOLATED or CROSS
    val positionSizePercent: Double = 5.0,
    val rsiPeriod: Int = 14,
    val rsiOverbought: Double = 70.0,
    val rsiOversold: Double = 30.0,
    val emaShortPeriod: Int = 9,
    val emaLongPeriod: Int = 21,
    val timeframe: String = "1m",
    val isActive: Boolean = false,
    val symbolsCsv: String = "BTC/USDT:USDT,ETH/USDT:USDT,SOL/USDT:USDT,XRP/USDT:USDT",
    val accountBalance: Double = 10000.0 // Starting simulation balance
)

@Entity(tableName = "simulated_trades")
data class SimulatedTrade(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val side: String, // "LONG" or "SHORT"
    val entryPrice: Double,
    val exitPrice: Double? = null,
    val quantity: Double,
    val leverage: Int,
    val takeProfitPrice: Double,
    val stopLossPrice: Double,
    val pnl: Double = 0.0,
    val status: String, // "OPEN", "CLOSED_TP", "CLOSED_SL", "CLOSED_MANUAL"
    val timestamp: Long = System.currentTimeMillis(),
    val exitTimestamp: Long? = null
)

@Entity(tableName = "trading_logs")
data class TradingLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val symbol: String? = null,
    val level: String, // "INFO", "WARNING", "TRIGGER", "ERROR"
    val message: String
)
