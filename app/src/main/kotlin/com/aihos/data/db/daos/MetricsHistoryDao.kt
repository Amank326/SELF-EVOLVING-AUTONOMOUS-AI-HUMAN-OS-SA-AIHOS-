package com.aihos.data.db.daos

import androidx.room.*
import com.aihos.data.db.entities.MetricsHistory
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Metrics History
 * Provides database operations for historical metrics
 */
@Dao
interface MetricsHistoryDao {
    
    @Insert
    suspend fun insertMetrics(metrics: MetricsHistory): Long
    
    @Insert
    suspend fun insertMetricsBatch(metrics: List<MetricsHistory>)
    
    @Query("SELECT * FROM metrics_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMetrics(): MetricsHistory?
    
    @Query("SELECT * FROM metrics_history WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getMetricsAfter(startTime: Long): List<MetricsHistory>
    
    @Query("SELECT * FROM metrics_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp")
    fun getMetricsRange(startTime: Long, endTime: Long): Flow<List<MetricsHistory>>
    
    @Query("""SELECT * FROM metrics_history 
        WHERE timestamp >= :startTime 
        ORDER BY timestamp DESC 
        LIMIT :limit""")
    suspend fun getRecentMetrics(limit: Int, startTime: Long = System.currentTimeMillis() - 86400000): List<MetricsHistory>
    
    @Query("""SELECT AVG(memory_usage) as avg_memory,
               AVG(reasoning_confidence) as avg_reasoning,
               AVG(autonomy_level) as avg_autonomy,
               AVG(evolution_progress) as avg_evolution,
               AVG(system_health) as avg_health
        FROM metrics_history
        WHERE timestamp >= :startTime""")
    suspend fun getAverageMetrics(startTime: Long = System.currentTimeMillis() - 3600000): MetricsAverage?
    
    @Query("SELECT COUNT(*) FROM metrics_history")
    suspend fun getTotalCount(): Int
    
    @Query("DELETE FROM metrics_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
    
    @Query("DELETE FROM metrics_history")
    suspend fun deleteAll()
}

/**
 * Data class for average metrics query results
 */
data class MetricsAverage(
    val avg_memory: Float,
    val avg_reasoning: Float,
    val avg_autonomy: Float,
    val avg_evolution: Float,
    val avg_health: Float
)
