package com.aihos.interaction

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.MotionEvent
import android.view.OrientationEventListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aihos.ai.autonomy.AutonomyController
import com.aihos.ai.evolution.EvolutionEngine
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Interaction Controller
 * Captures user touch, gestures, device state, and idle time
 * Broadcasts InteractionState to 3D system in real-time
 * 
 * Architecture:
 * - Touch capture: MotionEvent → GestureType detection
 * - Device sensors: Battery, orientation, foreground/background
 * - Time-based: Idle detection, time-of-day behavior
 * - Context: Usage history, ambient conditions
 * 
 * Updates: 10 Hz to 60 Hz (when interaction occurring)
 */
class InteractionController(
    private val context: Context,
    private val autonomyController: AutonomyController,
    private val evolutionEngine: EvolutionEngine
) : LifecycleEventObserver {

    // Listeners
    private val interactionListeners = mutableListOf<InteractionStateListener>()

    // Current state
    private var currentState = InteractionState()
    private var appIsInForeground = true

    // Touch tracking
    private var lastTouchTime = System.currentTimeMillis()
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var touchStartTime = 0L
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var lastTouchVelocityX = 0f
    private var lastTouchVelocityY = 0f

    // Gesture detection
    private var currentGestureType = GestureType.IDLE
    private var longPressDetected = false
    private var multiTouchCount = 0

    // Idle tracking
    private var idleCheckJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Context awareness
    private val contextAwareness = ContextAwarenessEngine(context)
    private var contextUpdateJob: Job? = null

    // Orientation tracking
    private var orientationListener: OrientationEventListener? = null
    private var currentOrientation = DeviceOrientation.PORTRAIT

    // Interaction history (for usage patterns)
    private val interactionHistory = mutableListOf<Long>()
    private val MAX_HISTORY = 100

    init {
        Timber.d("InteractionController initialized")
        setupOrientationListener()
    }

    /**
     * Handle lifecycle events
     */
    override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                appIsInForeground = true
                startIdleDetection()
                startContextUpdates()
                orientationListener?.enable()
                Timber.d("App resumed - interaction tracking active")
            }
            Lifecycle.Event.ON_PAUSE -> {
                appIsInForeground = false
                stopIdleDetection()
                stopContextUpdates()
                orientationListener?.disable()
                // Reset gesture type when app paused
                updateGestureType(GestureType.IDLE)
                Timber.d("App paused - interaction tracking paused")
            }
            else -> {}
        }
    }

    /**
     * Handle motion events (touch, multi-touch)
     */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        try {
            val x = event.x / event.device.motionRange(MotionEvent.AXIS_X, event.source).max.coerceAtLeast(1f)
            val y = event.y / event.device.motionRange(MotionEvent.AXIS_Y, event.source).max.coerceAtLeast(1f)
            val pressure = event.pressure
            val pointerCount = event.pointerCount

            multiTouchCount = pointerCount

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartTime = System.currentTimeMillis()
                    touchStartX = x
                    touchStartY = y
                    lastTouchX = x
                    lastTouchY = y
                    lastTouchTime = touchStartTime
                    longPressDetected = false
                    currentGestureType = GestureType.DRAG
                    
                    // Start long-press detection
                    detectLongPress()
                }

                MotionEvent.ACTION_MOVE -> {
                    lastTouchVelocityX = (x - lastTouchX) * 1000f / (System.currentTimeMillis() - lastTouchTime).coerceAtLeast(1)
                    lastTouchVelocityY = (y - lastTouchY) * 1000f / (System.currentTimeMillis() - lastTouchTime).coerceAtLeast(1)
                    lastTouchX = x
                    lastTouchY = y
                    lastTouchTime = System.currentTimeMillis()

                    // Detect swipe if moving significantly
                    val distance = hypot(x - touchStartX, y - touchStartY)
                    if (distance > 0.05f && !longPressDetected && pointerCount == 1) {
                        currentGestureType = GestureType.SWIPE
                    }

                    // Detect pinch (two fingers changing distance)
                    if (pointerCount == 2) {
                        currentGestureType = GestureType.PINCH
                    }

                    // Detect two-finger rotation
                    if (pointerCount == 2) {
                        val angle = getMultiTouchAngle(event)
                        if (angle != 0f) {
                            currentGestureType = GestureType.TWO_FINGER_ROTATE
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - touchStartTime
                    val distance = hypot(x - touchStartX, y - touchStartY)

                    // Determine final gesture type
                    when {
                        longPressDetected -> {
                            updateGestureType(GestureType.LONG_PRESS)
                            triggerReflectionMode()
                        }
                        distance < 0.02f && duration < 300 -> {
                            updateGestureType(GestureType.TAP)
                            triggerTapEffect()
                        }
                        currentGestureType == GestureType.SWIPE -> {
                            updateGestureType(GestureType.SWIPE)
                        }
                        else -> {
                            updateGestureType(GestureType.IDLE)
                        }
                    }

                    // Record interaction in history
                    recordInteraction()

                    // Reset after brief delay
                    scope.launch {
                        delay(200)
                        if (currentGestureType != GestureType.LONG_PRESS) {
                            updateGestureType(GestureType.IDLE)
                        }
                    }
                }
            }

            // Update current state with touch info
            lastTouchTime = System.currentTimeMillis()
            return true

        } catch (e: Exception) {
            Timber.e(e, "Error handling motion event")
            return false
        }
    }

    /**
     * Detect long-press (1s of holding)
     */
    private fun detectLongPress() {
        scope.launch {
            delay(1000)
            if (currentGestureType == GestureType.DRAG) {
                longPressDetected = true
                updateGestureType(GestureType.LONG_PRESS)
            }
        }
    }

    /**
     * Trigger AI reflection when user long-presses
     * Tells AI to deeply analyze recent decisions
     */
    private fun triggerReflectionMode() {
        Timber.d("Long-press triggered - entering reflection mode")
        // Signal AI to enter reflection
        autonomyController.requestDeepReflection()
    }

    /**
     * Trigger tap effect
     * Light interaction that creates visual feedback
     */
    private fun triggerTapEffect() {
        Timber.d("Tap detected - triggering visual feedback")
        // Could trigger evolution or confidence boost
    }

    /**
     * Get angle between two touch points (for rotation detection)
     */
    private fun getMultiTouchAngle(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)
        
        val dx = x1 - x0
        val dy = y1 - y0
        
        return Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    /**
     * Update gesture type and broadcast state
     */
    private fun updateGestureType(gestureType: GestureType) {
        currentGestureType = gestureType
        broadcastState()
    }

    /**
     * Record interaction in history for usage patterns
     */
    private fun recordInteraction() {
        val now = System.currentTimeMillis()
        interactionHistory.add(now)
        if (interactionHistory.size > MAX_HISTORY) {
            interactionHistory.removeAt(0)
        }
    }

    /**
     * Start idle detection (check every second)
     */
    private fun startIdleDetection() {
        stopIdleDetection()
        
        idleCheckJob = scope.launch {
            while (isActive) {
                delay(1000)
                updateIdleState()
                broadcastState()
            }
        }
    }

    /**
     * Stop idle detection
     */
    private fun stopIdleDetection() {
        idleCheckJob?.cancel()
        idleCheckJob = null
    }

    /**
     * Update idle state (called every second)
     */
    private fun updateIdleState() {
        val now = System.currentTimeMillis()
        val idleDuration = now - lastTouchTime
        val isIdling = idleDuration > 2000  // 2 second idle threshold

        // Decay factor: 1.0 at 0s, 0.0 at 10s
        val idleDecayFactor = (1f - (idleDuration / 10000f)).coerceIn(0f, 1f)

        currentState = currentState.copy(
            idleDuration = idleDuration,
            isIdling = isIdling,
            idleDecayFactor = idleDecayFactor
        )
    }

    /**
     * Start context awareness updates (check every second)
     */
    private fun startContextUpdates() {
        stopContextUpdates()
        
        contextUpdateJob = scope.launch {
            while (isActive) {
                delay(1000)
                val context = contextAwareness.computeContext()
                
                currentState = currentState.copy(
                    contextScore = context.contextScore,
                    timeOfDay = context.timeOfDay,
                    usageIntensity = context.usageIntensity,
                    deviceBattery = context.batteryLevel,
                    isCharging = context.isCharging
                )
                
                broadcastState()
            }
        }
    }

    /**
     * Stop context updates
     */
    private fun stopContextUpdates() {
        contextUpdateJob?.cancel()
        contextUpdateJob = null
    }

    /**
     * Setup orientation listener
     */
    private fun setupOrientationListener() {
        orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                currentOrientation = when {
                    orientation < 45 || orientation >= 315 -> DeviceOrientation.PORTRAIT
                    orientation >= 45 && orientation < 135 -> DeviceOrientation.LANDSCAPE
                    orientation >= 135 && orientation < 225 -> DeviceOrientation.REVERSE_PORTRAIT
                    else -> DeviceOrientation.REVERSE_LANDSCAPE
                }
                
                currentState = currentState.copy(deviceOrientation = currentOrientation)
            }
        }
    }

    /**
     * Broadcast current interaction state to listeners
     */
    private fun broadcastState() {
        val state = currentState.copy(
            appForeground = appIsInForeground,
            gestureType = currentGestureType,
            touchX = lastTouchX,
            touchY = lastTouchY,
            totalInteractionsCount = interactionHistory.size.toLong(),
            recentInteractionIntensity = computeRecentIntensity(),
            timestamp = System.currentTimeMillis()
        )

        currentState = state
        interactionListeners.forEach { it.onInteractionStateChanged(state) }
    }

    /**
     * Compute recent interaction intensity (0-1)
     * Based on frequency of interactions in last 5 seconds
     */
    private fun computeRecentIntensity(): Float {
        val fiveSecondsAgo = System.currentTimeMillis() - 5000
        val recentInteractions = interactionHistory.count { it > fiveSecondsAgo }
        return (recentInteractions / 10f).coerceIn(0f, 1f)
    }

    /**
     * Add interaction listener
     */
    fun addListener(listener: InteractionStateListener) {
        interactionListeners.add(listener)
    }

    /**
     * Remove interaction listener
     */
    fun removeListener(listener: InteractionStateListener) {
        interactionListeners.remove(listener)
    }

    /**
     * Get current interaction state
     */
    fun getState(): InteractionState = currentState

    /**
     * Cleanup resources
     */
    fun destroy() {
        stopIdleDetection()
        stopContextUpdates()
        orientationListener?.disable()
        scope.cancel()
        interactionListeners.clear()
        Timber.d("InteractionController destroyed")
    }
}

/**
 * Listener for interaction state changes
 */
interface InteractionStateListener {
    fun onInteractionStateChanged(state: InteractionState)
}
