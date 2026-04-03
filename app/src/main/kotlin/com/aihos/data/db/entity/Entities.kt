package com.aihos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Memory Entity
 * Stores AI system memories with importance and access tracking
 */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "type")
    val type: String,  // "semantic", "episodic", "procedural"

    @ColumnInfo(name = "importance")
    val importance: Float = 0.5f,

    @ColumnInfo(name = "accessCount")
    val accessCount: Int = 0,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "lastAccessedAt")
    val lastAccessedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "metadata")
    val metadata: String? = null
)

/**
 * Reasoning Rule Entity
 * Stores learned reasoning rules with success/failure tracking
 */
@Entity(tableName = "reasoning_rules")
data class ReasoningRuleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "condition")
    val condition: String,

    @ColumnInfo(name = "action")
    val action: String,

    @ColumnInfo(name = "weight")
    val weight: Float = 1.0f,

    @ColumnInfo(name = "successCount")
    val successCount: Int = 0,

    @ColumnInfo(name = "failureCount")
    val failureCount: Int = 0,

    @ColumnInfo(name = "isActive")
    val isActive: Boolean = true,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "lastUsedAt")
    val lastUsedAt: Long? = null
)

/**
 * Insight Entity
 * Stores discovered insights from AI analysis
 */
@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "discoveredAt")
    val discoveredAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "implementedAt")
    val implementedAt: Long? = null,

    @ColumnInfo(name = "sourceData")
    val sourceData: String? = null
)

/**
 * Evolution Log Entity
 * Tracks system evolution changes over time
 */
@Entity(tableName = "evolution_log")
data class EvolutionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "entityId")
    val entityId: String,

    @ColumnInfo(name = "changeType")
    val changeType: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "oldValue")
    val oldValue: String? = null,

    @ColumnInfo(name = "newValue")
    val newValue: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Autonomous Decision Entity
 * Tracks autonomous AI decisions and their outcomes
 */
@Entity(tableName = "autonomous_decisions")
data class AutonomousDecisionEntity(
    @PrimaryKey
    val decisionId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "actionType")
    val actionType: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "executed")
    val executed: Boolean = false,

    @ColumnInfo(name = "userApprovalRequested")
    val userApprovalRequested: Boolean = false,

    @ColumnInfo(name = "userApproved")
    val userApproved: Boolean? = null,

    @ColumnInfo(name = "outcome")
    val outcome: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Performance Metric Entity
 * Stores system performance metrics
 */
@Entity(tableName = "performance_metrics")
data class PerformanceMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "metricName")
    val metricName: String,

    @ColumnInfo(name = "value")
    val value: Float,

    @ColumnInfo(name = "unit")
    val unit: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Feedback Entity
 * Stores user feedback for AI improvement
 */
@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "decisionId")
    val decisionId: String? = null,

    @ColumnInfo(name = "feedbackType")
    val feedbackType: String,

    @ColumnInfo(name = "content")
    val content: String? = null,

    @ColumnInfo(name = "rating")
    val rating: Int? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * System Config Entity
 * Stores system configuration key-value pairs
 */
@Entity(tableName = "system_config")
data class SystemConfigEntity(
    @PrimaryKey
    val key: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)

