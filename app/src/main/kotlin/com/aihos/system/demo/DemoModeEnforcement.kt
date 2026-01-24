package com.aihos.system.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Demo Mode Enforcer
 *
 * Applies demo mode constraints to AI cognition in real-time.
 * Acts as an interception layer between the core AI systems and cognition scheduling.
 *
 * Responsibilities:
 * - Enforce minimum cognition intervals
 * - Prevent evolution when frozen
 * - Delay state transitions for visibility
 * - Log demo-specific events
 */
class DemoModeEnforcer(
    private val demoModeManager: DemoModeManager,
    private val scope: CoroutineScope
) {

    private var lastCognitionTimeMs = 0L
    private var allowedToEvolve = true
    private var allowedToReflect = true

    /**
     * Check if cognition should be throttled based on demo mode
     * Returns the minimum time to wait before next cognition cycle
     */
    fun getMinimumWaitBeforeCognitionMs(): Long {
        val config = demoModeManager.getConfig()
        if (!config.isEnabled) {
            return 0L
        }

        val now = System.currentTimeMillis()
        val timeSinceLastCognition = now - lastCognitionTimeMs
        val minWait = config.cognitionIntervalMs

        val waitMs = maxOf(0L, minWait - timeSinceLastCognition)
        
        if (waitMs > 0) {
            Timber.d("⏱️ Demo throttling: waiting ${waitMs}ms before next cognition")
        }

        return waitMs
    }

    /**
     * Record that a cognition cycle just completed
     * Called after reasoning phase
     */
    fun recordCognitionCycleCompleted() {
        lastCognitionTimeMs = System.currentTimeMillis()
        
        val config = demoModeManager.getConfig()
        if (config.isEnabled && config.pauseBetweenCyclesMs > 0) {
            Timber.d("⏸️ Demo pause: ${config.pauseBetweenCyclesMs}ms before next cycle")
        }
    }

    /**
     * Check if evolution should be allowed
     */
    fun canEvolve(): Boolean {
        val config = demoModeManager.getConfig()
        
        if (!config.isEnabled) {
            return true  // Normal operation
        }

        if (config.freezeEvolution) {
            if (allowedToEvolve) {
                allowedToEvolve = false
                Timber.i("🔒 Demo: Evolution frozen - rule mutations prevented")
            }
            return false
        }

        return true
    }

    /**
     * Check if reflection should be allowed
     */
    fun canReflect(): Boolean {
        val config = demoModeManager.getConfig()
        
        if (!config.isEnabled) {
            return true
        }

        if (config.freezeReflection) {
            if (allowedToReflect) {
                allowedToReflect = false
                Timber.i("🔒 Demo: Reflection frozen")
            }
            return false
        }

        return true
    }

    /**
     * Apply state transition delay for visibility
     */
    suspend fun delayForStateTransition() {
        val config = demoModeManager.getConfig()
        
        if (config.isEnabled && config.useSlowTransitions) {
            Timber.d("⏳ Demo transition delay: ${config.transitionDurationMs}ms")
            delay(config.transitionDurationMs)
        }
    }

    /**
     * Reset enforcer state (useful when disabling demo mode)
     */
    fun reset() {
        lastCognitionTimeMs = 0L
        allowedToEvolve = true
        allowedToReflect = true
        Timber.i("🔄 Demo enforcer reset")
    }
}

/**
 * Demo Mode Telemetry
 *
 * Collects metrics about demo mode execution for debugging and optimization.
 */
data class DemoTelemetry(
    val totalCognitionCycles: Long = 0,
    val throttledCycles: Long = 0,
    val evolvedRules: Long = 0,
    val frozenEvolutionAttempts: Long = 0,
    val totalReflections: Long = 0,
    val pausedReflections: Long = 0,
    val stateTransitions: Long = 0,
    val durationSeconds: Long = 0
) {
    val throttlingPercentage: Float
        get() = if (totalCognitionCycles > 0) {
            (throttledCycles * 100f) / totalCognitionCycles
        } else {
            0f
        }

    val evolutionBlockedPercentage: Float
        get() = if (frozenEvolutionAttempts > 0) {
            100f  // 100% if evolution frozen
        } else {
            0f
        }
}

