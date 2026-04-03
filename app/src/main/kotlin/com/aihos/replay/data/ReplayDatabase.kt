package com.aihos.replay.data

import androidx.room.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ─────────────────────────────────────────────────────────────────────────────
// PART 2 — EVENT STORAGE (Room Database)
// Efficient storage for replay events with time indexing, pagination,
// and memory-bounded retention.
// ─────────────────────────────────────────────────────────────────────────────

private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

// ═════════════════════════════════════════════════════════════════════════════
// ROOM ENTITIES
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Primary replay event entity.
 * 
 * The cognitive and decision snapshots are stored as serialized JSON blobs
 * to avoid deep-join complexity while keeping fast time-indexed retrieval.
 */
@Entity(
    tableName = "replay_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["sessionId"]),
        Index(value = ["sequenceNumber"], unique = true),
        Index(value = ["eventType"]),
        Index(value = ["sessionId", "sequenceNumber"])     // Composite for replay
    ]
)
data class ReplayEventEntity(
    @PrimaryKey
    val id: String,

    val sequenceNumber: Long,
    val timestamp: Long,
    val sessionId: String,
    val eventType: String,

    // Serialized JSON blobs
    @ColumnInfo(name = "cognitive_snapshot_json")
    val cognitiveSnapshotJson: String,

    @ColumnInfo(name = "decision_snapshot_json")
    val decisionSnapshotJson: String?,      // null for non-decision events

    @ColumnInfo(name = "rule_updates_json")
    val ruleUpdatesJson: String,            // JSON array

    @ColumnInfo(name = "memory_changes_json")
    val memoryChangesJson: String,          // JSON array

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String,

    // Denormalized fields for fast queries without deserialization
    @ColumnInfo(name = "cognitive_load")
    val cognitiveLoad: Float,

    @ColumnInfo(name = "confidence_level")
    val confidenceLevel: Float,

    @ColumnInfo(name = "action_taken")
    val actionTaken: String?,

    @ColumnInfo(name = "estimated_size_bytes")
    val estimatedSizeBytes: Int
)

/**
 * Session metadata entity for grouping replay events.
 */
@Entity(
    tableName = "replay_sessions",
    indices = [Index(value = ["startTime"])]
)
data class ReplaySessionEntity(
    @PrimaryKey
    val sessionId: String,
    val startTime: Long,
    val endTime: Long?,
    val eventCount: Int,
    val totalSizeBytes: Long,
    val description: String?
)

/**
 * Timeline bookmark entity — marks significant events for quick navigation.
 */
@Entity(
    tableName = "replay_bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = ReplayEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["eventId"])]
)
data class ReplayBookmarkEntity(
    @PrimaryKey
    val id: String,
    val eventId: String,
    val label: String,
    val bookmarkType: String,       // "spike", "anomaly", "user_marked"
    val timestamp: Long,
    val description: String?
)


// ═════════════════════════════════════════════════════════════════════════════
// DATA ACCESS OBJECTS (DAOs)
// ═════════════════════════════════════════════════════════════════════════════

@Dao
interface ReplayEventDao {

    // ── Insert ────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ReplayEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(events: List<ReplayEventEntity>)

    // ── Query by sequence range (for step forward/backward) ──────────────
    @Query("""
        SELECT * FROM replay_events 
        WHERE sessionId = :sessionId 
          AND sequenceNumber BETWEEN :fromSeq AND :toSeq
        ORDER BY sequenceNumber ASC
    """)
    suspend fun getBySequenceRange(
        sessionId: String,
        fromSeq: Long,
        toSeq: Long
    ): List<ReplayEventEntity>

    // ── Query single event by sequence ───────────────────────────────────
    @Query("""
        SELECT * FROM replay_events 
        WHERE sessionId = :sessionId AND sequenceNumber = :seq
    """)
    suspend fun getBySequence(sessionId: String, seq: Long): ReplayEventEntity?

    // ── Query by time range ──────────────────────────────────────────────
    @Query("""
        SELECT * FROM replay_events 
        WHERE sessionId = :sessionId 
          AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY sequenceNumber ASC
    """)
    suspend fun getByTimeRange(
        sessionId: String,
        startTime: Long,
        endTime: Long
    ): List<ReplayEventEntity>

    // ── Jump to nearest event at timestamp ────────────────────────────────
    @Query("""
        SELECT * FROM replay_events 
        WHERE sessionId = :sessionId AND timestamp <= :timestamp
        ORDER BY timestamp DESC LIMIT 1
    """)
    suspend fun getNearestBefore(sessionId: String, timestamp: Long): ReplayEventEntity?

    // ── Get events by type (for timeline markers) ────────────────────────
    @Query("""
        SELECT * FROM replay_events 
        WHERE sessionId = :sessionId AND eventType = :type
        ORDER BY sequenceNumber ASC
    """)
    suspend fun getByType(sessionId: String, type: String): List<ReplayEventEntity>

