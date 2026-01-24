package com.aihos.system.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * AI Cognitive State for Demo Visualization
 *
 * Represents the current state of the AI's cognitive process,
 * suitable for display in a UI indicator or overlay.
 */
@Serializable
enum class AICognitiveState {
    IDLE,        // No active cognition
    THINKING,    // Reasoning phase active
    REFLECTING,  // Reflection phase active
    EVOLVING,    // Evolution phase active
    PAUSED       // Demo mode paused
}

/**
 * Demo Mode Configuration
 *
 * Controls presentation-safe AI behavior for demos and screen recordings.
 * When enabled:
 * - Cognition frequency is limited to predictable intervals
 * - Evolution changes are frozen to prevent unexpected rule mutations
 * - AI state transitions are deterministic
 * - All behaviors remain functional but safer for demonstrations
 *
 * DemoMode is OPTIONAL and REVERSIBLE. The system continues to function
 * normally with autonomous capabilities intact.
 */
@Serializable
data class DemoModeConfig(
    // Main demo mode toggle
    val isEnabled: Boolean = false,

    // Cognitive frequency control (how often AI thinks)
    // In demo mode, cognition is slowed to allow observation
    val cognitionIntervalMs: Long = 2000L,  // 2 seconds between thinking cycles
    val minCognitionIntervalMs: Long = 1000L,  // Never faster than 1 second

    // Evolution freezing
    val freezeEvolution: Boolean = true,  // Prevent rule mutations
    val freezeReflection: Boolean = false, // Still reflect, but don't mutate rules

    // State transition control
    val useSlowTransitions: Boolean = true,  // Animate state changes smoothly
    val transitionDurationMs: Long = 500L,  // How long state changes take

    // UI visibility
    val showCognitiveStateIndicator: Boolean = true,  // Display current state
    val showStatisticsBadge: Boolean = true,  // Show thinking/evolution stats
    val verboseLogging: Boolean = true,  // Log every state change

    // Safety limits for demo
    val maxDemoDurationMinutes: Long = 60L,  // Reset after 1 hour for safety
    val pauseBetweenCyclesMs: Long = 500L,  // Add intentional pause for clarity

    // Description for UI
    val description: String = "Demo mode: Predictable, observable AI behavior"
) {
    val isFullyFrozen: Boolean
        get() = freezeEvolution && freezeReflection

    val isDemoSafe: Boolean
        get() = isEnabled && !isFullyFrozen
}

/**
 * Demo Mode Manager
 *
 * Manages demo mode state and provides reactive access to:
 * - Current demo mode configuration
 * - Current AI cognitive state
 * - Demo session timer
 *
 * Thread-safe via StateFlow. All state changes are observable.
 */
class DemoModeManager {

    // ==================== STATE MANAGEMENT ====================

    private val _demoConfig = MutableStateFlow(DemoModeConfig())
    val demoConfig: StateFlow<DemoModeConfig> = _demoConfig.asStateFlow()

    private val _aiCognitiveState = MutableStateFlow(AICognitiveState.IDLE)
    val aiCognitiveState: StateFlow<AICognitiveState> = _aiCognitiveState.asStateFlow()

    private val _demoDurationMs = MutableStateFlow(0L)
    val demoDurationMs: StateFlow<Long> = _demoDurationMs.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var demoStartTimeMs = 0L

    // ==================== CONFIGURATION ====================

    /**
     * Enable demo mode with optional custom configuration
     */
    fun enableDemoMode(config: DemoModeConfig = DemoModeConfig(isEnabled = true)) {
        _demoConfig.value = config.copy(isEnabled = true)
        _isRunning.value = true
        demoStartTimeMs = System.currentTimeMillis()
        
        Timber.i("✨ Demo mode ENABLED")
        Timber.i("   Cognition interval: ${config.cognitionIntervalMs}ms")
        Timber.i("   Freeze evolution: ${config.freezeEvolution}")
        Timber.i("   Freeze reflection: ${config.freezeReflection}")
        Timber.i("   Show cognitive state: ${config.showCognitiveStateIndicator}")
    }

    /**
     * Disable demo mode and return to normal operation
     */
    fun disableDemoMode() {
        _demoConfig.value = DemoModeConfig(isEnabled = false)
        _isRunning.value = false
        demoStartTimeMs = 0L
        _demoDurationMs.value = 0L
        
        Timber.i("✨ Demo mode DISABLED - returning to normal operation")
    }

    /**
     * Toggle demo mode on/off
     */
    fun toggleDemoMode() {
        if (_demoConfig.value.isEnabled) {
            disableDemoMode()
        } else {
            enableDemoMode()
        }
    }