/**
 * Demo Session Manager
 *
 * Manages the complete lifecycle of a demo session:
 * - Session start/stop
 * - Telemetry collection
 * - Session recording
 */
class DemoSessionManager(
    private val demoModeManager: DemoModeManager,
    private val scope: CoroutineScope
) {

    private var telemetry = DemoTelemetry()
    private var sessionActive = false

    /**
     * Start a demo session with given preset
     */
    fun startSession(preset: DemoModeConfig) {
        demoModeManager.enableDemoMode(preset)
        sessionActive = true
        telemetry = DemoTelemetry()

        Timber.i("🎬 Demo session started: ${preset.description}")
    }

    /**
     * End the current demo session and generate report
     */
    fun endSession(): DemoSessionReport {
        demoModeManager.disableDemoMode()
        sessionActive = false

        return DemoSessionReport(
            durationSeconds = telemetry.durationSeconds,
            cognitionCycles = telemetry.totalCognitionCycles,
            throttledCycles = telemetry.throttledCycles,
            reflections = telemetry.totalReflections,
            pausedReflections = telemetry.pausedReflections,
            evolutionAttempts = telemetry.frozenEvolutionAttempts,
            stateTransitions = telemetry.stateTransitions,
            throttlingPercentage = telemetry.throttlingPercentage,
            evolutionBlockedPercentage = telemetry.evolutionBlockedPercentage
        )
    }

    /**
     * Record a cognition cycle
     */
    fun recordCognitionCycle(throttled: Boolean = false) {
        telemetry = telemetry.copy(
            totalCognitionCycles = telemetry.totalCognitionCycles + 1,
            throttledCycles = if (throttled) telemetry.throttledCycles + 1 else telemetry.throttledCycles
        )
    }

    /**
     * Record a reflection cycle
     */
    fun recordReflection(paused: Boolean = false) {
        telemetry = telemetry.copy(
            totalReflections = telemetry.totalReflections + 1,
            pausedReflections = if (paused) telemetry.pausedReflections + 1 else telemetry.pausedReflections
        )
    }

    /**
     * Record evolution attempt
     */
    fun recordEvolutionAttempt(blocked: Boolean = false) {
        telemetry = telemetry.copy(
            frozenEvolutionAttempts = if (blocked) telemetry.frozenEvolutionAttempts + 1 else telemetry.frozenEvolutionAttempts,
            evolvedRules = if (!blocked) telemetry.evolvedRules + 1 else telemetry.evolvedRules
        )
    }

    /**
     * Record state transition
     */
    fun recordStateTransition() {
        telemetry = telemetry.copy(
            stateTransitions = telemetry.stateTransitions + 1
        )
    }

    /**
     * Get current telemetry
     */
    fun getTelemetry(): DemoTelemetry = telemetry

    /**
     * Check if session is active
     */
    fun isSessionActive(): Boolean = sessionActive
}

/**
 * Demo session report generated at end of session
 */
data class DemoSessionReport(
    val durationSeconds: Long,
    val cognitionCycles: Long,
    val throttledCycles: Long,
    val reflections: Long,
    val pausedReflections: Long,
    val evolutionAttempts: Long,
    val stateTransitions: Long,
    val throttlingPercentage: Float,
    val evolutionBlockedPercentage: Float
) {
    fun toFormattedString(): String = """
        ╔════════════════════════════════════════╗
        ║        DEMO SESSION REPORT             ║
        ╠════════════════════════════════════════╣
        ║ Duration: $durationSeconds seconds
        ║ Cognition Cycles: $cognitionCycles
        ║   - Throttled: $throttledCycles (${String.format("%.1f", throttlingPercentage)}%)
        ║ Reflections: $reflections
        ║   - Paused: $pausedReflections
        ║ Evolution Attempts: $evolutionAttempts
        ║   - Blocked: ${String.format("%.1f", evolutionBlockedPercentage)}%
        ║ State Transitions: $stateTransitions
        ╚════════════════════════════════════════╝
    """.trimIndent()
}
