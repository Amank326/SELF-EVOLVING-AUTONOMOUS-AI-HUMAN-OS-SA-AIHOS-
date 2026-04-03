package com.aihos.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User Interaction Event Entity
 * Tracks user interactions for engagement analytics
 */
@Entity(tableName = "user_interactions")
data class UserInteractionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "interaction_type")
    val interactionType: String,  // "tap", "swipe", "gesture", etc
    
    @ColumnInfo(name = "screen_name")
    val screenName: String,
    
    @ColumnInfo(name = "element_id")
    val elementId: String? = null,
    
    @ColumnInfo(name = "action")
    val action: String,
    
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L,
    
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String? = null
) {
    companion object {
        fun fromInteraction(
            interactionType: String,
            screenName: String,
            action: String,
            elementId: String? = null,
            durationMs: Long = 0L,
            metadataJson: String? = null
        ) = UserInteractionEvent(
            interactionType = interactionType,
            screenName = screenName,
            elementId = elementId,
            action = action,
            durationMs = durationMs,
            metadataJson = metadataJson
        )
    }
}
