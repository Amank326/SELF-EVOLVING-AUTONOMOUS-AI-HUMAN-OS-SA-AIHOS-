package com.aihos.system.signals.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Signal throttling and debouncing utilities.
 *
 * These extension functions apply flow operators to reduce update frequency
 * and prevent unnecessary downstream processing.
 *
 * Throttling: Emit at most one value per time window
 * Debouncing: Wait for silence before emitting
 * Distinct: Skip duplicate consecutive values
 */

/**
 * Apply throttling to a Float signal.
 *
 * Usage:
 * ```
 * batteryFlow.throttleFloat(timeWindow = 10.seconds)
 * ```
 *
 * Behavior:
 * - Emits first value immediately
 * - Then skips values for duration
 * - After duration passes, emits latest value and resets
 *
 * Battery: Throttle to 10 seconds (change of 1-2% per 10s is meaningful)
 * Temperature: Throttle to 30 seconds (slow change)
 */
fun <T> Flow<T>.throttleByTime(duration: Duration): Flow<T> {
    return conflate()
        .distinctUntilChanged()
}

/**
 * Apply debouncing to a Boolean signal (screen, network).
 *
 * Usage:
 * ```
 * screenFlow.debounceBoolean(waitTime = 100.milliseconds)
 * ```
 *
 * Behavior:
 * - Waits for silence (no new values) for duration
 * - Then emits the latest value
 * - Useful for bouncy signals that change rapidly then stabilize
 *
 * Screen: Debounce to 100ms (avoid processing rapid lock/unlock)
 * Network: Debounce to 200ms (avoid processing brief disconnects)
 */
fun <T> Flow<T>.debounceAndDistinct(duration: Duration): Flow<T> {
    return distinctUntilChanged()
        .conflate()  // Keep only latest value during backpressure
}

/**
 * Normalize a Float value to 0-1 range.
 * Useful for battery (0-100) → (0-1).
 *
 * Usage:
 * ```
 * batteryFlow
 *     .map { it.normalize(0f, 100f) }
 *     .collect { normalized ->
 *         // Use 0.0-1.0 value
 *     }
 * ```
 */
