package com.aihos.data.db.daos

import androidx.room.*
import com.aihos.data.db.entity.*

/**
 * Data Access Object for Memory entities
 * Provides database operations for the memory layer
 */
@Dao
interface MemoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long
    
    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: String): MemoryEntity?
    
    @Query("SELECT * FROM memories WHERE type = :type ORDER BY importance DESC, accessCount DESC")
    suspend fun getMemoriesByType(type: String): List<MemoryEntity>
    
    @Query("SELECT * FROM memories ORDER BY importance DESC LIMIT :limit")
    suspend fun getTopMemoriesByImportance(limit: Int): List<MemoryEntity>
    
    @Query("SELECT * FROM memories ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int): List<MemoryEntity>
    
    @Query("SELECT * FROM memories ORDER BY accessCount DESC LIMIT :limit")
    suspend fun getMostAccessedMemories(limit: Int): List<MemoryEntity>
    
    @Update
    suspend fun updateMemory(memory: MemoryEntity)
    
    @Query("UPDATE memories SET accessCount = accessCount + 1 WHERE id = :id")
    suspend fun incrementAccessCount(id: String)
    
    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)
    
    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)
    
    @Query("DELETE FROM memories")
    suspend fun deleteAllMemories()
    
    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getMemoryCount(): Int
    
    @Query("SELECT SUM(LENGTH(content)) FROM memories")
    suspend fun getTotalMemorySize(): Long
}

/**
 * Data Access Object for Reasoning Rule entities
 * Provides database operations for learned reasoning rules
 */
@Dao
interface ReasoningRuleDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ReasoningRuleEntity): Long
    
    @Query("SELECT * FROM reasoning_rules WHERE id = :id")
    suspend fun getRuleById(id: String): ReasoningRuleEntity?
    
    @Query("SELECT * FROM reasoning_rules WHERE isActive = 1")
    suspend fun getActiveRules(): List<ReasoningRuleEntity>
    
    @Query("SELECT * FROM reasoning_rules")
    suspend fun getAllRules(): List<ReasoningRuleEntity>
    
    @Query("SELECT * FROM reasoning_rules ORDER BY weight DESC LIMIT :limit")
    suspend fun getTopRulesByWeight(limit: Int): List<ReasoningRuleEntity>
    
    @Query("SELECT * FROM reasoning_rules ORDER BY (CAST(successCount AS REAL) / (successCount + failureCount + 1)) DESC LIMIT :limit")
    suspend fun getTopRulesBySuccessRate(limit: Int): List<ReasoningRuleEntity>
    
    @Update
    suspend fun updateRule(rule: ReasoningRuleEntity)
    
    @Query("UPDATE reasoning_rules SET successCount = successCount + 1 WHERE id = :id")
    suspend fun incrementSuccess(id: String)
    
    @Query("UPDATE reasoning_rules SET failureCount = failureCount + 1 WHERE id = :id")
    suspend fun incrementFailure(id: String)
    
    @Delete
    suspend fun deleteRule(rule: ReasoningRuleEntity)
    
    @Query("DELETE FROM reasoning_rules WHERE id = :id")
    suspend fun deleteRuleById(id: String)
}

/**
 * Data Access Object for Insight entities
 * Provides database operations for discovered insights
 */
@Dao
interface InsightDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: InsightEntity): Long
    
    @Query("SELECT * FROM insights WHERE id = :id")
    suspend fun getInsightById(id: String): InsightEntity?
    
    @Query("SELECT * FROM insights WHERE type = :type")
    suspend fun getInsightsByType(type: String): List<InsightEntity>
    
    @Query("SELECT * FROM insights WHERE implementedAt IS NULL ORDER BY confidence DESC")
    suspend fun getUnimplementedInsights(): List<InsightEntity>
    
    @Query("SELECT * FROM insights ORDER BY discoveredAt DESC LIMIT :limit")
    suspend fun getRecentInsights(limit: Int): List<InsightEntity>
    
    @Update
    suspend fun updateInsight(insight: InsightEntity)
    
    @Delete
    suspend fun deleteInsight(insight: InsightEntity)
}

/**
 * Data Access Object for Evolution Log entities
 * Provides database operations for tracking system evolution
 */
