package com.aihos.data.db.daos

import androidx.room.*
import com.aihos.data.db.entities.UserInteractionEvent

/**
 * Data Access Object for User Interactions
 */
@Dao
interface UserInteractionDao {
    
    @Insert
    suspend fun insertInteraction(interaction: UserInteractionEvent): Long
    
    @Insert
    suspend fun insertInteractionsBatch(interactions: List<UserInteractionEvent>)
    
    @Query("""SELECT * FROM user_interactions 
        WHERE timestamp >= :startTime 
        ORDER BY timestamp DESC""")
    suspend fun getInteractionsAfter(startTime: Long): List<UserInteractionEvent>
    
    @Query("""SELECT * FROM user_interactions 
        WHERE screen_name = :screenName 
        ORDER BY timestamp DESC 
        LIMIT :limit""")
    suspend fun getScreenInteractions(screenName: String, limit: Int = 100): List<UserInteractionEvent>
    
    @Query("""SELECT COUNT(*) as total_interactions,
               COUNT(DISTINCT screen_name) as unique_screens,
               AVG(duration_ms) as avg_duration
        FROM user_interactions
        WHERE timestamp >= :startTime""")
    suspend fun getEngagementMetrics(startTime: Long = System.currentTimeMillis() - 3600000): EngagementMetrics?
    
    @Query("DELETE FROM user_interactions WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}

/**
 * Data class for engagement metrics
 */
data class EngagementMetrics(
    val total_interactions: Int,
    val unique_screens: Int,
    val avg_duration: Float
)
