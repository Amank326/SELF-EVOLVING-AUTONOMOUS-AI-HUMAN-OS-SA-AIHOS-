package com.aihos.ai

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AIApplication - Singleton managing the AI system lifecycle
 */
object AIApplication {
    private var coordinator: AICoordinator? = null
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Initialize AI system - call from MainActivity.onCreate()
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        scope.launch {
            coordinator = AICoordinator(scope)
            coordinator?.initialize()
            coordinator?.startAILoop(cycleIntervalMs = 2000) // Cycle every 2 seconds
            isInitialized = true
            android.util.Log.d("SAI-HOS", "AI System initialized and started")
        }
    }

    /**
     * Get AI Coordinator instance
     */
    fun getCoordinator(): AICoordinator? = coordinator

    /**
     * Get current system state
     */
    fun getSystemState(): com.aihos.ai.AISystemState? = coordinator?.systemState?.value

    /**
     * Get quick status
     */
    fun getQuickStatus(): Map<String, Any> = coordinator?.getQuickStatus() ?: emptyMap()

    /**
     * Get all metrics
     */
    fun getAllMetrics(): Map<String, Any> = coordinator?.getAllMetrics() ?: emptyMap()

    /**
     * Make autonomous decision
     */
    fun makeDecision(context: String): String? {
        val coordinator = coordinator ?: return null
        var result: String? = null
        scope.launch {
            result = coordinator.makeAutonomousDecision(context)
        }
        return result
    }

    /**
     * Teach system new skill
     */
    fun teachSkill(skillName: String, proficiency: Float = 0.1f) {
        scope.launch {
            coordinator?.teachSkill(skillName, proficiency)
        }
    }

    /**
     * Shutdown AI system - call from MainActivity.onDestroy()
     */
    fun shutdown() {
        scope.launch {
            coordinator?.shutdown()
            coordinator = null
            isInitialized = false
            android.util.Log.d("SAI-HOS", "AI System shutdown")
        }
    }

    /**
     * Check if AI is running
     */
    fun isRunning(): Boolean = coordinator?.isRunning?.value ?: false

    /**
     * Get system broadcast (for UI updates)
     */
    fun getBroadcast(): AIBroadcast? = coordinator?.broadcast?.value
}
