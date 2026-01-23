package com.aihos.ai.reasoning

import kotlinx.serialization.Serializable

/**
 * Core abstraction for Reasoning Layer
 * Implements logical inference, decision-making, and problem-solving
 * Supports both symbolic and sub-symbolic reasoning
 */
interface ReasoningLayer {
    
    /**
     * Perform logical inference based on premises
     * @param premises The input facts and rules
     * @return Inferred conclusions
     */
    suspend fun infer(premises: List<String>): ReasoningResult
    
    /**
     * Evaluate a hypothesis against available knowledge
     * @param hypothesis The hypothesis to evaluate
     * @return Evaluation result with confidence score
     */
    suspend fun evaluateHypothesis(hypothesis: String): HypothesisEvaluation
    
    /**
     * Generate multiple solutions for a problem
     * @param problem The problem statement
     * @param maxSolutions Maximum number of solutions to generate
     * @return List of possible solutions with confidence scores
     */
    suspend fun generateSolutions(problem: String, maxSolutions: Int = 5): List<Solution>
    
    /**
     * Make a decision based on available information
     * @param options The decision options
     * @param context Additional context for decision
     * @return Selected option with reasoning explanation
     */
    suspend fun makeDecision(options: List<String>, context: String = ""): DecisionResult
    
    /**
     * Explain the reasoning chain for a conclusion
     * @param conclusion The conclusion to explain
     * @return Chain of reasoning steps
     */
    suspend fun explainReasoning(conclusion: String): List<ReasoningStep>
}

/**
 * Result of a reasoning inference operation
 */
@Serializable
data class ReasoningResult(
    val conclusions: List<String> = emptyList(),
    val confidenceScores: List<Float> = emptyList(),
    val reasoning: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Evaluation of a hypothesis
 */
@Serializable
data class HypothesisEvaluation(
    val hypothesis: String = "",
    val confidence: Float = 0f,
    val supportingEvidence: List<String> = emptyList(),
    val contradictions: List<String> = emptyList(),
    val recommendation: String = ""
)

/**
 * A potential solution to a problem
 */
@Serializable
data class Solution(
    val description: String = "",
    val confidence: Float = 0f,
    val steps: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val risks: List<String> = emptyList()
)

/**
 * Result of a decision-making operation
 */
@Serializable
data class DecisionResult(
    val selectedOption: String = "",
    val confidence: Float = 0f,
    val reasoning: String = "",
    val alternatives: List<String> = emptyList(),
    val expectedOutcome: String = ""
)

/**
 * A single step in a reasoning chain
 */
@Serializable
data class ReasoningStep(
    val step: Int = 0,
    val statement: String = "",
    val basis: String = "",
    val confidence: Float = 0f
)
