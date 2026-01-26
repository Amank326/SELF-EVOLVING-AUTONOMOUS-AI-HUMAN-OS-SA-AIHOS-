package com.aihos.ai.reasoning

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.exp

/**
 * Reasoning Engine - Core inference and decision logic
 * Implements:
 * - Pattern recognition and matching
 * - Confidence scoring and uncertainty quantification
 * - Multi-step reasoning chains
 * - Belief propagation and Bayesian inference
 */

data class Belief(
    val id: String = java.util.UUID.randomUUID().toString(),
    val statement: String,
    val confidence: Float = 0.5f,
    val evidence: List<String> = emptyList(),
    val sources: List<String> = emptyList()
)

data class InferenceChain(
    val id: String = java.util.UUID.randomUUID().toString(),
    val premise: List<String> = emptyList(),
    val conclusion: String = "",
    val confidence: Float = 0.5f,
    val steps: List<String> = emptyList(),
    val reasoning_type: String = "deductive" // deductive, inductive, abductive
)

data class PatternMatch(
    val patternId: String,
    val matchedElements: List<String>,
    val matchStrength: Float,
    val relevantVariables: Map<String, Any> = emptyMap()
)

data class ReasoningState(
    val activeBeliefs: List<Belief> = emptyList(),
    val inferenceChains: List<InferenceChain> = emptyList(),
    val confidence: Float = 0.5f,
    val complexity: Float = 0.5f,
    val processingTime: Long = 0L
)

/**
 * Advanced Reasoning Engine
 */
class ReasoningEngine {
    private val _beliefs = MutableStateFlow<List<Belief>>(emptyList())
    val beliefs: StateFlow<List<Belief>> = _beliefs
    
    private val _inferenceChains = MutableStateFlow<List<InferenceChain>>(emptyList())
    val inferenceChains: StateFlow<List<InferenceChain>> = _inferenceChains
    
    private val _confidence = MutableStateFlow(0.5f)
    val confidence: StateFlow<Float> = _confidence
    
    private val _complexity = MutableStateFlow(0.5f)
    val complexity: StateFlow<Float> = _complexity
    
    private val _reasoningState = MutableStateFlow(ReasoningState())
    val reasoningState: StateFlow<ReasoningState> = _reasoningState
    
    // Pattern database
    private val patterns = mutableListOf<String>()
    
    // Confidence threshold for action
    private val MIN_CONFIDENCE = 0.3f
    private val HIGH_CONFIDENCE = 0.8f

    /**
     * Add belief to belief set
     */
    suspend fun addBelief(statement: String, confidence: Float, evidence: List<String> = emptyList()) {
        val belief = Belief(
            statement = statement,
            confidence = confidence.coerceIn(0f, 1f),
            evidence = evidence
        )
        val current = _beliefs.value.toMutableList()
        current.add(belief)
        _beliefs.emit(current)
        updateReasoningMetrics()
    }

    /**
     * Perform deductive reasoning: If A→B and A is true, then B
     */
    suspend fun deductiveReasoning(premises: List<String>, conclusion: String): InferenceChain? {
        val startTime = System.currentTimeMillis()
        
        val matchedPremises = _beliefs.value.filter { premises.contains(it.statement) }
        if (matchedPremises.isEmpty()) return null
        
        val avgConfidence = matchedPremises.map { it.confidence }.average().toFloat()
        
        // Deductive confidence is product of all premise confidences
        val deductiveConfidence = if (matchedPremises.size > 1) {
            matchedPremises.map { it.confidence }.reduce { a, b -> a * b }
        } else {
            avgConfidence
        }
        
        val chain = InferenceChain(
            premise = premises,
            conclusion = conclusion,
            confidence = deductiveConfidence,
            steps = listOf("Premise matching", "Confidence calculation", "Conclusion derivation"),
            reasoning_type = "deductive"
        )
        
        val current = _inferenceChains.value.toMutableList()
        current.add(chain)
        _inferenceChains.emit(current)
        
        val processingTime = System.currentTimeMillis() - startTime
        updateComplexity(processingTime)
        
        return chain
    }

    /**
     * Inductive reasoning: Multiple observations → General pattern
     */
    suspend fun inductiveReasoning(observations: List<String>): InferenceChain? {
        val startTime = System.currentTimeMillis()
        
        // Match observations against beliefs
        val relevantBeliefs = _beliefs.value.filter { belief ->
            observations.any { belief.statement.contains(it, ignoreCase = true) }
        }
        
        if (relevantBeliefs.size < 2) return null
        
        val pattern = "Pattern_${observations.hashCode()}"
        val inductiveConfidence = relevantBeliefs.map { it.confidence }.average().toFloat() * 0.9f // Slightly lower for induction
        
        val chain = InferenceChain(
            premise = observations,
            conclusion = "General pattern: $pattern",
            confidence = inductiveConfidence,
            steps = listOf("Observation collection", "Pattern detection", "Generalization"),
            reasoning_type = "inductive"
        )
        
        val current = _inferenceChains.value.toMutableList()
        current.add(chain)
        _inferenceChains.emit(current)
        
        val processingTime = System.currentTimeMillis() - startTime
        updateComplexity(processingTime)
        
        return chain
    }

