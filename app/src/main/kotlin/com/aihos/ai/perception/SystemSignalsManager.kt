package com.aihos.ai.perception

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aihos.ai.memory.MemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * SystemSignalsManager: Observes and normalizes Android system signals as environmental context
 * for AI perception and cognition.
 * 
 * Treats OS signals as "sensory input" from the environment:
 * - App lifecycle → Environmental attention
 * - Screen state → Environmental engagement
 * - Battery/Network → Environmental constraints
 * - Time of day → Environmental temporal context
 * - User activity → Environmental interaction intensity
 * 
 * Privacy-First Design:
 * - No personal data collection (no location, contacts, messages)
 * - No app tracking beyond SA-AIHOS itself
 * - Minimal permission requirements
 * - Abstract normalization (signals, not raw data)
 */
interface SystemSignalsManager {
    /**
     * Start observing system signals
     */
    fun startObserving()
    
    /**
     * Stop observing system signals
     */
    fun stopObserving()
    
    /**
     * Get current environmental context
     */
    suspend fun getEnvironmentContext(): EnvironmentContext
    
    /**
     * Subscribe to environment context changes
     */
    fun observeEnvironmentContext(): StateFlow<EnvironmentContext>
}

/**
 * Enumeration of app lifecycle states as perceived by the AI
 */
@Serializable
enum class AppLifecycleState {
    CREATED,        // App started but not visible
    STARTED,        // App created and started
    RESUMED,        // App in foreground, actively displayed
    PAUSED,         // App in background or paused
    STOPPED,        // App stopped (in background)
    DESTROYED       // App being destroyed
}

/**
 * Screen state as environmental signal
 */
@Serializable
enum class ScreenState {
    ON,             // Device screen is on, visible
    OFF,            // Device screen is off, locked
    DIMMED,         // Device screen in low-power state
    UNKNOWN         // Unable to determine state
}

/**
 * Battery context affecting system behavior
 */
@Serializable
data class BatteryContext(
    val levelPercent: Int,          // 0-100
    val isCharging: Boolean,        // Is device plugged in?
    val isInLowPowerMode: Boolean,  // Low battery mode active?
    val temperatureC: Float = 0f    // Estimated, not always available
) {
    /**
     * Indicates if device is in critical battery state
     */
    val isCritical: Boolean
        get() = levelPercent < 15 && !isCharging
    
    /**
     * Indicates if device has abundant power
     */
    val hasAbundantPower: Boolean
        get() = levelPercent > 80 || isCharging
}

/**
 * Network connectivity context
 */
@Serializable
enum class NetworkState {
    CONNECTED,      // Network available
    DISCONNECTED,   // No network
    METERED,        // Limited/metered connection
    UNKNOWN         // Unable to determine
}

/**
 * Time-based environmental context
 */
@Serializable
data class TemporalContext(
    val hourOfDay: Int,             // 0-23
    val dayOfWeek: String,          // "MONDAY", "TUESDAY", etc.
    val isNightTime: Boolean,       // 20:00 - 08:00 (default)
    val isDayTime: Boolean,         // 08:00 - 20:00
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Time period classification for AI reasoning
     */
    val timePeriod: String
        get() = when {
            hourOfDay in 5..11 -> "MORNING"
            hourOfDay in 12..17 -> "AFTERNOON"
            hourOfDay in 18..23 -> "EVENING"
            else -> "NIGHT"
        }
}

/**
 * User activity level inferred from app interaction
 */
@Serializable
enum class UserActivityLevel {
    IDLE,           // No recent user interaction
    LIGHT,          // Occasional taps/interactions
    ACTIVE,         // Frequent interactions
    INTENSE,        // Rapid consecutive interactions
    UNKNOWN         // Unable to determine
}

/**
 * Unified environmental context model for AI perception
 * 
 * Represents the "sensory input" from the environment that affects AI cognition.
 * Abstract and privacy-preserving - signals only, no personal data.
 */
