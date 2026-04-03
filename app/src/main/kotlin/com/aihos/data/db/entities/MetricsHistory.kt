package com.aihos.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metrics History Entity
 * Stores historical metric values for trend analysis
 */
@Entity(tableName = "metrics_history")
data class MetricsHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "memory_usage")
    val memoryUsage: Float,
    
    @ColumnInfo(name = "reasoning_confidence")
    val reasoningConfidence: Float,
    
    @ColumnInfo(name = "autonomy_level")
    val autonomyLevel: Float,
    
    @ColumnInfo(name = "evolution_progress")
    val evolutionProgress: Float,
    
    @ColumnInfo(name = "system_health")
    val systemHealth: Float,
    
    @ColumnInfo(name = "cycle_count")
    val cycleCount: Long
) {
    companion object {
        /**
         * Create from ViewModel metrics
         */
        fun fromMetrics(
            memoryUsage: Float,
            reasoningConfidence: Float,
            autonomyLevel: Float,
            evolutionProgress: Float,
            systemHealth: Float,
            cycleCount: Long
        ) = MetricsHistory(
            memoryUsage = memoryUsage,
            reasoningConfidence = reasoningConfidence,
            autonomyLevel = autonomyLevel,
            evolutionProgress = evolutionProgress,
            systemHealth = systemHealth,
            cycleCount = cycleCount
        )
    }
}
