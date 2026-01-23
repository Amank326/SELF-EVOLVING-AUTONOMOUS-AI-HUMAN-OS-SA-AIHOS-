package com.aihos.selfevolving.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room Database entities for offline storage
 */

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val timestamp: Long,
    val type: String,
    val importance: Float,
    val associatedMemories: String, // JSON array
    val retrievalCount: Int,
    val lastAccessed: Long
)

@Entity(tableName = "reasoning_contexts")
data class ReasoningContextEntity(
    @PrimaryKey val id: String,
    val query: String,
    val inferenceSteps: String, // JSON
    val conclusion: String?,
    val confidence: Float,
    val timestamp: Long
)

@Entity(tableName = "reflections")
data class ReflectionEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val performanceMetrics: String, // JSON
    val insights: String, // JSON array
    val improvements: String, // JSON array
    val behaviorAnalysis: String // JSON
)

@Entity(tableName = "evolution_states")
data class EvolutionStateEntity(
    @PrimaryKey val id: String,
    val version: Int,
    val timestamp: Long,
    val learningProgress: Float,
    val adaptations: String, // JSON array
    val capabilities: String // JSON array
)

@Entity(tableName = "autonomous_tasks")
data class AutonomousTaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val priority: String,
    val status: String,
    val createdAt: Long,
    val scheduledAt: Long?,
    val completedAt: Long?,
    val dependencies: String, // JSON array
    val outcome: String? // JSON
)

@Entity(tableName = "ai_state")
data class AiStateEntity(
    @PrimaryKey val id: String = "current_state",
    val isActive: Boolean,
    val currentMode: String,
    val memoryUtilization: Float,
    val processingLoad: Float,
    val evolutionStage: Int,
    val autonomyLevel: Float,
    val lastUpdated: Long
)

/**
 * DAOs (Data Access Objects)
 */

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity)
    
    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getById(id: String): MemoryEntity?
    
    @Query("SELECT * FROM memories WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: String): Flow<List<MemoryEntity>>
    
    @Query("SELECT * FROM memories ORDER BY importance DESC, timestamp DESC LIMIT :limit")
    fun getMostImportant(limit: Int): Flow<List<MemoryEntity>>
    
    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' ORDER BY importance DESC LIMIT :limit")
    fun searchMemories(query: String, limit: Int): Flow<List<MemoryEntity>>
    
    @Query("UPDATE memories SET importance = :importance WHERE id = :id")
    suspend fun updateImportance(id: String, importance: Float)
    
    @Query("UPDATE memories SET retrievalCount = retrievalCount + 1, lastAccessed = :timestamp WHERE id = :id")
    suspend fun incrementRetrievalCount(id: String, timestamp: Long)
    
    @Query("DELETE FROM memories WHERE importance < :threshold AND timestamp < :oldTimestamp")
    suspend fun pruneOldMemories(threshold: Float, oldTimestamp: Long)
    
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>
}

@Dao
interface ReasoningDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(context: ReasoningContextEntity)
    
    @Query("SELECT * FROM reasoning_contexts WHERE id = :id")
    suspend fun getById(id: String): ReasoningContextEntity?
    
    @Query("SELECT * FROM reasoning_contexts ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ReasoningContextEntity>>
    
    @Query("UPDATE reasoning_contexts SET inferenceSteps = :steps WHERE id = :id")
    suspend fun updateInferenceSteps(id: String, steps: String)
    
    @Query("UPDATE reasoning_contexts SET conclusion = :conclusion, confidence = :confidence WHERE id = :id")
    suspend fun updateConclusion(id: String, conclusion: String, confidence: Float)
}

@Dao
interface ReflectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reflection: ReflectionEntity)
    
    @Query("SELECT * FROM reflections ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ReflectionEntity>>
    
    @Query("SELECT * FROM reflections WHERE timestamp BETWEEN :start AND :end")
    suspend fun getInTimeRange(start: Long, end: Long): List<ReflectionEntity>
}

@Dao
interface EvolutionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: EvolutionStateEntity)
    
    @Query("SELECT * FROM evolution_states ORDER BY version DESC LIMIT 1")
    suspend fun getCurrent(): EvolutionStateEntity?
    
    @Query("SELECT * FROM evolution_states ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<EvolutionStateEntity>>
}

@Dao
interface AutonomyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: AutonomousTaskEntity)
    
    @Query("SELECT * FROM autonomous_tasks WHERE status IN ('PENDING', 'SCHEDULED') ORDER BY priority DESC, createdAt ASC")
    fun getPendingTasks(): Flow<List<AutonomousTaskEntity>>
    
    @Query("SELECT * FROM autonomous_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<AutonomousTaskEntity>>
    
    @Query("UPDATE autonomous_tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
    
    @Query("UPDATE autonomous_tasks SET status = :status, completedAt = :completedAt, outcome = :outcome WHERE id = :id")
    suspend fun completeTask(id: String, status: String, completedAt: Long, outcome: String)
}

@Dao
interface AiStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: AiStateEntity)
    
    @Query("SELECT * FROM ai_state WHERE id = 'current_state'")
    fun getState(): Flow<AiStateEntity?>
    
    @Query("UPDATE ai_state SET currentMode = :mode, lastUpdated = :timestamp WHERE id = 'current_state'")
    suspend fun updateMode(mode: String, timestamp: Long)
}

/**
 * Room Database
 */

@Database(
    entities = [
        MemoryEntity::class,
        ReasoningContextEntity::class,
        ReflectionEntity::class,
        EvolutionStateEntity::class,
        AutonomousTaskEntity::class,
        AiStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AiHosDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun reasoningDao(): ReasoningDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun evolutionDao(): EvolutionDao
    abstract fun autonomyDao(): AutonomyDao
    abstract fun aiStateDao(): AiStateDao
}
