package com.aihos.graphics.bridge

import com.aihos.ai.autonomy.AISystemController
import com.aihos.graphics.filament.AICoreMaterial
import com.aihos.graphics.filament.AIState
import com.aihos.graphics.filament.Native3DEngine
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * AI State to 3D Bridge
 *
 * Connects the AI System Controller to the Native3DEngine,
 * translating AI cognitive state into visual representations.
 *
 * Responsibilities:
 * - Map AISystemController.AIState to Filament AIState
 * - Update material properties based on AI metrics
 * - Drive procedural animations from AI cycle times
 * - Manage lifecycle synchronization
 */
class AI3DBridge(
    private val engine: Native3DEngine,
    private val scope: CoroutineScope
) {
    
    private var isActive = false
    private var updateJob: Job? = null
    
    /**
     * Start observing AI state and updating 3D visuals
     */
    fun start() {
        isActive = true
        Timber.d("AI3DBridge: Started")
    }
    
    /**
     * Stop updating 3D visuals
     */
    fun stop() {
        isActive = false
        updateJob?.cancel()
        Timber.d("AI3DBridge: Stopped")
    }
    
    /**
     * Update 3D visuals based on AI state
     * Called from ViewModel or AISystemController observer
     */
    fun updateFromAIState(
        aiState: AISystemController.AIState,
        cycleMetrics: AISystemController.CycleMetrics,
        lastDecision: AISystemController.CognitiveDecision?,
        lastInsight: AISystemController.ReflectionInsight?
    ) {
        if (!isActive) return
        
        scope.launch(Dispatchers.Main) {
            try {
                // Update material state
                updateMaterialFromState(aiState, cycleMetrics)
                
                // Update procedural animation
                updateAnimationFromMetrics(cycleMetrics)
                
                // Update lighting based on confidence
                updateLightingFromState(aiState, cycleMetrics)
            } catch (e: Exception) {
                Timber.e(e, "AI3DBridge: Error updating 3D visuals")
            }
        }
    }
    
    /**
     * Map AI cognitive state to 3D material properties
     */
    private fun updateMaterialFromState(
        aiState: AISystemController.AIState,
        metrics: AISystemController.CycleMetrics
    ) {
        val material = engine.getAICoreMaterial() ?: return
        
        // Map AI state to visual state
        val visualState = when (aiState) {
            AISystemController.AIState.Idle -> AIState.Idle
            AISystemController.AIState.Initializing -> AIState.Initializing
            AISystemController.AIState.Thinking -> AIState.Thinking
            AISystemController.AIState.Acting -> AIState.Acting
            AISystemController.AIState.Reflecting -> AIState.Reflecting
            AISystemController.AIState.Evolving -> AIState.Evolving
            AISystemController.AIState.Paused -> AIState.Paused
            AISystemController.AIState.Stopped -> AIState.Stopped
            is AISystemController.AIState.Error -> AIState.Error
        }
        
        // Update material
        material.setAIState(visualState)
        
        // Adjust roughness based on decision confidence if available
        val confidenceLevel = when (aiState) {
            AISystemController.AIState.Thinking -> 0.3f
            AISystemController.AIState.Acting -> 0.2f
            AISystemController.AIState.Reflecting -> 0.4f
            AISystemController.AIState.Evolving -> 0.35f
            else -> 0.5f
        }
        material.setRoughness(confidenceLevel)
        
        Timber.d("AI3DBridge: Material updated to state=$visualState, roughness=$confidenceLevel")
    }
    
    /**
     * Drive procedural animation from cycle metrics
     * Animation intensity represents cognitive load
     */
    private fun updateAnimationFromMetrics(metrics: AISystemController.CycleMetrics) {
        // Calculate rotation speed from cycle time
        // Faster cycles = faster rotation
        val rotationSpeed = if (metrics.lastCycleTimeMs > 0) {
            16.67f / metrics.lastCycleTimeMs.coerceAtLeast(1)
        } else {
            1f
        }
        
        // Current time-based rotation (continuous)
        val timeRadians = (System.currentTimeMillis() % 6283) / 1000f // 2π seconds for full rotation
        
        // Rotation target based on cognitive phase
        val rotationX = kotlin.math.sin(timeRadians * rotationSpeed) * 0.3f
        val rotationY = timeRadians * rotationSpeed * 0.5f
        val rotationZ = kotlin.math.cos(timeRadians * rotationSpeed) * 0.2f
        
        engine.setRotationTarget(rotationX, rotationY, rotationZ)
        
        // Scale based on cycle health
        val health = if (metrics.targetCycleTimeMs == 0L) 100
        else ((metrics.targetCycleTimeMs.toFloat() / metrics.lastCycleTimeMs.coerceAtLeast(1)) * 100).toInt()
        
        val scale = 0.9f + (health / 200f) * 0.2f  // Scale between 0.9 and 1.1
        engine.setScaleTarget(scale)
    }
    
    /**
     * Update lighting intensity based on AI state
     * Lights up during active thinking/acting, dims during idle
     */
    private fun updateLightingFromState(
        aiState: AISystemController.AIState,
        metrics: AISystemController.CycleMetrics
    ) {
        val baseIntensity = when (aiState) {
            AISystemController.AIState.Idle -> 20000f
            AISystemController.AIState.Initializing -> 25000f
            AISystemController.AIState.Thinking -> 35000f
            AISystemController.AIState.Acting -> 40000f
            AISystemController.AIState.Reflecting -> 30000f
            AISystemController.AIState.Evolving -> 45000f
            AISystemController.AIState.Paused -> 15000f
            AISystemController.AIState.Stopped -> 10000f
            is AISystemController.AIState.Error -> 50000f
        }
        
        // Modulate by cycle health
        val health = if (metrics.targetCycleTimeMs == 0L) 100
        else ((metrics.targetCycleTimeMs.toFloat() / metrics.lastCycleTimeMs.coerceAtLeast(1)) * 100).toInt()
        
        val modulation = 0.8f + (health / 200f) * 0.4f  // 80% to 100% modulation
        val finalIntensity = baseIntensity * modulation
        
        engine.setLightIntensity(finalIntensity)
    }
    
    /**
     * Destroy the bridge
     */
    fun destroy() {
        stop()
        Timber.d("AI3DBridge: Destroyed")
    }
}

/**
 * Extension function to easily create bridge from ViewModel
 */
fun Native3DEngine.createAI3DBridge(scope: CoroutineScope): AI3DBridge {
    return AI3DBridge(this, scope)
}
