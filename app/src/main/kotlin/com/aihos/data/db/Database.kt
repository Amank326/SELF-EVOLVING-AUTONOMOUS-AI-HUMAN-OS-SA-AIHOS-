package com.aihos.data.db

import androidx.room.*
import com.aihos.ai.memory.Episode
import com.aihos.ai.memory.BehavioralRule
import com.aihos.ai.memory.SemanticFact
import com.aihos.ai.memory.Outcome
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room Database Entities
 * These map to the SQLite schema
 */

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val decision: String,
    val action: String,
    val contextJson: String, // JSON serialized map
    val outcome: String,
    val reasoning: String,
    val reflection: String,
    val createdAt: Long
)

@Entity(tableName = "behavioral_rules")
data class BehavioralRuleEntity(
    @PrimaryKey val id: String,
    val condition: String,
    val action: String,
    val weight: Float,
    val successCount: Int,
    val failureCount: Int,
    val createdAt: Long,
    val evolvedAt: Long,
    val isActive: Boolean
)

@Entity(tableName = "semantic_facts")
data class SemanticFactEntity(
    @PrimaryKey val id: String,
    val fact: String,
    val confidence: Float,
    val sourcesJson: String, // JSON list of episode IDs
    val lastUpdated: Long
)

@Entity(
    tableName = "evolution_log",
    foreignKeys = [
        ForeignKey(
            entity = BehavioralRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EvolutionLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val ruleId: String,
    val changeType: String,
    val oldValue: String?,
    val newValue: String?,
    val reflection: String
)

@Entity(tableName = "autonomy_audit")
data class AutonomyAuditEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val decisionType: String,
    val actionTaken: String,
    val userApproved: Boolean,
    val outcome: String
)

/**
 * DAOs - Data Access Objects
 */

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(episode: EpisodeEntity): Long
    
    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: String): EpisodeEntity?
    
    @Query("SELECT * FROM episodes ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<EpisodeEntity>
    
    @Query("SELECT * FROM episodes WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun queryByTimeRange(startTime: Long, endTime: Long): List<EpisodeEntity>
    
    @Query("SELECT COUNT(*) FROM episodes")
    suspend fun count(): Int
    
    @Query("DELETE FROM episodes WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long): Int
}

@Dao
interface BehavioralRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: BehavioralRuleEntity): Long
    
    @Query("SELECT * FROM behavioral_rules WHERE id = :id")
    suspend fun getById(id: String): BehavioralRuleEntity?
    
    @Query("SELECT * FROM behavioral_rules")
    suspend fun getAll(): List<BehavioralRuleEntity>
    
    @Query("SELECT * FROM behavioral_rules WHERE isActive = 1")
    suspend fun getActive(): List<BehavioralRuleEntity>
    
    @Update
    suspend fun update(rule: BehavioralRuleEntity)
    
    @Query("COUNT(*) FROM behavioral_rules")
    suspend fun count(): Int
}

@Dao
interface SemanticFactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fact: SemanticFactEntity): Long
    
    @Query("SELECT * FROM semantic_facts WHERE fact LIKE '%' || :pattern || '%'")
    suspend fun queryByPattern(pattern: String): List<SemanticFactEntity>
    
    @Query("SELECT * FROM semantic_facts ORDER BY confidence DESC LIMIT :limit")
    suspend fun getHighConfidence(limit: Int): List<SemanticFactEntity>
}

@Dao
interface EvolutionLogDao {
    @Insert
    suspend fun insert(entry: EvolutionLogEntity): Long
    
    @Query("SELECT * FROM evolution_log WHERE ruleId = :ruleId ORDER BY timestamp DESC")
    suspend fun getHistoryForRule(ruleId: String): List<EvolutionLogEntity>
    
    @Query("SELECT * FROM evolution_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<EvolutionLogEntity>
}

@Dao
interface AutonomyAuditDao {
    @Insert
    suspend fun insert(audit: AutonomyAuditEntity): Long
    
    @Query("SELECT * FROM autonomy_audit ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<AutonomyAuditEntity>
}

/**
 * Main Database
 */
@Database(
    entities = [
        EpisodeEntity::class,
        BehavioralRuleEntity::class,
        SemanticFactEntity::class,
        EvolutionLogEntity::class,
        AutonomyAuditEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SAIHOSDatabase : RoomDatabase() {
    abstract fun episodeDao(): EpisodeDao
    abstract fun ruleDao(): BehavioralRuleDao
    abstract fun factDao(): SemanticFactDao
    abstract fun evolutionLogDao(): EvolutionLogDao
    abstract fun auditDao(): AutonomyAuditDao
}

/**
 * Converter for complex types
 */
class Converters {
    private val json = Json
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromOutcome(value: Outcome): String {
        return value.name
    }
    
    @TypeConverter
    fun toOutcome(value: String): Outcome {
        return Outcome.valueOf(value)
    }
}
