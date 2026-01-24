package com.aihos.system.signals.impl

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.aihos.system.signals.DeviceContext
import com.aihos.system.signals.SignalCollector
import timber.log.Timber

/**
 * Default implementation of DeviceContext.
 */
data class DeviceContextImpl(
    override val batteryLevel: Float = 50.0f,
    override val temperature: Float = 35.0f,
    override val isScreenOn: Boolean = true,
    override val foregroundApp: String = "com.android.launcher",
    override val usageIntensity: Float = 0.5f,
    override val timeOfDay: Float = 0.5f,
    override val isNetworkConnected: Boolean = true,
    override val timestamp: Long = System.currentTimeMillis()
) : DeviceContext

/**
 * Implementation of SignalCollector.
 * Aggregates all device signals into a unified DeviceContext.
 */
class SignalCollectorImpl(
    private val batterySignal: BatterySignalImpl,
    private val temperatureSignal: TemperatureSignalImpl
) : SignalCollector {

    private val _deviceContext = MutableStateFlow<DeviceContext>(
        DeviceContextImpl()
    )
    override val deviceContext: StateFlow<DeviceContext> = _deviceContext.asStateFlow()

    private var isCollecting = false

    override suspend fun start() {
        Timber.d("SignalCollector: Starting signal collection")
        isCollecting = true
        
        // TODO: Set up periodic update task
        // updateSignals()
    }

    override suspend fun stop() {
        Timber.d("SignalCollector: Stopping signal collection")
        isCollecting = false
    }

    /**
     * Aggregate all signals into device context.
     * Called periodically.
     */
    private suspend fun updateSignals() {
        if (!isCollecting) return

        // Update individual signals
        batterySignal.updateBatteryLevel()
        temperatureSignal.updateTemperature()

        // Aggregate into device context
        val context = DeviceContextImpl(
            batteryLevel = batterySignal.value.value,
            temperature = temperatureSignal.value.value,
            isScreenOn = true,  // TODO: Get from actual system
            foregroundApp = "com.android.launcher",  // TODO: Get from actual system
            usageIntensity = calculateUsageIntensity(),
            timeOfDay = calculateTimeOfDay(),
            isNetworkConnected = true,  // TODO: Get from actual system
            timestamp = System.currentTimeMillis()
        )

        _deviceContext.emit(context)
        Timber.d("DeviceContext updated: battery=${context.batteryLevel}%, temp=${context.temperature}°C")
    }

    /**
     * Calculate usage intensity from various factors.
     */
    private fun calculateUsageIntensity(): Float {
        // Simple heuristic: based on time of day and battery
        val timeOfDay = calculateTimeOfDay()
        val batteryFactor = batterySignal.value.value / 100.0f
        return (timeOfDay + batteryFactor) / 2.0f
    }

    /**
     * Calculate time of day as 0.0 (midnight) to 1.0 (next midnight).
     */
    private fun calculateTimeOfDay(): Float {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        return ((hour * 60 + minute).toFloat() / (24.0f * 60.0f))
    }
}
