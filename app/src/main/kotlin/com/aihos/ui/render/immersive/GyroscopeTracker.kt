package com.aihos.ui.render.immersive

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

/**
 * GyroscopeTracker — Sensor-based head-tracking simulation.
 *
 * Maps device tilt → camera orientation offset for pseudo-VR.
 *
 * ┌────────────────────────────────────────────────────────────────┐
 * │  Sensor Pipeline                                               │
 * │                                                                │
 * │  Gyroscope (rad/s)                                            │
 * │       │                                                        │
 * │       ▼                                                        │
 * │  Low-pass filter: α * new + (1-α) * old                      │
 * │       │                                                        │
 * │       ▼                                                        │
 * │  Clamp to maxRotation                                          │
 * │       │                                                        │
 * │       ▼                                                        │
 * │  HeadOrientation (yaw, pitch, roll)                            │
 * │       │                                                        │
 * │       ▼                                                        │
 * │  AtomicReference → consumed by GL thread once per frame        │
 * └────────────────────────────────────────────────────────────────┘
 *
 * Thread safety:
 *   Sensor callbacks arrive on sensor thread.
 *   GL thread reads via consumeOrientation().
 *   AtomicReference ensures lock-free exchange.
 *
 * The data class is immutable so no torn reads.
 */
class GyroscopeTracker(context: Context) : SensorEventListener {

    /**
     * Immutable orientation snapshot.
     * yaw = rotation around Y (left-right head turn)
     * pitch = rotation around X (look up/down)
     * roll = rotation around Z (head tilt)
     * All in radians.
     */
    data class HeadOrientation(
        val yaw: Float = 0f,
        val pitch: Float = 0f,
        val roll: Float = 0f
    )

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val orientationRef = AtomicReference(HeadOrientation())

    // Low-pass filtered state (sensor thread only)
    private var filteredYaw = 0f
    private var filteredPitch = 0f
    private var filteredRoll = 0f
    private var lastTimestampNs = 0L

    private var alpha = 0.08f
    private var sensitivity = 1.0f
    private var maxRotation = 0.25f
    private var isActive = false

    /**
     * Start listening to gyroscope.
     * Call from Activity onResume.
     */
    fun start(config: ImmersiveDepthConfig) {
        if (gyroscope == null) {
            Timber.w("GyroscopeTracker: no gyroscope sensor available")
            return
        }
        alpha = config.sensorSmoothingAlpha
        sensitivity = config.headTrackSensitivity
        maxRotation = config.maxHeadRotation
        isActive = true
        sensorManager?.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        Timber.i("GyroscopeTracker: started (α=$alpha, sens=$sensitivity)")
    }

    /**
     * Stop listening. Call from Activity onPause.
     */
    fun stop() {
        isActive = false
        sensorManager?.unregisterListener(this)
        // Reset to zero smoothly (GL thread will read this)
        orientationRef.set(HeadOrientation())
        filteredYaw = 0f; filteredPitch = 0f; filteredRoll = 0f
        lastTimestampNs = 0L
        Timber.i("GyroscopeTracker: stopped")
    }

    /**
     * Read the latest smoothed orientation from GL thread.
     * Zero allocation (returns existing immutable object).
     */
    fun consumeOrientation(): HeadOrientation = orientationRef.get()

    fun updateConfig(config: ImmersiveDepthConfig) {
        alpha = config.sensorSmoothingAlpha
        sensitivity = config.headTrackSensitivity
        maxRotation = config.maxHeadRotation
    }

    // ═══════════════════════════════════════════════════════════════
    // SensorEventListener (sensor thread)
    // ═══════════════════════════════════════════════════════════════

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isActive) return

        val timestampNs = event.timestamp
        if (lastTimestampNs == 0L) {
            lastTimestampNs = timestampNs
            return
        }

        val dtSec = (timestampNs - lastTimestampNs) / 1_000_000_000f
        lastTimestampNs = timestampNs

        // Gyroscope gives angular velocity in rad/s
        val gyroX = event.values[0] * sensitivity  // pitch
        val gyroY = event.values[1] * sensitivity  // yaw
        val gyroZ = event.values[2] * sensitivity  // roll

        // Integrate angular velocity → angle
        val rawYaw = filteredYaw + gyroY * dtSec
        val rawPitch = filteredPitch + gyroX * dtSec
        val rawRoll = filteredRoll + gyroZ * dtSec

        // Low-pass filter: smoothed = α * new + (1-α) * old
        filteredYaw = alpha * rawYaw + (1f - alpha) * filteredYaw
        filteredPitch = alpha * rawPitch + (1f - alpha) * filteredPitch
        filteredRoll = alpha * rawRoll + (1f - alpha) * filteredRoll

        // Clamp to maximum rotation
        filteredYaw = filteredYaw.coerceIn(-maxRotation, maxRotation)
        filteredPitch = filteredPitch.coerceIn(-maxRotation, maxRotation)
        filteredRoll = filteredRoll.coerceIn(-maxRotation * 0.5f, maxRotation * 0.5f)

        // Decay toward zero when device is still (prevents drift)
        val decay = 0.998f
        filteredYaw *= decay
        filteredPitch *= decay
        filteredRoll *= decay

        // Publish (immutable data class, safe for cross-thread)
        orientationRef.set(HeadOrientation(filteredYaw, filteredPitch, filteredRoll))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

