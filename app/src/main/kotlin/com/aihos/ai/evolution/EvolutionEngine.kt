package com.aihos.ai.evolution

import kotlinx.serialization.Serializable

/**
 * Evolution Engine for continuous AI improvement
 * Implements self-improvement mechanisms, learning from experience,
 * and adaptation to new situations
 */
interface EvolutionEngine {
    
    /**
     * Process feedback and learn from it
     * @param feedback The feedback to learn from
     * @param context Context in which feedback was given
     */
    suspend fun learnFromFeedback(feedback: String, context: String = ""): LearningResult
    
    /**
     * Adapt strategy based on results
     * @param strategy Current strategy
     * @param results Results of applying the strategy
     * @return Adapted strategy
     */
    suspend fun adaptStrategy(strategy: String, results: String): AdaptationResult
    
    /**
     * Mutate/improve an approach
     * @param approach Current approach
     * @return List of improved variations
     */
    suspend fun generateImprovement(approach: String): List<ImprovedVariation>
    
    /**
     * Consolidate learning into long-term knowledge
     * @param shortTermLearning Recent learning to consolidate
     */
    suspend fun consolidateLearning(shortTermLearning: List<String>)
    
    /**
     * Track evolution metrics
     * @return Current evolution status
     */
    suspend fun getEvolutionMetrics(): EvolutionMetrics
    
    /**
     * Apply genetic algorithm-style selection
     * @param candidates Candidate improvements
     * @return Best candidates for advancement
     */
    suspend fun selectBestVariants(candidates: List<String>, count: Int = 3): List<String>
}

/**
 * Result of a learning operation
 */
@Serializable
data class LearningResult(
    val feedback: String = "",
    val lesson: String = "",
    val applicability: List<String> = emptyList(),
    val confidenceInApplicability: Float = 0f,
    val timesApplied: Int = 0
)

/**
 * Result of a strategy adaptation
 */
@Serializable
data class AdaptationResult(
    val originalStrategy: String = "",
    val adaptedStrategy: String = "",
    val changeReasoning: String = "",
    val expectedImprovement: Float = 0f,
    val implementationSteps: List<String> = emptyList()
)

/**
 * An improved variation of an approach
 */
@Serializable
data class ImprovedVariation(
    val originalApproach: String = "",
    val improvedApproach: String = "",
    val improvementArea: String = "",
    val estimatedBenefit: Float = 0f,
    val implementation: String = ""
)

/**
 * Metrics tracking the evolution progress
 */
@Serializable
data class EvolutionMetrics(
    val generationNumber: Int = 0,
    val totalIterations: Int = 0,
    val averageImprovement: Float = 0f,
    val successRate: Float = 0f,
    val adaptationCount: Int = 0,
    val lastEvolutionTime: Long = 0,
    val knowledgeExpansion: Float = 0f,
    val capabilityGrowth: Map<String, Float> = emptyMap()
)

/**
 * Evolution strategy options
 */
enum class EvolutionStrategy {
    RANDOM_MUTATION,      // Random variations
    GRADIENT_DESCENT,     // Directed improvement
    GENETIC_ALGORITHM,    // Selection-based evolution
    SIMULATED_ANNEALING,  // Temperature-based exploration
    BAYESIAN_OPTIMIZATION // Probabilistic search
}
