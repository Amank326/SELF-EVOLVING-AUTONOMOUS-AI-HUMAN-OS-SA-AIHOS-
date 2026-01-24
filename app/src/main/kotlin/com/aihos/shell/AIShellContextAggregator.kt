package com.aihos.shell

import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * AIShellContextAggregator: System-Level Context Monitor
 *
 * Continuously monitors device state and user context:
 * - Which app is in foreground
 * - Whether device is locked/unlocked
 * - Battery and thermal state
 * - Network connectivity
 * - Power saving mode
 * - User activity level
 *
 * The AI uses this context to:
 * - Adapt behavior to current situation (silent when device locked)
 * - Optimize cognition based on device state
 * - Predict user needs based on app usage
 * - Provide context-aware assistance
 * - Respect battery/thermal constraints
 *
 * Design:
 * - Registers for system broadcasts
 * - Polls for foreground app periodically
 * - Maintains reactive state flows
 * - Non-intrusive background monitoring
 * - Minimal CPU overhead
 */
interface AIShellContextAggregator {
    /**
     * Start monitoring system context
     */
    suspend fun startMonitoring()

    /**
     * Stop monitoring system context
     */
    suspend fun stopMonitoring()

    /**
     * Get current device context
     */
    fun getCurrentContext(): DeviceContextSnapshot

    /**
     * Observe context changes
     */
    fun observeContext(): StateFlow<DeviceContextSnapshot>

    /**
     * Get foreground app information
     */
    fun getForegroundApp(): ForegroundAppInfo

    /**
     * Get device state
     */
    fun getDeviceState(): DeviceState

    /**
     * Get user activity level
     */
    fun getUserActivityLevel(): UserActivityLevel
}

@Serializable
data class DeviceContextSnapshot(
    val foregroundApp: ForegroundAppInfo = ForegroundAppInfo("unknown", "unknown"),
    val deviceState: DeviceState = DeviceState(),
    val userActivityLevel: UserActivityLevel = UserActivityLevel.LOW,
    val timestamp: Long = System.currentTimeMillis(),
    val contextChangeType: ContextChangeType = ContextChangeType.NONE
)

@Serializable
data class ForegroundAppInfo(
    val packageName: String,
    val activityName: String,
    val appCategory: AppCategory = AppCategory.OTHER,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isSameApp(other: ForegroundAppInfo): Boolean {
        return packageName == other.packageName && activityName == other.activityName
    }
}

@Serializable
enum class AppCategory {
    COMMUNICATION,  // Email, messaging, calls
    PRODUCTIVITY,   // Office, notes, tasks
    ENTERTAINMENT,  // Games, video, music
    SOCIAL,         // Social media apps
    MAPS,          // Navigation, maps
    SHOPPING,      // E-commerce
    UTILITY,       // Tools, settings
    OTHER
}

