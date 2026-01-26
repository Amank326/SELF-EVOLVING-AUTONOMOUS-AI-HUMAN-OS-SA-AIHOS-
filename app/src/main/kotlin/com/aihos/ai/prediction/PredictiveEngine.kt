package com.aihos.ai.prediction

import com.aihos.ai.neural.NeuralNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Predictive Engine - Forecasts behavior and patterns
 * Uses neural networks to predict:
 * - User behavior
 * - System performance
 * - Decision outcomes
 * - Memory consolidation timing
 */

data class Prediction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String, // "behavior", "performance", "decision", "memory"
    val prediction: Float,
    val confidence: Float,
    val actualValue: Float? = null,
    val error: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PredictionModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String,
    val accuracy: Float = 0f,
    val trainedEpochs: Int = 0,
    val isTrained: Boolean = false
)

/**
 * Predictive Engine for behavior and performance forecasting
 */
class PredictiveEngine {
    // Neural networks for different prediction tasks
    private val behaviorPredictor = NeuralNetwork(
        architecture = listOf(10, 32, 16, 5), // Input: autonomy, memory, confidence, reasoning, complexity
        activations = listOf("relu", "relu", "sigmoid")
    )

    private val performancePredictor = NeuralNetwork(
        architecture = listOf(8, 24, 12, 4), // Input: 8 metrics → predict performance scores
        activations = listOf("relu", "relu", "sigmoid")
    )

    private val decisionOutcomePredictor = NeuralNetwork(
        architecture = listOf(12, 36, 18, 5), // Input: decision context → predict outcomes
        activations = listOf("relu", "relu", "sigmoid")
    )

    private val memoryPredictor = NeuralNetwork(
        architecture = listOf(6, 16, 8, 3), // Input: memory metrics → predict consolidation timing
        activations = listOf("relu", "relu", "sigmoid")
    )

    // Tracking
    private val _predictions = MutableStateFlow<List<Prediction>>(emptyList())
    val predictions: StateFlow<List<Prediction>> = _predictions

    private val _models = MutableStateFlow<List<PredictionModel>>(emptyList())
    val models: StateFlow<List<PredictionModel>> = _models

    private val _overallAccuracy = MutableStateFlow(0f)
    val overallAccuracy: StateFlow<Float> = _overallAccuracy

    /**
     * Predict behavior based on current AI state
     */
    suspend fun predictBehavior(
        autonomyLevel: Float,
        memoryLoad: Float,
        confidence: Float,
        reasoning: Float,
        complexity: Float
    ): Prediction {
        val input = floatArrayOf(autonomyLevel, memoryLoad, confidence, reasoning, complexity, 0f, 0f, 0f, 0f, 0f)
        val output = behaviorPredictor.predict(input)
        val predictedBehavior = output[0].coerceIn(0f, 1f)

        val prediction = Prediction(
            type = "behavior",
            prediction = predictedBehavior,
            confidence = output[1].coerceIn(0f, 1f)
        )

        addPrediction(prediction)
        return prediction
    }

    /**
     * Predict next performance level
     */
    suspend fun predictPerformance(
        health: Float,
        autonomy: Float,
        memory: Float,
        reasoning: Float,
        evolution: Float,
        reflection: Float,
        learning: Float,
        efficiency: Float
    ): Prediction {
        val input = floatArrayOf(health, autonomy, memory, reasoning, evolution, reflection, learning, efficiency)
        val output = performancePredictor.predict(input)
        val performanceScore = output[0].coerceIn(0f, 1f)

        val prediction = Prediction(
            type = "performance",
            prediction = performanceScore,
            confidence = output[1].coerceIn(0f, 1f)
        )

        addPrediction(prediction)
        return prediction
    }

    /**
     * Predict decision outcome
     */
    suspend fun predictDecisionOutcome(
        decisionContext: String,
        optionCount: Int,
        confidence: Float,
        pastSuccessRate: Float,
        complexity: Float,
        timesPredicted: Int,
        riskLevel: Float,
        learningRate: Float,
        adaptability: Float,
        historicalAccuracy: Float,
        similarPastOutcomes: Int,
        contextRelevance: Float
    ): Prediction {
        val input = floatArrayOf(
            optionCount / 10f,
            confidence,
            pastSuccessRate,
            complexity,
            timesPredicted / 100f,
            riskLevel,
            learningRate,
            adaptability,
            historicalAccuracy,
            similarPastOutcomes / 10f,
            contextRelevance,
            decisionContext.hashCode() / 1000000f
        )
        val output = decisionOutcomePredictor.predict(input)
        val outcomeScore = output[0].coerceIn(0f, 1f)

        val prediction = Prediction(
            type = "decision",
            prediction = outcomeScore,
            confidence = output[1].coerceIn(0f, 1f)
        )

        addPrediction(prediction)
        return prediction
    }

