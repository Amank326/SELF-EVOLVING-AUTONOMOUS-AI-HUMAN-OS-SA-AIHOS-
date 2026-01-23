package com.aihos.graphics.filament

import com.google.android.filament.*
import timber.log.Timber

/**
 * AICoreMaterial - Physically-Based Material for AI Core Visualization
 *
 * Manages material properties that respond to AI cognitive state:
 * - Color and metallic/roughness for state indication
 * - Emissive properties for thinking/activity indication
 * - Real-time updates without shader recompilation
 *
 * Material Parameters Mapped to AI State:
 * - Idle: Cool blue, low emission, smooth
 * - Thinking: Bright cyan, increased emission, smooth
 * - Reflecting: Purple, pulsing emission, moderate roughness
 * - Evolving: Green, high emission, dynamic
 * - Error: Red, flashing emission, high roughness
 */
class AICoreMaterial(private val engine: Engine) {
    
    // ==================== FILAMENT MATERIAL ====================
    
    private lateinit var material: Material
    private lateinit var materialInstance: MaterialInstance
    
    // ==================== STATE-DRIVEN PROPERTIES ====================
    
    // Base color (RGB) - changes with AI state
    private var baseColor = FloatArray(3) { 0f }
    
    // Metallic [0..1] - how metal-like the surface is
    private var metallic = 0.8f
    
    // Roughness [0..1] - surface smoothness
    private var roughness = 0.3f
    
    // Emission intensity [0..1] - self-illumination
    private var emissionIntensity = 0.2f
    
    // Emission color tint
    private var emissionColor = FloatArray(3) { 1f }
    
    // Animation parameters
    private var pulsePhase = 0f
    private var pulseAmplitude = 0.3f
    
    init {
        createMaterial()
    }
    
    /**
     * Create the base PBR material
     * Uses a built-in Filament material with customizable parameters
     */
    private fun createMaterial() {
        try {
            // Create material from engine defaults
            // Filament provides default materials that can be customized
            material = Material.Builder()
                .name("AICore")
                .shadingModel(Material.ShadingModel.STANDARD) // PBR standard
                .requiredAttributes(
                    VertexAttribute.POSITION,
                    VertexAttribute.NORMAL,
                    VertexAttribute.TANGENTS,
                    VertexAttribute.COLOR
                )
                .build(engine)
            
            // Create material instance for per-instance customization
            materialInstance = material.createInstance()
            
            // Set initial state (Idle)
            setAIState(AIState.Idle)
            
            Timber.d("AICoreMaterial: Created successfully")
        } catch (e: Exception) {
            Timber.e(e, "AICoreMaterial: Creation failed")
            throw e
        }
    }
    
    /**
     * Update material based on AI cognitive state
     * Smooth transitions using emissive properties and color
     */
    fun setAIState(state: AIState) {
        when (state) {
            AIState.Idle -> {
                // Cool blue, low energy
                setBaseColor(0.1f, 0.4f, 0.6f)
                metallic = 0.8f
                roughness = 0.3f
                emissionIntensity = 0.1f
                setEmissionColor(0.1f, 0.4f, 0.8f)
            }
            
            AIState.Initializing -> {
                // Yellow, warming up
                setBaseColor(0.4f, 0.6f, 0.2f)
                metallic = 0.8f
                roughness = 0.3f
                emissionIntensity = 0.3f
                setEmissionColor(1f, 1f, 0.3f)
            }
            
            AIState.Thinking -> {
                // Bright cyan, active thinking
                setBaseColor(0.2f, 0.8f, 1f)
                metallic = 0.7f
                roughness = 0.25f
                emissionIntensity = 0.5f
                setEmissionColor(0.2f, 1f, 1f)
                pulseAmplitude = 0.4f
            }
            
            AIState.Acting -> {
                // Green, execution
                setBaseColor(0.2f, 1f, 0.4f)
                metallic = 0.6f
                roughness = 0.2f
                emissionIntensity = 0.4f
                setEmissionColor(0.2f, 1f, 0.4f)
            }
            
            AIState.Reflecting -> {
                // Purple, introspection
                setBaseColor(0.7f, 0.3f, 1f)
                metallic = 0.8f
                roughness = 0.4f
                emissionIntensity = 0.3f
                setEmissionColor(0.9f, 0.4f, 1f)
                pulseAmplitude = 0.5f // Stronger pulse for reflection
            }
            
            AIState.Evolving -> {
                // Green spirals, adaptation
                setBaseColor(0.3f, 1f, 0.3f)
                metallic = 0.5f
                roughness = 0.35f
                emissionIntensity = 0.6f
                setEmissionColor(0.3f, 1f, 0.3f)
                pulseAmplitude = 0.6f // Strongest pulse for evolution
            }
            
            AIState.Paused -> {
                // Red-orange, paused
                setBaseColor(1f, 0.4f, 0.1f)
                metallic = 0.7f
                roughness = 0.4f
                emissionIntensity = 0.2f
                setEmissionColor(1f, 0.4f, 0.1f)
            }
            
            AIState.Stopped -> {
                // Dark gray, inactive
                setBaseColor(0.2f, 0.2f, 0.2f)
                metallic = 0.6f
                roughness = 0.6f
                emissionIntensity = 0.05f
                setEmissionColor(0.5f, 0.5f, 0.5f)
            }
            
            AIState.Error -> {
                // Red, error state
                setBaseColor(1f, 0f, 0f)
                metallic = 0.4f
                roughness = 0.8f
                emissionIntensity = 0.8f
                setEmissionColor(1f, 0f, 0f)
                pulseAmplitude = 0.7f // Fast pulsing for errors
            }
        }
        
        updateMaterialProperties()
    }
    