    // ── Session boundaries ───────────────────────────────────────────────
    @Query("SELECT MIN(sequenceNumber) FROM replay_events WHERE sessionId = :sessionId")
    suspend fun getMinSequence(sessionId: String): Long?

    @Query("SELECT MAX(sequenceNumber) FROM replay_events WHERE sessionId = :sessionId")
    suspend fun getMaxSequence(sessionId: String): Long?

    @Query("SELECT MIN(timestamp) FROM replay_events WHERE sessionId = :sessionId")
    suspend fun getFirstTimestamp(sessionId: String): Long?

    @Query("SELECT MAX(timestamp) FROM replay_events WHERE sessionId = :sessionId")
    suspend fun getLastTimestamp(sessionId: String): Long?

    // ── Counts and statistics ────────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM replay_events WHERE sessionId = :sessionId")
    suspend fun getEventCount(sessionId: String): Int

    @Query("SELECT SUM(estimated_size_bytes) FROM replay_events WHERE sessionId = :sessionId")
    suspend fun getTotalSizeBytes(sessionId: String): Long?

    @Query("SELECT COUNT(*) FROM replay_events")
    suspend fun getTotalEventCount(): Int

    @Query("SELECT SUM(estimated_size_bytes) FROM replay_events")
    suspend fun getTotalStorageBytes(): Long?

    // ── Cognitive spikes (for timeline highlighting) ─────────────────────
    @Query("""
        SELECT * FROM replay_events 
        WHERE sessionId = :sessionId AND cognitive_load > :threshold
        ORDER BY sequenceNumber ASC
    """)
    suspend fun getCognitiveSpikes(sessionId: String, threshold: Float = 0.7f): List<ReplayEventEntity>

    // ── Pagination support ───────────────────────────────────────────────
    @Query("""
        SELECT * FROM replay_events 
        WHERE sessionId = :sessionId 
        ORDER BY sequenceNumber ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPage(sessionId: String, limit: Int, offset: Int): List<ReplayEventEntity>

    // ── Preload batch for buffer-ahead ────────────────────────────────────
    @Query("""
        SELECT * FROM replay_events 
        WHERE sessionId = :sessionId 
          AND sequenceNumber >= :startSeq
        ORDER BY sequenceNumber ASC 
        LIMIT :batchSize
    """)
    suspend fun preloadBatch(sessionId: String, startSeq: Long, batchSize: Int): List<ReplayEventEntity>

    // ── Cleanup ──────────────────────────────────────────────────────────
    @Query("DELETE FROM replay_events WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String): Int

    @Query("DELETE FROM replay_events WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long): Int

    @Query("""
        DELETE FROM replay_events 
        WHERE id IN (
            SELECT id FROM replay_events 
            ORDER BY timestamp ASC 
            LIMIT :count
        )
    """)
    suspend fun deleteOldest(count: Int): Int
}

@Dao
interface ReplaySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ReplaySessionEntity): Long

    @Update
    suspend fun update(session: ReplaySessionEntity)

    @Query("SELECT * FROM replay_sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<ReplaySessionEntity>

    @Query("SELECT * FROM replay_sessions WHERE sessionId = :sessionId")
    suspend fun getById(sessionId: String): ReplaySessionEntity?

    @Query("DELETE FROM replay_sessions WHERE sessionId = :sessionId")
    suspend fun delete(sessionId: String)
}

@Dao
interface ReplayBookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: ReplayBookmarkEntity): Long

    @Query("""
        SELECT b.* FROM replay_bookmarks b
        INNER JOIN replay_events e ON b.eventId = e.id
        WHERE e.sessionId = :sessionId
        ORDER BY b.timestamp ASC
    """)
    suspend fun getForSession(sessionId: String): List<ReplayBookmarkEntity>

    @Query("DELETE FROM replay_bookmarks WHERE id = :id")
    suspend fun delete(id: String)
}


// ═════════════════════════════════════════════════════════════════════════════
// TYPE CONVERTERS for Room
// ═════════════════════════════════════════════════════════════════════════════

class ReplayTypeConverters {
    @TypeConverter
    fun fromReplayEventType(value: String): String = value

    @TypeConverter
    fun toReplayEventType(value: String): String = value
}


// ═════════════════════════════════════════════════════════════════════════════
// DATABASE DEFINITION — extends existing SAIHOSDatabase
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Replay database — separate from the main AI database to avoid
 * migration conflicts and allow independent cleanup policies.
 */
@Database(
    entities = [
        ReplayEventEntity::class,
        ReplaySessionEntity::class,
        ReplayBookmarkEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(ReplayTypeConverters::class)
abstract class ReplayDatabase : RoomDatabase() {
    abstract fun replayEventDao(): ReplayEventDao
    abstract fun replaySessionDao(): ReplaySessionDao
    abstract fun replayBookmarkDao(): ReplayBookmarkDao
}
