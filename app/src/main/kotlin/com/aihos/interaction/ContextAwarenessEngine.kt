package com.aihos.interaction

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import timber.log.Timber
import java.util.*

/**
 * Context Awareness Engine
 * Computes behavioral context from device state, time, and usage patterns
 * 
 * Provides:
 * - Time-of-day behavior (circadian rhythms)
 * - Device state (battery, network, location)
 * - Usage patterns (calm vs active periods)
 * - Environmental context
 * 
 * Used by: InteractionController → 3D visualization (affects mood/intensity)
 */
class ContextAwarenessEngine(private val context: Context) {

    /**
     * Current context snapshot
     */
    data class ContextSnapshot(
        val contextScore: Float,      // 0-1, overall "activeness"
        val timeOfDay: Float,         // 0-1, circadian curve
        val usageIntensity: Float,    // 0-1, from usage patterns
        val batteryLevel: Float,      // 0-1
        val isCharging: Boolean,      // Device plugged in?
        val brightness: Float,        // 0-1, screen brightness
        val isBattery: Boolean,       // Not plugged in?
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Compute current context
     */
    fun computeContext(): ContextSnapshot {
        val timeOfDay = computeTimeOfDay()
        val batteryInfo = getBatteryInfo()
        val brightness = getScreenBrightness()
        val usageIntensity = estimateUsageIntensity()

        // Combine factors into context score
        val contextScore = (
            timeOfDay * 0.3f +      // More active during day
            brightness * 0.2f +      // More active with high brightness
            usageIntensity * 0.3f +  // Usage patterns
            (if (!batteryInfo.isCharging) 0.2f else -0.2f)  // Slightly lower when charging
        ).coerceIn(0f, 1f)

        return ContextSnapshot(
            contextScore = contextScore,
            timeOfDay = timeOfDay,
            usageIntensity = usageIntensity,
            batteryLevel = batteryInfo.level,
            isCharging = batteryInfo.isCharging,
            brightness = brightness,
            isBattery = !batteryInfo.isCharging
        )
    }

    /**
     * Compute time of day curve (circadian)
     * Returns 0 at midnight, peaks at noon, returns to 0
     * 
     * Sleep hours (10pm-6am): 0.0-0.2 (low activity)
     * Wake hours (6am-10pm): 0.3-1.0 (varying activity)
     */
    private fun computeTimeOfDay(): Float {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute

        return when {
            // Deep sleep: 10pm-6am
            (hour >= 22 || hour < 6) -> {
                val sleepPosition = if (hour >= 22) {
                    (totalMinutes - (22 * 60)) / 480f  // 10pm-midnight
                } else {
                    (totalMinutes + 120) / 480f  // midnight-6am
                }
                0.1f * (1f - abs(sleepPosition - 0.5f) * 2f)
            }

            // Morning ramp-up: 6am-9am
            hour in 6..8 -> {
                (hour - 6 + minute / 60f) / 3f * 0.6f + 0.2f
            }

            // Active day: 9am-5pm
            hour in 9..16 -> {
                0.9f + (if (hour == 12) 0.1f else 0f)  // Peak at noon
            }

            // Evening wind-down: 5pm-10pm
            hour in 17..21 -> {
                1f - (hour - 17 + minute / 60f) / 4f * 0.7f
            }

            else -> 0.5f
        }
    }

    /**
     * Get battery information
     */
    private fun getBatteryInfo(): BatteryInfo {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, intentFilter)

            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            BatteryInfo(
                level = (level / scale.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f),
                isCharging = isCharging
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting battery info")
            BatteryInfo(level = 0.5f, isCharging = false)
        }
    }

    /**
     * Get screen brightness (0-1)
     */
    private fun getScreenBrightness(): Float {
        return try {
            val brightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            (brightness / 255f).coerceIn(0f, 1f)
        } catch (e: Exception) {
            0.5f
        }
    }

    /**
     * Estimate usage intensity based on time period
     * Could be enhanced with actual app usage statistics
     * 
     * Currently returns conservative estimate:
     * - Weekends lower than weekdays
     * - Peak hours higher
     * - Night hours lower
     */
    private fun estimateUsageIntensity(): Float {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        val weekdayMultiplier = if (isWeekend) 0.7f else 1.0f

        val hourIntensity = when (hour) {
            in 6..8 -> 0.6f      // Morning ramp
            in 9..12 -> 0.9f     // Morning peak
            in 13..14 -> 0.8f    // Lunch
            in 15..18 -> 0.95f   // Afternoon peak
            in 19..21 -> 0.7f    // Evening
            in 22..23 -> 0.4f    // Late night
            else -> 0.1f         // Deep night
        }

        return (hourIntensity * weekdayMultiplier).coerceIn(0f, 1f)
    }

    /**
     * Battery information holder
     */
    data class BatteryInfo(
        val level: Float,
        val isCharging: Boolean
    )

    /**
     * Get human-readable context description
     */
    fun getContextDescription(context: ContextSnapshot): String {
        return when {
            context.contextScore > 0.8f -> "highly active"
            context.contextScore > 0.6f -> "active"
            context.contextScore > 0.4f -> "normal"
            context.contextScore > 0.2f -> "calm"
            else -> "very calm"
        }
    }
}
