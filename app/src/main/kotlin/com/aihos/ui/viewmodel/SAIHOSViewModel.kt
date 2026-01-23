package com.aihos.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aihos.ai.AISystemController
import com.aihos.ai.autonomy.AutonomyController
import com.aihos.ai.evolution.EvolutionEngine
import com.aihos.ai.memory.MemorySystem
import com.aihos.ai.reasoning.ReasoningEngine
import com.aihos.ai.reflection.ReflectionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * SA-AIHOS ViewModel (Refactored)
 *
 * Wraps AISystemController for safe UI consumption.
 *
 * Responsibilities:
 * - Initialize and manage AI system lifecycle
 * - Expose AI state as Flows for UI to observe
 * - Handle lifecycle events (onCleared)
 * - Safe coroutine scope management
 *
 * This is the interface between the AI system and the UI layer.
 * UI never touches AISystemController directly; all state comes through here.
 */
@HiltViewModel
class SAIHOSViewModel @Inject constructor(
    application: Application,
    private val reasoningEngine: ReasoningEngine,
    private val reflectionEngine: ReflectionEngine,
    private val evolutionEngine: EvolutionEngine,
    private val memorySystem: MemorySystem,
    private val autonomyController: AutonomyController
) : AndroidViewModel(application) {

    // AI System Controller instance
    private val aiSystemController = AISystemController(
        context = application.applicationContext,
        reasoningEngine = reasoningEngine,
        reflectionEngine = reflectionEngine,
        evolutionEngine = evolutionEngine,
        memorySystem = memorySystem,
        scope = viewModelScope
    )

    // ==================== PUBLIC STATE FLOWS ====================

    /**
     * Current AI state (Idle, Thinking, Acting, Reflecting, Evolving, Paused, Stopped, Error)
     * UI subscribes to this to show AI status
     */
    val aiState: StateFlow<AISystemController.AIState> = aiSystemController.aiState

    /**
     * Current execution phase (for minimal indicator or animation)
     */
    val executionPhase: StateFlow<AISystemController.ExecutionPhase> = aiSystemController.executionPhase

    /**
     * Last made decision (for showing reasoning)
     */
    val lastDecision: StateFlow<AISystemController.CognitiveDecision?> = aiSystemController.lastDecision

    /**
     * Last reflection insight (for showing learning)
     */
    val lastInsight: StateFlow<AISystemController.ReflectionInsight?> = aiSystemController.lastInsight

    /**
     * Cycle performance metrics (for performance monitoring)
     */
    val cycleMetrics: StateFlow<AISystemController.CycleMetrics> = aiSystemController.cycleMetrics

    /**
     * Evolution events (for showing when rules change)
     */
    val evolutionEvents: SharedFlow<AISystemController.EvolutionEvent> = aiSystemController.evolutionEvents

    // ==================== LIFECYCLE MANAGEMENT ====================

    /**
     * Start the AI system.
     * Called from MainActivity onCreate or when app comes to foreground.
     */
    fun startAI() {
        Timber.d("ViewModel: Starting AI system")
        aiSystemController.start()
    }

    /**
     * Pause the AI system (app going to background).
     * State is preserved.
     */
    fun pauseAI() {
        Timber.d("ViewModel: Pausing AI system")
        aiSystemController.pause()
    }

    /**
     * Resume the AI system (app coming to foreground).
     */
    fun resumeAI() {
        Timber.d("ViewModel: Resuming AI system")
        aiSystemController.resume()
    }

    /**
     * Called automatically when ViewModel is destroyed (activity destroyed).
     * Stops AI system and cleans up resources.
     */
    override fun onCleared() {
        Timber.d("ViewModel: Cleared (destroying AI system)")
        aiSystemController.stop()
        super.onCleared()
    }

    // ==================== UI STATE HELPERS ====================

    /**
     * Human-readable description of current AI state.
     * Useful for status indicators.
     */
    fun getStateDescription(): String = when (aiState.value) {
        AISystemController.AIState.Idle -> "Idle"
        AISystemController.AIState.Initializing -> "Initializing..."
        AISystemController.AIState.Thinking -> "Thinking"
        AISystemController.AIState.Acting -> "Acting"
        AISystemController.AIState.Reflecting -> "Reflecting"
        AISystemController.AIState.Evolving -> "Evolving"
        AISystemController.AIState.Paused -> "Paused"
        AISystemController.AIState.Stopped -> "Stopped"
        is AISystemController.AIState.Error -> "Error"
    }

    /**
     * Get color for current state (for UI visualization).
     */
    fun getStateColor(): String = when (aiState.value) {
        AISystemController.AIState.Idle -> "#808080" // Gray
        AISystemController.AIState.Initializing -> "#FFFF00" // Yellow
        AISystemController.AIState.Thinking -> "#0088FF" // Blue
        AISystemController.AIState.Acting -> "#00FF00" // Green
        AISystemController.AIState.Reflecting -> "#FF8800" // Orange
        AISystemController.AIState.Evolving -> "#FF00FF" // Magenta
        AISystemController.AIState.Paused -> "#FF0000" // Red
        AISystemController.AIState.Stopped -> "#000000" // Black
        is AISystemController.AIState.Error -> "#FF0000" // Red
    }

    /**
     * Check if AI is currently running and not paused.
     */
    val isAIActive: Boolean
        get() = aiState.value !in listOf(
            AISystemController.AIState.Idle,
            AISystemController.AIState.Paused,
            AISystemController.AIState.Stopped
        )

    /**
     * Get current cycle performance as percentage of target.
     * 100% means we're meeting 60 FPS target.
     * >100% means we're slower than target.
     */
}