@Dao
interface EvolutionLogDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogEntry(entry: EvolutionLogEntity): Long
    
    @Query("SELECT * FROM evolution_log WHERE entityId = :entityId ORDER BY timestamp DESC")
    suspend fun getLogForEntity(entityId: String): List<EvolutionLogEntity>
    
    @Query("SELECT * FROM evolution_log WHERE changeType = :changeType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLogByChangeType(changeType: String, limit: Int = 100): List<EvolutionLogEntity>
    
    @Query("SELECT * FROM evolution_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLog(limit: Int): List<EvolutionLogEntity>
    
    @Query("DELETE FROM evolution_log WHERE timestamp < :olderThan")
    suspend fun deleteOldEntries(olderThan: Long)
}

/**
 * Data Access Object for Autonomous Decision entities
 * Provides database operations for tracking autonomous actions
 */
@Dao
interface AutonomousDecisionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: AutonomousDecisionEntity): Long
    
    @Query("SELECT * FROM autonomous_decisions WHERE decisionId = :decisionId")
    suspend fun getDecision(decisionId: String): AutonomousDecisionEntity?
    
    @Query("SELECT * FROM autonomous_decisions WHERE actionType = :actionType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getDecisionsByActionType(actionType: String, limit: Int): List<AutonomousDecisionEntity>
    
    @Query("SELECT * FROM autonomous_decisions WHERE executed = 1 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getExecutedDecisions(limit: Int): List<AutonomousDecisionEntity>
    
    @Query("SELECT * FROM autonomous_decisions WHERE userApprovalRequested = 1 AND userApproved IS NULL")
    suspend fun getPendingApprovals(): List<AutonomousDecisionEntity>
    
    @Query("SELECT * FROM autonomous_decisions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentDecisions(limit: Int): List<AutonomousDecisionEntity>
    
    @Update
    suspend fun updateDecision(decision: AutonomousDecisionEntity)
}

/**
 * Data Access Object for Performance Metric entities
 * Provides database operations for system metrics
 */
@Dao
interface PerformanceMetricDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: PerformanceMetricEntity): Long
    
    @Query("SELECT * FROM performance_metrics WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getMetricsInRange(startTime: Long, endTime: Long): List<PerformanceMetricEntity>
    
    @Update
    suspend fun updateMetric(metric: PerformanceMetricEntity)
    
    @Delete
    suspend fun deleteMetric(metric: PerformanceMetricEntity)
    
    @Query("DELETE FROM performance_metrics WHERE timestamp < :olderThan")
    suspend fun deleteOldMetrics(olderThan: Long)
}

/**
 * Data Access Object for Feedback entities
 * Provides database operations for user feedback
 */
@Dao
interface FeedbackDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity): Long
    
    @Query("SELECT * FROM feedback WHERE decisionId = :decisionId")
    suspend fun getFeedbackForDecision(decisionId: String): List<FeedbackEntity>
    
    @Query("SELECT * FROM feedback WHERE feedbackType = :feedbackType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getFeedbackByType(feedbackType: String, limit: Int): List<FeedbackEntity>
    
    @Query("SELECT * FROM feedback WHERE rating IS NOT NULL ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRatedFeedback(limit: Int): List<FeedbackEntity>
    
    @Query("SELECT AVG(rating) FROM feedback WHERE rating IS NOT NULL")
    suspend fun getAverageRating(): Float?
    
    @Update
    suspend fun updateFeedback(feedback: FeedbackEntity)
    
    @Delete
    suspend fun deleteFeedback(feedback: FeedbackEntity)
}

/**
 * Data Access Object for System Config entities
 * Provides database operations for system configuration
 */
@Dao
interface SystemConfigDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: SystemConfigEntity): Long
    
    @Query("SELECT * FROM system_config WHERE key = :key")
    suspend fun getConfig(key: String): SystemConfigEntity?
    
    @Query("SELECT value FROM system_config WHERE key = :key")
    suspend fun getConfigValue(key: String): String?
    
    @Query("SELECT * FROM system_config")
    suspend fun getAllConfigs(): List<SystemConfigEntity>
    
    @Update
    suspend fun updateConfig(config: SystemConfigEntity)
    
    @Delete
    suspend fun deleteConfig(config: SystemConfigEntity)
}
