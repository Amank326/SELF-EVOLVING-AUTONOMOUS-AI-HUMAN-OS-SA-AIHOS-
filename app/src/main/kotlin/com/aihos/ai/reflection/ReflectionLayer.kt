package com.aihos.ai.reflection

import kotlinx.serialization.Serializable

/**
 * Core abstraction for Reflection Layer
 * Enables self-awareness, self-evaluation, and meta-cognitive processes
 * The AI can examine its own thoughts, decisions, and performance
 */
interface ReflectionLayer {
    
    /**
     * Analyze the AI's own decision process
     * @param recentDecisions The recent decisions to analyze
     * @return Self-evaluation and insights
     */
    suspend fun reflectOnDecisions(recentDecisions: List<String>): ReflectionInsight
    
    /**
     * Evaluate performance on a task
     * @param taskDescription Description of the completed task
     * @param outcome The outcome/result
     * @return Performance evaluation
     */
    suspend fun evaluatePerformance(taskDescription: String, outcome: String): PerformanceEvaluation
    
    /**
     * Identify patterns in behavior and thinking
     * @param behaviors List of recent behaviors/actions
     * @return Identified patterns
     */
    suspend fun identifyPatterns(behaviors: List<String>): List<BehaviorPattern>
    
    /**
     * Self-critique and suggest improvements
     * @param action The action to critique
     * @return Critique with suggestions
     */
    suspend fun selfCritique(action: String): Critique
    
    /**
     * Assess confidence in own knowledge
     * @param domain The domain of knowledge
     * @return Confidence assessment
     */
    suspend fun assessConfidence(domain: String): ConfidenceAssessment
    
    /**
     * Identify knowledge gaps
     * @return List of identified gaps
     */
    suspend fun identifyKnowledgeGaps(): List<KnowledgeGap>
}

/**
 * Insights gained from reflection on decisions
 */
@Serializable
data class ReflectionInsight(
    val summary: String = "",
    val strengthsIdentified: List<String> = emptyList(),
    val weaknessesIdentified: List<String> = emptyList(),
    val learnings: List<String> = emptyList(),
    val suggestedImprovements: List<String> = emptyList()
)

/**
 * Evaluation of performance on a task
 */
@Serializable
data class PerformanceEvaluation(
    val taskDescription: String = "",
    val successRate: Float = 0f,
    val efficiency: Float = 0f,
    val quality: Float = 0f,
    val reasoning: String = "",
    val recommendations: List<String> = emptyList()
)

/**
 * A pattern identified in behavior
 */
@Serializable
data class BehaviorPattern(
    val pattern: String = "",
    val frequency: Int = 0,
    val context: String = "",
    val impact: String = "",
    val isPositive: Boolean = true
)

/**
 * Critique of an action with suggestions
 */
@Serializable
data class Critique(
    val action: String = "",
    val assessment: String = "",
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val alternativeApproaches: List<String> = emptyList()
)

/**
 * Assessment of confidence in a knowledge domain
 */
@Serializable
data class ConfidenceAssessment(
    val domain: String = "",
    val overallConfidence: Float = 0f,
    val areasOfExpertise: List<String> = emptyList(),
    val areasOfUncertainty: List<String> = emptyList(),
    val recommendation: String = ""
)

/**
 * A gap in knowledge
 */
@Serializable
data class KnowledgeGap(
    val topic: String = "",
    val description: String = "",
    val importance: Float = 0f,
    val suggestedLearning: String = ""
)
