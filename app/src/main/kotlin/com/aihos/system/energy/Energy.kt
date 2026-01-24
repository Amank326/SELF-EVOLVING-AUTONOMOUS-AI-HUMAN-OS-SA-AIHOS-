package com.aihos.system.energy

import kotlinx.coroutines.flow.StateFlow

/**
 * Energy state of the device.
 */
enum class EnergyState {
    ABUNDANT,  // Battery > 50%, plugged in
    NORMAL,    // Battery 25-50%
    LOW,       // Battery 15-25%
    CRITICAL   // Battery < 15%
}

/**
 * Thermal state of the device.
 */
enum class ThermalState {
    NORMAL,    // < 35°C
    LIGHT,     // 35-40°C
    MODERATE,  // 40-45°C
    SEVERE,    // 45-50°C
    CRITICAL   // > 50°C
}

/**
 * Constraint on AI cognition based on device state.
 */
data class CognitiveConstraint(
    val maxDecisionsPerSecond: Float,
    val maxMemoryUsage: Long,
    val maxCpuUsage: Float,
    val reason: String
)

/**
 * Manages device energy state and provides constraints.
 */
interface EnergyManager {
    /**
     * Current energy state.
     */
    val energyState: StateFlow<EnergyState>
    
    /**
     * Current battery percentage (0-100).
     */
    val batteryPercentage: StateFlow<Float>
    
    /**
     * Get cognitive constraints based on current energy state.
     */
    suspend fun getConstraints(): CognitiveConstraint
    
    /**
     * Register a listener for energy state changes.
     */
    fun onEnergyStateChanged(callback: (EnergyState) -> Unit)
}

/**
 * Manages device thermal state and provides constraints.
 */
interface ThermalManager {
    /**
     * Current thermal state.
     */
    val thermalState: StateFlow<ThermalState>
    
    /**
     * Current device temperature (celsius).
     */
    val temperature: StateFlow<Float>
    
    /**
     * Get cognitive constraints based on current thermal state.
     */
    suspend fun getConstraints(): CognitiveConstraint
    
    /**
     * Register a listener for thermal state changes.
     */
    fun onThermalStateChanged(callback: (ThermalState) -> Unit)
}

/**
 * Manages constraint-aware cognition.
 * Adapts AI behavior based on energy and thermal constraints.
 */
interface ConstraintManager {
    /**
     * Get effective constraints from both energy and thermal managers.
     */
    suspend fun getEffectiveConstraints(): CognitiveConstraint
    
    /**
     * Check if AI should continue operating under current constraints.
     */
    suspend fun canContinueOperation(): Boolean
    
    /**
     * Get recommended cognition cycle frequency (Hz) based on constraints.
     */
    suspend fun getRecommendedCycleFrequency(): Float
}
