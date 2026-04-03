package com.aihos.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Performance Metrics History Entity
 * Stores app-level performance data history for analytics
 */
@Entity(tableName = "performance_history")
data class PerformanceMetric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "metric_name")
    val metricName: String,  // "fps", "memory", "cpu", "latency"
    
    @ColumnInfo(name = "metric_value")
    val metricValue: Float,
    
    @ColumnInfo(name = "unit")
    val unit: String,  // "fps", "mb", "%", "ms"
    
    @ColumnInfo(name = "threshold_type")
    val thresholdType: String? = null,  // "normal", "warning", "critical"
    
    @ColumnInfo(name = "screen_name")
    val screenName: String? = null,
    
    @ColumnInfo(name = "additional_data_json")
    val additionalDataJson: String? = null
)