    /**
     * Predict memory consolidation timing
     */
    suspend fun predictConsolidationTiming(
        memoryLoad: Float,
        episodicCount: Int,
        consolidationProgress: Float,
        lastConsolidationTime: Long,
        importanceAverage: Float
    ): Prediction {
        val timeSinceLastConsolidation = (System.currentTimeMillis() - lastConsolidationTime) / 1000f / 60f // minutes
        val input = floatArrayOf(
            memoryLoad,
            episodicCount / 100f,
            consolidationProgress,
            minOf(timeSinceLastConsolidation / 60f, 10f), // cap at 10 hours
            importanceAverage
        )
        val output = memoryPredictor.predict(input)
        val consolidationLikelihood = output[0].coerceIn(0f, 1f)

        val prediction = Prediction(
            type = "memory",
            prediction = consolidationLikelihood,
            confidence = output[1].coerceIn(0f, 1f)
        )

        addPrediction(prediction)
        return prediction
    }

    /**
     * Train predictive models with historical data
     */
    suspend fun trainModels(
        behaviorData: List<Pair<FloatArray, FloatArray>>? = null,
        performanceData: List<Pair<FloatArray, FloatArray>>? = null,
        decisionData: List<Pair<FloatArray, FloatArray>>? = null,
        memoryData: List<Pair<FloatArray, FloatArray>>? = null
    ) {
        // Generate synthetic training data if not provided
        if (behaviorData != null) {
            behaviorPredictor.train(
                behaviorData.map { it.first },
                behaviorData.map { it.second },
                epochs = 50
            )
        }

        if (performanceData != null) {
            performancePredictor.train(
                performanceData.map { it.first },
                performanceData.map { it.second },
                epochs = 50
            )
        }

        if (decisionData != null) {
            decisionOutcomePredictor.train(
                decisionData.map { it.first },
                decisionData.map { it.second },
                epochs = 50
            )
        }

        if (memoryData != null) {
            memoryPredictor.train(
                memoryData.map { it.first },
                memoryData.map { it.second },
                epochs = 50
            )
        }

        updateModelStats()
    }

    /**
     * Record actual outcome and update prediction accuracy
     */
    suspend fun recordOutcome(predictionId: String, actualValue: Float) {
        val current = _predictions.value.toMutableList()
        val index = current.indexOfFirst { it.id == predictionId }
        if (index >= 0) {
            val prediction = current[index]
            val error = kotlin.math.abs(prediction.prediction - actualValue)
            current[index] = prediction.copy(
                actualValue = actualValue,
                error = error
            )
            _predictions.emit(current)
            updateAccuracy()
        }
    }

    /**
     * Get predictions by type
     */
    fun getPredictionsByType(type: String): List<Prediction> {
        return _predictions.value.filter { it.type == type }
    }

    /**
     * Get recent predictions
     */
    fun getRecentPredictions(count: Int = 10): List<Prediction> {
        return _predictions.value.sortedByDescending { it.timestamp }.take(count)
    }

    /**
     * Get prediction accuracy for type
     */
    fun getTypeAccuracy(type: String): Float {
        val typePredictions = getPredictionsByType(type).filter { it.error != null }
        if (typePredictions.isEmpty()) return 0f

        val mse = typePredictions.map { (it.error ?: 0f).pow(2) }.average().toFloat()
        return 1f / (1f + sqrt(mse))
    }

    /**
     * Should trust prediction
     */
    fun shouldTrustPrediction(prediction: Prediction): Boolean {
        return prediction.confidence > 0.7f
    }

    /**
     * Get prediction summary
     */
    fun getPredictionSummary(): Map<String, Any> {
        return mapOf(
            "totalPredictions" to _predictions.value.size,
            "overallAccuracy" to _overallAccuracy.value,
            "behaviorAccuracy" to getTypeAccuracy("behavior"),
            "performanceAccuracy" to getTypeAccuracy("performance"),
            "decisionAccuracy" to getTypeAccuracy("decision"),
            "memoryAccuracy" to getTypeAccuracy("memory"),
            "recentPredictions" to getRecentPredictions(5),
            "modelAccuracies" to _models.value.associate { it.type to it.accuracy }
        )
    }

    /**
     * Add prediction to history
     */
    private suspend fun addPrediction(prediction: Prediction) {
        val current = _predictions.value.toMutableList()
        current.add(prediction)
        if (current.size > 1000) {
            current.drop(current.size - 1000)
        }
        _predictions.emit(current)
    }

    /**
     * Update model statistics
     */
    private suspend fun updateModelStats() {
        val models = listOf(
            PredictionModel("behavior", "behavior", behaviorPredictor.accuracy.value, 50, true),
            PredictionModel("performance", "performance", performancePredictor.accuracy.value, 50, true),
            PredictionModel("decision", "decision", decisionOutcomePredictor.accuracy.value, 50, true),
            PredictionModel("memory", "memory", memoryPredictor.accuracy.value, 50, true)
        )
        _models.emit(models)
    }

    /**
     * Update overall accuracy
     */
    private suspend fun updateAccuracy() {
        val predictions = _predictions.value.filter { it.error != null }
        if (predictions.isEmpty()) return

        val mse = predictions.map { (it.error ?: 0f).pow(2) }.average().toFloat()
        val accuracy = 1f / (1f + sqrt(mse))
        _overallAccuracy.emit(accuracy.coerceIn(0f, 1f))
    }

    /**
     * Clear predictions
     */
    suspend fun reset() {
        _predictions.emit(emptyList())
        _models.emit(emptyList())
        _overallAccuracy.emit(0f)
        behaviorPredictor.reset()
        performancePredictor.reset()
        decisionOutcomePredictor.reset()
        memoryPredictor.reset()
    }
}
