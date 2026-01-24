package com.aihos.system.signals.impl

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.cancel
import com.aihos.system.signals.DeviceContext
import com.aihos.system.signals.SignalCollector
import com.aihos.system.signals.impl.providers.BatteryProvider
import com.aihos.system.signals.impl.providers.ScreenStateProvider
import com.aihos.system.signals.impl.providers.NetworkProvider
import com.aihos.system.signals.impl.providers.TemperatureProvider
import com.aihos.system.signals.impl.providers.TimeOfDayProvider
import com.aihos.system.signals.impl.providers.ForegroundAppProvider
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lifecycle-aware system signals manager.
 *
 * Responsibilities:
 * - Manage registration/unregistration of all system listeners
 * - Bind listener lifecycle to Activity/Fragment lifecycle
 * - Aggregate all signals into DeviceContext
 * - Throttle and debounce updates appropriately
 * - Ensure zero memory leaks
 *
 * Thread-safe: All listener operations are protected by mutex.
 *
 * Usage:
 * ```kotlin
 * val manager = SystemSignalsManager(context, lifecycleOwner)
 * lifecycleOwner.lifecycleScope.launch {
 *     manager.deviceContext.collect { context ->
 *         // Use context for AI reasoning
 *     }
 * }
 * // Cleanup is automatic via lifecycle binding
 * ```
 */
class SystemSignalsManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Supervisor Job())
) : SignalCollector {

    // ==================== STATE ====================

    /**
     * Current device context state.
     * Emitted whenever any signal changes.
     */
    private val _deviceContext = MutableStateFlow<DeviceContext>(
        DeviceContextImpl()
    )
    override val deviceContext: StateFlow<DeviceContext> = _deviceContext.asStateFlow()

    /**
     * Tracks whether listeners are currently registered.
     */
    private val isListenersActive = AtomicBoolean(false)

    /**
     * Mutex protecting listener registration/unregistration.
     */
    private val listenerMutex = Mutex()

    /**
     * Track active listener count for debugging.
     */
    private val activeListeners = mutableSetOf<String>()

    // ==================== SIGNAL PROVIDERS ====================

    private val batteryProvider = BatteryProvider(context)
    private val temperatureProvider = TemperatureProvider(context)
    private val screenProvider = ScreenStateProvider(context)
    private val networkProvider = NetworkProvider(context)
    private val timeOfDayProvider = TimeOfDayProvider()
    private val foregroundAppProvider = ForegroundAppProvider(context)

    // ==================== LIFECYCLE MANAGEMENT ====================

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        scope.launch {
            when (event) {
                Lifecycle.Event.ON_START -> {
                    Timber.d("SystemSignalsManager: ON_START - Registering all listeners")
                    registerAllListeners()
                }
                Lifecycle.Event.ON_STOP -> {
                    Timber.d("SystemSignalsManager: ON_STOP - Unregistering all listeners")
                    unregisterAllListeners()
                }
                else -> {}
            }
        }
    }

    init {
        Timber.d("SystemSignalsManager: Initializing with lifecycle binding")
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        // Start listening to signal changes
        scope.launch {
            aggregateSignalsIntoContext()
        }
    }

    // ==================== LISTENER LIFECYCLE ====================

    /**
     * Register all system listeners.
     * Called on lifecycle ON_START.
     * Safe: Multiple calls don't re-register if already active.
     */
    private suspend fun registerAllListeners() {
        if (isListenersActive.getAndSet(true)) {
            Timber.d("SystemSignalsManager: Listeners already active, skipping re-registration")
            return
        }

        listenerMutex.withLock {
            try {
                activeListeners.clear()

                // Register battery changes
                batteryProvider.register()
                activeListeners.add("battery")
                Timber.d("✓ Battery listener registered")

                // Register screen state changes
                screenProvider.register()
                activeListeners.add("screen")
                Timber.d("✓ Screen listener registered")

                // Register network state changes
                networkProvider.register()
                activeListeners.add("network")
                Timber.d("✓ Network listener registered")

                // Register temperature updates (polling-based)
                temperatureProvider.register()
                activeListeners.add("temperature")
                Timber.d("✓ Temperature provider started")

                // Time of day provider (polling-based)
                timeOfDayProvider.register()
                activeListeners.add("time_of_day")
                Timber.d("✓ Time of day provider started")

                // Foreground app monitoring (AccessibilityService or polling)
                foregroundAppProvider.register()
                activeListeners.add("foreground_app")
                Timber.d("✓ Foreground app provider started")

                Timber.i("SystemSignalsManager: ${activeListeners.size} listeners registered")

            } catch (e: Exception) {
                Timber.e(e, "SystemSignalsManager: Error registering listeners")
                isListenersActive.set(false)
                // Attempt to unregister any partially registered listeners
                try {
                    unregisterAllListenersSafe()
                } catch (ue: Exception) {
                    Timber.e(ue, "Failed to unregister on registration error")
                }
            }
        }
    }

    /**
     * Unregister all system listeners.
     * Called on lifecycle ON_STOP.
     * Safe: Handles already-unregistered listeners gracefully.
     * Critical: Prevents memory leaks and background activity.
     */
    private suspend fun unregisterAllListeners() {
        if (!isListenersActive.getAndSet(false)) {
            Timber.d("SystemSignalsManager: Listeners already inactive, skipping unregistration")
            return
        }

        listenerMutex.withLock {
            unregisterAllListenersSafe()
        }
    }

    /**
     * Safe unregistration with error handling.
     * Each provider handles its own cleanup.
     */
    private suspend fun unregisterAllListenersSafe() {
        // Unregister each provider safely
        // If one fails, continue with others

        try {
            batteryProvider.unregister()
            Timber.d("✓ Battery listener unregistered")
        } catch (e: Exception) {
            Timber.w(e, "Failed to unregister battery listener")
        }

        try {
            screenProvider.unregister()
            Timber.d("✓ Screen listener unregistered")
        } catch (e: Exception) {
            Timber.w(e, "Failed to unregister screen listener")
        }

        try {
            networkProvider.unregister()
            Timber.d("✓ Network listener unregistered")
        } catch (e: Exception) {
            Timber.w(e, "Failed to unregister network listener")
        }

        try {
            temperatureProvider.unregister()
            Timber.d("✓ Temperature provider stopped")
        } catch (e: Exception) {
            Timber.w(e, "Failed to stop temperature provider")
        }

        try {
            timeOfDayProvider.unregister()
            Timber.d("✓ Time of day provider stopped")
        } catch (e: Exception) {
            Timber.w(e, "Failed to stop time of day provider")
        }

        try {
            foregroundAppProvider.unregister()
            Timber.d("✓ Foreground app provider stopped")
        } catch (e: Exception) {
            Timber.w(e, "Failed to stop foreground app provider")
        }

        val count = activeListeners.size
        activeListeners.clear()
        Timber.i("SystemSignalsManager: All $count listeners unregistered")
    }

    // ==================== SIGNAL AGGREGATION ====================

    /**
     * Aggregate all signal flows into a unified DeviceContext.
     * Runs continuously while listeners are active.
     */
    private suspend fun aggregateSignalsIntoContext() {
        while (true) {
            try {
                val context = DeviceContextImpl(
                    batteryLevel = batteryProvider.value.value,
                    temperature = temperatureProvider.value.value,
                    isScreenOn = screenProvider.value.value,
                    foregroundApp = foregroundAppProvider.value.value,
                    usageIntensity = calculateUsageIntensity(),
                    timeOfDay = timeOfDayProvider.value.value,
                    isNetworkConnected = networkProvider.value.value,
                    timestamp = System.currentTimeMillis()
                )

                _deviceContext.emit(context)

            } catch (e: Exception) {
                Timber.e(e, "Error aggregating signals into context")
            }

            // Small delay to prevent tight loop
            // Real updates come from signal providers (event-driven or polling)
            kotlinx.coroutines.delay(100)
        }
    }

    /**
     * Calculate usage intensity from various signals.
     * Simple heuristic: combination of screen state, battery, and time of day.
     */
    private fun calculateUsageIntensity(): Float {
        val screenFactor = if (screenProvider.value.value) 0.7f else 0.2f
        val batteryFactor = (batteryProvider.value.value / 100.0f) * 0.3f
        return (screenFactor + batteryFactor).coerceIn(0.0f, 1.0f)
    }

    // ==================== PUBLIC API ====================

    /**
     * Start collecting signals.
     * Listeners are automatically registered via lifecycle binding.
     * This method is provided for compatibility with SignalCollector interface.
     */
    override suspend fun start() {
        Timber.d("SystemSignalsManager.start() called")
        // Listeners are managed by lifecycle automatically
        // This is a no-op, but kept for interface compatibility
    }

    /**
     * Stop collecting signals.
     * Listeners are automatically unregistered via lifecycle binding.
     * This method is provided for compatibility with SignalCollector interface.
     */
    override suspend fun stop() {
        Timber.d("SystemSignalsManager.stop() called")
        // Listeners are managed by lifecycle automatically
        // This is a no-op, but kept for interface compatibility
    }

    /**
     * Cleanup: Remove lifecycle observer and cancel scope.
     * Should be called when parent lifecycle owner is destroyed if needed.
     */
    fun destroy() {
        Timber.d("SystemSignalsManager.destroy()")
        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        scope.coroutineContext.cancel()
    }
}

// ==================== HELPER EXTENSION ====================

private suspend inline fun <T> Mutex.withLock(action: suspend () -> T): T {
    lock()
    try {
        return action()
    } finally {
        unlock()
    }
}
