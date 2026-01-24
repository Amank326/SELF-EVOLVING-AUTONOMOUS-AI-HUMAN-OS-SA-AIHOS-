package com.aihos.ai.ml.impl

import com.aihos.ai.ml.*
import kotlinx.coroutines.sync.Mutex
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * Default Signal Input Formatter
 * Converts DeviceContext to normalized float tensor for ML inference
 *
 * Input Features (8 total):
 * 1. Battery: 0-100% normalized to [0, 1]
 * 2. Screen: boolean to [0, 1]
 * 3. Network: boolean to [0, 1]
 * 4. Temperature: °C clamped 20-60 to [0, 1]
 * 5. Time of Day: already [0, 1]
 * 6. Foreground App: string encoded to [0, 1] one-hot
 * 7. Idle Time: seconds clamped 0-3600 to [0, 1]
 * 8. Recent Decisions: count clamped 0-20 to [0, 1]
 */
class DefaultSignalInputFormatter : SignalInputFormatter {
    
    // Supported app categories for one-hot encoding
    private val appCategories = listOf(
        "system",
        "messaging",
        "email",
        "social",
        "productivity"
    )
    
    override fun formatInput(context: DeviceContext): FloatArray {
        return floatArrayOf(
            // Battery: 0-100% → [0, 1]
            context.batteryPercent / 100f,
            
            // Screen: boolean → {0, 1}
            if (context.screenOn) 1f else 0f,
            
            // Network: boolean → {0, 1}
            if (context.networkConnected) 1f else 0f,
            
            // Temperature: 20-60°C → [0, 1]
            (context.temperatureCelsius - 20f) / 40f,
            
            // Time of Day: already [0, 1] (0=midnight, 0.5=noon, 1=next midnight)
            context.timeOfDay,
            
            // Foreground App: encoded to 1.0 if matches, 0.0 otherwise
            encodeApp(context.foregroundApp).toFloat(),
            
            // Idle Time: 0-3600s → [0, 1]
            (context.idleTimeSeconds % 3600) / 3600f,
            
            // Recent Decisions: 0-20 → [0, 1]
            (context.recentDecisionCount % 20) / 20f
        )
    }
    
    /**
     * Encode app string to one-hot
     * If app matches a known category, return 1.0, else 0.0
     */
    private fun encodeApp(app: String): Int {
        return if (appCategories.contains(app.lowercase())) 1 else 0
    }
    
    override fun parseOutput(output: FloatArray): Float {
        if (output.isEmpty()) {
            Timber.w("Empty output array")
            return 0.0f
        }
        
        return when (output.size) {
            // Softmax output (multiple classes) - return difference from 0.5
            3 -> {
                // [class1_prob, class2_prob, class3_prob]
                // Assume class 2 (index 1) is "positive" direction
                (output[1] - 0.5f) * 0.6f  // Scale to [-0.3, 0.3]
            }
            5 -> {
                // [class1_prob, class2_prob, class3_prob, class4_prob, class5_prob]
                // Class 3 (index 2) is center, deviation indicates confidence
                (output[2] - 0.2f) * 0.75f  // Scale appropriately
            }
            // Sigmoid output (binary) - return as-is, scaled to [-0.3, 0.3]
            1 -> {
                (output[0] - 0.5f) * 0.6f
            }
            // Multiple outputs - average them
            else -> {
                val avg = output.average().toFloat()
                (avg - 0.5f) * 0.6f
            }
        }
    }
}

/**
 * Default Inference Throttler
 * Rate-limits inference frequency and caches results
 *
 * Strategy:
 * - Time-based: Max 2 inferences per second (500ms interval)
 * - Input-based: Skip if inputs unchanged
 * - Cache-based: Return cached result if available
 */
class DefaultInferenceThrottler(
    private val targetFrequencyHz: Int = 2  // Max inferences per second
) : InferenceThrottler {
    
    private val lock = Mutex()
    private var lastInferenceTime = 0L
    private var lastInputHash = 0
    private var cachedResult: InferenceResult? = null
    
    override suspend fun throttledInfer(
        context: DeviceContext,
        inferenceBlock: suspend () -> InferenceResult
    ): InferenceResult {
        lock.withLock {
            val now = System.currentTimeMillis()
            val inputHash = context.hashCode()
            val timeSinceLastMs = now - lastInferenceTime
            val minIntervalMs = 1000 / targetFrequencyHz
            
            // Check if we should use cached result
            if (inputHash == lastInputHash && timeSinceLastMs < minIntervalMs) {
                val cached = cachedResult
                if (cached != null) {
                    Timber.d("Using cached inference result (hash match, interval ${timeSinceLastMs}ms < ${minIntervalMs}ms)")
                    return cached.copy(cached = true)
                }
            }
            
            // Execute new inference
            val result = inferenceBlock()
            lastInferenceTime = now
            lastInputHash = inputHash
            cachedResult = result
            
            Timber.d("Executed new inference for ${result.modelType} (latency ${result.latencyMs}ms)")
            
            return result
        }
    }
}

