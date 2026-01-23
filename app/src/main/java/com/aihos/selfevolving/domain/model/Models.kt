package com.aihos.selfevolving.domain.model

/**
 * Core domain models for the Self-Evolving AI Human OS
 */

// Memory Layer Models
data class Memory(
    val id: String,
    val content: String,
    val timestamp: Long,
    val type: MemoryType,
    val importance: Float, // 0.0 to 1.0
    val associatedMemories: List<String> = emptyList(),
    val retrievalCount: Int = 0,
    val lastAccessed: Long = timestamp
)

enum class MemoryType {
    SHORT_TERM,
    LONG_TERM,
    EPISODIC,
    SEMANTIC,
    PROCEDURAL
}

// Reasoning Layer Models
data class ReasoningContext(
    val id: String,
    val query: String,
    val relevantMemories: List<Memory>,
    val inferenceSteps: List<InferenceStep>,
    val conclusion: String?,
    val confidence: Float,
    val timestamp: Long
)

data class InferenceStep(
    val step: Int,
    val description: String,
    val inputData: Map<String, Any>,
    val outputData: Map<String, Any>,
    val reasoning: String
)

// Reflection Layer Models
data class ReflectionEntry(
    val id: String,
    val timestamp: Long,
    val performanceMetrics: PerformanceMetrics,
    val insights: List<String>,
    val improvements: List<String>,
    val behaviorAnalysis: BehaviorAnalysis
)

data class PerformanceMetrics(
    val taskSuccessRate: Float,
    val responseTime: Long,
    val memoryEfficiency: Float,
    val reasoningAccuracy: Float,
    val userSatisfaction: Float
)

data class BehaviorAnalysis(
    val patterns: List<String>,
    val anomalies: List<String>,
    val trends: List<String>
)

// Evolution Layer Models
data class EvolutionState(
    val id: String,
    val version: Int,
    val timestamp: Long,
    val learningProgress: Float,
    val adaptations: List<Adaptation>,
    val capabilities: List<Capability>
)

data class Adaptation(
    val id: String,
    val description: String,
    val timestamp: Long,
    val trigger: String,
    val impact: Float,
    val status: AdaptationStatus
)

enum class AdaptationStatus {
    PROPOSED,
    TESTING,
    ACTIVE,
    DEPRECATED
}

data class Capability(
    val name: String,
    val level: Float, // 0.0 to 1.0
    val evolution: List<EvolutionPoint>
)

data class EvolutionPoint(
    val timestamp: Long,
    val level: Float,
    val trigger: String
)

// Autonomy Layer Models
data class AutonomousTask(
    val id: String,
    val name: String,
    val description: String,
    val priority: Priority,
    val status: TaskStatus,
    val createdAt: Long,
    val scheduledAt: Long?,
    val completedAt: Long?,
    val dependencies: List<String> = emptyList(),
    val outcome: TaskOutcome? = null
)

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class TaskStatus {
    PENDING,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class TaskOutcome(
    val success: Boolean,
    val result: String,
    val learnings: List<String>,
    val timestamp: Long
)

// AI State
data class AiState(
    val isActive: Boolean,
    val currentMode: AiMode,
    val memoryUtilization: Float,
    val processingLoad: Float,
    val evolutionStage: Int,
    val autonomyLevel: Float
)

enum class AiMode {
    IDLE,
    LEARNING,
    REASONING,
    REFLECTING,
    EVOLVING,
    AUTONOMOUS
}
