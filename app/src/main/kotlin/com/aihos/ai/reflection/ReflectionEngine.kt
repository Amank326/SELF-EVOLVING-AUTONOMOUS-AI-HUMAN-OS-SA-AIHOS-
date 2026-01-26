package com.aihos.ai.reflection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * Reflection Engine - Self-analysis and introspection
 * Enables:
 * - Performance analysis and evaluation
 * - Self-awareness metrics
 * - Behavioral pattern detection
 * - Improvement identification
 * - Meta-cognition (thinking about thinking)
 */

data class PerformanceMetric(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val value: Float = 0.5f,
    val trend: Float = 0f, // Positive = improving, Negative = declining
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class SelfAssessment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: String, // "reasoning", "memory", "autonomy", "evolution"
    val score: Float = 0.5f,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class ReflectionState(
    val selfAwareness: Float = 0.5f, // How well aware of own capabilities
    val confidenceInAbilities: Float = 0.5f,
    val identifiedImprovements: List<String> = emptyList(),
    val lastReflectionTime: Long = Instant.now().toEpochMilli(),
    val reflectionFrequency: Int = 0
)

/**
 * Reflection Engine - Self-analysis and improvement planning
 */
class ReflectionEngine {
    private val _performanceMetrics = MutableStateFlow<List<PerformanceMetric>>(emptyList())
    val performanceMetrics: StateFlow<List<PerformanceMetric>> = _performanceMetrics
    
    private val _assessments = MutableStateFlow<List<SelfAssessment>>(emptyList())
    val assessments: StateFlow<List<SelfAssessment>> = _assessments
    
    private val _selfAwareness = MutableStateFlow(0.5f)
    val selfAwareness: StateFlow<Float> = _selfAwareness
    
    private val _reflectionState = MutableStateFlow(ReflectionState())
    val reflectionState: StateFlow<ReflectionState> = _reflectionState
    
    private val _insights = MutableStateFlow<List<String>>(emptyList())
    val insights: StateFlow<List<String>> = _insights
    
    private var reflectionCount = 0

    /**
     * Record performance metric
     */
    suspend fun recordMetric(name: String, value: Float) {
        val value_clamped = value.coerceIn(0f, 1f)
        val existing = _performanceMetrics.value.find { it.name == name }
        
        val trend = if (existing != null) {
            value_clamped - existing.value
        } else {
            0f
        }
        
        val metric = PerformanceMetric(
            name = name,
            value = value_clamped,
            trend = trend
        )
        
        val current = _performanceMetrics.value.toMutableList()
        existing?.let { current.remove(it) }
        current.add(metric)
        _performanceMetrics.emit(current)
    }

    /**
     * Perform self-assessment in category
     */
    suspend fun selfAssess(
        category: String,
        score: Float,
        strengths: List<String> = emptyList(),
        weaknesses: List<String> = emptyList()
    ): SelfAssessment {
        val improvements = generateImprovementPlan(weaknesses)
        
        val assessment = SelfAssessment(
            category = category,
            score = score.coerceIn(0f, 1f),
            strengths = strengths,
            weaknesses = weaknesses,
            improvements = improvements
        )
        
        val current = _assessments.value.toMutableList()
        current.add(assessment)
        _assessments.emit(current)
        
        return assessment
    }

    /**
     * Full self-reflection cycle
     */
    suspend fun reflect(
        reasoningConfidence: Float,
        memoryLoad: Float,
        autonomyLevel: Float,
        evolutionProgress: Float
    ): ReflectionState {
        reflectionCount++
        
        // Assess each domain
        val reasoningAssess = selfAssess(
            category = "reasoning",
            score = reasoningConfidence,
            strengths = listOf("Pattern recognition", "Logical inference"),
            weaknesses = if (reasoningConfidence < 0.5f) listOf("High uncertainty") else emptyList()
        )
        
        val memoryAssess = selfAssess(
            category = "memory",
            score = 1f - memoryLoad,
            strengths = listOf("Data retention", "Consolidation"),
            weaknesses = if (memoryLoad > 0.8f) listOf("Limited capacity", "Forgetting risk") else emptyList()
        )
        
        val autonomyAssess = selfAssess(
            category = "autonomy",
            score = autonomyLevel,
            strengths = if (autonomyLevel > 0.6f) listOf("Self-directed action", "Decision making") else emptyList(),
            weaknesses = if (autonomyLevel < 0.6f) listOf("Needs guidance", "Low confidence") else emptyList()
        )
        
        val evolutionAssess = selfAssess(
            category = "evolution",
            score = evolutionProgress,
            strengths = if (evolutionProgress > 0.5f) listOf("Learning capability", "Skill development") else emptyList(),
            weaknesses = if (evolutionProgress < 0.5f) listOf("Slow adaptation") else emptyList()
        )
        
        // Calculate overall self-awareness
        val allScores = listOf(
            reasoningConfidence,
            1f - memoryLoad,
            autonomyLevel,
            evolutionProgress
        )
        
        val overallScore = allScores.average().toFloat()
        val variance = allScores.map { (it - overallScore) * (it - overallScore) }.average()
        
        // Self-awareness = how balanced and consistent performance is
        val awareness = overallScore * (1f - kotlin.math.sqrt(variance).toFloat())
        _selfAwareness.emit(awareness.coerceIn(0f, 1f))
        
        // Generate insights
        val generatedInsights = generateInsights(
            reasoningAssess,
            memoryAssess,
            autonomyAssess,
            evolutionAssess
        )
        
        val current = _insights.value.toMutableList()
        current.addAll(generatedInsights)
        if (current.size > 20) {
            current.drop(current.size - 20)
        }
        _insights.emit(current)
        
        // Update reflection state
        val newState = ReflectionState(
            selfAwareness = awareness.coerceIn(0f, 1f),
            confidenceInAbilities = overallScore,
            identifiedImprovements = generatedInsights,
            lastReflectionTime = Instant.now().toEpochMilli(),
            reflectionFrequency = reflectionCount
        )
        
        _reflectionState.emit(newState)
        
        return newState
    }

    /**
     * Generate improvement plan for identified weaknesses
     */
    private fun generateImprovementPlan(weaknesses: List<String>): List<String> {
        val improvements = mutableListOf<String>()
        
        for (weakness in weaknesses) {
            when {
                weakness.contains("uncertainty", ignoreCase = true) -> {
                    improvements.add("Increase data collection for pattern learning")
                    improvements.add("Implement confidence calibration")
                }
                weakness.contains("capacity", ignoreCase = true) -> {
                    improvements.add("Implement memory compression")
                    improvements.add("Increase consolidation frequency")
                }
                weakness.contains("forgetting", ignoreCase = true) -> {
                    improvements.add("Strengthen episodic links")
                    improvements.add("Regular memory rehearsal")
                }
                weakness.contains("guidance", ignoreCase = true) -> {
                    improvements.add("Increase autonomous decision attempts")
                    improvements.add("Learn from feedback")
                }
                weakness.contains("confidence", ignoreCase = true) -> {
                    improvements.add("Practice decision-making")
                    improvements.add("Build successful outcome history")
                }
                weakness.contains("adaptation", ignoreCase = true) -> {
                    improvements.add("Increase evolutionary cycles")
                    improvements.add("Practice new skills more frequently")
                }
                else -> {
                    improvements.add("Monitor and analyze $weakness")
                    improvements.add("Develop targeted improvement strategy")
                }
            }
        }
        
        return improvements
    }

    /**
     * Generate insights from self-assessment
     */
    private fun generateInsights(
        reasoning: SelfAssessment,
        memory: SelfAssessment,
        autonomy: SelfAssessment,
        evolution: SelfAssessment
    ): List<String> {
        val insights = mutableListOf<String>()
        
        val avgScore = listOf(reasoning.score, memory.score, autonomy.score, evolution.score).average()
        
        // Overall performance insight
        when {
            avgScore > 0.8f -> insights.add("🌟 Excellent overall performance - System is highly capable")
            avgScore > 0.6f -> insights.add("✓ Good performance - Room for targeted improvements")
            avgScore > 0.4f -> insights.add("⚠ Moderate performance - Focus on weak areas")
            else -> insights.add("⚡ Critical improvement needed - Systematic enhancement required")
        }
        
        // Specific domain insights
        if (reasoning.score > 0.8f) {
            insights.add("• Strong logical reasoning capability")
        }
        if (memory.score > 0.8f) {
            insights.add("• Excellent memory consolidation and retention")
        }
        if (autonomy.score > 0.8f) {
            insights.add("• High autonomous decision-making capability")
        }
        if (evolution.score > 0.8f) {
            insights.add("• Rapid learning and skill development")
        }
        
        // Imbalance detection
        val scores = listOf(reasoning.score, memory.score, autonomy.score, evolution.score)
        val maxScore = scores.maxOrNull() ?: 0.5f
        val minScore = scores.minOrNull() ?: 0.5f
        val imbalance = maxScore - minScore
        
        if (imbalance > 0.4f) {
            insights.add("⚠ Large performance imbalance detected - Focus on weaker areas")
        }
        
        return insights
    }

    /**
     * Get performance trends
     */
    fun getPerformanceTrends(): Map<String, Float> {
        return _performanceMetrics.value.associate { it.name to it.trend }
    }

    /**
     * Get overall assessment
     */
    fun getOverallAssessment(): Map<String, Any> {
        val byCategory = _assessments.value.groupBy { it.category }
        val categoryScores = byCategory.mapValues { (_, assessments) ->
            assessments.map { it.score }.average()
        }
        
        return mapOf(
            "overallScore" to categoryScores.values.average(),
            "categoryScores" to categoryScores,
            "reflectionCount" to reflectionCount,
            "selfAwareness" to _selfAwareness.value,
            "generatedInsights" to _insights.value.size
        )
    }

    /**
     * Get latest insights
     */
    fun getLatestInsights(count: Int = 5): List<String> {
        return _insights.value.takeLast(count)
    }

    /**
     * Should initiate self-improvement
     */
    fun shouldImprove(): Boolean {
        return _reflectionState.value.confidenceInAbilities < 0.7f
    }

    /**
     * Clear reflection state
     */
    suspend fun reset() {
        _performanceMetrics.emit(emptyList())
        _assessments.emit(emptyList())
        _insights.emit(emptyList())
        _selfAwareness.emit(0.5f)
        reflectionCount = 0
    }
}
