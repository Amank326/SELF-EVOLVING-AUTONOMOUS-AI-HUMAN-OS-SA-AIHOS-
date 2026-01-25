package com.aihos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * Entity for storing memory items in local Room database
 * Supports semantic search through dedicated indexing
 */
@Entity(
    tableName = "memories",
    indices = [
        Index("type"),
        Index("importance"),
        Index("createdAt"),
        Index("accessCount")
    ]
)
@Serializable
data class MemoryEntity(
    @PrimaryKey
    val id: String,
    val type: String,  // EPISODIC, SEMANTIC, PROCEDURAL, EMOTIONAL, CONTEXTUAL
    val content: String,
    val semanticVector: String = "",  // JSON encoded float array
    val metadata: String = "{}",  // JSON encoded map
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val importance: Float = 0.5f,
    val accessCount: Int = 0
)

/**
 * Entity for storing reasoning rules that the AI has learned
 * These rules drive decision-making behavior
 */
@Entity(
    tableName = "reasoning_rules",
    indices = [
        Index("isActive"),
        Index("weight")
    ]
)
@Serializable
data class ReasoningRuleEntity(
    @PrimaryKey
    val id: String,
    val condition: String,  // The condition under which this rule applies
    val action: String,     // The action to take if condition is met
    val weight: Float = 0.5f,  // Confidence/importance weight
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val evolvedAt: Long = System.currentTimeMillis()
) {
    val successRate: Float
        get() = if (successCount + failureCount == 0) 0f 
                else successCount.toFloat() / (successCount + failureCount)
}

/**
 * Entity for storing insights discovered through reflection
 */
@Entity(
    tableName = "insights",
    indices = [
        Index("type"),
        Index("discoveredAt")
    ]
)
@Serializable
data class InsightEntity(
    @PrimaryKey
    val id: String,
    val type: String,  // Type of insight (TIMING_ISSUE, ACTION_REFINEMENT, etc)
    val description: String,
    val relatedMemoryIds: String = "[]",  // JSON array of related memory IDs
    val confidence: Float = 0.5f,
    val actionTaken: String = "",
    val discoveredAt: Long = System.currentTimeMillis(),
    val implementedAt: Long? = null
)

/**
 * Entity for tracking the evolution of the AI system
 */
@Entity(
    tableName = "evolution_log",
    indices = [
        Index("changeType"),
        Index("timestamp")
    ]
)
@Serializable
data class EvolutionLogEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val entityId: String,  // ID of the entity being evolved (rule, memory, etc)
    val changeType: String,  // WEIGHT_UPDATE, CREATION, DEPRECATION, etc
    val oldValue: String? = null,
    val newValue: String? = null,
    val reflection: String = ""
)

/**
 * Entity for storing autonomous decisions made by the system
 */
@Entity(
    tableName = "autonomous_decisions",
    indices = [
        Index("decisionId"),
        Index("timestamp"),
        Index("actionType")
    ]
)
@Serializable
data class AutonomousDecisionEntity(
    @PrimaryKey
    val id: String,
    val decisionId: String,  // Unique ID for this decision
    val actionType: String,  // Type of action taken
    val actionDescription: String,
    val reasoning: String,
    val confidence: Float = 0.5f,
    val executed: Boolean = false,
    val userApprovalRequested: Boolean = false,
    val userApproved: Boolean? = null,
    val outcome: String = "",
    val feedback: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Entity for storing system performance metrics
 */
@Entity(tableName = "performance_metrics")
@Serializable
data class PerformanceMetricEntity(
    @PrimaryKey
    val timestamp: Long,
    val memoryAccessTime: Long = 0,  // ms
    val reasoningTime: Long = 0,     // ms
    val reflectionTime: Long = 0,    // ms
    val evolutionTime: Long = 0,     // ms
    val decisionTime: Long = 0,      // ms
    val memoryUtilization: Float = 0f,  // 0-1
    val cpuUtilization: Float = 0f,     // 0-1
    val errorCount: Int = 0,
    val successCount: Int = 0
)

/**
 * Entity for storing user feedback about autonomous actions
 */
@Entity(
    tableName = "feedback",
    indices = [
        Index("decisionId"),
        Index("feedbackType"),
        Index("timestamp")
    ]
)
@Serializable
data class FeedbackEntity(
    @PrimaryKey
    val id: String,
    val decisionId: String,  // Related autonomous decision
    val feedbackType: String,  // POSITIVE, NEGATIVE, NEUTRAL
    val rating: Int? = null,   // 1-5 star rating
    val comment: String = "",
    val suggestedAction: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Entity for storing system configuration and preferences
 */
@Entity(tableName = "system_config")
@Serializable
data class SystemConfigEntity(
    @PrimaryKey
    val key: String,
    val value: String,  // JSON encoded value
    val type: String,   // STRING, INT, FLOAT, BOOLEAN, JSON
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
