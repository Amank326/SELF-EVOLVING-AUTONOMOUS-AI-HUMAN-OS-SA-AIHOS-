package com.aihos.data.db.daos

import androidx.room.*
import com.aihos.data.db.entities.SystemEventLog
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for System Events
 */
@Dao
interface SystemEventLogDao {
    
    @Insert
    suspend fun insertEvent(event: SystemEventLog): Long
    
    @Insert
    suspend fun insertEventsBatch(events: List<SystemEventLog>)
    
    @Query("SELECT * FROM system_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEvent(): SystemEventLog?
    
    @Query("""SELECT * FROM system_events 
        WHERE timestamp >= :startTime 
        ORDER BY timestamp DESC""")
    fun getEventsAfter(startTime: Long): Flow<List<SystemEventLog>>
    
    @Query("""SELECT * FROM system_events 
        WHERE severity IN ('warning', 'error', 'critical')
        ORDER BY timestamp DESC 
        LIMIT :limit""")
    suspend fun getAlertEvents(limit: Int = 100): List<SystemEventLog>
    
    @Query("SELECT * FROM system_events WHERE event_type = :eventType ORDER BY timestamp DESC")
    suspend fun getEventsByType(eventType: String): List<SystemEventLog>
    
    @Query("""SELECT * FROM system_events 
        WHERE source_engine = :engine 
        ORDER BY timestamp DESC 
        LIMIT :limit""")
    suspend fun getEventsByEngine(engine: String, limit: Int = 100): List<SystemEventLog>
    
    @Query("""SELECT COUNT(*) 
        FROM system_events 
        WHERE severity = 'critical' AND timestamp >= :startTime""")
    suspend fun getCriticalEventCount(startTime: Long = System.currentTimeMillis() - 3600000): Int
    
    @Query("DELETE FROM system_events WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}
