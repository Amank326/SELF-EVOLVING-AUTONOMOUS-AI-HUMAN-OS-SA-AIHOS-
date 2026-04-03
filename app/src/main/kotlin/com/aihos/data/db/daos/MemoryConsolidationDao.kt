package com.aihos.data.db.daos

import androidx.room.*
import com.aihos.data.db.entities.MemoryConsolidationEvent

/**
 * Data Access Object for Memory Consolidation Events
 */
@Dao
interface MemoryConsolidationDao {
    
    @Insert
    suspend fun insertConsolidation(consolidation: MemoryConsolidationEvent): Long
    
    @Update
    suspend fun updateConsolidation(consolidation: MemoryConsolidationEvent)
    
    @Query("SELECT * FROM memory_consolidations ORDER BY start_time DESC LIMIT 1")
    suspend fun getLatestConsolidation(): MemoryConsolidationEvent?
    
    @Query("""SELECT * FROM memory_consolidations 
        WHERE start_time >= :startTime 
        ORDER BY start_time DESC""")
    suspend fun getConsolidationsAfter(startTime: Long): List<MemoryConsolidationEvent>
    
    @Query("""SELECT * FROM memory_consolidations 
        WHERE success = 1 
        ORDER BY start_time DESC 
        LIMIT :limit""")
    suspend fun getSuccessfulConsolidations(limit: Int = 100): List<MemoryConsolidationEvent>
    
    @Query("""SELECT * FROM memory_consolidations 
        WHERE success = 0 
        ORDER BY start_time DESC 
        LIMIT :limit""")
    suspend fun getFailedConsolidations(limit: Int = 50): List<MemoryConsolidationEvent>
    
    @Query("""SELECT AVG((initial_capacity - final_capacity) / initial_capacity) as avg_efficiency,
               COUNT(*) as total_count,
               SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as success_count
        FROM memory_consolidations
        WHERE start_time >= :startTime AND initial_capacity > 0""")
    suspend fun getConsolidationStatistics(startTime: Long = System.currentTimeMillis() - 86400000): ConsolidationStats?
    
    @Query("DELETE FROM memory_consolidations WHERE start_time < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}

/**
 * Data class for consolidation statistics
 */
data class ConsolidationStats(
    val avg_efficiency: Float,
    val total_count: Int,
    val success_count: Int
)
