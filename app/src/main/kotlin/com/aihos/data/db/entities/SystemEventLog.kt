package com.aihos.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * System Event Log Entity
 * Records all significant system events for audit trail
 */
@Entity(tableName = "system_events")
data class SystemEventLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "event_type")
    val eventType: String,
    
    @ColumnInfo(name = "event_name")
    val eventName: String,
    
    @ColumnInfo(name = "severity")
    val severity: String,  // "info", "warning", "error", "critical"
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String? = null,
    
    @ColumnInfo(name = "source_engine")
    val sourceEngine: String? = null,  // Which AI engine triggered this
    
    @ColumnInfo(name = "related_cycle_count")
    val relatedCycleCount: Long? = null
) {
    companion object {
        fun fromSystemEvent(
            eventType: String,
            eventName: String,
            severity: String,
            description: String? = null,
            metadataJson: String? = null,
            sourceEngine: String? = null,
            cycleCount: Long? = null
        ) = SystemEventLog(
            eventType = eventType,
            eventName = eventName,
            severity = severity,
            description = description,
            metadataJson = metadataJson,
            sourceEngine = sourceEngine,
            relatedCycleCount = cycleCount
        )
    }
}
