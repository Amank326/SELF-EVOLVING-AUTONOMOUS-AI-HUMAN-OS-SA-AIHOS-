package com.aihos.data.db.daos

import androidx.room.*
import com.aihos.data.db.entities.HealthSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Health Snapshots
 */
@Dao
interface HealthSnapshotDao {
    
    @Insert
    suspend fun insertSnapshot(snapshot: HealthSnapshot): Long
    
    @Query("SELECT * FROM health_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(): HealthSnapshot?
    
    @Query("""SELECT * FROM health_snapshots 
        WHERE timestamp >= :startTime 
        ORDER BY timestamp DESC""")
    fun getSnapshotsAfter(startTime: Long): Flow<List<HealthSnapshot>>
    
    @Query("""SELECT AVG(overall_health) as avg_health,
               MIN(overall_health) as min_health,
               MAX(overall_health) as max_health
        FROM health_snapshots
        WHERE timestamp >= :startTime""")
    suspend fun getHealthTrend(startTime: Long = System.currentTimeMillis() - 86400000): HealthTrend?
    
    @Query("DELETE FROM health_snapshots WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}

/**
 * Data class for health trend analysis
 */
data class HealthTrend(
    val avg_health: Float,
    val min_health: Float,
    val max_health: Float
)
