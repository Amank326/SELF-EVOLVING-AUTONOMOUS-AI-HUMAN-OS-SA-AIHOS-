package com.aihos.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Memory Consolidation Event Entity
 * Tracks memory consolidation cycles and their outcomes
 */
@Entity(tableName = "memory_consolidations")
data class MemoryConsolidationEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,
    
    @ColumnInfo(name = "initial_capacity")
    val initialCapacity: Float,
    
    @ColumnInfo(name = "final_capacity")
    val finalCapacity: Float? = null,
    
    @ColumnInfo(name = "consolidation_type")
    val consolidationType: String,  // "semantic", "episodic", "procedural"
    
    @ColumnInfo(name = "success")
    val success: Boolean = false,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    
    @ColumnInfo(name = "items_consolidated")
    val itemsConsolidated: Int = 0,
    
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L
) {
    val durationSeconds: Double
        get() = durationMs / 1000.0
    
    val efficiency: Float
        get() = if (finalCapacity != null && initialCapacity > 0) {
            (initialCapacity - finalCapacity!!) / initialCapacity
        } else {
            0f
        }
}
