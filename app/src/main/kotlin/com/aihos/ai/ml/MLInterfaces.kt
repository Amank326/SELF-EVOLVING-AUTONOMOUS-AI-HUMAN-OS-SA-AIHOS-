package com.aihos.ai.ml

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * ML Model type enumeration
 * Defines available on-device ML models for confidence augmentation
 */
enum class MLModelType {
    BEHAVIOR_CLASSIFIER,      // Predicts user behavior class (ACTIVE, PASSIVE, TRANSITIONING)
    PRIORITY_SCORER,          // Scores decision priority (CRITICAL, HIGH, NORMAL, LOW, DEFER)
    MEMORY_CONFIDENCE         // Assesses decision memorability (MEMORABLE, FORGETTABLE)
}

/**
 * ML Inference status
 */
enum class MLManagerStatus {
    UNINITIALIZED,  // Before ON_START
    LOADING,        // Models loading from disk
    READY,          // All interpreters initialized, ready for inference
    INFERRING,      // Inference in progress
    IDLE,           // Awaiting inference request
    ERROR,          // Unrecoverable error occurred
    SHUTDOWN        // Cleanup completed
}

/**
 * Result of single inference execution
 * Represents ML confidence adjustment to rule-based decision
 */
@Serializable
data class InferenceResult(
    val confidence: Float,  // -0.3 to +0.3 (adjustment factor)
    val modelType: String = "unknown",
    val latencyMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val cached: Boolean = false,      // True if from cache (didn't execute)
    val fallback: Boolean = false,    // True if ML unavailable, returned neutral
    val error: String? = null         // Error message if failed
)

/**
 * ML System metrics
 * Tracks performance and health of ML subsystem
 */
@Serializable
data class MLMetrics(
    val inferencesExecuted: Long = 0L,
    val inferencesFromCache: Long = 0L,
    val inferencesFailedTotal: Long = 0L,
    val averageLatencyMs: Float = 0f,
    val peakMemoryUsageMb: Float = 0f,
    val modelsLoaded: Int = 0,
    val lastErrorMessage: String? = null,
    val uptimeMs: Long = 0L
)

/**
 * ML Interpreter Manager
 * Lifecycle-aware coordinator for TensorFlow Lite interpreters
 *
 * Responsibilities:
 * - Manage interpreter lifecycle (init on ON_START, cleanup on ON_STOP)
 * - Provide thread-safe async inference access
 * - Implement graceful degradation on errors
 * - Track metrics and health
 * - Prevent memory leaks
 */
interface MLInterpreterManager : LifecycleEventObserver {
    
    /**
     * Execute inference and get confidence adjustment
     * Safe to call from any thread
     * Dispatches to IO thread pool (Dispatcher.Default)
     */
    suspend fun inferConfidenceAdjustment(
        context: DeviceContext,
        modelType: MLModelType
    ): Float  // Returns -0.3 to +0.3
    
    /**
     * Get current manager status
     */
    fun getStatus(): MLManagerStatus
    
    /**
     * Check if ML is currently available and ready
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * Get current metrics
     */
    fun getMetrics(): MLMetrics
    
    /**
     * Observable status flow
     */
    val statusFlow: StateFlow<MLManagerStatus>
}

/**
 * Individual TensorFlow Lite interpreter provider
 * Encapsulates single model's interpreter, tensor handling, execution
 */
interface TFLiteInterpreterProvider {
    
    /**
     * Model metadata
     */
    val modelMetadata: ModelMetadata
    
    /**
     * Initialize interpreter
     * Can throw IOException if model not found
     */
    suspend fun initialize(): Boolean
    
    /**
     * Execute inference
     * Returns null if failed
     */
    suspend fun infer(
        input: FloatArray,
        timeoutMs: Long = 100L
    ): FloatArray?
    
    /**
     * Clean up resources
     * Safe to call multiple times
     */
    suspend fun cleanup()
    
    /**
     * Check if interpreter ready
     */
    fun isInitialized(): Boolean
}

/**
 * Model metadata
 */
@Serializable
data class ModelMetadata(
    val type: String,               // "BEHAVIOR_CLASSIFIER", etc.
    val version: String,            // "1.0", "1.5", etc.
    val filePath: String,           // "models/behavior_classifier_v1.tflite"
    val inputShape: List<Int>,      // [1, 8] for batch_size=1, 8 features
    val outputSize: Int,            // Number of output values
    val typicalInferenceTimeMs: Int // Expected latency
)

/**
 * Device context provided by system signals
 * Input features for ML inference
 */
@Serializable
data class DeviceContext(
    val batteryPercent: Int,           // 0-100
    val screenOn: Boolean,
    val networkConnected: Boolean,
    val temperatureCelsius: Float,     // 20-60 typical
    val timeOfDay: Float,              // 0.0-1.0 (0=midnight, 0.5=noon)
    val foregroundApp: String,         // "system", "messaging", "email", etc.
    val idleTimeSeconds: Long,         // Time since last user interaction
    val recentDecisionCount: Int,      // Decisions in last hour
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Signal input formatter
 * Converts DeviceContext to normalized tensor input
 */
interface SignalInputFormatter {
    
    /**
     * Format device context to input tensor
     */
    fun formatInput(context: DeviceContext): FloatArray
    
    /**
     * Reverse: parse tensor output to confidence
     */
    fun parseOutput(output: FloatArray): Float
}

/**
 * Inference throttler
 * Rate-limits inference frequency, caches results
 */
interface InferenceThrottler {
    
    /**
     * Execute inference with throttling
     */
    suspend fun throttledInfer(
        context: DeviceContext,
        inferenceBlock: suspend () -> InferenceResult
    ): InferenceResult
}

/**
 * ML Confidence Augmenter
 * Integrates ML results into rule-based reasoning
 */
interface MLConfidenceAugmenter {
    
    /**
     * Augment rule confidence with ML adjustment
     */
    suspend fun augmentConfidence(
        ruleConfidence: Float,
        context: DeviceContext
    ): AugmentedConfidence
    
    /**
     * Explain augmentation (both rule and ML reasoning)
     */
    fun explainAugmentation(augmented: AugmentedConfidence): String
}

/**
 * Augmented confidence result
 */
@Serializable
data class AugmentedConfidence(
    val ruleConfidence: Float,      // Primary: from rule engine
    val mlAdjustment: Float,        // Secondary: from ML models
    val finalConfidence: Float,     // Combined result
    val mlAvailable: Boolean,       // Was ML available?
    val mlExplanation: String = ""  // Reasoning from ML side
)
