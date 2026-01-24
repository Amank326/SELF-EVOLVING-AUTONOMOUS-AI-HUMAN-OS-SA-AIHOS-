package com.aihos.system.signals.impl.providers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar

/**
 * Time of day signal provider.
 *
 * Responsibilities:
 * - Track current time of day (0.0 = midnight, 0.5 = noon, 1.0 = midnight)
 * - Emit time changes periodically
 * - Support day/night context for AI reasoning
 *
 * Behavior:
 * - Polls time every 60 seconds (once per minute)
 * - Uses device calendar (respects device timezone)
 * - Provides normalized value (0.0-1.0) for easy reasoning
 * - Day: 6:00 AM to 6:00 PM (value > 0.25 and < 0.75)
 * - Night: 6:00 PM to 6:00 AM (value >= 0.75 or < 0.25)
 *
 * Safety:
 * - Polling runs in separate coroutine scope
 * - Stops cleanly when unregistered
 * - Thread-safe StateFlow
 *
 * Note: This is a system signal, no permissions required.
 */
class TimeOfDayProvider(
    private val pollIntervalMs: Long = 60_000L // Poll every 60 seconds
) {

    private val _value = MutableStateFlow(0.5f) // Default: noon
    val value: StateFlow<Float> = _value.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isRegistered = false

    /**
     * Start time polling.
     */
    fun register() {
        if (isRegistered) {
            Timber.d("TimeOfDayProvider: Already registered, skipping")
            return
        }

        try {
            isRegistered = true
            // Emit initial time immediately
            updateTimeOfDay()
            // Then start polling
            scope.launch {
                pollTimeOfDay()
            }
            Timber.d("TimeOfDayProvider: Polling started")

        } catch (e: Exception) {
            Timber.e(e, "TimeOfDayProvider: Failed to start polling")
            isRegistered = false
            throw e
        }
    }

    /**
     * Stop time polling.
     */
    fun unregister() {
        if (!isRegistered) {
            Timber.d("TimeOfDayProvider: Not registered, skipping")
            return
        }

        isRegistered = false
        Timber.d("TimeOfDayProvider: Polling stopped")
    }

    /**
     * Polling loop that runs continuously while registered.
     */
    private suspend fun pollTimeOfDay() {
        while (isRegistered) {
            try {
                updateTimeOfDay()
            } catch (e: Exception) {
                Timber.w(e, "TimeOfDayProvider: Error updating time of day")
            }

            delay(pollIntervalMs)
        }
        Timber.d("TimeOfDayProvider: Polling loop stopped")
    }

    /**
     * Update current time of day and emit.
     */
    private fun updateTimeOfDay() {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val minuteOfHour = calendar.get(Calendar.MINUTE)

        // Calculate time of day as fraction: 0.0 = midnight, 1.0 = next midnight
        val totalMinutes = (hourOfDay * 60 + minuteOfHour).toFloat()
        val minutesPerDay = 24 * 60
        val timeOfDay = (totalMinutes / minutesPerDay).coerceIn(0f, 1f)

        _value.tryEmit(timeOfDay)

        // Log day/night context
        val isDaytime = isDaytime(timeOfDay)
        Timber.v("TimeOfDayProvider: ${"%02d:%02d".format(hourOfDay, minuteOfHour)} (value: ${"%.2f".format(timeOfDay)}, isDaytime: $isDaytime)")
    }

    /**
     * Determine if it's daytime based on time of day value.
     * Daytime: 6:00 AM (0.25) to 6:00 PM (0.75)
     */
    private fun isDaytime(timeOfDay: Float): Boolean {
        return timeOfDay in 0.25f..0.75f
    }
}
