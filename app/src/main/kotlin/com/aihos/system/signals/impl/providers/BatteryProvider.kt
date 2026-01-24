package com.aihos.system.signals.impl.providers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Battery level and charging state signal provider.
 *
 * Responsibilities:
 * - Listen to battery changed broadcasts
 * - Update battery level and charging status
 * - Ensure broadcast receiver is properly unregistered
 * - Provide current battery value via StateFlow
 *
 * Safety:
 * - Broadcast receiver is unregistered in unregister() to prevent memory leaks
 * - Null checks on battery info
 * - Thread-safe StateFlow
 *
 * Behavior:
 * - Emits immediately on first registration (queries battery status)
 * - Emits on battery change broadcast from system
 * - Throttled to prevent excessive updates (handled by caller)
 */
class BatteryProvider(private val context: Context) {

    private val _value = MutableStateFlow(80f) // Default 80%
    val value: StateFlow<Float> = _value.asStateFlow()

    private var receiver: BatteryBroadcastReceiver? = null
    private var isRegistered = false

    /**
     * Register battery broadcast receiver.
     * Safe: Checks if already registered, updates state immediately.
     */
    fun register() {
        if (isRegistered) {
            Timber.d("BatteryProvider: Already registered, skipping")
            return
        }

        try {
            // Create and register receiver
            receiver = BatteryBroadcastReceiver { level ->
                _value.tryEmit(level)
            }

            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(receiver, filter)
            isRegistered = true

            // Get current battery level immediately
            val batteryLevel = getBatteryLevel()
            _value.tryEmit(batteryLevel)

            Timber.d("BatteryProvider: Registered successfully, current level: ${batteryLevel}%")

        } catch (e: Exception) {
            Timber.e(e, "BatteryProvider: Failed to register battery receiver")
            receiver = null
            isRegistered = false
            throw e
        }
    }

    /**
     * Unregister battery broadcast receiver.
     * Safe: Checks if registered before unregistering.
     * Critical: Prevents memory leak from unmanaged broadcast receiver.
     */
    fun unregister() {
        if (!isRegistered || receiver == null) {
            Timber.d("BatteryProvider: Not registered or already unregistered")
            return
        }

        try {
            context.unregisterReceiver(receiver)
            isRegistered = false
            receiver = null
            Timber.d("BatteryProvider: Unregistered successfully")

        } catch (e: Exception) {
            // Receiver might not have been registered (system quirk)
            Timber.w(e, "BatteryProvider: Failed to unregister (may not have been registered)")
            isRegistered = false
            receiver = null
        }
    }

    /**
     * Get current battery level without relying on broadcast.
     * Used for immediate state on first registration.
     */
    private fun getBatteryLevel(): Float {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level >= 0 && scale > 0) {
                ((level / scale.toFloat()) * 100).coerceIn(0f, 100f)
            } else {
                80f // Default if unable to determine
            }
        } catch (e: Exception) {
            Timber.w(e, "BatteryProvider: Failed to get battery level")
            80f // Default on error
        }
    }

    /**
     * Broadcast receiver for battery change events.
     * Creates new instance on each register to ensure clean state.
     */
    private class BatteryBroadcastReceiver(
        private val onLevelChange: (Float) -> Unit
    ) : android.content.BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return

            try {
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

                if (level >= 0 && scale > 0) {
                    val batteryLevel = ((level / scale.toFloat()) * 100).coerceIn(0f, 100f)
                    onLevelChange(batteryLevel)
                    Timber.v("BatteryProvider: Battery level updated to ${batteryLevel}%")
                }

                // Optional: Log charging status for context
                val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                Timber.v("BatteryProvider: Charging = $isCharging")

            } catch (e: Exception) {
                Timber.e(e, "BatteryProvider: Error processing battery broadcast")
            }
        }
    }
}
