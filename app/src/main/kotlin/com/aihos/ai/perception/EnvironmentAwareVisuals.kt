package com.aihos.ai.perception

import com.aihos.graphics.filament.AICoreMaterial
import timber.log.Timber

/**
 * EnvironmentAwareVisuals: Environment context reflected in 3D visuals
 * 
 * The 3D AI Core visualization adapts based on environmental context:
 * - Battery state: Color shifts, emission changes
 * - Network availability: Glow intensity modulates
 * - User activity: Rotation speed reflects activity
 * - Time of day: Lighting warmth adapts
 * - Calmness level: Material smoothness adjusts
 * 
 * This creates visual feedback of environmental pressure on the AI.
 */

/**
 * Visual intensity modulation based on environmental state
 */
fun calculateVisualIntensity(context: EnvironmentContext): Float {
    // Base intensity follows environmental calmness
    var intensity = 0.5f + (context.environmentalCalmness * 0.5f)
    
    // Battery state affects intensity
    when {
        context.battery.isCritical -> intensity *= 0.6f      // Dim during critical
        context.battery.isInLowPowerMode -> intensity *= 0.8f // Slightly dim
        context.battery.hasAbundantPower -> intensity *= 1.1f  // Bright when abundant
    }
    
    // Network affects intensity
    if (context.networkState == NetworkState.DISCONNECTED) {
        intensity *= 0.9f
    }
    
    // Time of day affects intensity
    if (context.temporal.isNightTime) {
        intensity *= 0.85f  // Dimmer at night
    }
    
    return intensity.coerceIn(0.2f, 1.0f)
}

/**
 * Visual animation speed based on environmental activity
 */
fun calculateAnimationSpeed(context: EnvironmentContext): Float {
    // Base on user activity
    var speed = when (context.userActivityLevel) {
        UserActivityLevel.IDLE -> 0.5f
        UserActivityLevel.LIGHT -> 0.8f
        UserActivityLevel.ACTIVE -> 1.0f
        UserActivityLevel.INTENSE -> 1.3f
        UserActivityLevel.UNKNOWN -> 0.8f
    }
    
    // Slow down when battery critical
    if (context.battery.isCritical) {
        speed *= 0.6f
    }
    
    // Speed up in calm environment
    speed *= (0.8f + context.environmentalCalmness * 0.4f)
    
    return speed.coerceIn(0.1f, 2.0f)
}

/**
 * Visual pulse frequency based on environment
 */
fun calculatePulseFrequency(context: EnvironmentContext): Float {
    // Base frequency in Hz
    var frequency = 1.5f
    
    // User activity affects pulse
    frequency *= when (context.userActivityLevel) {
        UserActivityLevel.IDLE -> 0.8f
        UserActivityLevel.LIGHT -> 1.0f
        UserActivityLevel.ACTIVE -> 1.2f
        UserActivityLevel.INTENSE -> 1.5f
        UserActivityLevel.UNKNOWN -> 1.0f
    }
    
    // Network affects pulse
    if (context.networkState == NetworkState.DISCONNECTED) {
        frequency *= 0.9f  // Slower pulse without network
    }
    
    // Battery affects pulse
    if (context.battery.isCritical) {
        frequency *= 0.6f  // Slow, weak pulse when critical
    } else if (context.battery.hasAbundantPower) {
        frequency *= 1.2f  // Faster pulse when charging
    }
    
    return frequency.coerceIn(0.5f, 3.0f)
}

/**
 * Material roughness adjustment based on environmental constraints
 */
fun calculateMaterialRoughness(baseRoughness: Float, context: EnvironmentContext): Float {
    var roughness = baseRoughness
    
    // Constrained environment: rougher (less reflective, more stable)
    roughness += context.environmentalConstraints * 0.3f
    
    // Calm environment: smoother (more polished, responsive)
    roughness -= (context.environmentalCalmness * 0.2f)
    
    // Battery state affects surface
    if (context.battery.isCritical) {
        roughness += 0.2f  // Dull surface when critical
    }
    
    return roughness.coerceIn(0.0f, 1.0f)
}

/**
 * Material metallic value based on network and power
 */