    /**
     * Update demo configuration (merge with current config)
     */
    fun updateConfig(updates: DemoModeConfig.() -> DemoModeConfig) {
        val current = _demoConfig.value
        _demoConfig.value = current.updates()
    }

    /**
     * Get current demo configuration (read-only)
     */
    fun getConfig(): DemoModeConfig = _demoConfig.value

    // ==================== AI STATE TRACKING ====================

    /**
     * Update the current AI cognitive state
     * Called by AISystemController during phase transitions
     */
    fun setAICognitiveState(state: AICognitiveState) {
        val oldState = _aiCognitiveState.value
        
        if (oldState != state) {
            _aiCognitiveState.value = state
            
            if (_demoConfig.value.verboseLogging) {
                Timber.d("🧠 AI State: $oldState → $state")
            }
        }
    }

    /**
     * Get current AI cognitive state (read-only)
     */
    fun getAICognitiveState(): AICognitiveState = _aiCognitiveState.value

    /**
     * Get human-readable description of current state
     */
    fun getStateDescription(): String = when (_aiCognitiveState.value) {
        AICognitiveState.IDLE -> "AI is idle, waiting for input"
        AICognitiveState.THINKING -> "AI is reasoning about the situation"
        AICognitiveState.REFLECTING -> "AI is reflecting on its decisions"
        AICognitiveState.EVOLVING -> "AI is adapting its decision rules"
        AICognitiveState.PAUSED -> "AI is paused (demo mode)"
    }

    // ==================== DEMO SESSION ====================

    /**
     * Update demo session duration
     * Should be called periodically by a timer
     */
    fun updateDemoDuration() {
        if (_isRunning.value) {
            val elapsed = System.currentTimeMillis() - demoStartTimeMs
            _demoDurationMs.value = elapsed
            
            val config = _demoConfig.value
            if (config.maxDemoDurationMinutes > 0) {
                val maxMs = config.maxDemoDurationMinutes * 60 * 1000
                if (elapsed > maxMs) {
                    Timber.w("⏱️ Demo mode duration exceeded ${config.maxDemoDurationMinutes}min - disabling")
                    disableDemoMode()
                }
            }
        }
    }

    /**
     * Get demo session duration as formatted string
     */
    fun getFormattedDuration(): String {
        val ms = _demoDurationMs.value
        val seconds = ms / 1000
        val minutes = seconds / 60
        return when {
            minutes > 0 -> "%d:%02d".format(minutes, seconds % 60)
            else -> "%ds".format(seconds)
        }
    }

    /**
     * Reset demo session timer
     */
    fun resetDemoTimer() {
        demoStartTimeMs = System.currentTimeMillis()
        _demoDurationMs.value = 0L
    }

    // ==================== PRESETS ====================

    companion object {
        /**
         * Quick demo setup - 3 minute demo with all safety features
         */
        fun quickDemoConfig() = DemoModeConfig(
            isEnabled = true,
            cognitionIntervalMs = 3000L,  // Slower thinking
            freezeEvolution = true,        // No rule changes
            freezeReflection = false,      // Still reflect
            showCognitiveStateIndicator = true,
            showStatisticsBadge = true,
            verboseLogging = true,
            maxDemoDurationMinutes = 3L
        )

        /**
         * Screen recording setup - Very predictable behavior
         */
        fun screenRecordingConfig() = DemoModeConfig(
            isEnabled = true,
            cognitionIntervalMs = 4000L,  // Very slow
            freezeEvolution = true,        // Freeze everything
            freezeReflection = true,       // No mutations at all
            useSlowTransitions = true,
            transitionDurationMs = 800L,  // Visible transitions
            showCognitiveStateIndicator = true,
            showStatisticsBadge = true,
            verboseLogging = true,
            pauseBetweenCyclesMs = 1000L, // Clear breaks
            maxDemoDurationMinutes = 10L
        )

        /**
         * Live presentation setup - Observable but not too slow
         */
        fun livePresentationConfig() = DemoModeConfig(
            isEnabled = true,
            cognitionIntervalMs = 2500L,  // Moderate speed
            freezeEvolution = true,        // Safe
            freezeReflection = false,      // Show reflection
            useSlowTransitions = true,
            showCognitiveStateIndicator = true,
            showStatisticsBadge = true,
            verboseLogging = false,        // Less spam
            maxDemoDurationMinutes = 60L   // Full hour
        )

        /**
         * Development demo - More realistic speed but still observable
         */
        fun devDemoConfig() = DemoModeConfig(
            isEnabled = true,
            cognitionIntervalMs = 1500L,
            freezeEvolution = false,       // Allow evolution
            freezeReflection = false,
            useSlowTransitions = false,    // Normal transitions
            showCognitiveStateIndicator = true,
            showStatisticsBadge = true,
            verboseLogging = true
        )
    }
}
