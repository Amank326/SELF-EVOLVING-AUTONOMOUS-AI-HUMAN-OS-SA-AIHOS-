package com.aihos.ai.reflection

import com.aihos.ai.perception.EnvironmentAwareReasoningContext
import com.aihos.ai.perception.EnvironmentContext
import com.aihos.ai.perception.UserActivityLevel
import timber.log.Timber

/**
 * EnvironmentAwareReflection: Extensions to reflection that consider environmental context
 * 
 * The environment affects how the AI reflects on its decisions and experiences:
 * - Calm, night-time environment: Deep, introspective reflection
 * - Active, daytime environment: Quick tactical review
 * - Battery-critical: Focus on energy efficiency in future decisions
 * - Network available: Can consider cloud-based insights
 * - User active: More conservative, proven strategies
 * - User idle: Opportunity for creative exploration
 */
fun ReflectionEngine.reflectWithEnvironment(
    decisionRecord: com.aihos.ai.reasoning.DecisionRecord,
    outcome: com.aihos.ai.memory.Outcome,
    context: EnvironmentAwareReasoningContext,
    feedback: String = ""
): ReflectionOutcome {
    
    val originalOutcome = this.reflect(decisionRecord, outcome, feedback)
    
    // Adjust insights based on environmental context
    val adjustedInsights = mutableListOf<String>()
    adjustedInsights.addAll(originalOutcome.insights)
    
    // Environmental pressure modifications
    if (context.isHighPressureEnvironment) {
        adjustedInsights.add("ENVIRONMENTAL: High pressure environment detected. Prioritize reliable, proven strategies.")
    }
    
    if (context.shouldConserveEnergy) {
        adjustedInsights.add("ENVIRONMENTAL: Battery critical. Optimize for minimal computation in future decisions.")
    }
    
    // Reflection quality adjustments
    if (context.isReflectionTime) {
        adjustedInsights.add("ENVIRONMENTAL: Optimal reflection time (calm, idle). Exploring deeper patterns and connections.")
    } else if (context.userIsFocused && context.environment.userActivityLevel == UserActivityLevel.INTENSE) {
        adjustedInsights.add("ENVIRONMENTAL: User active and focused. Reflection will be tactical, not strategic.")
    }
    
    // Network-based insights
    if (!context.hasNetworkAvailability && originalOutcome.insights.any { it.contains("external") }) {
        adjustedInsights.add("ENVIRONMENTAL: Network unavailable. Offline insights prioritized.")
    }
    
    // Temporal reflection adjustments
    if (context.environment.temporal.isNightTime) {
        adjustedInsights.add("TEMPORAL: Night reflection - integrating subconscious patterns.")
    }
    
    // Adjust learning rate based on pressure and availability
    val learningMultiplier = when {
        context.shouldConserveEnergy -> 0.5f        // Conservative learning when battery critical
        context.isOptimalLearningTime -> 1.2f       // Accelerated learning when optimal
        context.isHighPressureEnvironment -> 0.8f   // Reduced learning under pressure
        else -> 1.0f
    }
    
    Timber.d(
        "ReflectionEngine: Reflected with environment - " +
        "calmness=%.2f, constraints=%.2f, learningMultiplier=%.1f, insights=%d".format(
            context.environment.environmentalCalmness,
            context.environment.environmentalConstraints,
            learningMultiplier,
            adjustedInsights.size
        )
    )
    
    return originalOutcome.copy(
        insights = adjustedInsights
    )
}

/**
 * Check if reflection should be triggered based on environment
 */
fun shouldTriggerReflection(
    context: EnvironmentAwareReasoningContext,
    decisionsSinceLast: Int
): Boolean {
    // Always reflect on significant events
    if (decisionsSinceLast > 5) return true
    
    // Reflect more frequently in optimal conditions
    if (context.isReflectionTime && decisionsSinceLast > 2) return true
    
    // Skip reflection in high-pressure situations unless critical
    if (context.isHighPressureEnvironment && decisionsSinceLast < 8) return false
    
    return decisionsSinceLast > 3
}
