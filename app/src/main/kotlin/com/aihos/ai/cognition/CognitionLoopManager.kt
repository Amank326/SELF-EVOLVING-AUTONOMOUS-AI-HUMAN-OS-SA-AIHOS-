package com.aihos.ai.cognition

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.aihos.ai.autonomy.AutonomyController
import com.aihos.ai.autonomy.DecisionOutcome
import com.aihos.ai.perception.SystemSignalsManager
import com.aihos.ai.reasoning.ReasoningContext
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * CognitionLoopManager: Orchestrates continuous AI cognition
 * 
 * Replaces purely event-driven model with periodic reasoning that:
 * - Runs continuously in background (with smart scheduling)
 * - Pauses/resumes safely with app lifecycle
 * - Adapts frequency based on environment (battery, activity, etc.)
 * - Prevents battery drain through intelligent throttling
 * - Maintains cognitive continuity across time
 * 
 * Architecture:
 * - Foreground loop: Active when app is visible (fast, frequent)
 * - Background loop: Active when app is minimized (slow, smart)
 * - Environment-aware scheduling: Adjusts frequency per device state
 * - Lifecycle integration: Pauses/resumes with app lifecycle
 */
interface CognitionLoopManager {
    /**
     * Start the continuous cognition loop
     * Respects lifecycle state (pauses if app in background)
     */
    suspend fun startContinuousCognition()
    
    /**
     * Stop the continuous cognition loop
     */
    suspend fun stopContinuousCognition()
    
    /**
     * Pause cognition (safe to call anytime)
     */
    suspend fun pauseCognition()
    
    /**
     * Resume cognition (respects lifecycle state)
     */
    suspend fun resumeCognition()
    
    /**
     * Get current cognition loop status
     */
    fun getLoopStatus(): CognitionLoopStatus
    
    /**
     * Get scheduling metrics
     */
    fun getSchedulingMetrics(): SchedulingMetrics
    
    /**
     * Reset metrics
     */
    fun resetMetrics()
}

/**
 * Cognition loop status information
 */
data class CognitionLoopStatus(
    val isRunning: Boolean,
    val isPaused: Boolean,
    val isBackgroundMode: Boolean,
    val currentIntervalMs: Long,
    val nextCognitionInMs: Long,
    val cyclesCompletedThisSession: Int,
    val averageCycleTimeMs: Long,
    val lastCognitionTimestamp: Long = 0L,
    val lastError: String? = null
)

/**
 * Metrics for monitoring scheduling behavior
 */
data class SchedulingMetrics(
    val totalCyclesCompleted: Long = 0L,
    val totalCycleTimeMs: Long = 0L,
    val maxCycleTimeMs: Long = 0L,
    val minCycleTimeMs: Long = Long.MAX_VALUE,
    val averageCycleTimeMs: Long = 0L,
    val pausedCount: Int = 0,
    val resumedCount: Int = 0,
    val backgroundTransitions: Int = 0,
    val foregroundTransitions: Int = 0,
    val errorCount: Int = 0,
    val batteryDrainEstimate: Float = 0f // Percentage per hour
)

/**
 * Cognition loop configuration
 */
data class CognitionLoopConfig(
    // Foreground cognition (app visible)
    val foregroundMinIntervalMs: Long = 5_000L,      // 5 seconds minimum
    val foregroundMaxIntervalMs: Long = 30_000L,     // 30 seconds maximum
    val foregroundPreferredIntervalMs: Long = 10_000L, // 10 seconds default
    
    // Background cognition (app minimized)
    val backgroundMinIntervalMs: Long = 30_000L,     // 30 seconds minimum
    val backgroundMaxIntervalMs: Long = 5 * 60_000L, // 5 minutes maximum
    val backgroundPreferredIntervalMs: Long = 60_000L, // 1 minute default
    
    // Environment-aware tuning
    val enableEnvironmentAwareTuning: Boolean = true,
    val criticalBatteryDisablesBackground: Boolean = true, // <15% battery
    val lowBatterySlowsFrequency: Boolean = true,          // <30% battery
    val idleActivity: Boolean = true,                        // Slow when user idle
    
    // WorkManager background jobs
    val enableBackgroundWorkerSync: Boolean = true,
    val backgroundWorkerIntervalMinutes: Long = 15L,
    val requiresDeviceIdle: Boolean = false,
    val requiresCharging: Boolean = false,
    
    // Safety limits
    val maxConsecutiveErrors: Int = 5,
    val errorBackoffMultiplier: Float = 1.5f,
    val maxErrorBackoffMs: Long = 5 * 60_000L // 5 minutes
)