@Serializable
data class EnvironmentContext(
    // App state
    val appLifecycle: AppLifecycleState = AppLifecycleState.CREATED,
    val screenState: ScreenState = ScreenState.UNKNOWN,
    val isAppInForeground: Boolean = false,
    
    // Device constraints
    val battery: BatteryContext = BatteryContext(
        levelPercent = 50,
        isCharging = false,
        isInLowPowerMode = false
    ),
    val networkState: NetworkState = NetworkState.UNKNOWN,
    
    // Temporal context
    val temporal: TemporalContext = TemporalContext(
        hourOfDay = 12,
        dayOfWeek = "UNKNOWN",
        isNightTime = false,
        isDayTime = true
    ),
    
    // User engagement
    val userActivityLevel: UserActivityLevel = UserActivityLevel.UNKNOWN,
    val lastInteractionTimeMs: Long = System.currentTimeMillis(),
    val timeSinceLastInteractionSeconds: Long = 0,
    
    // Derived environmental assessment
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Assess overall environmental "calmness" for AI mood/behavior
     * Higher = more calm/stable environment
     * Lower = more hectic/constrained environment
     */
    val environmentalCalmness: Float
        get() {
            var calmness = 0.5f  // Baseline
            
            // Battery constraint
            when {
                battery.isCritical -> calmness -= 0.3f      // Critical
                battery.isInLowPowerMode -> calmness -= 0.2f // Low power
                battery.hasAbundantPower -> calmness += 0.1f  // Abundant
                battery.levelPercent > 50 -> calmness += 0.05f
            }
            
            // Network availability
            when (networkState) {
                NetworkState.METERED -> calmness -= 0.1f
                NetworkState.DISCONNECTED -> calmness -= 0.15f
                NetworkState.CONNECTED -> calmness += 0.05f
                else -> {}
            }
            
            // Time of day
            if (temporal.isNightTime) {
                calmness += 0.1f  // Night is calmer for AI
            }
            
            // User activity
            when (userActivityLevel) {
                UserActivityLevel.IDLE -> calmness += 0.15f
                UserActivityLevel.LIGHT -> calmness += 0.05f
                UserActivityLevel.ACTIVE -> calmness -= 0.1f
                UserActivityLevel.INTENSE -> calmness -= 0.2f
                UserActivityLevel.UNKNOWN -> {}
            }
            
            // App in background
            if (!isAppInForeground) {
                calmness += 0.05f
            }
            
            return calmness.coerceIn(0f, 1f)
        }
    
    /**
     * Assess environmental "constraints" on AI behavior
     * Higher = more constrained (battery low, network limited, etc.)
     * Lower = fewer constraints
     */
    val environmentalConstraints: Float
        get() {
            var constraints = 0.3f  // Baseline some constraints
            
            if (battery.isCritical) constraints += 0.4f
            else if (battery.isInLowPowerMode) constraints += 0.2f
            else if (battery.levelPercent < 30) constraints += 0.1f
            
            if (networkState == NetworkState.DISCONNECTED) constraints += 0.2f
            else if (networkState == NetworkState.METERED) constraints += 0.1f
            
            return constraints.coerceIn(0f, 1f)
        }
    
    /**
     * Assess environmental "openness" for AI evolution and exploration
     * Higher = safe to explore, learn, evolve
     * Lower = focus on stability and efficiency
     */
    val evolutionaryOpenness: Float
        get() {
            // Safe to evolve when: charged, connected, time available, calm
            return 1f - environmentalConstraints
        }
}

/**
 * Default implementation of SystemSignalsManager
 */
