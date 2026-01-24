package com.aihos.system.signals.impl.providers

import android.content.Context
import android.os.BatteryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Temperature signal provider.
 *
 * Responsibilities:
 * - Poll battery manager for device temperature
 * - Update temperature state periodically
 * - Provide current temperature via StateFlow
 * - Ensure polling loop is stopped on unregister
 *
 * Safety:
 * - Polling loop runs in separate coroutine scope
 * - Polling can be safely started/stopped
 * - Thread-safe StateFlow
 *
 * Behavior:
 * - Polls temperature every 10 seconds (configurable)
 * - Uses BatteryManager.BATTERY_PROPERTY_TEMPERATURE (Android 5.0+)
 * - Returns temperature in Celsius, range 20-60°C (clamped)
 * - Confidence: 0.75f (estimated from BatteryManager, not direct sensor)
 *
 * Note: Requires no additional permissions (BatteryManager is system API)
 */
class TemperatureProvider(
    private val context: Context,
    private val pollIntervalMs: Long = 10_000L // Poll every 10 seconds
) {

    private val _value = MutableStateFlow(35f) // Default: room temperature
    val value: StateFlow<Float> = _value.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isRegistered = false

    /**
     * Start temperature polling.
     */
    fun register() {
        if (isRegistered) {
            Timber.d("TemperatureProvider: Already registered, skipping")
            return
        }

        try {
            isRegistered = true
            scope.launch {
                pollTemperature()
            }
            Timber.d("TemperatureProvider: Polling started")

        } catch (e: Exception) {
            Timber.e(e, "TemperatureProvider: Failed to start polling")
            isRegistered = false
            throw e
        }
    }

    /**
     * Stop temperature polling.
     */
    fun unregister() {
        if (!isRegistered) {
            Timber.d("TemperatureProvider: Not registered, skipping")
            return
        }

        isRegistered = false
        Timber.d("TemperatureProvider: Polling stopped")
    }

    /**
     * Polling loop that runs continuously while registered.
     * Emits new temperature every pollIntervalMs milliseconds.
     */
    private suspend fun pollTemperature() {
        while (isRegistered) {
            try {
                val temperature = getCurrentTemperature()
                _value.emit(temperature)
                Timber.v("TemperatureProvider: Temperature polled: ${temperature}°C")

            } catch (e: Exception) {
                Timber.w(e, "TemperatureProvider: Error polling temperature")
            }

            delay(pollIntervalMs)
        }
        Timber.d("TemperatureProvider: Polling loop stopped")
    }

    /**
     * Get current device temperature from BatteryManager.
     * Returns temperature in Celsius.
     * Returns default (35°C) on error.
     */
    private fun getCurrentTemperature(): Float {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE)
                as? BatteryManager ?: return 35f

            // BatteryManager.getIntProperty returns temperature in 0.1°C units
            // e.g., 350 = 35.0°C
            // Requires API 21+
            val tempCentigrade = batteryManager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_TEMPERATURE
            )

            // Convert from 0.1°C units to °C
            val tempCelsius = tempCentigrade / 10.0f

            // Clamp to reasonable range: 20-60°C
            // Outside this range likely indicates sensor error
            val clampedTemp = tempCelsius.coerceIn(20f, 60f)

            // Calculate confidence based on range
            val confidence = if (tempCelsius in 20f..60f) 0.75f else 0.5f
            Timber.v("Temperature: $clampedTemp°C (confidence: $confidence)")

            clampedTemp

        } catch (e: Exception) {
            Timber.w(e, "TemperatureProvider: Failed to get temperature, returning default")
            35f // Default room temperature on error
        }
    }
}
