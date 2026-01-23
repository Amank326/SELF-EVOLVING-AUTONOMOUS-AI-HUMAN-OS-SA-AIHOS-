package com.aihos.interaction

import java.io.Serializable

/**
 * Complete snapshot of user interaction state
 * Captured at touch events and broadcast to 3D system
 * 
 * Used by: InteractionController → AndroidBridge → ProceduralAnimationController
 */
data class InteractionState(
    // Touch/Gesture Information
    val gestureType: GestureType = GestureType.IDLE,
    val touchX: Float = 0f,  // Normalized 0-1 (left to right)
    val touchY: Float = 0f,  // Normalized 0-1 (top to bottom)
    val touchPressure: Float = 0f,  // Normalized 0-1 (light to heavy)
    val multiTouchCount: Int = 0,  // Number of simultaneous touches

    // Gesture Intensity & Duration
    val gestureDuration: Long = 0,  // Milliseconds since gesture started
    val gestureIntensity: Float = 0f,  // 0-1 based on pressure and velocity
    val gestureVelocity: Float = 0f,  // Pixels per second

    // Idle State
    val idleDuration: Long = 0,  // Milliseconds since last interaction
    val isIdling: Boolean = true,  // True if no touch for >2 seconds
    val idleDecayFactor: Float = 1f,  // 0-1, decreases over idle time

    // Context Information
    val contextScore: Float = 0.5f,  // 0-1, "activeness" of environment
    val timeOfDay: Float = 0.5f,  // 0-1, 0=midnight, 0.5=noon, 1=next midnight
    val usageIntensity: Float = 0.5f,  // 0-1, historical usage patterns
    val appForeground: Boolean = true,  // Is app in foreground?
    val deviceBattery: Float = 0.5f,  // 0-1 battery level
    val isCharging: Boolean = false,  // Device charging?

    // Device State
    val deviceOrientation: DeviceOrientation = DeviceOrientation.PORTRAIT,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,

    // Accumulated State
    val totalInteractionsCount: Long = 0,  // Lifetime interaction count
    val recentInteractionIntensity: Float = 0f,  // Average of last 5 interactions
    val isInReflectionMode: Boolean = false,  // Long-press triggered reflection

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

    /**
     * Convert to JSON for Android→JS bridge
     */
    fun toJson(): String {
        return """{
            "gestureType": "$gestureType",
            "touchX": $touchX,
            "touchY": $touchY,
            "touchPressure": $touchPressure,
            "multiTouchCount": $multiTouchCount,
            "gestureDuration": $gestureDuration,
            "gestureIntensity": $gestureIntensity,
            "gestureVelocity": $gestureVelocity,
            "idleDuration": $idleDuration,
            "isIdling": $isIdling,
            "idleDecayFactor": $idleDecayFactor,
            "contextScore": $contextScore,
            "timeOfDay": $timeOfDay,
            "usageIntensity": $usageIntensity,
            "appForeground": $appForeground,
            "deviceBattery": $deviceBattery,
            "isCharging": $isCharging,
            "deviceOrientation": "$deviceOrientation",
            "screenWidth": $screenWidth,
            "screenHeight": $screenHeight,
            "totalInteractionsCount": $totalInteractionsCount,
            "recentInteractionIntensity": $recentInteractionIntensity,
            "isInReflectionMode": $isInReflectionMode,
            "timestamp": $timestamp
        }"""
    }

    /**
     * Get "interaction energy" for animation intensity
     * Combines pressure, velocity, and frequency
     */
    fun getInteractionEnergy(): Float {
        return (touchPressure * 0.4f +
                gestureIntensity * 0.4f +
                recentInteractionIntensity * 0.2f).coerceIn(0f, 1f)
    }

    /**
     * Get effective animation influence (0-1)
     * Decays over idle time
     */
    fun getEffectiveInfluence(): Float {
        return (1f - (idleDuration / 3000f)).coerceIn(0f, 1f) * idleDecayFactor
    }
}

/**
 * Gesture types that trigger different 3D animations
 */
enum class GestureType {
    IDLE,           // No touch
    TAP,            // Quick single touch (creates spark/pulse)
    LONG_PRESS,     // Held touch >1s (triggers reflection)
    SWIPE,          // Directional swipe (creates flow)
    PINCH,          // Two-finger pinch (controls breathing rate)
    DOUBLE_TAP,     // Two quick taps (creates burst)
    TWO_FINGER_ROTATE,  // Two-finger rotation (spins core)
    DRAG,           // Touch and move (rotates view)
}

/**
 * Device orientation state
 */
enum class DeviceOrientation {
    PORTRAIT,
    LANDSCAPE,
    REVERSE_PORTRAIT,
    REVERSE_LANDSCAPE,
    FLAT_FACE_UP,
    FLAT_FACE_DOWN,
}
