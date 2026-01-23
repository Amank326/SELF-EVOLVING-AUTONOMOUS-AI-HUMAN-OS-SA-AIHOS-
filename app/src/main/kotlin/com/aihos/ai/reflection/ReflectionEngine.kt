package com.aihos.ai.reflection

import com.aihos.ai.memory.Episode
import com.aihos.ai.memory.Outcome
import com.aihos.ai.reasoning.DecisionRecord
import kotlinx.serialization.Serializable

/**
 * Reflection engine: analyzes past decisions to extract learning
 * This is how AI learns what works and what doesn't
 */
interface ReflectionEngine {
    /**
     * Analyze the outcome of a decision and generate reflection
     */
    suspend fun analyzeOutcome(
        decision: DecisionRecord,
        actualOutcome: Outcome,
        outcomeFeedback: String = ""
    ): ReflectionResult
    
    /**
     * Identify patterns across multiple decisions
     */
    suspend fun identifyPatterns(decisions: List<DecisionRecord>): List<Pattern>
    
    /**
     * Validate assumptions made during reasoning
     */
    suspend fun validateAssumptions(reflection: ReflectionResult): List<AssumptionValidation>
}

/**
 * Result of reflection analysis
 */
@Serializable
data class ReflectionResult(
    val decisionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val expectedOutcome: String,
    val actualOutcome: Outcome,
    val outcomeCorrect: Boolean,
    val confidenceInAnalysis: Float, // How sure are we about this reflection?
    val insights: List<Insight> = emptyList(),
    val recommendation: String = ""
)

/**
 * A single insight from reflection
 */
@Serializable
data class Insight(
    val id: String,
    val type: InsightType,
    val description: String,
    val importance: Float // 0.0 to 1.0
)

enum class InsightType {
    CAUSALITY,           // This caused the outcome
    ASSUMPTION_INVALID,  // Our assumption was wrong
    CONTEXT_MISSED,      // We missed important context
    SUCCESS_PATTERN,     // Pattern that leads to success
    FAILURE_PATTERN,     // Pattern that leads to failure
    USER_PREFERENCE,     // User prefers something
    TIMING_ISSUE,        // Timing was wrong
    ACTION_REFINEMENT    // Action could be better
}

/**
 * Pattern: recurring situations and outcomes
 */
@Serializable
data class Pattern(
    val id: String,
    val condition: String, // What situations trigger this?
    val action: String,    // What action is taken?
    val outcomeFrequency: Map<Outcome, Int>, // Outcome -> count
    val confidenceLevel: Float
)

/**
 * Validation of an assumption
 */
@Serializable
data class AssumptionValidation(
    val assumption: String,
    val wasCorrect: Boolean,
    val evidence: String,
    val suggestionIfWrong: String = ""
)

/**
 * Default implementation of ReflectionEngine
 */
class DefaultReflectionEngine : ReflectionEngine {
    
    override suspend fun analyzeOutcome(
        decision: DecisionRecord,
        actualOutcome: Outcome,
        outcomeFeedback: String
    ): ReflectionResult {
        val expectedOutcome = decision.chosenOption.expectedOutcome
        val outcomeCorrect = when {
            actualOutcome == Outcome.SUCCESS -> true
            actualOutcome == Outcome.PARTIAL && decision.riskLevel == RiskLevel.HIGH -> true
            actualOutcome == Outcome.FAILURE -> false
            else -> actualOutcome == Outcome.SUCCESS
        }
        
        val insights = mutableListOf<Insight>()
        
        // Analyze causality
        if (outcomeCorrect) {
            insights.add(Insight(
                id = "insight_1",
                type = InsightType.CAUSALITY,
                description = "Action correctly predicted outcome. Decision reasoning was sound.",
                importance = 0.9f
            ))
        } else {
            insights.add(Insight(
                id = "insight_2",
                type = InsightType.CAUSALITY,
                description = "Outcome differed from expectation. Reasoning may have missed factors.",
                importance = 0.9f
            ))
        }
        
        // Analyze assumptions
        insights.add(Insight(
            id = "insight_3",
            type = InsightType.ASSUMPTION_INVALID,
            description = "Assumption: 'user engagement = fatigue' - Consider validating with user feedback",
            importance = 0.7f
        ))
        
        return ReflectionResult(
            decisionId = decision.id,
            expectedOutcome = expectedOutcome,
            actualOutcome = actualOutcome,
            outcomeCorrect = outcomeCorrect,
            confidenceInAnalysis = 0.75f,
            insights = insights,
            recommendation = if (outcomeCorrect) 
                "Reinforce this decision pattern" 
            else 
                "Review decision process and refine action selection"
        )
    }
    
    override suspend fun identifyPatterns(decisions: List<DecisionRecord>): List<Pattern> {
        // Group decisions by similar contexts
        val patterns = mutableListOf<Pattern>()
        
        // Example pattern: evening high usage leads to focus reminders
        val eveningDecisions = decisions.filter { 
            it.context.currentTime.startsWith("2") // 20:00-23:59
        }
        
        if (eveningDecisions.isNotEmpty()) {
            patterns.add(Pattern(
                id = "pattern_evening_focus",
                condition = "time > 20:00 AND appUsageDuration > 120min",
                action = "send_focus_reminder",
                outcomeFrequency = mapOf(Outcome.SUCCESS to 8, Outcome.PARTIAL to 3, Outcome.FAILURE to 4),
                confidenceLevel = 0.6f
            ))
        }
        
        return patterns
    }
    
    override suspend fun validateAssumptions(reflection: ReflectionResult): List<AssumptionValidation> {
        return listOf(
            AssumptionValidation(
                assumption = "User engagement correlates with fatigue",
                wasCorrect = reflection.outcomeCorrect,
                evidence = when (reflection.actualOutcome) {
                    Outcome.SUCCESS -> "User did take a break after reminder"
                    Outcome.FAILURE -> "User ignored reminder and continued working"
                    else -> "Unknown outcome"
                },
                suggestionIfWrong = "Consider measuring fatigue directly instead of inferring from usage"
            ),
            AssumptionValidation(
                assumption = "Intervention timing is optimal",
                wasCorrect = reflection.outcomeCorrect,
                evidence = "Reminder was sent, but user may have been in focus state",
                suggestionIfWrong = "Check user's current activity type before intervening"
            )
        )
    }
}

// Utility extension to support reflection
enum class RiskLevel {
    LOW, MEDIUM, HIGH
}
