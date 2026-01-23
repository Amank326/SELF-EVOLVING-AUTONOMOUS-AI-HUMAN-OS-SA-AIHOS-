package com.aihos.ai.perception

import com.aihos.ai.evolution.EvolutionEngine
import com.aihos.ai.evolution.EvolutionFeedback
import timber.log.Timber

/**
 * EnvironmentAwareEvolution: Extensions to evolution that consider environmental context
 * 
 * Evolution (learning new rules and strategies) is gated by environmental conditions:
 * - Rich environment: Encourage experimentation and learning
 * - Constrained environment: Stick to proven, efficient strategies
 * - Battery-critical: Freeze learning, run in safe mode
 * - Night-time: Introspective evolution, pattern analysis
 * - User active: Evolution must maintain stability
 * - User idle: Safe time for rule modifications
 */
interface EnvironmentAwareEvolutionGate {
    /**
     * Determine if evolution should proceed based on environment
     */
    fun shouldAllowEvolution(
        context: EnvironmentAwareReasoningContext,
        evolutionType: EvolutionType
    ): Boolean
    
    /**
     * Get evolution aggressiveness factor (0.0 = conservative, 1.0 = exploratory)
     */
    fun getEvolutionAggressiveness(context: EnvironmentAwareReasoningContext): Float
}

enum class EvolutionType {
    RULE_MODIFICATION,      // Change decision rules
    STRATEGY_EXPLORATION,   // Try new strategies
    PATTERN_LEARNING,       // Learn from experience
    PARAMETER_TUNING        // Adjust parameters
}

/**
 * Default implementation of environment-aware evolution gating
 */
class DefaultEnvironmentAwareEvolutionGate : EnvironmentAwareEvolutionGate {
    
    override fun shouldAllowEvolution(
        context: EnvironmentAwareReasoningContext,
        evolutionType: EvolutionType
    ): Boolean {
        // Critical battery → no evolution
        if (context.shouldConserveEnergy) {
            return evolutionType == EvolutionType.PATTERN_LEARNING  // Only safe learning
        }
        
        // High pressure → conservative evolution only
        if (context.isHighPressureEnvironment) {
            return evolutionType in listOf(
                EvolutionType.PATTERN_LEARNING,
                EvolutionType.PARAMETER_TUNING
            )
        }
        
        // User active with intense activity → no evolution
        if (context.environment.userActivityLevel == UserActivityLevel.INTENSE) {
            return false
        }
        
        // Optimal conditions → allow all evolution
        if (context.isOptimalLearningTime) {
            return true
        }
        
        // Default: allow pattern learning and parameter tuning
        return evolutionType in listOf(
            EvolutionType.PATTERN_LEARNING,
            EvolutionType.PARAMETER_TUNING
        )
    }
    
    override fun getEvolutionAggressiveness(context: EnvironmentAwareReasoningContext): Float {
        var aggressiveness = 0.5f  // Baseline
        
        // Battery constraints reduce aggressiveness
        aggressiveness *= (1f - context.environment.environmentalConstraints)
        
        // Optimal time increases aggressiveness
        if (context.isOptimalLearningTime) {
            aggressiveness = (aggressiveness * 1.5f).coerceIn(0f, 1f)
        }
        
        // Night time: introspective evolution
        if (context.environment.temporal.isNightTime) {
            aggressiveness *= 0.8f  // Slightly more conservative at night
        }
        
        // Network availability impacts certain evolution types
        if (!context.hasNetworkAvailability) {
            aggressiveness *= 0.9f  // Slightly more conservative without network
        }
        
        return aggressiveness.coerceIn(0f, 1f)
    }
}

/**
 * Apply environmental constraints to evolution feedback
 */
fun EvolutionFeedback.withEnvironmentContext(
    context: EnvironmentAwareReasoningContext,
    gate: EnvironmentAwareEvolutionGate
): EvolutionFeedback {
    
    // Determine which evolution types are allowed
    val allowRuleModification = gate.shouldAllowEvolution(context, EvolutionType.RULE_MODIFICATION)
    val allowExploration = gate.shouldAllowEvolution(context, EvolutionType.STRATEGY_EXPLORATION)
    val allowLearning = gate.shouldAllowEvolution(context, EvolutionType.PATTERN_LEARNING)
    val allowTuning = gate.shouldAllowEvolution(context, EvolutionType.PARAMETER_TUNING)
    
    val aggressiveness = gate.getEvolutionAggressiveness(context)
    
    // Adjust feedback values based on environment
    val adjustedFeedback = this.copy(
        successRate = if (allowLearning) this.successRate else 0f,
        noveltyScore = if (allowExploration) this.noveltyScore * aggressiveness else 0f,
        impactScore = this.impactScore * (if (allowTuning) 1f else 0.5f),
        learningRate = this.learningRate * aggressiveness
    )
    
    Timber.d(
        "EvolutionEngine: Applied environmental constraints - " +
        "allowMods=$allowRuleModification, " +
        "allowExplore=$allowExploration, " +
        "aggressiveness=%.2f".format(aggressiveness)
    )
    
    return adjustedFeedback
}

/**
 * Check if evolution should be throttled based on environment
 */
fun shouldThrottleEvolution(context: EnvironmentAwareReasoningContext): Boolean {
    // Throttle in high-pressure situations
    if (context.isHighPressureEnvironment) return true
    
    // Throttle during intense user activity
    if (context.environment.userActivityLevel == UserActivityLevel.INTENSE) return true
    
    // Throttle if battery critical
    if (context.shouldConserveEnergy) return true
    
    return false
}
