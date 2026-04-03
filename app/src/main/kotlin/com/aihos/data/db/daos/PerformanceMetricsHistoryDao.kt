package com.aihos.data.db.daos

import androidx.room.*
import com.aihos.data.db.entities.PerformanceMetric

/**
 * Data Access Object for Performance Metrics History
 */
@Dao
interface PerformanceMetricsHistoryDao {
    
    @Insert
    suspend fun insertMetric(metric: PerformanceMetric): Long
    
    @Insert
    suspend fun insertMetricsBatch(metrics: List<PerformanceMetric>)
    
    @Query("""SELECT * FROM performance_history 
        WHERE metric_name = :metricName 
        ORDER BY timestamp DESC 
        LIMIT :limit""")
    suspend fun getMetricHistory(metricName: String, limit: Int = 100): List<PerformanceMetric>
    
    @Query("""SELECT AVG(metric_value) as avg_value,
               MIN(metric_value) as min_value,
               MAX(metric_value) as max_value
        FROM performance_history
        WHERE metric_name = :metricName AND timestamp >= :startTime""")
    suspend fun getMetricStatistics(
        metricName: String,
        startTime: Long = System.currentTimeMillis() - 3600000
    ): MetricStatistics?
    
    @Query("""SELECT * FROM performance_history 
        WHERE threshold_type IN ('warning', 'critical')
        ORDER BY timestamp DESC 
        LIMIT :limit""")
    suspend fun getAnomalies(limit: Int = 100): List<PerformanceMetric>
    
    @Query("DELETE FROM performance_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}

/**
 * Data class for metric statistics
 */
data class MetricStatistics(
    val avg_value: Float,
    val min_value: Float,
    val max_value: Float
)
