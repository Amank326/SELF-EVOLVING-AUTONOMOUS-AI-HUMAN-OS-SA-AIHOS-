package com.aihos.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a decision made by the AI system.
 * Part of the domain model (no Android dependencies).
 */
@Serializable
data class DecisionRecord(
    val id: String,
    val timestamp: Long,
    val context: String,
    val selectedOption: String,
    val confidence: Float,
    val reasoning: String,
    val executionTime: Long = 0L
)

/**
 * Represents the outcome of a decision.
 */
@Serializable
data class DecisionOutcome(
    val decisionId: String,
    val actualOutcome: String,
    val success: Boolean,
    val timestamp: Long,
    val feedback: String = ""
)

/**
 * Represents a learned rule in the AI system.
 */
@Serializable
data class LearnedRule(
    val id: String,
    val condition: String,
    val action: String,
    val confidence: Float,
    val timesExecuted: Int = 0,
    val successCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis()
)

/**
 * Cognitive state of the AI system at a point in time.
 */
@Serializable
data class CognitiveState(
    val executionPhase: ExecutionPhase,
    val currentDecision: DecisionRecord?,
    val lastReflection: ReflectionData?,
    val energyLevel: Float,
    val thermalState: ThermalState,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data from a reflection cycle.
 */
@Serializable
data class ReflectionData(
    val decisionId: String,
    val expectedOutcome: String,
    val actualOutcome: String,
    val insights: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Execution phase of the cognition loop.
 */
enum class ExecutionPhase {
    IDLE, THINKING, ACTING, REFLECTING, EVOLVING, ERROR
}

/**
 * Thermal state of the device.
 */
enum class ThermalState {
    NORMAL, WARNING, CRITICAL
}
