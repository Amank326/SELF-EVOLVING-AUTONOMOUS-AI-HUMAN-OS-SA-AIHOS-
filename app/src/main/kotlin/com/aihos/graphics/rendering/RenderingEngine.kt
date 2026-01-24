package com.aihos.graphics.rendering

import com.aihos.domain.model.CognitiveState

/**
 * Quality level for graphics rendering.
 */
enum class RenderingQuality {
    HIGH,    // Full resolution, all effects
    MEDIUM,  // Reduced resolution, simplified effects
    LOW,     // Minimal rendering, performance mode
    OFF      // No rendering
}

/**
 * Rendering parameters derived from cognitive state.
 */
data class RenderingParameters(
    val quality: RenderingQuality,
    val opacity: Float,
    val particleCount: Int,
    val animationSpeed: Float,
    val colorIntensity: Float,
    val updateFrequency: Float  // Hz
)

/**
 * Converts cognitive state to visual representation parameters.
 */
interface VisualizationMapper {
    /**
     * Map cognitive state to rendering parameters.
     */
    fun mapToRenderingParameters(state: CognitiveState): RenderingParameters
    
    /**
     * Map execution phase to color.
     */
    fun mapPhaseToColor(phase: String): String
    
    /**
     * Map confidence to visual intensity.
     */
    fun mapConfidenceToIntensity(confidence: Float): Float
}

/**
 * Rendering engine interface.
 * Handles native 3D/AR rendering.
 */
interface RenderingEngine {
    /**
     * Initialize the rendering engine.
     */
    suspend fun initialize()
    
    /**
     * Render with given parameters.
     */
    suspend fun render(parameters: RenderingParameters)
    
    /**
     * Update rendering quality based on device constraints.
     */
    suspend fun setQuality(quality: RenderingQuality)
    
    /**
     * Clean up resources.
     */
    suspend fun cleanup()
    
    /**
     * Check if rendering engine is ready.
     */
    fun isReady(): Boolean
}

/**
 * Bridge between Android/Kotlin and graphics rendering system.
 * Handles serialization, threading, and lifecycle.
 */
interface GraphicsAndroidBridge {
    /**
     * Send cognitive state update to graphics system.
     */
    suspend fun updateCognitiveState(state: CognitiveState)
    
    /**
     * Send rendering quality update.
     */
    suspend fun updateRenderingQuality(quality: RenderingQuality)
    
    /**
     * Initialize graphics system.
     */
    suspend fun initialize()
    
    /**
     * Cleanup graphics system.
     */
    suspend fun cleanup()
}
