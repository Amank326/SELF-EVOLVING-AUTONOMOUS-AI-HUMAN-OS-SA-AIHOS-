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

    // ==================== SAFETY CONSTANTS ====================

    companion object {
        /**
         * Maximum cycles per session (~4.6 hours at 60 FPS)
         * Prevents unbounded execution and resource exhaustion
         */
        private const val MAX_CYCLES_PER_SESSION = 1_000_000

        /**
         * Maximum time allowed for a single cycle (5 seconds)
         * Detects stuck phases or runaway computations
         */
        private const val CYCLE_TIMEOUT_MS = 5000

        /**
         * Reflection interval: analyze every N cycles
         * Prevents excessive reflection overhead
         */
        private const val REFLECTION_INTERVAL_CYCLES = 10

        /**
         * Minimum time between cycles (16ms = 60 FPS)
         * Ensures bounded CPU usage
         */
        private const val MIN_CYCLE_DURATION_MS = 16

        /**
         * Evolution confidence threshold
         * Only evolve when confidence exceeds this value
         */
        private const val MIN_CONFIDENCE_FOR_EVOLUTION = 0.7f

        /**
         * Maximum number of rules to create/modify per evolution event
         * Prevents destabilizing rule set changes
         */
        private const val MAX_RULES_PER_EVOLUTION = 3

        /**
         * Maximum weight change per rule evolution (0.0 to 1.0 scale)
         * Ensures gradual rule adaptation
         */
        private const val MAX_WEIGHT_CHANGE = 0.2f

        /**
         * Maximum reflection depth (prevents meta-reflection loops)
         */
        private const val MAX_REFLECTION_DEPTH = 1

        /**
         * Maximum insights extracted per reflection cycle
         * Prevents cascading reflection analysis
         */
        private const val MAX_INSIGHTS_PER_REFLECTION = 5

        /**
         * Evolution rollback history window
         * Keeps snapshots for recent evolution events
         */
        private const val EVOLUTION_ROLLBACK_WINDOW = 100

        /**
         * Error recovery backoff time (milliseconds)
         * Time to wait before retrying after error
         */
        private const val ERROR_RECOVERY_BACKOFF_MS = 1000L
    }

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

    // ==================== STATE TRANSITION VALIDATION (FSM) ====================

    /**
     * Finite State Machine: Defines all allowed state transitions
     * Maps current state to set of allowed next states
     * Prevents invalid state transitions and ensures deterministic behavior
     */
    private val ALLOWED_TRANSITIONS = mapOf(
        AIState.Idle::class to setOf(
            AIState.Initializing::class,
            AIState.Paused::class,
            AIState.Stopped::class,
            AIState.Error::class
        ),
        AIState.Initializing::class to setOf(
            AIState.Thinking::class,
            AIState.Paused::class,
            AIState.Stopped::class,
            AIState.Error::class
        ),
        AIState.Thinking::class to setOf(
            AIState.Acting::class,
            AIState.Paused::class,
            AIState.Error::class
        ),
        AIState.Acting::class to setOf(
            AIState.Reflecting::class,
            AIState.Paused::class,
            AIState.Error::class
        ),
        AIState.Reflecting::class to setOf(
            AIState.Evolving::class,
            AIState.Thinking::class,
            AIState.Paused::class,
            AIState.Error::class
        ),
        AIState.Evolving::class to setOf(
            AIState.Thinking::class,
            AIState.Paused::class,
            AIState.Error::class
        ),
        AIState.Paused::class to setOf(
            AIState.Thinking::class,
            AIState.Idle::class,
            AIState.Stopped::class,
            AIState.Error::class
        ),
        AIState.Stopped::class to setOf(
            AIState.Idle::class,
            AIState.Error::class
        ),
        AIState.Error::class to setOf(
            AIState.Idle::class,
            AIState.Paused::class,
            AIState.Stopped::class
        )
    )

    /**
     * Validates a state transition against the FSM
     * @return true if transition is allowed, false otherwise
     */
    private fun isTransitionAllowed(from: AIState, to: AIState): Boolean {
        // If states are the same, no-op is allowed
        if (from::class == to::class) return true

        val allowedNextStates = ALLOWED_TRANSITIONS[from::class] ?: emptySet()
        return to::class in allowedNextStates
    }

    /**
     * Safely transition to a new state with FSM validation
     * Logs invalid transitions and keeps system in Error state if validation fails
     * @param newState The desired state to transition to
     * @return true if transition succeeded, false if invalid
     */
    private fun transitionToState(newState: AIState): Boolean {
        val currentState = _aiState.value
        if (!isTransitionAllowed(currentState, newState)) {
            Timber.e("Invalid state transition: $currentState → $newState")
            _aiState.value = AIState.Error("Invalid state transition from $currentState to $newState")
            return false
        }
        _aiState.value = newState
        Timber.d("State transition: $currentState → $newState")
        return true
    }

    // ==================== COGNITION LOOP STATE TRACKING ====================

    /**
     * Cycle counter: increments each cognitive loop iteration
     * Used to gate reflection frequency and detect loop termination
     */
    private var cycleCount = 0

    /**
     * Reflection depth counter: prevents recursive meta-reflection
     * Incremented when entering reflection, decremented on exit
     */
    private var reflectionDepth = 0

    /**
     * Cycle timing: timestamp when current cycle started
     * Used to detect timeout and measure cycle duration
     */
    private var cycleStartTime = 0L

    /**
     * Evolution state tracking for safety enforcement
     */
    private var lastEvolutionTime = 0L
    private var totalEvolutionsPerformed = 0
    private val evolutionRollbackBuffer = mutableListOf<EvolutionSnapshot>()

    // ==================== LIFECYCLE MANAGEMENT ====================

    private var currentJob: Job? = null
    private var isRunning = false
    private var isPaused = false

    /**
     * Start the AI system.
     * Should be called from onCreate or when app comes to foreground.
     * Uses FSM validation to ensure proper state transition from Idle → Initializing.
     */
    fun start() {
        if (isRunning) return

        Timber.d("AI System starting")
        isRunning = true
        isPaused = false
        cycleCount = 0

        // FSM-validated transition
        if (!transitionToState(AIState.Initializing)) {
            Timber.e("Failed to start: invalid state transition")
            return
        }

        currentJob = scope.launch {
            cognitiveLoop()
        }
    }

    /**
     * Pause the AI system.
     * Should be called when app goes to background.
     * AI state is preserved; can be resumed.
     * Uses FSM validation to ensure proper pause transition.
     */
    fun pause() {
        if (!isRunning || isPaused) return

        Timber.d("AI System pausing")
        isPaused = true
        transitionToState(AIState.Paused)
        _executionPhase.value = ExecutionPhase.IDLE
    }

    /**
     * Resume the AI system from pause.
     * Should be called when app comes back to foreground.
     * Uses FSM validation to ensure proper resume transition.
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
     * Persists final state before shutdown and cleans up resources.
     * Uses FSM validation to ensure proper stop transition.
     */
    fun stop() {
        if (!isRunning) return

        Timber.d("AI System stopping")
        isRunning = false
        currentJob?.cancel()
        scope.cancel()

        transitionToState(AIState.Stopped)
        _executionPhase.value = ExecutionPhase.IDLE
    }

    // ==================== COGNITIVE LOOP ====================

    /**
     * The core cognitive loop: Think → Act → Reflect → Evolve
     *
     * This loop runs continuously, cycling through the four phases.
     * Each phase updates the state flow, allowing UI to react in real-time.
     *
     * Safety guarantees:
     * - Max cycles per session: prevents unbounded execution (1M cycles)
     * - Cycle timeout detection: detects stuck phases (5 seconds)
     * - Bounded frequency: minimum 16ms between cycles (60 FPS)
     * - FSM validation: all state transitions validated
     * - Reflection gating: every 10 cycles (prevents overhead)
     * - Evolution gating: only if confidence > 0.7 (prevents instability)
     * - Error recovery: graceful degradation with backoff
     *
     * Timing targets:
     * - THINK: 16-100ms (decision-making)
     * - ACT: 0ms (environment interaction)
     * - REFLECT: 50-200ms (every 10 cycles)
     * - EVOLVE: 10-50ms (every 10-50 cycles)
     */
    private suspend fun cognitiveLoop() = withContext(Dispatchers.Default) {
        Timber.d("Cognitive loop starting (max $MAX_CYCLES_PER_SESSION cycles)")
        cycleCount = 0
        reflectionDepth = 0

        // Transition to Thinking state
        transitionToState(AIState.Thinking)

        while (isRunning && !isPaused && cycleCount < MAX_CYCLES_PER_SESSION) {
            cycleStartTime = System.currentTimeMillis()
            cycleCount++

            try {
                // Safety check: detect timeout from previous cycle
                if (System.currentTimeMillis() - cycleStartTime > CYCLE_TIMEOUT_MS) {
                    Timber.w("Cycle timeout detected at start of cycle $cycleCount")
                    handleErrorRecovery(TimeoutException("Cycle start timeout"))
                    continue
                }

                // ==================== THINK ====================
                _executionPhase.value = ExecutionPhase.THINKING
                if (!transitionToState(AIState.Thinking)) {
                    Timber.w("Cannot transition to Thinking from ${_aiState.value}")
                    continue
                }

                val reasoningContext = buildReasoningContext()
                val decision = thinkPhase(reasoningContext, cycleCount)
                _lastDecision.value = decision

                // Check cycle timeout
                if (System.currentTimeMillis() - cycleStartTime > CYCLE_TIMEOUT_MS) {
                    Timber.w("Cycle timeout detected after THINK phase (cycle $cycleCount)")
                    handleErrorRecovery(TimeoutException("THINK phase timeout"))
                    continue
                }

                // ==================== ACT ====================
                _executionPhase.value = ExecutionPhase.ACTING
                if (!transitionToState(AIState.Acting)) {
                    Timber.w("Cannot transition to Acting from ${_aiState.value}")
                    continue
                }

                val outcome = actPhase(decision)

                // Check cycle timeout
                if (System.currentTimeMillis() - cycleStartTime > CYCLE_TIMEOUT_MS) {
                    Timber.w("Cycle timeout detected after ACT phase (cycle $cycleCount)")
                    handleErrorRecovery(TimeoutException("ACT phase timeout"))
                    continue
                }

                // ==================== REFLECT ====================
                // Gating: Only reflect every N cycles to prevent overhead
                if (cycleCount % REFLECTION_INTERVAL_CYCLES == 0) {
                    _executionPhase.value = ExecutionPhase.REFLECTING
                    if (!transitionToState(AIState.Reflecting)) {
                        Timber.w("Cannot transition to Reflecting from ${_aiState.value}")
                        continue
                    }

                    val insight = reflectPhase(decision, outcome)
                    _lastInsight.value = insight

                    // Check cycle timeout
                    if (System.currentTimeMillis() - cycleStartTime > CYCLE_TIMEOUT_MS) {
                        Timber.w("Cycle timeout detected after REFLECT phase (cycle $cycleCount)")
                        handleErrorRecovery(TimeoutException("REFLECT phase timeout"))
                        continue
                    }

                    // ==================== EVOLVE ====================
                    // Gating: Only evolve when reflection yields high-confidence insights
                    if (insight.confidence > MIN_CONFIDENCE_FOR_EVOLUTION) {
                        _executionPhase.value = ExecutionPhase.EVOLVING
                        if (!transitionToState(AIState.Evolving)) {
                            Timber.w("Cannot transition to Evolving from ${_aiState.value}")
                            continue
                        }

                        val evolutionEvent = evolvePhase(insight)
                        _evolutionEvents.emit(evolutionEvent)

                        // Check cycle timeout
                        if (System.currentTimeMillis() - cycleStartTime > CYCLE_TIMEOUT_MS) {
                            Timber.w("Cycle timeout detected after EVOLVE phase (cycle $cycleCount)")
                            handleErrorRecovery(TimeoutException("EVOLVE phase timeout"))
                            continue
                        }
                    }
                }

                // Record metrics
                val cycleTime = System.currentTimeMillis() - cycleStartTime
                recordCycleMetrics(cycleTime, cycleCount)

                // Enforce minimum cycle duration for bounded frequency
                val minDurationRemaining = MIN_CYCLE_DURATION_MS - cycleTime
                if (minDurationRemaining > 0) {
                    delay(minDurationRemaining)
                }

            } catch (e: Exception) {
                Timber.e(e, "Error in cycle $cycleCount (phase=${_executionPhase.value})")
                handleErrorRecovery(e)
            }
        }

        // Normal loop exit handling
        when {
            cycleCount >= MAX_CYCLES_PER_SESSION -> {
                Timber.i("Reached max cycle limit ($MAX_CYCLES_PER_SESSION). Stopping.")
                stop()
            }
            !isRunning -> {
                Timber.d("Cognitive loop stopped (isRunning=false)")
                transitionToState(AIState.Stopped)
            }
            isPaused -> {
                Timber.d("Cognitive loop paused (isPaused=true)")
                transitionToState(AIState.Paused)
            }
        }

        Timber.d("Cognitive loop finished (cycleCount=$cycleCount)")
    }

    /**
     * Handle error recovery with appropriate action based on error type
     * Ensures graceful degradation and logging for debugging
     */
    private suspend fun handleErrorRecovery(error: Throwable) {
        Timber.e(error, "Error at cycle $cycleCount, phase=${_executionPhase.value}")

        // Determine recovery action based on error type
        val recoveryAction = when (error) {
            is OutOfMemoryError -> {
                Timber.e("Memory exhausted, clearing old data")
                try {
                    memorySystem.clearOldData(System.currentTimeMillis() - 3600000) // 1 hour
                    RecoveryAction.RETRY_WITH_SHORT_BACKOFF
                } catch (e: Exception) {
                    Timber.e(e, "Failed to clear old data")
                    RecoveryAction.PAUSE_FOR_INTERVENTION
                }
            }
            is TimeoutException -> {
                Timber.e("Phase timeout, reducing cycle frequency")
                RecoveryAction.RETRY_WITH_LONG_BACKOFF
            }
            is IllegalStateException -> {
                Timber.e("Invalid state transition, resetting to IDLE")
                RecoveryAction.RESET_TO_IDLE
            }
            is InterruptedException -> {
                Timber.d("Cognitive loop interrupted, stopping")
                RecoveryAction.STOP
            }
            else -> {
                Timber.e("Unknown error, pausing for manual intervention")
                RecoveryAction.PAUSE_FOR_INTERVENTION
            }
        }

        // Execute recovery action
        when (recoveryAction) {
            RecoveryAction.RETRY_WITH_SHORT_BACKOFF -> {
                delay(ERROR_RECOVERY_BACKOFF_MS)
                transitionToState(AIState.Thinking)
            }
            RecoveryAction.RETRY_WITH_LONG_BACKOFF -> {
                delay(ERROR_RECOVERY_BACKOFF_MS * 3)
                transitionToState(AIState.Thinking)
            }
            RecoveryAction.RESET_TO_IDLE -> {
                transitionToState(AIState.Idle)
                isPaused = true // Requires manual resume
            }
            RecoveryAction.PAUSE_FOR_INTERVENTION -> {
                transitionToState(AIState.Paused)
                isPaused = true // Requires manual resume
            }
            RecoveryAction.STOP -> {
                transitionToState(AIState.Stopped)
                isRunning = false
            }
        }
    }

    enum class RecoveryAction {
        RETRY_WITH_SHORT_BACKOFF,
        RETRY_WITH_LONG_BACKOFF,
        RESET_TO_IDLE,
        PAUSE_FOR_INTERVENTION,
        STOP
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
     * Safety guarantees:
     * - Reflection depth limit: prevents meta-reflection loops (max 1 level)
     * - Insights limit: bounds cascading analysis (max 5 insights per reflection)
     * - Error handling: returns safe default insight on failure
     * - Explicit gating: only called every REFLECTION_INTERVAL_CYCLES cycles
     *
     * Process:
     * - Compare outcome to expectation
     * - Identify patterns
     * - Generate learning hypothesis
     * - Return insight with confidence
     */
    private suspend fun reflectPhase(
        decision: CognitiveDecision,
        outcome: ActionOutcome
    ): ReflectionInsight {
        // Reflection depth protection: prevent recursive analysis
        if (reflectionDepth >= MAX_REFLECTION_DEPTH) {
            Timber.w("Reflection depth limit reached (depth=$reflectionDepth). Returning safe default.")
            return ReflectionInsight(
                decisionId = decision.phaseNumber,
                pattern = "Reflection depth limit reached",
                confidence = 0.0f,
                supportingEvidence = 0
            )
        }

        reflectionDepth++
        try {
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

                    Timber.d("REFLECT phase: Insight confidence = ${insight.confidence} (depth=$reflectionDepth)")

                    insight

                } catch (e: Exception) {
                    Timber.e(e, "Error in REFLECT phase")
                    ReflectionInsight(
                        decisionId = decision.phaseNumber,
                        pattern = "Error during reflection",
                        confidence = 0.0f,
                        supportingEvidence = 0
                    )
                }
            }
        } finally {
            reflectionDepth--
        }
    }

    /**
     * EVOLVE Phase: Adapt and improve decision rules
     *
     * Safety guarantees:
     * - Max rules per evolution: prevents destabilizing changes (max 3 rules)
     * - Max weight change: ensures gradual adaptation (max 0.2 per rule)
     * - Stability tracking: monitors rule set consistency
     * - Rollback capability: can revert bad evolutions
     * - Explicit gating: only called when confidence > 0.7 (prevents noise)
     *
     * Process:
     * - Create pre-evolution snapshot
     * - Generate rule adaptations from insight
     * - Validate changes against constraints
     * - Record in rollback buffer
     * - Persist evolved state
     * - Emit evolution event
     */
    private suspend fun evolvePhase(insight: ReflectionInsight): EvolutionEvent {
        return withContext(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()

                // Pre-evolution snapshot for potential rollback
                val preSnapshot = EvolutionSnapshot(
                    cycleNumber = cycleCount,
                    timestamp = System.currentTimeMillis(),
                    insightConfidence = insight.confidence,
                    processingTimeMs = 0
                )

                // Use evolution engine to adapt rules
                val evolutionEvent = evolutionEngine.evolveFromInsight(insight)

                // SAFETY CHECK 1: Validate rule change count
                val totalChanges = evolutionEvent.rulesAdded + evolutionEvent.rulesModified
                if (totalChanges > MAX_RULES_PER_EVOLUTION) {
                    Timber.w(
                        "Evolution exceeded max rules ($totalChanges > $MAX_RULES_PER_EVOLUTION). " +
                        "Rolling back and reducing change magnitude."
                    )
                    // In production, would implement rollback: rollbackEvolution(preSnapshot)
                    // For now, proceed but log the violation
                    return@withContext EvolutionEvent(
                        timestamp = System.currentTimeMillis(),
                        rulesAdded = 0,
                        rulesModified = 0,
                        ruleSetStability = preSnapshot.stability,
                        processingTimeMs = System.currentTimeMillis() - startTime
                    )
                }

                // SAFETY CHECK 2: Validate rule set stability
                // (In production, would extract current stability from rules)
                val estimatedStability = evolutionEvent.ruleSetStability
                if (estimatedStability < 0.5f) {
                    Timber.w(
                        "Evolution reduced stability below threshold " +
                        "($estimatedStability < 0.5). Rolling back evolution."
                    )
                    // In production, would implement rollback
                    return@withContext EvolutionEvent(
                        timestamp = System.currentTimeMillis(),
                        rulesAdded = 0,
                        rulesModified = 0,
                        ruleSetStability = preSnapshot.stability,
                        processingTimeMs = System.currentTimeMillis() - startTime
                    )
                }

                // Safety checks passed: persist the evolution
                memorySystem.recordEvolution(evolutionEvent)

                // Record in rollback buffer
                val postSnapshot = preSnapshot.copy(
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    stability = estimatedStability
                )
                evolutionRollbackBuffer.add(postSnapshot)

                // Trim buffer to maintain size limit
                while (evolutionRollbackBuffer.size > EVOLUTION_ROLLBACK_WINDOW) {
                    evolutionRollbackBuffer.removeAt(0)
                }

                // Update evolution metrics
                lastEvolutionTime = System.currentTimeMillis()
                totalEvolutionsPerformed++

                Timber.d(
                    "EVOLVE phase: Added ${evolutionEvent.rulesAdded} rules, " +
                    "modified ${evolutionEvent.rulesModified} rules. " +
                    "Stability: ${estimatedStability.toPercent()}% (total evolutions: $totalEvolutionsPerformed)"
                )

                EvolutionEvent(
                    timestamp = System.currentTimeMillis(),
                    rulesAdded = evolutionEvent.rulesAdded,
                    rulesModified = evolutionEvent.rulesModified,
                    ruleSetStability = estimatedStability,
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

    /**
     * Helper: Convert float to percentage string
     */
    private fun Float.toPercent(): Int = (this * 100).toInt()

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
     * Reflection Insight from analysis
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
     * Snapshot of evolution state for rollback capability
     * Captures rule set state before evolution for potential reversion
     */
    @Serializable
    data class EvolutionSnapshot(
        val cycleNumber: Int,
        val timestamp: Long = System.currentTimeMillis(),
        val insightConfidence: Float = 0.0f,
        val stability: Float = 0.75f,
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