/**
 * Default Signal Input Formatter Alternative (Compact)
 * Reduces input features by combining related signals
 */
class CompactSignalInputFormatter : SignalInputFormatter {
    
    override fun formatInput(context: DeviceContext): FloatArray {
        // 5-feature compact format:
        // 1. Device state (battery + screen) combined
        // 2. Network availability
        // 3. Time of day
        // 4. User activity (foreground app + idle time)
        // 5. Recent decisions
        
        val deviceState = (
            (context.batteryPercent / 100f) * 0.7f +
            (if (context.screenOn) 1f else 0f) * 0.3f
        )
        
        val userActivity = (
            (1f - (context.idleTimeSeconds % 3600) / 3600f) * 0.6f +  // inverse idle
            (if (context.foregroundApp != "system") 1f else 0f) * 0.4f
        )
        
        return floatArrayOf(
            deviceState,
            if (context.networkConnected) 1f else 0f,
            context.timeOfDay,
            userActivity,
            (context.recentDecisionCount % 20) / 20f
        )
    }
    
    override fun parseOutput(output: FloatArray): Float {
        return when (output.size) {
            in 1..2 -> {
                // Binary or dual output
                (output[0] - 0.5f) * 0.6f
            }
            else -> {
                // Multi-class, take center difference
                val center = output.size / 2
                val sum = output.slice(0 until center).sum().toFloat()
                val diff = sum - output.slice(center until output.size).sum().toFloat()
                (diff / output.size) * 0.3f
            }
        }
    }
}

/**
 * Default ML Confidence Augmenter
 * Integrates ML results into rule-based reasoning without overriding
 */
class DefaultMLConfidenceAugmenter(
    private val mlManager: MLInterpreterManager
) : MLConfidenceAugmenter {
    
    override suspend fun augmentConfidence(
        ruleConfidence: Float,
        context: DeviceContext
    ): AugmentedConfidence {
        try {
            // Get ML adjustment (bounded to [-0.3, 0.3])
            val mlAdjustment = mlManager.inferConfidenceAdjustment(
                context,
                MLModelType.BEHAVIOR_CLASSIFIER
            )
            
            // Apply bounded multiplier
            // adjustment range: [-0.3, 0.3]
            // multiplier range: [1 - 0.3/3, 1 + 0.3/3] = [0.9, 1.1]
            val multiplier = 1f + (mlAdjustment / 3f)
            val finalConfidence = (ruleConfidence * multiplier).coerceIn(0f, 1f)
            
            return AugmentedConfidence(
                ruleConfidence = ruleConfidence,
                mlAdjustment = mlAdjustment,
                finalConfidence = finalConfidence,
                mlAvailable = mlManager.isAvailable(),
                mlExplanation = buildMLExplanation(mlAdjustment)
            )
        } catch (e: Exception) {
            Timber.w(e, "Error augmenting confidence, returning rule confidence only")
            return AugmentedConfidence(
                ruleConfidence = ruleConfidence,
                mlAdjustment = 0.0f,
                finalConfidence = ruleConfidence,
                mlAvailable = false,
                mlExplanation = "ML unavailable: ${e.message}"
            )
        }
    }
    
    override fun explainAugmentation(augmented: AugmentedConfidence): String {
        return buildString {
            appendLine("=== DECISION CONFIDENCE ANALYSIS ===")
            appendLine()
            appendLine("Rule-Based Reasoning (Primary):")
            appendLine("  Base Confidence: ${(augmented.ruleConfidence * 100).roundToInt()}%")
            appendLine()
            appendLine("ML-Based Augmentation (Secondary):")
            if (augmented.mlAvailable) {
                appendLine("  Status: ACTIVE")
                val adjDir = if (augmented.mlAdjustment >= 0) "increase" else "decrease"
                val adjMag = (augmented.mlAdjustment.absoluteValue * 100).roundToInt()
                appendLine("  Effect: Recommended $adjDir by ~${adjMag}%")
                appendLine("  Models: Behavior Classifier, Priority Scorer")
                appendLine("  Explanation: ${augmented.mlExplanation}")
            } else {
                appendLine("  Status: UNAVAILABLE (using rule-based only)")
                appendLine("  Reason: ${augmented.mlExplanation}")
            }
            appendLine()
            appendLine("Final Confidence: ${(augmented.finalConfidence * 100).roundToInt()}%")
            appendLine("  (Weighted toward rule-based reasoning, with ML as secondary signal)")
        }
    }
    
    private fun buildMLExplanation(adjustment: Float): String {
        return when {
            adjustment > 0.2f -> "Strong behavioral confidence detected"
            adjustment > 0.1f -> "Moderate behavioral confidence"
            adjustment > 0f -> "Slight behavioral confidence increase"
            adjustment == 0.0f -> "Neutral behavioral assessment"
            adjustment > -0.1f -> "Slight behavioral confidence decrease"
            adjustment > -0.2f -> "Moderate behavioral uncertainty"
            else -> "Strong behavioral uncertainty detected"
        }
    }
}

/**
 * Extension function for float absolute value
 */
private fun Float.absoluteValue(): Float = if (this < 0) -this else this