fun calculateMaterialMetallic(baseMetallic: Float, context: EnvironmentContext): Float {
    var metallic = baseMetallic
    
    // More metallic when well-powered and connected
    if (context.battery.hasAbundantPower) {
        metallic += 0.15f
    }
    
    if (context.networkState == NetworkState.CONNECTED) {
        metallic += 0.1f
    }
    
    // Less metallic when constrained
    if (context.battery.isCritical || context.networkState == NetworkState.DISCONNECTED) {
        metallic -= 0.2f
    }
    
    return metallic.coerceIn(0.0f, 1.0f)
}

/**
 * Lighting intensity based on battery and environmental conditions
 */
fun calculateLightingIntensity(baseIntensity: Float, context: EnvironmentContext): Float {
    var intensity = baseIntensity
    
    // Brighter in optimal conditions
    intensity *= (0.7f + context.environmentalCalmness * 0.6f)
    
    // Battery state
    when {
        context.battery.isCritical -> intensity *= 0.5f       // Very dim
        context.battery.isInLowPowerMode -> intensity *= 0.75f
        context.battery.hasAbundantPower -> intensity *= 1.2f
    }
    
    // Network affects intensity
    if (context.networkState == NetworkState.DISCONNECTED) {
        intensity *= 0.85f
    }
    
    // Time of day
    if (context.temporal.isNightTime) {
        intensity *= 0.8f
    }
    
    return intensity.coerceIn(5000f, 60000f)  // Lux range
}

/**
 * Color warmth adjustment based on time of day and battery
 */
data class VisualColorAdjustment(
    val colorShift: Float,      // -1.0 (cooler) to +1.0 (warmer)
    val saturation: Float       // 0.0 (grayscale) to 1.0 (full color)
) {
    companion object {
        fun calculate(context: EnvironmentContext): VisualColorAdjustment {
            var colorShift = 0f  // Neutral
            var saturation = 1f  // Full color
            
            // Time of day affects warmth
            if (context.temporal.isNightTime) {
                colorShift = 0.3f  // Warmer colors at night
            } else if (context.temporal.timePeriod == "MORNING") {
                colorShift = -0.2f  // Cooler colors in morning
            }
            
            // Battery affects saturation
            if (context.battery.isCritical) {
                saturation = 0.6f  // Less saturated when critical
            } else if (context.battery.hasAbundantPower) {
                saturation = 1.1f  // More saturated when abundant (capped at 1.0)
            }
            
            // High constraints: desaturate slightly
            saturation *= (1f - context.environmentalConstraints * 0.3f)
            
            return VisualColorAdjustment(
                colorShift = colorShift.coerceIn(-1f, 1f),
                saturation = saturation.coerceIn(0f, 1f)
            )
        }
    }
}

/**
 * Scale factor for visual prominence based on environment
 */
fun calculateVisualScale(context: EnvironmentContext): Float {
    var scale = 1.0f
    
    // More prominent when calm and engaged
    scale *= (0.9f + context.environmentalCalmness * 0.2f)
    
    // User activity affects presence
    scale *= when (context.userActivityLevel) {
        UserActivityLevel.IDLE -> 0.95f
        UserActivityLevel.LIGHT -> 1.0f
        UserActivityLevel.ACTIVE -> 1.05f
        UserActivityLevel.INTENSE -> 1.1f
        UserActivityLevel.UNKNOWN -> 1.0f
    }
    
    // Battery state affects prominence
    if (context.battery.isCritical) {
        scale *= 0.9f  // Recede visually when critical
    }
    
    return scale.coerceIn(0.7f, 1.3f)
}

/**
 * Log visual adjustments for debugging
 */
fun logVisualAdjustments(
    context: EnvironmentContext,
    intensity: Float,
    speed: Float,
    roughness: Float,
    lightingIntensity: Float
) {
    Timber.d(
        "EnvironmentAwareVisuals: Adjusted - " +
        "intensity=%.2f, " +
        "speed=%.2f, " +
        "roughness=%.2f, " +
        "lighting=%.0f lux, " +
        "battery=%d%%, " +
        "network=%s".format(
            intensity,
            speed,
            roughness,
            lightingIntensity,
            context.battery.levelPercent,
            context.networkState
        )
    )
}