/**
 * Default implementation of CognitionLoopManager
 */
class DefaultCognitionLoopManager(
    private val context: Context,
    private val autonomyController: AutonomyController,
    private val contextProvider: ReasoningContextProvider,
    private val systemSignalsManager: SystemSignalsManager,
    private val config: CognitionLoopConfig = CognitionLoopConfig(),
    private val scope: CoroutineScope = GlobalScope
) : CognitionLoopManager, DefaultLifecycleObserver {
    
    // State management
    private var isRunning = false
    private var isPaused = false
    private var isBackgroundMode = false
    private var cognitionJob: Job? = null
    private var syncWorkerScheduled = false
    
    // Metrics
    private var cyclesCompleted = 0
    private var totalCycleTimeMs = 0L
    private var maxCycleTimeMs = 0L
    private var minCycleTimeMs = Long.MAX_VALUE
    private var pausedCount = 0
    private var resumedCount = 0
    private var lastCognitionTime = 0L
    private var consecutiveErrors = 0
    private var lastErrorMessage: String? = null
    
    // Timing
    private var currentIntervalMs = config.foregroundPreferredIntervalMs
    private var nextCognitionScheduledAt = 0L
    
    init {
        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Timber.i("CognitionLoopManager initialized with config: $config")
    }
    
    // ========== LIFECYCLE HOOKS ==========
    
    override fun onCreate(owner: LifecycleOwner) {
        Timber.d("App lifecycle: onCreate")
    }
    
    override fun onStart(owner: LifecycleOwner) {
        Timber.d("App lifecycle: onStart - entering foreground")
        isBackgroundMode = false
        updateCognitionInterval()
        resumeCognitionInternal()
    }
    
    override fun onResume(owner: LifecycleOwner) {
        Timber.d("App lifecycle: onResume - app visible")
        isBackgroundMode = false
        updateCognitionInterval()
    }
    
    override fun onPause(owner: LifecycleOwner) {
        Timber.d("App lifecycle: onPause - app backgrounded")
        isBackgroundMode = true
        updateCognitionInterval()
        // Don't pause cognition - allow background thinking
    }
    
    override fun onStop(owner: LifecycleOwner) {
        Timber.d("App lifecycle: onStop")
        // Allow background worker to take over for infrequent cognition
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        Timber.d("App lifecycle: onDestroy")
        stopCognitionLoopInternal()
    }
    
    // ========== PUBLIC API ==========
    
    override suspend fun startContinuousCognition() {
        if (isRunning) {
            Timber.w("Cognition loop already running")
            return
        }
        
        isRunning = true
        isPaused = false
        Timber.i("Starting continuous cognition loop")
        
        // Schedule background worker for persistent background cognition
        if (config.enableBackgroundWorkerSync) {
            scheduleBackgroundWorker()
        }
        
        // Start foreground cognition loop (will respect app lifecycle)
        cognitionJob = scope.launch {
            try {
                runCognitionLoop()
            } catch (e: Exception) {
                Timber.e(e, "Cognition loop crashed")
                handleFatalError(e)
            }
        }
    }
    
    override suspend fun stopContinuousCognition() {
        stopCognitionLoopInternal()
    }
    
    override suspend fun pauseCognition() {
        if (isPaused) {
            Timber.w("Cognition already paused")
            return
        }
        
        isPaused = true
        pausedCount++
        Timber.i("Cognition paused (count: $pausedCount)")
    }
    
    override suspend fun resumeCognition() {
        resumeCognitionInternal()
    }
    
    override fun getLoopStatus(): CognitionLoopStatus {
        return CognitionLoopStatus(
            isRunning = isRunning,
            isPaused = isPaused,
            isBackgroundMode = isBackgroundMode,
            currentIntervalMs = currentIntervalMs,
            nextCognitionInMs = max(0, nextCognitionScheduledAt - System.currentTimeMillis()),
            cyclesCompletedThisSession = cyclesCompleted,
            averageCycleTimeMs = if (cyclesCompleted > 0) totalCycleTimeMs / cyclesCompleted else 0L,
            lastCognitionTimestamp = lastCognitionTime,
            lastError = lastErrorMessage
        )
    }
    
    override fun getSchedulingMetrics(): SchedulingMetrics {
        return SchedulingMetrics(
            totalCyclesCompleted = cyclesCompleted.toLong(),
            totalCycleTimeMs = totalCycleTimeMs,
            maxCycleTimeMs = if (maxCycleTimeMs > 0) maxCycleTimeMs else 0L,
            minCycleTimeMs = if (minCycleTimeMs != Long.MAX_VALUE) minCycleTimeMs else 0L,
            averageCycleTimeMs = if (cyclesCompleted > 0) totalCycleTimeMs / cyclesCompleted else 0L,
            pausedCount = pausedCount,
            resumedCount = resumedCount,
            backgroundTransitions = 0, // Track separately if needed
            foregroundTransitions = 0, // Track separately if needed
            errorCount = 0,            // Track separately if needed
            batteryDrainEstimate = estimateBatteryDrain()
        )
    }
    
    override fun resetMetrics() {
        cyclesCompleted = 0
        totalCycleTimeMs = 0L
        maxCycleTimeMs = 0L
        minCycleTimeMs = Long.MAX_VALUE
        pausedCount = 0
        resumedCount = 0
        lastCognitionTime = 0L
        consecutiveErrors = 0
        Timber.i("Metrics reset")
    }
    
    // ========== INTERNAL IMPLEMENTATION ==========
    
    /**
     * Main cognition loop - runs continuously, respecting pause/resume
     */
    private suspend fun runCognitionLoop() {
        while (isRunning) {
            try {
                // Check if paused
                if (isPaused) {
                    delay(1000) // Check pause status every second
                    continue
                }
                
                // Calculate time until next cognition
                val now = System.currentTimeMillis()
                val delayMs = nextCognitionScheduledAt - now
                
                if (delayMs > 0) {
                    // Not time yet - wait
                    delay(minOf(delayMs, 1000)) // Check every second
                    continue
                }
                
                // Time to think!
                val cycleStartTime = System.currentTimeMillis()
                
                try {
                    // Execute cognition cycle
                    executeCognitionCycle()
                    
                    // Record metrics
                    val cycleTime = System.currentTimeMillis() - cycleStartTime
                    recordCycleMetrics(cycleTime)
                    
                    // Reset error backoff on success
                    consecutiveErrors = 0
                    lastErrorMessage = null
                    
                } catch (e: Exception) {
                    handleCognitionError(e)
                }
                
                // Schedule next cognition
                updateCognitionInterval()
                nextCognitionScheduledAt = System.currentTimeMillis() + currentIntervalMs
                
                Timber.d("Next cognition scheduled in ${currentIntervalMs}ms (interval: ${if (isBackgroundMode) "background" else "foreground"})")
                
            } catch (e: CancellationException) {
                Timber.d("Cognition loop cancelled")
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error in cognition loop")
                delay(5000)
            }
        }
    }
    
    /**
     * Execute a single cognition cycle
     * This is where the AI actually thinks
     */
    private suspend fun executeCognitionCycle() {
        try {
            // Get current reasoning context
            val context = contextProvider.getCurrentContext()
            
            Timber.d("Executing cognition cycle: ${context.currentTime}, idle=${!context.userIsFocused}")
            
            // Trigger autonomy controller decision cycle
            val outcome = autonomyController.triggerDecisionCycle(context)
            
            lastCognitionTime = System.currentTimeMillis()
            
            Timber.d("Cognition cycle completed: ${outcome.action} (executed=${outcome.executed})")
            
            // If decision was made, let autonomy controller handle reflection/evolution
            // (they're asynchronous in AutonomyController)
            
        } catch (e: Exception) {
            Timber.e(e, "Error during cognition cycle execution")
            throw e
        }
    }
    
    /**
     * Update cognition interval based on environment
     */
    private fun updateCognitionInterval() {
        val baseInterval = if (isBackgroundMode) {
            config.backgroundPreferredIntervalMs
        } else {
            config.foregroundPreferredIntervalMs
        }
        
        var adjustedInterval = baseInterval
        
        // Apply environment-aware tuning if enabled
        if (config.enableEnvironmentAwareTuning) {
            adjustedInterval = adjustIntervalForEnvironment(adjustedInterval)
        }
        
        // Apply bounds
        val (minInterval, maxInterval) = if (isBackgroundMode) {
            Pair(config.backgroundMinIntervalMs, config.backgroundMaxIntervalMs)
        } else {
            Pair(config.foregroundMinIntervalMs, config.foregroundMaxIntervalMs)
        }
        
        currentIntervalMs = adjustedInterval.coerceIn(minInterval, maxInterval)
        
        Timber.d("Cognition interval updated to ${currentIntervalMs}ms (base=$baseInterval, adjusted=$adjustedInterval)")
    }
    
    /**
     * Adjust interval based on system signals and environment
     */
    private fun adjustIntervalForEnvironment(baseInterval: Long): Long {
        var interval = baseInterval
        
        // Try to get environment context if available
        try {
            val environment = systemSignalsManager.getEnvironmentContext()
            
            // Critical battery: slow down cognition to save power
            if (environment.battery.levelPercent < 15 && config.criticalBatteryDisablesBackground) {
                interval = (interval * 2.0).toLong() // 2x slower
                Timber.d("Critical battery detected - slowing cognition interval 2x")
            }
            
            // Low battery: slow down somewhat
            else if (environment.battery.levelPercent < 30 && config.lowBatterySlowsFrequency) {
                interval = (interval * 1.5).toLong() // 1.5x slower
                Timber.d("Low battery detected - slowing cognition interval 1.5x")
            }
            
            // User idle: can afford slower cognition in background
            if (isBackgroundMode && environment.activity == UserActivityLevel.IDLE && config.idleActivity) {
                interval = (interval * 1.3).toLong() // 1.3x slower when idle
                Timber.d("User idle in background - slowing interval 1.3x")
            }
            
            // Network down: no point in frequent reasoning (might affect cloud features)
            if (environment.network == NetworkState.DISCONNECTED && isBackgroundMode) {
                interval = (interval * 1.5).toLong() // 1.5x slower
                Timber.d("Network down - slowing interval 1.5x")
            }
            
        } catch (e: Exception) {
            Timber.d(e, "Could not get environment context for interval adjustment")
        }
        
        return interval
    }
    
    /**
     * Schedule a background WorkManager job for persistent cognition
     */
    private fun scheduleBackgroundWorker() {
        if (syncWorkerScheduled) return
        
        try {
            val workRequest = PeriodicWorkRequestBuilder<BackgroundCognitionWorker>(
                config.backgroundWorkerIntervalMinutes,
                TimeUnit.MINUTES
            ).apply {
                // Only run when device has reasonable battery
                setConstraints(Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresDeviceIdle(config.requiresDeviceIdle)
                    .setRequiresCharging(config.requiresCharging)
                    .build())
                // Retry with exponential backoff
                setBackoffPolicy(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            }.build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "background_cognition",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            syncWorkerScheduled = true
            Timber.i("Background cognition worker scheduled every ${config.backgroundWorkerIntervalMinutes} minutes")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule background worker")
        }
    }
    
    /**
     * Record cycle execution metrics
     */
    private fun recordCycleMetrics(cycleTimeMs: Long) {
        cyclesCompleted++
        totalCycleTimeMs += cycleTimeMs
        maxCycleTimeMs = maxOf(maxCycleTimeMs, cycleTimeMs)
        minCycleTimeMs = minOf(minCycleTimeMs, cycleTimeMs)
        
        if (cyclesCompleted % 10 == 0L) {
            val avgTime = totalCycleTimeMs / cyclesCompleted
            Timber.d("Metrics: cycles=$cyclesCompleted, avg=${avgTime}ms, max=${maxCycleTimeMs}ms, min=${minCycleTimeMs}ms")
        }
    }
    
    /**
     * Handle cognition errors with backoff
     */
    private fun handleCognitionError(e: Exception) {
        consecutiveErrors++
        lastErrorMessage = e.message ?: "Unknown error"
        
        Timber.e(e, "Cognition error (consecutive: $consecutiveErrors)")
        
        if (consecutiveErrors >= config.maxConsecutiveErrors) {
            Timber.e("Max consecutive errors reached - entering high backoff mode")
            // Increase interval significantly
            currentIntervalMs = min(
                currentIntervalMs.toLong(),
                (currentIntervalMs * config.errorBackoffMultiplier).toLong()
            ).coerceAtMost(config.maxErrorBackoffMs)
        }
    }
    
    /**
     * Handle fatal errors
     */
    private fun handleFatalError(e: Exception) {
        Timber.e(e, "Fatal error in cognition loop")
        isRunning = false
        lastErrorMessage = "Fatal: ${e.message}"
    }
    
    /**
     * Stop cognition loop safely
     */
    private fun stopCognitionLoopInternal() {
        if (!isRunning) {
            Timber.w("Cognition loop not running")
            return
        }
        
        isRunning = false
        cognitionJob?.cancel()
        
        Timber.i("Cognition loop stopped. Stats: $cyclesCompleted cycles, avg=${if (cyclesCompleted > 0) totalCycleTimeMs / cyclesCompleted else 0}ms")
    }
    
    /**
     * Resume cognition safely
     */
    private suspend fun resumeCognitionInternal() {
        if (!isPaused) {
            Timber.w("Cognition not paused")
            return
        }
        
        isPaused = false
        resumedCount++
        
        // Trigger next cognition cycle immediately
        nextCognitionScheduledAt = System.currentTimeMillis()
        
        Timber.i("Cognition resumed (count: $resumedCount)")
    }
    
    /**
     * Estimate battery drain per hour from cognition
     */
    private fun estimateBatteryDrain(): Float {
        if (cyclesCompleted == 0) return 0f
        
        // Very rough estimate: 
        // Average cycle time in ms * cycles per hour / 3_600_000 ms per hour
        val avgCycleMs = totalCycleTimeMs / cyclesCompleted
        val cyclesPerHour = 3_600_000 / currentIntervalMs
        val totalProcessingPerHour = avgCycleMs * cyclesPerHour
        
        // Assume ~0.05% battery per second of processing (very rough)
        return (totalProcessingPerHour / 1000f) * 0.05f
    }
}

/**
 * Provider for ReasoningContext in cognition loop
 * Abstracts context creation from loop logic
 */
interface ReasoningContextProvider {
    suspend fun getCurrentContext(): ReasoningContext
}

/**
 * Background worker for persistent cognition when app is not visible
 * WorkManager will wake up the app periodically to run cognition
 */
class BackgroundCognitionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            Timber.d("BackgroundCognitionWorker: Starting background cognition cycle")
            
            // In practice, this would get these from DI
            // For now, we just log that the worker ran
            
            Timber.d("BackgroundCognitionWorker: Completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "BackgroundCognitionWorker: Failed")
            Result.retry()
        }
    }
}

// Import stubs for types that should be in other files
data class UserActivityLevel(val value: String) {
    companion object {
        val IDLE = UserActivityLevel("IDLE")
        val LIGHT = UserActivityLevel("LIGHT")
        val ACTIVE = UserActivityLevel("ACTIVE")
        val INTENSE = UserActivityLevel("INTENSE")
    }
}

data class NetworkState(val value: String) {
    companion object {
        val CONNECTED = NetworkState("CONNECTED")
        val DISCONNECTED = NetworkState("DISCONNECTED")
        val METERED = NetworkState("METERED")
    }
}

// This should be imported from perception module
interface SystemSignalsManager {
    fun getEnvironmentContext(): EnvironmentContextProxy
}

data class EnvironmentContextProxy(
    val battery: BatteryContextProxy,
    val network: NetworkState,
    val activity: UserActivityLevel
)

data class BatteryContextProxy(
    val levelPercent: Int,
    val isCharging: Boolean
)