class DefaultSystemSignalsManager(
    private val context: Context,
    private val memoryRepository: MemoryRepository,
    private val scope: CoroutineScope
) : SystemSignalsManager, DefaultLifecycleObserver {
    
    private val _environmentContext = MutableStateFlow(EnvironmentContext())
    override fun observeEnvironmentContext(): StateFlow<EnvironmentContext> = _environmentContext.asStateFlow()
    
    // Signal collection state
    private var appLifecycleState = AppLifecycleState.CREATED
    private var screenState = ScreenState.UNKNOWN
    private var userActivityLevel = UserActivityLevel.UNKNOWN
    private var lastUserInteractionTimeMs = System.currentTimeMillis()
    
    // Broadcast receivers
    private var batteryReceiver: BroadcastReceiver? = null
    private var screenReceiver: BroadcastReceiver? = null
    
    // Activity tracking
    private val activityTracker = UserActivityTracker()
    
    init {
        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Timber.d("SystemSignalsManager: Initialized")
    }
    
    override fun startObserving() {
        Timber.d("SystemSignalsManager: Starting observation")
        
        // Register battery status receiver
        batteryReceiver = BatteryReceiver { battery, screen ->
            updateSignals(battery = battery, screenState = screen)
        }
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED)
        
        // Register screen state receiver
        screenReceiver = ScreenStateReceiver { screen ->
            updateSignals(screenState = screen)
        }
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenReceiver, screenFilter, Context.RECEIVER_NOT_EXPORTED)
        
        // Start activity tracking
        activityTracker.start()
        
        Timber.d("SystemSignalsManager: Observation started")
    }
    
    override fun stopObserving() {
        Timber.d("SystemSignalsManager: Stopping observation")
        
        // Unregister receivers
        batteryReceiver?.let { context.unregisterReceiver(it) }
        screenReceiver?.let { context.unregisterReceiver(it) }
        activityTracker.stop()
        
        Timber.d("SystemSignalsManager: Observation stopped")
    }
    
    override suspend fun getEnvironmentContext(): EnvironmentContext {
        return _environmentContext.value
    }
    
    private fun updateSignals(
        battery: BatteryContext? = null,
        screenState: ScreenState? = null,
        userActivity: UserActivityLevel? = null
    ) {
        val current = _environmentContext.value
        
        val updated = current.copy(
            appLifecycle = appLifecycleState,
            battery = battery ?: current.battery,
            screenState = screenState ?: current.screenState,
            isAppInForeground = appLifecycleState == AppLifecycleState.RESUMED,
            userActivityLevel = userActivity ?: current.userActivityLevel,
            lastInteractionTimeMs = lastUserInteractionTimeMs,
            timeSinceLastInteractionSeconds = (System.currentTimeMillis() - lastUserInteractionTimeMs) / 1000,
            temporal = getCurrentTemporalContext(),
            timestamp = System.currentTimeMillis()
        )
        
        _environmentContext.value = updated
        
        Timber.d(
            "SystemSignalsManager: Updated context - " +
            "lifecycle=${updated.appLifecycle}, " +
            "battery=${updated.battery.levelPercent}%, " +
            "network=${updated.networkState}, " +
            "activity=${updated.userActivityLevel}, " +
            "calmness=%.2f".format(updated.environmentalCalmness)
        )
    }
    
    private fun getCurrentTemporalContext(): TemporalContext {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val hour = now.hour
        val dayOfWeek = now.dayOfWeek.name
        val isNight = hour < 8 || hour >= 20
        
        return TemporalContext(
            hourOfDay = hour,
            dayOfWeek = dayOfWeek,
            isNightTime = isNight,
            isDayTime = !isNight,
            timestamp = System.currentTimeMillis()
        )
    }
    
    // Lifecycle callbacks
    override fun onCreate(owner: LifecycleOwner) {
        appLifecycleState = AppLifecycleState.CREATED
        updateSignals()
    }
    
    override fun onStart(owner: LifecycleOwner) {
        appLifecycleState = AppLifecycleState.STARTED
        updateSignals()
    }
    
    override fun onResume(owner: LifecycleOwner) {
        appLifecycleState = AppLifecycleState.RESUMED
        updateSignals()
    }
    
    override fun onPause(owner: LifecycleOwner) {
        appLifecycleState = AppLifecycleState.PAUSED
        updateSignals()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        appLifecycleState = AppLifecycleState.STOPPED
        updateSignals()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        appLifecycleState = AppLifecycleState.DESTROYED
        stopObserving()
    }
    
    /**
     * Register user interaction to track activity level
     */
    fun recordUserInteraction() {
        lastUserInteractionTimeMs = System.currentTimeMillis()
        val activity = activityTracker.updateActivity()
        updateSignals(userActivity = activity)
    }
}

/**
 * Broadcast receiver for battery status updates
 */
private class BatteryReceiver(
    private val onUpdate: (BatteryContext, ScreenState) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
        
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val batteryPercent = (level * 100 / scale).coerceIn(0, 100)
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
        
        val powerManager = context?.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInLowPowerMode = if (Build.VERSION.SDK_INT >= 21) {
            powerManager?.isPowerSaveMode ?: false
        } else {
            false
        }
        
        val battery = BatteryContext(
            levelPercent = batteryPercent,
            isCharging = isCharging,
            isInLowPowerMode = isInLowPowerMode
        )
        
        // Determine screen state from battery state (device is on if charging from USB power typically)
        val screenState = ScreenState.ON  // Will be updated by ScreenStateReceiver
        onUpdate(battery, screenState)
    }
}

/**
 * Broadcast receiver for screen state updates
 */
private class ScreenStateReceiver(
    private val onUpdate: (ScreenState) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON -> onUpdate(ScreenState.ON)
            Intent.ACTION_SCREEN_OFF -> onUpdate(ScreenState.OFF)
        }
    }
}

/**
 * Tracks user activity level based on interaction frequency and recency
 */
private class UserActivityTracker {
    private var interactionWindow = mutableListOf<Long>()
    private val windowSizeMs = 60_000L  // 1 minute window
    private var isRunning = false
    
    fun start() {
        isRunning = true
    }
    
    fun stop() {
        isRunning = false
        interactionWindow.clear()
    }
    
    fun updateActivity(): UserActivityLevel {
        if (!isRunning) return UserActivityLevel.UNKNOWN
        
        val now = System.currentTimeMillis()
        interactionWindow.add(now)
        
        // Remove old entries outside window
        interactionWindow.removeAll { it < now - windowSizeMs }
        
        return when {
            interactionWindow.isEmpty() -> UserActivityLevel.IDLE
            interactionWindow.size == 1 -> UserActivityLevel.LIGHT
            interactionWindow.size in 2..5 -> UserActivityLevel.ACTIVE
            else -> UserActivityLevel.INTENSE
        }
    }
}
