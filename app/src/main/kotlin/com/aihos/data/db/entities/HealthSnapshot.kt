package com.aihos.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * System Health Snapshot Entity
 * Hourly snapshots of system health for trend analysis
 */
@Entity(tableName = "health_snapshots")
data class HealthSnapshot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "overall_health")
    val overallHealth: Float,
    
    @ColumnInfo(name = "memory_health")
    val memoryHealth: Float,
    
    @ColumnInfo(name = "reasoning_health")
    val reasoningHealth: Float,
    
    @ColumnInfo(name = "autonomy_health")
    val autonomyHealth: Float,
    
    @ColumnInfo(name = "evolution_health")
    val evolutionHealth: Float,
    
    @ColumnInfo(name = "uptime_ms")
    val uptimeMs: Long,
    
    @ColumnInfo(name = "cycle_count")
    val cycleCount: Long,
    
    @ColumnInfo(name = "memory_usage_mb")
    val memoryUsageMb: Int,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null
)
