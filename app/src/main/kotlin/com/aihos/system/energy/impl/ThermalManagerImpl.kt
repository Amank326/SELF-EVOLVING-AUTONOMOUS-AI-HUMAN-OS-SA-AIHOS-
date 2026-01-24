package com.aihos.system.energy.impl

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.aihos.system.energy.CognitiveConstraint
import com.aihos.system.energy.ThermalManager
import com.aihos.system.energy.ThermalState
import timber.log.Timber

/**
 * Implementation of ThermalManager.
 * Monitors device temperature and provides thermal-aware constraints.
 */
class ThermalManagerImpl : ThermalManager {

    private val _thermalState = MutableStateFlow(ThermalState.NORMAL)
    override val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()

    private val _temperature = MutableStateFlow(35.0f)
    override val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val thermalStateChangeCallbacks = mutableListOf<(ThermalState) -> Unit>()

    /**
     * Determine thermal state from temperature.
     */
    private fun determineThermalState(temp: Float): ThermalState {
        return when {
            temp < 35.0f -> ThermalState.NORMAL
            temp in 35.0f..40.0f -> ThermalState.LIGHT
            temp in 40.0f..45.0f -> ThermalState.MODERATE
            temp in 45.0f..50.0f -> ThermalState.SEVERE
            else -> ThermalState.CRITICAL
        }
    }

    /**
     * Update temperature reading.
     * Called periodically by system.
     */
    suspend fun updateTemperature(newTemp: Float) {
        val newState = determineThermalState(newTemp)
        
        _temperature.emit(newTemp)
        
        if (newState != _thermalState.value) {
            _thermalState.emit(newState)
            onThermalStateChanged(newState)
            Timber.d("Thermal state changed: ${newState} (temperature: ${newTemp}°C)")
        }
    }

    override suspend fun getConstraints(): CognitiveConstraint {
        return when (thermalState.value) {
            ThermalState.NORMAL -> CognitiveConstraint(
                maxDecisionsPerSecond = 10.0f,
                maxMemoryUsage = 512 * 1024,
                maxCpuUsage = 0.8f,
                reason = "Temperature normal, full cognition available"
            )
            ThermalState.LIGHT -> CognitiveConstraint(
                maxDecisionsPerSecond = 5.0f,
                maxMemoryUsage = 256 * 1024,
                maxCpuUsage = 0.5f,
                reason = "Temperature warm, moderate cognition to reduce heat"
            )
            ThermalState.MODERATE -> CognitiveConstraint(
                maxDecisionsPerSecond = 2.0f,
                maxMemoryUsage = 128 * 1024,
                maxCpuUsage = 0.3f,
                reason = "Temperature moderate, reduced cognition to cool device"
            )
            ThermalState.SEVERE -> CognitiveConstraint(
                maxDecisionsPerSecond = 1.0f,
                maxMemoryUsage = 64 * 1024,
                maxCpuUsage = 0.1f,
                reason = "Temperature severe, minimal cognition to prevent overheating"
            )
            ThermalState.CRITICAL -> CognitiveConstraint(
                maxDecisionsPerSecond = 0.1f,
                maxMemoryUsage = 32 * 1024,
                maxCpuUsage = 0.05f,
                reason = "Temperature critical, pausing cognition to cool device"
            )
        }
    }

    override fun onThermalStateChanged(callback: (ThermalState) -> Unit) {
        thermalStateChangeCallbacks.add(callback)
    }

    private fun onThermalStateChanged(state: ThermalState) {
        thermalStateChangeCallbacks.forEach { it(state) }
    }
}
