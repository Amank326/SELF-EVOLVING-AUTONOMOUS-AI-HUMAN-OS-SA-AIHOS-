package com.aihos.ai.perception

import com.aihos.ai.reasoning.ReasoningEngine
import com.aihos.ai.reasoning.Option
import com.aihos.ai.reasoning.RiskLevel
import timber.log.Timber

/**
 * EnvironmentAwareReasoning: Extensions to reasoning that adapt to environmental context
 * 
 * The environment affects decision-making:
 * - Battery-critical: Prefer low-computation actions
 * - Network-unavailable: Avoid cloud-dependent actions
 * - User active: High-confidence strategies only
 * - User idle: Can explore lower-confidence options
 * - Night time: Quiet, introspective actions
 * - Day time: Interactive, visible actions
 * - High pressure: Risk-averse decisions
 */

/**
 * Environment-aware option filtering and scoring
 */
fun filterOptionsForEnvironment(
    options: List<Option>,
    context: EnvironmentAwareReasoningContext
): List<Option> {
    return options.mapNotNull { option ->
        var viability = 1f  // Start fully viable
        
        // Filter based on environment constraints
        
        // Battery-critical: prefer low-resource actions
        if (context.shouldConserveEnergy) {
            if (option.id.contains("compute") || option.id.contains("sync")) {
                viability *= 0.3f  // Heavily penalize resource-intensive actions
            }
        }
        
        // Network unavailable: avoid network-dependent actions
        if (!context.hasNetworkAvailability) {
            if (option.action.contains("sync") || option.action.contains("upload") ||
                option.action.contains("cloud")) {
                viability *= 0.2f  // Disable network-dependent actions
            }
        }
        
        // High pressure: risk-averse
        if (context.isHighPressureEnvironment) {
            if (option.riskLevel != RiskLevel.LOW) {
                viability *= 0.5f  // Penalize non-low-risk options
            }
        }
        
        // User active/focused: conservative strategies
        if (context.userIsFocused && context.environment.userActivityLevel == UserActivityLevel.INTENSE) {
            if (option.riskLevel == RiskLevel.HIGH) {
                viability *= 0.2f  // Avoid high-risk during active use
            }
        }
        
        // Temporal appropriateness
        if (context.environment.temporal.isNightTime) {
            // Night time: quiet actions, no notifications
            if (option.action.contains("notify") || option.action.contains("alert")) {
                viability *= 0.3f
            }
        } else {
            // Day time: can be more active
            if (option.action.contains("suggest") || option.action.contains("remind")) {
                viability *= 1.1f  // Slightly boost interactive actions during day
            }
        }
        
        // Only return if still viable
        if (viability > 0.1f) {
            option.copy(
                // Encode viability in expectedOutcome as metadata
                expectedOutcome = option.expectedOutcome + " [env_viable: %.1f]".format(viability)
            )
        } else {
            null
        }
    }
}

/**
 * Compute confidence adjustment based on environment
 */
fun adjustConfidenceForEnvironment(
    baseConfidence: Float,
    context: EnvironmentAwareReasoningContext
): Float {
    var confidence = baseConfidence
    
    // High pressure reduces confidence in untested strategies
    if (context.isHighPressureEnvironment) {
        confidence *= 0.85f
    }
    
    // Optimal conditions increase confidence
    if (context.isOptimalLearningTime) {
        confidence *= 1.1f
    }
    
    // Battery critical: only high-confidence actions
    if (context.shouldConserveEnergy) {
        confidence *= if (baseConfidence > 0.8f) 1.05f else 0.9f
    }
    
    // User actively engaged: require higher confidence
    if (context.environment.userActivityLevel == UserActivityLevel.INTENSE) {
        confidence *= 0.95f
    }
    
    return confidence.coerceIn(0f, 1f)
}

/**
 * Adjust reasoning latency tolerance based on environment
 */
fun getReasoningLatencyBudgetMs(context: EnvironmentAwareReasoningContext): Long {
    // Base budget: 1000ms
    var budgetMs = 1000L
    
    // Battery-critical: reduce compute time
    if (context.shouldConserveEnergy) {
        budgetMs = 500L  // Quick decisions only
    }
    
    // High pressure: faster decisions
    if (context.isHighPressureEnvironment) {
        budgetMs = 750L
    }
    
    // Optimal learning time: allow deeper reasoning
    if (context.isOptimalLearningTime) {
        budgetMs = 1500L
    }
    
    // Reflection time: deep thinking
    if (context.isReflectionTime) {
        budgetMs = 2000L
    }
    
    // User inactive: can think longer
    if (context.environment.userActivityLevel == UserActivityLevel.IDLE) {
        budgetMs *= 1.2f
    }
    
    return budgetMs.toLong()
}

/**
 * Determine reasoning conservativeness vs. exploration
 */
fun getReasoningExplorativenessBalance(context: EnvironmentAwareReasoningContext): Pair<Float, Float> {
    // (conservativeness, explorativeness)
    
    // Default: balanced
    var conservative = 0.5f
    var exploratory = 0.5f
    
    // High pressure: favor conservative
    if (context.isHighPressureEnvironment) {
        conservative = 0.8f
        exploratory = 0.2f
    }
    
    // Optimal learning: favor exploratory
    if (context.isOptimalLearningTime) {
        conservative = 0.3f
        exploratory = 0.7f
    }
    
    // Battery critical: highly conservative
    if (context.shouldConserveEnergy) {
        conservative = 0.9f
        exploratory = 0.1f
    }
    
    // Reflection time: balanced introspection
    if (context.isReflectionTime) {
        conservative = 0.6f
        exploratory = 0.4f
    }
    
    return Pair(conservative, exploratory)
}

/**
 * Filter reasoning rules based on environment
 */
fun filterRulesForEnvironment(
    rules: Map<String, String>,  // rule name -> rule logic
    context: EnvironmentAwareReasoningContext
): Map<String, String> {
    return rules.filterKeys { ruleName ->
        // Disable resource-intensive rules when battery critical
        if (context.shouldConserveEnergy && ruleName.contains("heavy")) {
            return@filterKeys false
        }
        
        // Disable risky rules under high pressure
        if (context.isHighPressureEnvironment && ruleName.contains("risky")) {
            return@filterKeys false
        }
        
        // Disable network-dependent rules without connectivity
        if (!context.hasNetworkAvailability && ruleName.contains("cloud")) {
            return@filterKeys false
        }
        
        true
    }
}

/**
 * Log environment-aware reasoning decisions
 */
fun logEnvironmentAwareReasoning(
    context: EnvironmentAwareReasoningContext,
    decision: String,
    confidence: Float,
    reasoning: String
) {
    Timber.d(
        "ReasoningEngine: Environment-aware decision - " +
        "decision=$decision, " +
        "confidence=%.2f, " +
        "calmness=%.2f, " +
        "constraints=%.2f, " +
        "pressure=${if (context.isHighPressureEnvironment) "HIGH" else "NORMAL"}, " +
        "reasoning=$reasoning".format(
            confidence,
            context.environment.environmentalCalmness,
            context.environment.environmentalConstraints
        )
    )
}
