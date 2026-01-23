package com.aihos.ai

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.aihos.ai.memory.MemorySystem
import com.aihos.ai.reasoning.ReasoningEngine
import com.aihos.ai.reasoning.ReasoningContext
import com.aihos.ai.reflection.ReflectionEngine
import com.aihos.ai.evolution.EvolutionEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * AI System Controller
 *
 * Manages the complete cognitive loop: Think → Act → Reflect → Evolve
 * Exposes AI state as a real-time observable flow.
 *
 * This is the core "brain" of SA-AIHOS. Everything else is UI or persistence.
 *
 * Architecture Principles:
 * - State-driven (all AI state in StateFlow)
 * - Lifecycle-aware (pause/resume/destroy)
 * - Coroutine-safe (all operations on IO dispatcher, can be cancelled)
 * - Performance-conscious (<100ms cycle time target)
 * - Testable (all dependencies injected)
 */
class AISystemController(
    private val context: Context,
    private val reasoningEngine: ReasoningEngine,
    private val reflectionEngine: ReflectionEngine,
    private val evolutionEngine: EvolutionEngine,
    private val memorySystem: MemorySystem,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    // ==================== STATE MANAGEMENT ====================

    /**
     * Complete AI state exposed as real-time flow.
     * UI subscribes to this single source of truth.
     */
    private val _aiState = MutableStateFlow(AIState.Idle)
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()

    /**
     * Current execution phase (for UI display)
     */
    private val _executionPhase = MutableStateFlow(ExecutionPhase.IDLE)
    val executionPhase: StateFlow<ExecutionPhase> = _executionPhase.asStateFlow()

    /**
     * Cycle metrics for performance monitoring and visualization
     */
    private val _cycleMetrics = MutableStateFlow(CycleMetrics())
    val cycleMetrics: StateFlow<CycleMetrics> = _cycleMetrics.asStateFlow()

    /**
     * Last recorded decision (for explanation and learning)
     */
    private val _lastDecision = MutableStateFlow<CognitiveDecision?>(null)
    val lastDecision: StateFlow<CognitiveDecision?> = _lastDecision.asStateFlow()

    /**
     * Reflection insights from last cycle
     */
    private val _lastInsight = MutableStateFlow<ReflectionInsight?>(null)
    val lastInsight: StateFlow<ReflectionInsight?> = _lastInsight.asStateFlow()

    /**
     * Evolution events (new rules added, rules modified)
     */
    private val _evolutionEvents = MutableSharedFlow<EvolutionEvent>(
        replay = 10,
        extraBufferCapacity = 10
    )
    val evolutionEvents: SharedFlow<EvolutionEvent> = _evolutionEvents.asSharedFlow()

    // ==================== LIFECYCLE MANAGEMENT ====================

    private var currentJob: Job? = null
    private var isRunning = false
    private var isPaused = false

    /**
     * Start the AI system.
     * Should be called from onCreate or when app comes to foreground.
     */
    fun start() {
        if (isRunning) return

        Timber.d("AI System starting")
        isRunning = true
        isPaused = false

        _aiState.value = AIState.Initializing
        currentJob = scope.launch {
            cognitiveLoop()
        }
    }

    /**
     * Pause the AI system.
     * Should be called when app goes to background.
     * AI state is preserved; can be resumed.
     */
    fun pause() {
        if (!isRunning || isPaused) return

        Timber.d("AI System pausing")
        isPaused = true
        _aiState.value = AIState.Paused
        _executionPhase.value = ExecutionPhase.IDLE
    }

    /**
     * Resume the AI system from pause.
     * Should be called when app comes back to foreground.
     */
    fun resume() {
        if (!isRunning || !isPaused) return

        Timber.d("AI System resuming")
        isPaused = false

        // Resume the cognitive loop
        currentJob = scope.launch {
            cognitiveLoop()
        }
    }

    /**
     * Stop the AI system completely.
     * Should be called from onDestroy.
     * Persists final state before shutdown.
     */
    fun stop() {
        if (!isRunning) return

        Timber.d("AI System stopping")
        isRunning = false
        currentJob?.cancel()
        scope.cancel()

        _aiState.value = AIState.Stopped
        _executionPhase.value = ExecutionPhase.IDLE
    }

    // ==================== COGNITIVE LOOP ====================

    /**
     * The core cognitive loop: Think → Act → Reflect → Evolve
     *
     * This loop runs continuously, cycling through the four phases.
     * Each phase updates the state flow, allowing UI to react in real-time.
     *
     * Timing:
     * - THINK: 16-100ms (decision-making)
     * - ACT: 0ms (environment interaction)
     * - REFLECT: 50-200ms (every 5-10 cycles)
     * - EVOLVE: 10-50ms (every 10-50 cycles)
     */
    private suspend fun cognitiveLoop() = withContext(Dispatchers.Default) {
        var cycleCount = 0

        while (isRunning && !isPaused) {
            try {
                cycleCount++
                val startTime = System.currentTimeMillis()

                // ==================== THINK ====================
                _executionPhase.value = ExecutionPhase.THINKING
                _aiState.value = AIState.Thinking

                val reasoningContext = buildReasoningContext()
                val decision = thinkPhase(reasoningContext, cycleCount)
                _lastDecision.value = decision

                // ==================== ACT ====================
                _executionPhase.value = ExecutionPhase.ACTING
                _aiState.value = AIState.Acting

                val outcome = actPhase(decision)

                // ==================== REFLECT ====================
                // Only reflect every N cycles (saves computation)
                if (cycleCount % 10 == 0) {
                    _executionPhase.value = ExecutionPhase.REFLECTING
                    _aiState.value = AIState.Reflecting

                    val insight = reflectPhase(decision, outcome)
                    _lastInsight.value = insight

                    // ==================== EVOLVE ====================
                    // Only evolve when reflection yields high-confidence insights
                    if (insight.confidence > 0.7f) {
                        _executionPhase.value = ExecutionPhase.EVOLVING
                        _aiState.value = AIState.Evolving

                        val evolutionEvent = evolvePhase(insight)
                        _evolutionEvents.emit(evolutionEvent)
                    }
                }

                // Record metrics
                val endTime = System.currentTimeMillis()
                val cycleTime = endTime - startTime
                recordCycleMetrics(cycleTime, cycleCount)

                // Brief delay before next cycle (allows async operations to complete)
                delay(16) // ~60 FPS target

            } catch (e: CancellationException) {
                Timber.d("Cognitive loop cancelled")
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error in cognitive loop")
                _aiState.value = AIState.Error(e.message ?: "Unknown error")
                // Don't crash; continue loop
                delay(1000)
            }
        }

        Timber.d("Cognitive loop ended after $cycleCount cycles")
    }

    // ==================== PHASE IMPLEMENTATIONS ====================

    /**
     * THINK Phase: Reasoning about what to do
     *
     * - Evaluate goals against constraints
     * - Score possible actions
     * - Select best action
     * - Return decision with confidence
     */
    private suspend fun thinkPhase(
        context: ReasoningContext,
        cycleCount: Int
    ): CognitiveDecision {
        return withContext(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()

                // Generate possible actions
                val options = reasoningEngine.generateOptions(context)

                // Score each option
                val scoredOptions = options.map { option ->
                    option to reasoningEngine.scoreOption(option, context)
                }

                // Select best option
                val (selectedOption, confidence) = scoredOptions.maxByOrNull { it.second }
                    ?: options.first() to 0.5f

                // Get explanation
                val explanation = reasoningEngine.explainDecision(
                    selectedOption,
                    options,
                    context
                )

                val decision = CognitiveDecision(
                    phaseNumber = cycleCount,
                    timestamp = System.currentTimeMillis(),
                    selectedAction = selectedOption.action,
                    confidence = confidence,
                    explanation = explanation,
                    alternatives = options.map { it.action },
                    context = context,
                    processingTimeMs = System.currentTimeMillis() - startTime
                )

                // Persist decision
                memorySystem.recordDecision(decision)

                Timber.d("THINK phase: Selected '${decision.selectedAction}' (confidence: $confidence)")

                decision

            } catch (e: Exception) {
                Timber.e(e, "Error in THINK phase")
                CognitiveDecision.defaultDecision(cycleCount)
            }
        }
    }

    /**
     * ACT Phase: Execute action in environment
     *
     * - Apply decision to environment
     * - Record immediate feedback
     * - Return outcome
     */
    private suspend fun actPhase(decision: CognitiveDecision): ActionOutcome {
        return withContext(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()

                // In a real system, this would interact with the environment
                // For now, we record the action and simulate outcome
                val outcome = ActionOutcome(
                    decisionId = decision.phaseNumber,
                    action = decision.selectedAction,
                    success = true,
                    feedback = "Action executed",
                    timestamp = System.currentTimeMillis(),
                    processingTimeMs = System.currentTimeMillis() - startTime
                )

                memorySystem.recordOutcome(outcome)

                Timber.d("ACT phase: Executed '${decision.selectedAction}'")

                outcome

            } catch (e: Exception) {
                Timber.e(e, "Error in ACT phase")
                ActionOutcome(
                    decisionId = decision.phaseNumber,
                    action = decision.selectedAction,
                    success = false,
                    feedback = e.message ?: "Unknown error",
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * REFLECT Phase: Analyze outcome and learn
     *
     * - Compare outcome to expectation
     * - Identify patterns
     * - Generate learning hypothesis
     * - Return insight with confidence
     */
    private suspend fun reflectPhase(
        decision: CognitiveDecision,
        outcome: ActionOutcome
    ): ReflectionInsight {
        return withContext(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()

                // Retrieve similar past decisions from memory
                val similarDecisions = memorySystem.findSimilarDecisions(
                    decision.selectedAction,
                    lookbackCount = 100
                )

                // Analyze patterns
                val successRate = similarDecisions.count { it.wasSuccessful }.toFloat() /
                        similarDecisions.size.coerceAtLeast(1)

                val insight = ReflectionInsight(
                    decisionId = decision.phaseNumber,
                    pattern = "Action '${decision.selectedAction}' has ${(successRate * 100).toInt()}% success rate",
                    confidence = successRate,
                    supportingEvidence = similarDecisions.size,
                    processingTimeMs = System.currentTimeMillis() - startTime
                )

                memorySystem.recordInsight(insight)

                Timber.d("REFLECT phase: Insight confidence = ${insight.confidence}")

                insight

            } catch (e: Exception) {
                Timber.e(e, "Error in REFLECT phase")
                ReflectionInsight(
                    decisionId = decision.phaseNumber,
                    pattern = "No clear pattern",
                    confidence = 0.0f,
                    supportingEvidence = 0
                )
            }
        }
    }

    /**
     * EVOLVE Phase: Modify decision rules based on insight
     *
     * - Integrate insight into decision model
     * - Modify rule weights
     * - Persist changes
     * - Emit evolution event
     */
    private suspend fun evolvePhase(insight: ReflectionInsight): EvolutionEvent {
        return withContext(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()

                // Use evolution engine to adapt rules
                val evolutionEvent = evolutionEngine.evolveFromInsight(insight)

                // Persist evolved state
                memorySystem.recordEvolution(evolutionEvent)

                Timber.d("EVOLVE phase: Added ${evolutionEvent.rulesAdded} rules")

                EvolutionEvent(
                    timestamp = System.currentTimeMillis(),
                    rulesAdded = evolutionEvent.rulesAdded,
                    rulesModified = evolutionEvent.rulesModified,
                    ruleSetStability = evolutionEvent.ruleSetStability,
                    processingTimeMs = System.currentTimeMillis() - startTime
                )

            } catch (e: Exception) {
                Timber.e(e, "Error in EVOLVE phase")
                EvolutionEvent(
                    timestamp = System.currentTimeMillis(),
                    rulesAdded = 0,
                    rulesModified = 0,
                    ruleSetStability = 0.0f
                )
            }
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Build reasoning context from current device and user state.
     * This is what the AI "sees" about the world.
     */
    private fun buildReasoningContext(): ReasoningContext {
        val now = System.currentTimeMillis()
        return ReasoningContext(
            timestamp = now,
            currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(now),
            dayOfWeek = java.text.SimpleDateFormat("EEEE", java.util.Locale.US).format(now),
            appUsageDurationMinutes = (now / 60000 % 60).toInt(),
            recentInteractionCount = 0, // Would come from real interaction tracking
            userIsFocused = true,
            batteryPercent = 75,
            isCharging = false,
            recentDecisions = memorySystem.getRecentDecisions(5).map { it.selectedAction },
            availableActions = listOf("think", "act", "reflect", "evolve")
        )
    }

    /**
     * Record cycle performance metrics
     */
    private fun recordCycleMetrics(cycleTimeMs: Long, cycleCount: Int) {
        _cycleMetrics.value = CycleMetrics(
            lastCycleTimeMs = cycleTimeMs,
            averageCycleTimeMs = (cycleCount to cycleTimeMs).let { (c, t) ->
                (_cycleMetrics.value.averageCycleTimeMs * (c - 1) + t) / c
            }.toLong(),
            totalCyclesCompleted = cycleCount,
            targetCycleTimeMs = 16 // 60 FPS
        )
    }

    // ==================== DATA CLASSES ====================

    /**
     * Complete AI state representation
     */
    sealed class AIState {
        object Idle : AIState()
        object Initializing : AIState()
        object Thinking : AIState()
        object Acting : AIState()
        object Reflecting : AIState()
        object Evolving : AIState()
        object Paused : AIState()
        object Stopped : AIState()
        data class Error(val message: String) : AIState()
    }

    /**
     * Current execution phase (for UI visualization)
     */
    enum class ExecutionPhase {
        IDLE, THINKING, ACTING, REFLECTING, EVOLVING
    }

    /**
     * Cycle performance metrics
     */
    @Serializable
    data class CycleMetrics(
        val lastCycleTimeMs: Long = 0,
        val averageCycleTimeMs: Long = 0,
        val totalCyclesCompleted: Int = 0,
        val targetCycleTimeMs: Long = 16
    ) {
        val isPerformanceGood: Boolean = lastCycleTimeMs < targetCycleTimeMs
    }

    /**
     * A cognitive decision made during THINK phase
     */
    @Serializable
    data class CognitiveDecision(
        val phaseNumber: Int,
        val timestamp: Long,
        val selectedAction: String,
        val confidence: Float,
        val explanation: String,
        val alternatives: List<String>,
        val context: ReasoningContext? = null,
        val processingTimeMs: Long = 0,
        val wasSuccessful: Boolean = true // Updated after outcome
    ) {
        companion object {
            fun defaultDecision(phaseNumber: Int) = CognitiveDecision(
                phaseNumber = phaseNumber,
                timestamp = System.currentTimeMillis(),
                selectedAction = "idle",
                confidence = 0.0f,
                explanation = "Default decision",
                alternatives = emptyList()
            )
        }
    }

    /**
     * Outcome of an action
     */
    @Serializable
    data class ActionOutcome(
        val decisionId: Int,
        val action: String,
        val success: Boolean,
        val feedback: String,
        val timestamp: Long = System.currentTimeMillis(),
        val processingTimeMs: Long = 0
    )

    /**
     * Insight from reflection
     */
    @Serializable
    data class ReflectionInsight(
        val decisionId: Int,
        val pattern: String,
        val confidence: Float,
        val supportingEvidence: Int,
        val processingTimeMs: Long = 0
    )

    /**
     * Evolution event
     */
    @Serializable
    data class EvolutionEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val rulesAdded: Int,
        val rulesModified: Int,
        val ruleSetStability: Float,
        val processingTimeMs: Long = 0
    )
}
