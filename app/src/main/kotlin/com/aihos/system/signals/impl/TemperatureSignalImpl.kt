package com.aihos.system.signals.impl

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.aihos.system.signals.TemperatureSignal
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of TemperatureSignal.
 * Monitors device temperature.
 */
class TemperatureSignalImpl(private val context: Context) : TemperatureSignal {

    private val _value = MutableStateFlow(getCurrentTemperature())
    override val value: StateFlow<Float> = _value.asStateFlow()

    private val _confidence = MutableStateFlow(0.8f)
    override val confidence: StateFlow<Float> = _confidence.asStateFlow()

    override val name: String = "temperature"

    /**
     * Get current device temperature.
     * Android 5.0+ exposes this via BatteryManager.
     * Returns temperature in Celsius.
     */
    private fun getCurrentTemperature(): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val temp = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE) ?: 350
                // Convert from tenths of Celsius to Celsius
                (temp / 10.0f).coerceIn(20.0f, 60.0f)
            } catch (e: Exception) {
                Timber.e(e, "Error getting device temperature")
                35.0f  // Default temperature
            }
        } else {
            35.0f  // Default for older versions
        }
    }

    /**
     * Update temperature reading.
     * Called periodically by SignalCollector.
     */
    suspend fun updateTemperature() {
        val newTemp = getCurrentTemperature()
        _value.emit(newTemp)
        
        // Temperature readings on Android have moderate confidence
        // (estimated from BatteryManager, not direct sensor)
        _confidence.emit(0.75f)
        Timber.d("Temperature updated: ${newTemp}°C")
    }
}