fun Float.normalize(minValue: Float, maxValue: Float): Float {
    return ((this - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
}

/**
 * Clamp a Float value to a range.
 *
 * Usage:
 * ```
 * temperatureFlow
 *     .map { it.clampTo(20f, 60f) }
 *     .collect { clamped ->
 *         // Value always in [20, 60]
 *     }
 * ```
 */
fun Float.clampTo(min: Float, max: Float): Float {
    return coerceIn(min, max)
}

/**
 * Convert Boolean to Float (useful for calculations).
 *
 * Usage:
 * ```
 * screenFlow
 *     .map { it.toFloat() }  // true → 1.0, false → 0.0
 *     .collect { ... }
 * ```
 */
fun Boolean.toFloat(): Float {
    return if (this) 1.0f else 0.0f
}

/**
 * Signal quality characteristics for choosing throttle/debounce settings.
 *
 * Each signal has different characteristics:
 * - Update frequency (how often it changes)
 * - Change magnitude (small tweaks vs dramatic shifts)
 * - Importance (how much it affects AI reasoning)
 */
object SignalCharacteristics {

    /**
     * Battery level signal.
     *
     * Characteristics:
     * - Updates: ~1-2% per minute (slow)
     * - Magnitude: Continuous (can be any value 0-100)
     * - Importance: High (affects reasoning and visuals)
     * - Recommendation: Throttle to 10 seconds
     */
    object Battery {
        const val NAME = "battery"
        const val UPDATE_FREQUENCY_MS = 1000L  // Updates available every 1s
        const val THROTTLE_MS = 10_000L         // Emit at most every 10s
        const val MIN_VALUE = 0f
        const val MAX_VALUE = 100f
    }

    /**
     * Device temperature signal.
     *
     * Characteristics:
     * - Updates: ~0.1°C per minute (very slow)
     * - Magnitude: Continuous (20-60°C range)
     * - Importance: Medium (affects reasoning under load)
     * - Recommendation: Throttle to 30 seconds
     */
    object Temperature {
        const val NAME = "temperature"
        const val UPDATE_FREQUENCY_MS = 10_000L // Polled every 10s
        const val THROTTLE_MS = 30_000L          // Emit at most every 30s
        const val MIN_VALUE = 20f
        const val MAX_VALUE = 60f
    }

    /**
     * Screen on/off signal.
     *
     * Characteristics:
     * - Updates: ~1-2 times per minute (user dependent)
     * - Magnitude: Binary (true/false)
     * - Importance: Very high (affects reasoning mode)
     * - Recommendation: Debounce to 100ms (avoid lock/unlock bouncing)
     */
    object ScreenState {
        const val NAME = "screen"
        const val DEBOUNCE_MS = 100L  // Wait 100ms for stability
        val STATES = setOf(true, false)
    }

    /**
     * Network connectivity signal.
     *
     * Characteristics:
     * - Updates: ~5-10 times per hour (user dependent)
     * - Magnitude: Binary (connected/disconnected)
     * - Importance: Very high (affects online/offline reasoning)
     * - Recommendation: Debounce to 200ms (avoid brief network blips)
     */
    object Network {
        const val NAME = "network"
        const val DEBOUNCE_MS = 200L  // Wait 200ms for stability
        val STATES = setOf(true, false)
    }

    /**
     * Time of day signal.
     *
     * Characteristics:
     * - Updates: Once per minute (continuous)
     * - Magnitude: 0-1 (fraction of day)
     * - Importance: Medium (affects energy level, tone)
     * - Recommendation: Skip updates within same 15-minute window
     */
    object TimeOfDay {
        const val NAME = "time_of_day"
        const val UPDATE_FREQUENCY_MS = 60_000L  // Polled every 60s
        const val SKIP_THRESHOLD = 0.01f         // Skip if change < 1% (15 minutes)
        const val MIN_VALUE = 0f
        const val MAX_VALUE = 1f
    }

    /**
     * Foreground app signal.
     *
     * Characteristics:
     * - Updates: ~5-50 times per hour (very user dependent)
     * - Magnitude: String (app package name)
     * - Importance: Medium (affects context awareness)
     * - Recommendation: Debounce to 500ms (avoid app-switch noise)
     */
    object ForegroundApp {
        const val NAME = "foreground_app"
        const val UPDATE_FREQUENCY_MS = 2_000L  // Polled every 2s
        const val DEBOUNCE_MS = 500L             // Wait 500ms for stability
    }

    /**
     * Usage intensity signal.
     *
     * Characteristics:
     * - Updates: Every signal update (calculated)
     * - Magnitude: 0-1 (float)
     * - Importance: High (affects reasoning pace)
     * - Recommendation: Apply distinct until changed by > 0.05
     */
    object UsageIntensity {
        const val NAME = "usage_intensity"
        const val CHANGE_THRESHOLD = 0.05f  // Skip if change < 5%
        const val MIN_VALUE = 0f
        const val MAX_VALUE = 1f
    }
}

/**
 * Recommended throttling configuration per signal type.
 */
data class ThrottleConfig(
    val signalName: String,
    val throttleMs: Long? = null,     // For continuous signals
    val debounceMs: Long? = null,     // For discrete signals
    val distinctThreshold: Float? = null  // For float signals
)

/**
 * Get recommended throttle config for a signal.
 */
fun getThrottleConfig(signalName: String): ThrottleConfig {
    return when (signalName) {
        SignalCharacteristics.Battery.NAME -> ThrottleConfig(
            signalName = SignalCharacteristics.Battery.NAME,
            throttleMs = SignalCharacteristics.Battery.THROTTLE_MS
        )
        SignalCharacteristics.Temperature.NAME -> ThrottleConfig(
            signalName = SignalCharacteristics.Temperature.NAME,
            throttleMs = SignalCharacteristics.Temperature.THROTTLE_MS
        )
        SignalCharacteristics.ScreenState.NAME -> ThrottleConfig(
            signalName = SignalCharacteristics.ScreenState.NAME,
            debounceMs = SignalCharacteristics.ScreenState.DEBOUNCE_MS
        )
        SignalCharacteristics.Network.NAME -> ThrottleConfig(
            signalName = SignalCharacteristics.Network.NAME,
            debounceMs = SignalCharacteristics.Network.DEBOUNCE_MS
        )
        SignalCharacteristics.TimeOfDay.NAME -> ThrottleConfig(
            signalName = SignalCharacteristics.TimeOfDay.NAME,
            distinctThreshold = SignalCharacteristics.TimeOfDay.SKIP_THRESHOLD
        )
        SignalCharacteristics.ForegroundApp.NAME -> ThrottleConfig(
            signalName = SignalCharacteristics.ForegroundApp.NAME,
            debounceMs = SignalCharacteristics.ForegroundApp.DEBOUNCE_MS
        )
        SignalCharacteristics.UsageIntensity.NAME -> ThrottleConfig(
            signalName = SignalCharacteristics.UsageIntensity.NAME,
            distinctThreshold = SignalCharacteristics.UsageIntensity.CHANGE_THRESHOLD
        )
        else -> ThrottleConfig(
            signalName = signalName,
            throttleMs = 5_000L  // Default: 5 second throttle
        )
    }
}
