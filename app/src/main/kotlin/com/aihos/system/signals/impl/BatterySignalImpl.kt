package com.aihos.system.signals.impl

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.aihos.system.signals.BatterySignal
import timber.log.Timber

/**
 * Implementation of BatterySignal.
 * Monitors device battery level and charging state.
 */
class BatterySignalImpl(private val context: Context) : BatterySignal {

    private val _value = MutableStateFlow(getCurrentBatteryLevel())
    override val value: StateFlow<Float> = _value.asStateFlow()

    private val _confidence = MutableStateFlow(1.0f)
    override val confidence: StateFlow<Float> = _confidence.asStateFlow()

    override val name: String = "battery"

    /**
     * Get current battery level from BatteryManager.
     * Returns 0.0 to 100.0 representing percentage.
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
            50.0f // Default if unavailable
        }
    }

    /**
     * Update battery level.
     * Called periodically by SignalCollector.
     */
    suspend fun updateBatteryLevel() {
        val newLevel = getCurrentBatteryLevel()
        _value.emit(newLevel)
        // Battery signal has high confidence
        _confidence.emit(0.95f)
        Timber.d("Battery updated: $newLevel%")
    }
}