    /**
     * Abductive reasoning: Best explanation for observations
     */
    suspend fun abductiveReasoning(observations: List<String>, possibleExplanations: List<String>): String? {
        // Find explanation with highest confidence match
        val explanationScores = possibleExplanations.map { explanation ->
            val matchingBeliefs = _beliefs.value.filter { belief ->
                explanation.contains(belief.statement, ignoreCase = true)
            }
            val score = matchingBeliefs.map { it.confidence }.average()
            explanation to score
        }
        
        val bestExplanation = explanationScores.maxByOrNull { it.second }?.first
        
        if (bestExplanation != null) {
            val confidence = explanationScores.find { it.first == bestExplanation }?.second?.toFloat() ?: 0.5f
            addBelief("Abductive conclusion: $bestExplanation", confidence, observations)
        }
        
        return bestExplanation
    }

    /**
     * Pattern matching - Find similar patterns
     */
    suspend fun matchPatterns(query: String): List<PatternMatch> {
        val matches = mutableListOf<PatternMatch>()
        
        val relevantChains = _inferenceChains.value.filter { chain ->
            chain.premise.any { it.contains(query, ignoreCase = true) }
        }
        
        for (chain in relevantChains) {
            val matchStrength = chain.confidence * (relevantChains.size / maxOf(_inferenceChains.value.size, 1).toFloat())
            matches.add(
                PatternMatch(
                    patternId = chain.id,
                    matchedElements = chain.premise,
                    matchStrength = matchStrength.coerceIn(0f, 1f)
                )
            )
        }
        
        return matches.sortedByDescending { it.matchStrength }
    }

    /**
     * Confidence propagation through belief network
     */
    suspend fun propagateConfidence(): Float {
        val beliefs = _beliefs.value
        if (beliefs.isEmpty()) return 0.5f
        
        val weights = beliefs.mapIndexed { index, belief ->
            belief.confidence * exp(-index.toFloat() / beliefs.size)
        }
        
        val propagatedConfidence = weights.sum() / maxOf(weights.size, 1)
        _confidence.emit(propagatedConfidence.toFloat())
        
        return propagatedConfidence.toFloat()
    }

    /**
     * Get reasoning summary
     */
    fun getReasoningSummary(): ReasoningState {
        return ReasoningState(
            activeBeliefs = _beliefs.value,
            inferenceChains = _inferenceChains.value,
            confidence = _confidence.value,
            complexity = _complexity.value
        )
    }

    /**
     * Should act based on confidence level
     */
    fun shouldAct(): Boolean {
        return _confidence.value >= MIN_CONFIDENCE
    }

    /**
     * Is high confidence in decision
     */
    fun isHighConfidence(): Boolean {
        return _confidence.value >= HIGH_CONFIDENCE
    }

    /**
     * Update complexity based on reasoning load
     */
    private suspend fun updateComplexity(processingTime: Long) {
        val timeComplexity = (processingTime / 1000f).coerceIn(0f, 1f)
        val beliefComplexity = (_beliefs.value.size / 100f).coerceIn(0f, 1f)
        val chainComplexity = (_inferenceChains.value.size / 50f).coerceIn(0f, 1f)
        
        val avgComplexity = (timeComplexity + beliefComplexity + chainComplexity) / 3f
        _complexity.emit(avgComplexity)
    }

    /**
     * Update overall reasoning metrics
     */
    private suspend fun updateReasoningMetrics() {
        propagateConfidence()
        updateComplexity(0)
        
        val newState = ReasoningState(
            activeBeliefs = _beliefs.value,
            inferenceChains = _inferenceChains.value,
            confidence = _confidence.value,
            complexity = _complexity.value
        )
        _reasoningState.emit(newState)
    }

    /**
     * Clear reasoning state
     */
    suspend fun reset() {
        _beliefs.emit(emptyList())
        _inferenceChains.emit(emptyList())
        _confidence.emit(0.5f)
        _complexity.emit(0.5f)
    }

    /**
     * Get reasoning metrics
     */
    fun getMetrics(): Map<String, Any> {
        return mapOf(
            "beliefCount" to _beliefs.value.size,
            "inferenceChainCount" to _inferenceChains.value.size,
            "confidence" to _confidence.value,
            "complexity" to _complexity.value,
            "shouldAct" to shouldAct(),
            "highConfidence" to isHighConfidence()
        )
    }
}