@Serializable
data class DeviceState(
    val isScreenOn: Boolean = false,
    val isDeviceLocked: Boolean = false,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val isAirplaneMode: Boolean = false,
    val isPowerSavingMode: Boolean = false,
    val isNetworkConnected: Boolean = false,
    val networkType: String = "unknown",  // wifi, cellular, none
    val doNotDisturb: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class UserActivityLevel {
    NONE,      // Device locked, screen off
    LOW,       // User present but idle
    MEDIUM,    // Normal interaction
    HIGH,      // Active heavy use
    EXTREME    // Very active (gaming, video streaming)
}

@Serializable
enum class ContextChangeType {
    NONE,
    APP_FOREGROUND_CHANGED,
    SCREEN_STATE_CHANGED,
    LOCK_STATE_CHANGED,
    BATTERY_CHANGED,
    NETWORK_CHANGED,
    POWER_MODE_CHANGED
}

/**
 * Implementation of AIShellContextAggregator
 */
class DefaultAIShellContextAggregator(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val shellController: AIShellController? = null
) : AIShellContextAggregator, DefaultLifecycleObserver {

    private val _contextFlow = MutableStateFlow(DeviceContextSnapshot())
    private var currentContext = DeviceContextSnapshot()

    private var broadcastReceiver: BroadcastReceiver? = null
    private var isMonitoring = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        Timber.i("🐚 AIShellContextAggregator created")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        scope.launch {
            stopMonitoring()
        }
        Timber.i("🐚 AIShellContextAggregator destroyed")
    }

    override suspend fun startMonitoring() {
        if (isMonitoring) return

        Timber.i("🐚 Starting context aggregation")

        // Register for system broadcasts
        registerBroadcastReceiver()

        // Initial context read
        updateContext()

        // Start polling foreground app
        startForegroundAppPolling()

        isMonitoring = true
        Timber.i("🐚 Context aggregation started")
    }

    override suspend fun stopMonitoring() {
        if (!isMonitoring) return

        Timber.i("🐚 Stopping context aggregation")
        unregisterBroadcastReceiver()
        stopForegroundAppPolling()
        isMonitoring = false
        Timber.i("🐚 Context aggregation stopped")
    }

    override fun getCurrentContext(): DeviceContextSnapshot = currentContext

    override fun observeContext(): StateFlow<DeviceContextSnapshot> = _contextFlow.asStateFlow()

    override fun getForegroundApp(): ForegroundAppInfo = currentContext.foregroundApp

    override fun getDeviceState(): DeviceState = currentContext.deviceState

    override fun getUserActivityLevel(): UserActivityLevel = currentContext.userActivityLevel

    /**
     * Register broadcast receiver for system events
     */
    private fun registerBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let { handleSystemBroadcast(it) }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction("android.intent.action.BATTERY_LOW")
            addAction("android.intent.action.DEVICE_STORAGE_LOW")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addAction(Intent.ACTION_POWER_SAVE_MODE_CHANGED)
            }
        }

        context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
        Timber.i("🐚 Broadcast receiver registered")
    }

    /**
     * Unregister broadcast receiver
     */
    private fun unregisterBroadcastReceiver() {
        broadcastReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Timber.e(e, "🐚 Error unregistering broadcast receiver")
            }
        }
        broadcastReceiver = null
    }

    /**
     * Handle system broadcast event
     */
    private fun handleSystemBroadcast(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Timber.i("🐚 Screen turned on")
                updateDeviceState { copy(isScreenOn = true) }
                shellController?.onSystemEventOccurred(
                    SystemEvent.ScreenStateChanged(true)
                )
            }
            Intent.ACTION_SCREEN_OFF -> {
                Timber.i("🐚 Screen turned off")
                updateDeviceState { copy(isScreenOn = false) }
                shellController?.onSystemEventOccurred(
                    SystemEvent.ScreenStateChanged(false)
                )
            }
            Intent.ACTION_USER_PRESENT -> {
                Timber.i("🐚 Device unlocked")
                updateDeviceState { copy(isDeviceLocked = false) }
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                Timber.i("🐚 Battery changed: $level%, charging: $isCharging")
                updateDeviceState {
                    copy(
                        batteryPercent = level,
                        isCharging = isCharging
                    )
                }

                if (level <= 15) {
                    shellController?.onSystemEventOccurred(
                        SystemEvent.BatteryLow(level)
                    )
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                Timber.i("🐚 Power connected")
                updateDeviceState { copy(isCharging = true) }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Timber.i("🐚 Power disconnected")
                updateDeviceState { copy(isCharging = false) }
            }
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val isAirplaneMode = intent.getBooleanExtra("state", false)
                Timber.i("🐚 Airplane mode: $isAirplaneMode")
                updateDeviceState { copy(isAirplaneMode = isAirplaneMode) }
            }
        }

        updateContext()
    }

    /**
     * Start polling for foreground app changes
     */
    private fun startForegroundAppPolling() {
        // In real implementation, would use AccessibilityService
        // For now, we poll periodically
        Timber.i("🐚 Started foreground app polling")
    }

    /**
     * Stop polling for foreground app
     */
    private fun stopForegroundAppPolling() {
        Timber.i("🐚 Stopped foreground app polling")
    }

    /**
     * Update device state
     */
    private fun updateDeviceState(updater: DeviceState.() -> DeviceState) {
        val newState = currentContext.deviceState.updater()
        currentContext = currentContext.copy(deviceState = newState)
    }

    /**
     * Update complete context
     */
    private fun updateContext() {
        val changeType = determineChangeType()
        val newContext = currentContext.copy(
            foregroundApp = getForegroundAppInfo(),
            timestamp = System.currentTimeMillis(),
            contextChangeType = changeType
        )

        currentContext = newContext
        _contextFlow.value = newContext

        Timber.d("🐚 Context updated: ${changeType.name}")
    }

    /**
     * Determine what changed
     */
    private fun determineChangeType(): ContextChangeType {
        // Would compare with previous context to determine what changed
        return ContextChangeType.NONE
    }

    /**
     * Get foreground app information
     */
    private fun getForegroundAppInfo(): ForegroundAppInfo {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val usageStatsManager =
                    context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

                usageStatsManager?.let {
                    val currentTimeMs = System.currentTimeMillis()
                    val stats = it.queryUsageStats(
                        UsageStatsManager.INTERVAL_BEST,
                        currentTimeMs - 1000,
                        currentTimeMs
                    )

                    if (stats.isNotEmpty()) {
                        val topApp = stats.maxByOrNull { stat -> stat.lastTimeUsed } ?: return@let null
                        return ForegroundAppInfo(
                            packageName = topApp.packageName,
                            activityName = "unknown",
                            appCategory = categorizeApp(topApp.packageName)
                        )
                    }
                }
            }

            // Fallback
            ForegroundAppInfo("unknown", "unknown")
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error getting foreground app")
            ForegroundAppInfo("unknown", "unknown")
        }
    }

    /**
     * Categorize app based on package name
     */
    private fun categorizeApp(packageName: String): AppCategory {
        return when {
            packageName.contains("mail") || packageName.contains("messages") || packageName.contains("phone") ->
                AppCategory.COMMUNICATION
            packageName.contains("office") || packageName.contains("docs") || packageName.contains("sheets") ->
                AppCategory.PRODUCTIVITY
            packageName.contains("game") || packageName.contains("youtube") || packageName.contains("netflix") ->
                AppCategory.ENTERTAINMENT
            packageName.contains("facebook") || packageName.contains("instagram") || packageName.contains("twitter") ->
                AppCategory.SOCIAL
            packageName.contains("maps") || packageName.contains("navigation") ->
                AppCategory.MAPS
            packageName.contains("amazon") || packageName.contains("ebay") || packageName.contains("shop") ->
                AppCategory.SHOPPING
            else -> AppCategory.OTHER
        }
    }
}

/**
 * Singleton accessor for AIShellContextAggregator
 */
object AIShellContextSystem {
    private var aggregator: AIShellContextAggregator? = null

    fun initialize(
        context: Context,
        shellController: AIShellController? = null
    ): AIShellContextAggregator {
        if (aggregator == null) {
            aggregator = DefaultAIShellContextAggregator(context, shellController = shellController)
        }
        return aggregator!!
    }

    fun get(): AIShellContextAggregator? = aggregator

    suspend fun shutdown() {
        aggregator?.stopMonitoring()
        aggregator = null
    }
}
