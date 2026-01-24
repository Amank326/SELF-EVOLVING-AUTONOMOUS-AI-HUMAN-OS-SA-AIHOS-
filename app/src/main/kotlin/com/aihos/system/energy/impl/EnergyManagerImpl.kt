package com.aihos.system.energy.impl

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.aihos.system.energy.CognitiveConstraint
import com.aihos.system.energy.EnergyManager
import com.aihos.system.energy.EnergyState
import timber.log.Timber

/**
 * Implementation of EnergyManager.
 * Monitors device battery and provides energy-aware constraints.
 */
class EnergyManagerImpl(private val context: Context) : EnergyManager {

    private val _energyState = MutableStateFlow(EnergyState.NORMAL)
    override val energyState: StateFlow<EnergyState> = _energyState.asStateFlow()

    private val _batteryPercentage = MutableStateFlow(50.0f)
    override val batteryPercentage: StateFlow<Float> = _batteryPercentage.asStateFlow()

    private val energyStateChangeCallbacks = mutableListOf<(EnergyState) -> Unit>()

    init {
        // Initialize with current battery level
        updateBatteryLevel()
    }

    /**
     * Get current battery percentage.
     */
    private fun getCurrentBatteryLevel(): Float {
        val batteryIntent: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }

        return if (batteryIntent != null) {
            val level: Int = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            (level.toFloat() / scale.toFloat()) * 100.0f
        } else {
            50.0f
        }
    }

    /**
     * Determine energy state from battery percentage.
     */
    private fun determineEnergyState(battery: Float, isCharging: Boolean = false): EnergyState {
        return when {
            battery > 50.0f || isCharging -> EnergyState.ABUNDANT
            battery in 25.0f..50.0f -> EnergyState.NORMAL
            battery in 15.0f..25.0f -> EnergyState.LOW
            else -> EnergyState.CRITICAL
        }
    }

    /**
     * Update battery level and state.
     * Called periodically.
     */
    suspend fun updateBatteryLevel() {
        val newBattery = getCurrentBatteryLevel()
        val newState = determineEnergyState(newBattery)
        
        _batteryPercentage.emit(newBattery)
        
        if (newState != _energyState.value) {
            _energyState.emit(newState)
            onEnergyStateChanged(newState)
            Timber.d("Energy state changed: ${newState} (battery: ${newBattery}%)")
        }
    }

    override suspend fun getConstraints(): CognitiveConstraint {
        return when (energyState.value) {
            EnergyState.ABUNDANT -> CognitiveConstraint(
                maxDecisionsPerSecond = 10.0f,
                maxMemoryUsage = 512 * 1024,  // 512 KB
                maxCpuUsage = 0.8f,
                reason = "Battery abundant, full cognition available"
            )
            EnergyState.NORMAL -> CognitiveConstraint(
                maxDecisionsPerSecond = 5.0f,
                maxMemoryUsage = 256 * 1024,  // 256 KB
                maxCpuUsage = 0.5f,
                reason = "Normal battery, moderate cognition"
            )
            EnergyState.LOW -> CognitiveConstraint(
                maxDecisionsPerSecond = 2.0f,
                maxMemoryUsage = 128 * 1024,  // 128 KB
                maxCpuUsage = 0.3f,
                reason = "Low battery, reduced cognition to preserve power"
            )
            EnergyState.CRITICAL -> CognitiveConstraint(
                maxDecisionsPerSecond = 0.5f,
                maxMemoryUsage = 64 * 1024,   // 64 KB
                maxCpuUsage = 0.1f,
                reason = "Critical battery, minimal cognition to save power"
            )
        }
    }

    override fun onEnergyStateChanged(callback: (EnergyState) -> Unit) {
        energyStateChangeCallbacks.add(callback)
    }

    private fun onEnergyStateChanged(state: EnergyState) {
        energyStateChangeCallbacks.forEach { it(state) }
    }
}