    /**
     * Set base color (albedo)
     * This is the primary visual identifier of state
     */
    fun setBaseColor(r: Float, g: Float, b: Float) {
        baseColor[0] = r
        baseColor[1] = g
        baseColor[2] = b
    }
    
    /**
     * Set emission color tint
     * Emission is the self-illumination effect
     */
    fun setEmissionColor(r: Float, g: Float, b: Float) {
        emissionColor[0] = r
        emissionColor[1] = g
        emissionColor[2] = b
    }
    
    /**
     * Update metallic property
     * Higher values make surface more reflective like metal
     */
    fun setMetallic(value: Float) {
        metallic = value.coerceIn(0f, 1f)
    }
    
    /**
     * Update roughness property
     * Higher values make surface more diffuse (less shiny)
     */
    fun setRoughness(value: Float) {
        roughness = value.coerceIn(0f, 1f)
    }
    
    /**
     * Update emission intensity
     * Higher values make surface glow more
     */
    fun setEmissionIntensity(value: Float) {
        emissionIntensity = value.coerceIn(0f, 1f)
    }
    
    /**
     * Apply pulse animation to emission (for cognitive states)
     */
    fun updatePulseAnimation(deltaTime: Float) {
        pulsePhase = (pulsePhase + deltaTime * 2f) % (2f * Math.PI.toFloat())
        val pulseFactor = 1f + pulseAmplitude * kotlin.math.sin(pulsePhase)
        val pulsedEmission = (emissionIntensity * pulseFactor).coerceIn(0f, 1f)
        
        // Apply to material
        if (::materialInstance.isInitialized) {
            try {
                // Set emission factor with pulsing
                materialInstance.setParameter(
                    "emission",
                    pulsedEmission
                )
            } catch (e: Exception) {
                Timber.w(e, "AICoreMaterial: Could not set emission parameter")
            }
        }
    }
    
    /**
     * Update all material properties in Filament
     * Called after state changes
     */
    private fun updateMaterialProperties() {
        if (::materialInstance.isInitialized) {
            try {
                // Set base color
                materialInstance.setParameter(
                    "baseColor",
                    FloatArray(4) { i ->
                        if (i < 3) baseColor[i] else 1f
                    }
                )
                
                // Set metallic
                materialInstance.setParameter("metallic", metallic)
                
                // Set roughness
                materialInstance.setParameter("roughness", roughness)
                
                // Set emission
                materialInstance.setParameter("emission", emissionIntensity)
                
                // Set emission color
                materialInstance.setParameter(
                    "emissionColor",
                    FloatArray(4) { i ->
                        if (i < 3) emissionColor[i] else 1f
                    }
                )
                
                Timber.d("AICoreMaterial: Properties updated")
            } catch (e: Exception) {
                Timber.e(e, "AICoreMaterial: Error updating properties")
            }
        }
    }
    
    /**
     * Get the material instance for rendering
     */
    fun getMaterial(): Material = material
    
    /**
     * Get the material instance for parameter customization
     */
    fun getMaterialInstance(): MaterialInstance = materialInstance
    
    /**
     * Destroy the material and free resources
     */
    fun destroy() {
        try {
            if (::materialInstance.isInitialized) {
                material.destroyInstance(materialInstance)
            }
            if (::material.isInitialized) {
                engine.destroyMaterial(material)
            }
            Timber.d("AICoreMaterial: Destroyed")
        } catch (e: Exception) {
            Timber.e(e, "AICoreMaterial: Error during destroy")
        }
    }
}

/**
 * AI Cognitive States - Mapped to Visual Properties
 *
 * These states correspond to the AISystemController states
 * and drive the visual appearance of the AI Core
 */
enum class AIState {
    Idle,           // Waiting for input
    Initializing,   // Warming up
    Thinking,       // Reasoning
    Acting,         // Executing action
    Reflecting,     // Analyzing outcome
    Evolving,       // Modifying rules
    Paused,         // Suspended
    Stopped,        // Inactive
    Error           // Error state
}
