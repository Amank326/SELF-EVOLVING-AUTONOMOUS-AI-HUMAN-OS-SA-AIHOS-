package com.aihos.domain.use_case

import com.aihos.domain.model.CognitiveState
import com.aihos.domain.model.DecisionOutcome
import com.aihos.domain.model.DecisionRecord
import com.aihos.domain.model.ExecutionPhase
import com.aihos.domain.model.LearnedRule
import com.aihos.domain.model.ReflectionData
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case: Orchestrate a complete AI cognition cycle.
 * Coordinates Think → Act → Reflect → Evolve.
 * 
 * This interface separates the AI logic from the presentation layer.
 * No Android dependencies.
 */
interface AIBrainUseCase {
    /**
     * Current cognitive state, exposed as an observable flow.
     */
    val cognitiveState: StateFlow<CognitiveState>
    
    /**
     * Start the cognitive loop.
     */
    suspend fun start()
    
    /**
     * Pause the cognitive loop.
     */
    suspend fun pause()
    
    /**
     * Resume the cognitive loop.
     */
    suspend fun resume()
    
    /**
     * Stop the cognitive loop completely.
     */
    suspend fun stop()
    
    /**
     * Report the outcome of a decision.
     */
    suspend fun reportOutcome(outcome: DecisionOutcome)
}

/**
 * Use case: Generate decision options based on current context.
 * Part of the THINK phase.
 */
interface DecisionGenerationUseCase {
    /**
     * Generate decision options.
     * @param context Current device and user context
     * @return List of decision options with confidence scores
     */
    suspend fun generateOptions(context: String): List<String>
}

/**
 * Use case: Reflect on outcomes and extract learning.
 * Part of the REFLECT phase.
 */
interface ReflectionUseCase {
    /**
     * Analyze outcome of a decision.
     * @param decision The decision that was made
     * @param outcome The actual outcome
     * @return Reflection insights
     */
    suspend fun analyzeOutcome(
        decision: DecisionRecord,
        outcome: DecisionOutcome
    ): ReflectionData
}

/**
 * Use case: Evolve the AI system's rules.
 * Part of the EVOLVE phase.
 */
interface EvolutionUseCase {
    /**
     * Update rule confidences based on outcome.
     * @param decision The decision made
     * @param success Whether the outcome was successful
     */
    suspend fun updateRuleConfidences(
        decision: DecisionRecord,
        success: Boolean
    )
    
    /**
     * Learn new rules from successful patterns.
     */
    suspend fun learnNewRules(patterns: List<String>)
    
    /**
     * Get current rules.
     */
    suspend fun getRules(): List<LearnedRule>
}

/**
 * Use case: Query and store memory.
 */
interface MemoryUseCase {
    /**
     * Get past decisions relevant to current context.
     */
    suspend fun getRelevantMemory(context: String): List<DecisionRecord>
    
    /**
     * Store a decision in memory.
     */
    suspend fun recordDecision(decision: DecisionRecord)
    
    /**
     * Get learning history.
     */
    suspend fun getLearningHistory(): List<LearnedRule>
}
