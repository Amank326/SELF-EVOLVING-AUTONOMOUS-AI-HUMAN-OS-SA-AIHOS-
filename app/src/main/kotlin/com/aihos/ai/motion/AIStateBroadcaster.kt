package com.aihos.ai.motion

import com.aihos.ai.autonomy.AutonomyController
import com.aihos.ai.reasoning.DecisionRecord
import com.aihos.ai.reflection.ReflectionEngine
import com.aihos.ai.evolution.EvolutionMetrics
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * AI State Broadcaster: Continuously broadcasts AI cognitive state to 3D system
 * This is the connection point between AI decision loop and visual animation
 * 
 * The broadcaster:
 * - Monitors all AI layers for state changes
 * - Extracts meaningful metrics from each layer
 * - Computes composite AI motion state
 * - Broadcasts to WebView in real-time
 * - Maintains smooth transitions between states
 */

/**
 * Interface for receiving broadcasts of AI motion state
 */
interface AIMotionStateListener {
    suspend fun onAIMotionStateChanged(state: AIMotionState)
    suspend fun onAIError(error: String)
}

/**
 * Broadcaster that monitors AI and sends state to 3D system
 */
class AIStateBroadcaster(
    private val autonomyController: AutonomyController,
    private val reflectionEngine: ReflectionEngine,
    private val motionController: AIMotionController = AIMotionController()
) {
    
    private val listeners = mutableListOf<AIMotionStateListener>()
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    
    // State tracking
    private var currentState: AIMotionState? = null
    private var lastBroadcastTime = 0L
    private val broadcastIntervalMs = 100L  // Update 3D at 10 Hz
    
    // Smoothing
    private var smoothedBreathingRate = 1.0f
    private var smoothedRotationSpeed = 0.3f
    private val smoothingFactor = 0.2f  // Exponential smoothing
    
    // Metrics tracking
    private var lastDecisionTime = 0L
    private var recentDecisions = emptyList<DecisionRecord>()
    private var recentSuccessRate = 0.5f
    private var evolutionMetrics: EvolutionMetrics? = null
    
    fun addListener(listener: AIMotionStateListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: AIMotionStateListener) {
        listeners.remove(listener)
    }
    
    /**
     * Start monitoring AI and broadcasting state
     */
    fun startBroadcasting() {
        Timber.i("AI State Broadcaster started")
        scope.launch {
            while (isActive) {
                try {
                    // Compute current AI state
                    val newState = computeCurrentAIState()
                    
                    // Apply smoothing for fluid animation
                    val smoothedState = applySmoothingToState(newState)
                    
                    // Check if state changed significantly
                    if (hasSignificantStateChange(currentState, smoothedState)) {
                        currentState = smoothedState
                        broadcastToListeners(smoothedState)
                    }
                    
                    delay(broadcastIntervalMs)
                } catch (e: Exception) {
                    Timber.e(e, "Error in broadcaster loop")
                    broadcastError(e.message ?: "Unknown error")
                    delay(1000)
                }
            }
        }
    }
    
    /**
     * Stop monitoring and broadcasting
     */
    fun stopBroadcasting() {
        scope.cancel()
        Timber.i("AI State Broadcaster stopped")
    }
    
    /**
     * Compute the current AI state by analyzing all AI layers
     * This is the core logic that translates AI metrics to motion state
     */
    private suspend fun computeCurrentAIState(): AIMotionState {
        // Get current timestamp
        val now = System.currentTimeMillis()
        
        // Determine primary cognitive state by analyzing what the AI is doing
        val cognitiveState = determinePrimaryCognitiveState(now)
        
        // Determine operational mode (conservative, balanced, exploratory, etc.)
        val operationalMode = determineOperationalMode(now)
        
        // Extract confidence metrics from reasoning and reflection
        val confidence = extractConfidenceMetrics(now)
        
        // Extract processing metrics from autonomy loop and decision activity
        val processing = extractProcessingMetrics(now)
        
        // Compute motion state from all the above
        return motionController.computeMotionState(
            cognitiveState = cognitiveState,
            operationalMode = operationalMode,
            confidence = confidence,
            processing = processing,
            previousState = currentState
        )
    }
    
    /**
     * Determine what the AI is primarily thinking about
     * Analyzes decision frequency, reflection activity, evolution events
     */
    private suspend fun determinePrimaryCognitiveState(now: Long): AICognitiveState {
        // If there was a decision very recently, we're in THINKING or EXECUTING
        val timeSinceLastDecision = now - lastDecisionTime
        if (timeSinceLastDecision < 500) {
            return AICognitiveState.EXECUTING
        }
        
        // If we're within the thinking window
        if (timeSinceLastDecision < 2000) {
            return AICognitiveState.THINKING
        }
        
        // Estimate what AI is doing based on activity patterns
        // This is heuristic but provides good visual feedback
        
        // Check if AI is reflecting on past decisions
        if (timeSinceLastDecision in 2000..5000) {
            return AICognitiveState.REFLECTING
        }
        
        // Check if evolution is active (rules changing fast)
        if (evolutionMetrics?.lastEvolutionTime?.let { now - it < 3000 } == true) {
            return AICognitiveState.EVOLVING
        }
        
        // Check if AI is uncertain (low confidence across the board)
        // This would be detected from recent decision confidence levels
        
        // Default: idle
        return AICognitiveState.IDLE
    }
    
    /**
     * Determine operational mode based on recent decision patterns
     */
    private fun determineOperationalMode(now: Long): AIOperationalMode {
        // Use recent success rate to determine mode
        return when {
            recentSuccessRate > 0.8f -> AIOperationalMode.BALANCED
            recentSuccessRate > 0.65f -> AIOperationalMode.CONSERVATIVE
            recentSuccessRate < 0.4f -> AIOperationalMode.EXPLORATORY  // Low success = trying new things
            else -> AIOperationalMode.BALANCED
        }
    }
    
    /**
     * Extract confidence metrics from AI reasoning results
     * These drive visual intensity and glow
     */
    private suspend fun extractConfidenceMetrics(now: Long): ConfidenceMetrics {
        // Average confidence from recent decisions
        val recentDecisionConfidence = if (recentDecisions.isNotEmpty()) {
            recentDecisions.take(5).map { it.confidenceLevel }.average().toFloat()
        } else {
            0.5f
        }
        
        // Prediction confidence: estimate from how often predictions are correct
        val predictionConfidence = recentSuccessRate
        
        // Knowledge confidence: estimate from how stable our rules are
        val knowledgeConfidence = 0.6f  // Would come from evolution metrics
        
        return AIMetricsBuilder.buildConfidenceMetrics(
            decisionConfidence = recentDecisionConfidence,
            predictionConfidence = predictionConfidence,
            knowledgeConfidence = knowledgeConfidence
        )
    }
    
    /**
     * Extract processing metrics from AI activity
     * These drive animation speed, intensity, particle behavior
     */
    private suspend fun extractProcessingMetrics(now: Long): AIProcessingMetrics {
        // Cognitive load: how much are we thinking right now?
        // Estimate from decision frequency
        val cognitiveLoad = if (now - lastDecisionTime < 1000) {
            1.0f  // Just made a decision, high cognitive load
        } else if (now - lastDecisionTime < 3000) {
            0.6f  // Recently made decision
        } else {
            0.2f  // Idle
        }
        
        // Decision complexity: estimate from recent decisions
        val decisionComplexity = if (recentDecisions.isNotEmpty()) {
            // Use number of options and reasoning length as proxy
            min(recentDecisions.size.toFloat() / 5f, 1f)
        } else {
            0.3f
        }
        
        // Uncertainty: inverse of confidence
        val uncertaintyLevel = 1f - (recentSuccessRate * 0.7f + 0.3f)
        
        // Learning rate: how fast are we evolving?
        val learningRate = evolutionMetrics?.let {
            // Normalize: assume 10 adaptations per hour is 1.0
            min(it.adaptationCount.toFloat() / 10f, 1f)
        } ?: 0.0f
        
        // Success metrics: recent success rate
        val successMetrics = recentSuccessRate
        
        // Memory load: estimate from recent episodes (not available now, use default)
        val memoryLoad = 0.5f
        
        // Adaptation intensity: how much are rules changing?
        val adaptationIntensity = evolutionMetrics?.let {
            min(it.adaptationCount.toFloat() * 0.1f, 1f)
        } ?: 0.0f
        
        return AIMetricsBuilder.buildProcessingMetrics(
            cognitiveLoad = cognitiveLoad,
            decisionComplexity = decisionComplexity,
            uncertaintyLevel = uncertaintyLevel,
            learningRate = learningRate,
            successRate = successMetrics,
            memoryLoad = memoryLoad,
            adaptationIntensity = adaptationIntensity
        )
    }
    
    /**
     * Apply exponential smoothing to animation parameters
     * Prevents jittery animation, creates smooth transitions
     */
    private fun applySmoothingToState(state: AIMotionState): AIMotionState {
        if (currentState == null) {
            smoothedBreathingRate = state.breathingRate
            smoothedRotationSpeed = state.rotationSpeed
            return state
        }
        
        // Exponential smoothing: new_value = old_value * (1 - factor) + new_value * factor
        smoothedBreathingRate = smoothedBreathingRate * (1 - smoothingFactor) +
                              state.breathingRate * smoothingFactor
        smoothedRotationSpeed = smoothedRotationSpeed * (1 - smoothingFactor) +
                              state.rotationSpeed * smoothingFactor
        
        return state.copy(
            breathingRate = smoothedBreathingRate,
            rotationSpeed = smoothedRotationSpeed
        )
    }
    
    /**
     * Check if state changed meaningfully (not just noise)
     */
    private fun hasSignificantStateChange(
        oldState: AIMotionState?,
        newState: AIMotionState
    ): Boolean {
        if (oldState == null) return true
        
        // Significant change if:
        // 1. Primary state changed
        if (oldState.primaryState != newState.primaryState) return true
        
        // 2. Color theme changed
        if (oldState.colorTheme != newState.colorTheme) return true
        
        // 3. Breathing rate changed more than 20%
        if ((newState.breathingRate - oldState.breathingRate).toDouble() > 0.2f) return true
        
        // 4. Morphing intensity changed significantly
        if ((newState.morphingIntensity - oldState.morphingIntensity).toDouble() > 0.15f) return true
        
        return false
    }
    
    /**
     * Broadcast state to all listeners
     */
    private suspend fun broadcastToListeners(state: AIMotionState) {
        val now = System.currentTimeMillis()
        if (now - lastBroadcastTime >= broadcastIntervalMs) {
            lastBroadcastTime = now
            for (listener in listeners) {
                try {
                    listener.onAIMotionStateChanged(state)
                } catch (e: Exception) {
                    Timber.e(e, "Error notifying listener")
                }
            }
        }
    }
    
    /**
     * Broadcast error to listeners
     */
    private suspend fun broadcastError(message: String) {
        for (listener in listeners) {
            try {
                listener.onAIError(message)
            } catch (e: Exception) {
                Timber.e(e, "Error notifying listener of error")
            }
        }
    }
    
    /**
     * Update metrics when a new decision is made
     * Called by autonomy controller
     */
    suspend fun onDecisionMade(decision: DecisionRecord) {
        lastDecisionTime = System.currentTimeMillis()
        recentDecisions = (recentDecisions + decision).takeLast(10)
        
        Timber.d("Decision made: confidence=${decision.confidenceLevel}, options=${decision.allOptions.size}")
    }
    
    /**
     * Update success rate
     * Called by autonomy controller after decision outcome
     */
    fun onDecisionOutcome(isSuccess: Boolean) {
        // Exponential moving average: weight recent outcomes more
        recentSuccessRate = recentSuccessRate * 0.8f + (if (isSuccess) 1f else 0f) * 0.2f
        Timber.d("Decision outcome: success=$isSuccess, rate=$recentSuccessRate")
    }
    
    /**
     * Update evolution metrics
     * Called by AI system when evolution occurs
     */
    fun onEvolutionMetricsUpdated(metrics: EvolutionMetrics) {
        evolutionMetrics = metrics
        Timber.d("Evolution metrics: gen=${metrics.generationNumber}, rate=${metrics.averageImprovement}")
    }
}
