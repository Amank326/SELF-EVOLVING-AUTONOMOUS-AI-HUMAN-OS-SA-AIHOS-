package com.aihos.ai.ensemble

import kotlin.math.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * DISTRIBUTED ENSEMBLE ENGINE
 * Multi-model voting system with adaptive weighting
 * Combines Neural Networks, PSO, ACO, QGA, and Classical AI
 * for super-intelligent decision making
 */

data class ModelPrediction(
    val modelId: String = "",
    val prediction: Float = 0.5f,
    val confidence: Float = 0.5f,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float = 0.5f
)

data class EnsembleDecision(
    val finalDecision: Float = 0.5f,
    val confidence: Float = 0.5f,
    val modelAgreement: Float = 0.5f,
    val disagreementDetected: Boolean = false,
    val topContributors: List<Pair<String, Float>> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

class EnsembleEngine {
    // Models
    private val modelWeights = mutableMapOf<String, Float>()
    private val modelHistories = mutableMapOf<String, MutableList<ModelPrediction>>()
    private val modelAccuracies = mutableMapOf<String, Float>()

    // State
    private val _lastDecision = MutableStateFlow(EnsembleDecision())
    val lastDecision: StateFlow<EnsembleDecision> = _lastDecision

    private val _ensembleMetrics = MutableStateFlow(mapOf<String, Float>())
    val ensembleMetrics: StateFlow<Map<String, Float>> = _ensembleMetrics

    private var decisionCount = 0

    init {
        // Initialize models
        initializeModels()
    }

    /**
     * Combine predictions from multiple models
     */
    suspend fun combinePredictions(predictions: List<ModelPrediction>): EnsembleDecision {
        decisionCount++

        // Store predictions
        predictions.forEach { pred ->
            modelHistories.getOrPut(pred.modelId) { mutableListOf() }.apply {
                add(pred)
                if (size > 100) removeAt(0) // Keep last 100
            }
        }

        // Calculate weighted ensemble decision
        val totalWeight = modelWeights.values.sum()
        val weightedSum = predictions.sumOf { pred ->
            val weight = modelWeights[pred.modelId] ?: 0.5f
            (pred.prediction * weight).toDouble()
        }.toFloat()
        val finalDecision = if (totalWeight > 0) weightedSum / totalWeight else 0.5f

        // Calculate confidence
        val confidence = predictions.map { it.confidence }.average().toFloat()

        // Calculate model agreement
        val agreement = calculateModelAgreement(predictions)

        // Detect disagreement
        val disagreement = agreement < 0.6f

        // Get top contributors
        val topContributors = predictions
            .sortedByDescending { (modelWeights[it.modelId] ?: 0.5f) * it.confidence }
            .take(3)
            .map { Pair(it.modelId, (modelWeights[it.modelId] ?: 0.5f) * it.confidence) }

        val decision = EnsembleDecision(
            finalDecision = finalDecision,
            confidence = confidence.coerceIn(0f, 1f),
            modelAgreement = agreement,
            disagreementDetected = disagreement,
            topContributors = topContributors
        )

        _lastDecision.emit(decision)

        // Update metrics
        updateMetrics(predictions, decision)

        return decision
    }

    /**
     * Update model weights based on accuracy
     */
    suspend fun updateModelAccuracy(modelId: String, accuracy: Float) {
        modelAccuracies[modelId] = accuracy

        // Adaptive weight adjustment (softmax of accuracies)
        val accuracies = modelAccuracies.values.toList()
        if (accuracies.isNotEmpty()) {
            val maxAccuracy = accuracies.maxOrNull() ?: 0.5f
            val normalizedAccuracies = accuracies.map { it / (maxAccuracy + 0.001f) }
            
            val weights = softmax(normalizedAccuracies)
            modelAccuracies.keys.forEachIndexed { idx, modelId ->
                modelWeights[modelId] = weights.getOrNull(idx) ?: 0.5f
            }
        }
    }

    /**
     * Calculate agreement between models (0-1 scale)
     */
    private fun calculateModelAgreement(predictions: List<ModelPrediction>): Float {
        if (predictions.isEmpty()) return 1f

        val mean = predictions.map { it.prediction }.average()
        val variance = predictions.map { (it.prediction - mean).pow(2) }.average()
        val stdDev = sqrt(variance).toFloat()

        // Convert stdDev to agreement (low stdDev = high agreement)
        return 1f / (1f + stdDev)
    }

    /**
     * Softmax function for weight normalization
     */
    private fun softmax(values: List<Float>): List<Float> {
        val maxVal = values.maxOrNull() ?: 0f
        val expValues = values.map { exp(it - maxVal) }
        val sum = expValues.sum()
        return if (sum > 0) expValues.map { it / sum } else List(values.size) { 1f / values.size }
    }

    /**
     * Get model weights
     */
    fun getModelWeights(): Map<String, Float> = modelWeights.toMap()

    /**
     * Get model accuracy history
     */
    fun getModelHistory(modelId: String): List<ModelPrediction> {
        return modelHistories[modelId]?.toList() ?: emptyList()
    }

    /**
     * Get ensemble statistics
     */
    fun getEnsembleStatistics(): Map<String, Any> {
        val allPredictions = modelHistories.values.flatten()
        return mapOf(
            "totalDecisions" to decisionCount,
            "totalPredictions" to allPredictions.size,
            "modelCount" to modelWeights.size,
            "averageAgreement" to (allPredictions.takeIf { it.isNotEmpty() }?.let {
                it.groupBy { it.modelId }.map { (_, preds) ->
                    preds.map { it.prediction }.average()
                }.let { means ->
                    val m = means.average()
                    1f / (1f + sqrt(means.map { (it - m).pow(2) }.average()).toFloat())
                }
            } ?: 0f),
            "modelWeights" to modelWeights
        )
    }

    // ======================== PRIVATE HELPERS ========================

    private fun initializeModels() {
        // Initialize with equal weights
        val modelIds = listOf("neural", "pso", "aco", "qga", "classical_ai")
        modelIds.forEach { modelId ->
            modelWeights[modelId] = 0.2f
            modelAccuracies[modelId] = 0.5f
        }
    }

    private suspend fun updateMetrics(predictions: List<ModelPrediction>, decision: EnsembleDecision) {
        val metrics = mutableMapOf<String, Float>()
        metrics["final_decision"] = decision.finalDecision
        metrics["confidence"] = decision.confidence
        metrics["agreement"] = decision.modelAgreement
        metrics["decision_count"] = decisionCount.toFloat()

        predictions.forEach { pred ->
            metrics["weight_${pred.modelId}"] = modelWeights[pred.modelId] ?: 0f
            metrics["accuracy_${pred.modelId}"] = modelAccuracies[pred.modelId] ?: 0f
        }

        _ensembleMetrics.emit(metrics)
    }
}
