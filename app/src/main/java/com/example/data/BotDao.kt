package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BotDao {
    // Configuration
    @Query("SELECT * FROM bot_configurations WHERE id = 1 LIMIT 1")
    fun getConfiguration(): Flow<BotConfiguration?>

    @Query("SELECT * FROM bot_configurations WHERE id = 1 LIMIT 1")
    suspend fun getConfigurationDirect(): BotConfiguration?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfiguration(config: BotConfiguration)

    // Trades
    @Query("SELECT * FROM simulated_trades ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<SimulatedTrade>>

    @Query("SELECT * FROM simulated_trades WHERE status = 'OPEN'")
    fun getOpenTradesFlow(): Flow<List<SimulatedTrade>>

    @Query("SELECT * FROM simulated_trades WHERE status = 'OPEN'")
    suspend fun getOpenTradesDirect(): List<SimulatedTrade>

    @Query("SELECT * FROM simulated_trades WHERE symbol = :symbol AND status = 'OPEN'")
    suspend fun getOpenTradesForSymbol(symbol: String): List<SimulatedTrade>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: SimulatedTrade): Long

    @Update
    suspend fun updateTrade(trade: SimulatedTrade)

    @Query("DELETE FROM simulated_trades")
    suspend fun clearAllTrades()

    // Logs
    @Query("SELECT * FROM trading_logs ORDER BY timestamp DESC LIMIT 150")
    fun getRecentLogs(): Flow<List<TradingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TradingLog)

    @Query("DELETE FROM trading_logs")
    suspend fun clearLogs()
}
