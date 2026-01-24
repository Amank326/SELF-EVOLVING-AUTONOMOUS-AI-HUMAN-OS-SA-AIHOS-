package com.aihos.ai.cognition

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aihos.ai.autonomy.AutonomyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Safe wrapper around CognitionLoopManager that handles:
 * - Lifecycle awareness (pause in background, resume in foreground)
 * - Error recovery
 * - Configuration management
 * - Debugging utilities
 * 
 * This is the recommended entry point for integrating continuous cognition
 * into the application. It handles all the complexity while exposing
 * a simple, safe API.
 */
interface SafeCognitionController {
    /**
     * Initialize and start continuous cognition
     * Safe to call multiple times
     */
    suspend fun initialize()
    
    /**
     * Shutdown continuous cognition
     * Safe to call multiple times
     */
    suspend fun shutdown()
    
    /**
     * Get current loop status
     */
    fun getStatus(): CognitionLoopStatus
    
    /**
     * Get loop metrics
     */
    fun getMetrics(): SchedulingMetrics
    
    /**
     * Manually trigger a cognition cycle
     */
    suspend fun triggerCognitionCycle()
    
    /**
     * Update cognition config at runtime
     */
    fun updateConfig(config: CognitionLoopConfig)
    
    /**
     * Enable/disable background cognition
     */
    suspend fun setBackgroundEnabled(enabled: Boolean)
    
    /**
     * Pause/resume cognition
     */
    suspend fun setPaused(paused: Boolean)
}

/**
 * Default implementation with built-in safety mechanisms
 */
class DefaultSafeCognitionController(
    private val context: Context,
    private val autonomyController: AutonomyController,
    private val systemSignalsManager: SystemSignalsManager,
    initialConfig: CognitionLoopConfig = CognitionLoopConfig()
) : SafeCognitionController, DefaultLifecycleObserver {
    
    private var loopManager: CognitionLoopManager? = null
    private var loopMonitor: CognitionLoopMonitor? = null
    private var isInitialized = false
    private var isShuttingDown = false
    
    private var currentConfig = initialConfig
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    
    init {
        // Auto-register for lifecycle events
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        } catch (e: Exception) {
            Timber.w(e, "Could not register lifecycle observer - may be in testing environment")
        }
        
        Timber.i("SafeCognitionController initialized with config: $initialConfig")
    }
    
    // ========== PUBLIC API ==========
    
    override suspend fun initialize() {
        if (isInitialized || isShuttingDown) {
            Timber.w("Already initialized or shutting down")
            return
        }
        
        try {
            Timber.i("Initializing continuous cognition system...")
            
            // Create context provider
            val enricher = EnvironmentAwareReasoningContextEnricher()
            val contextProvider = DefaultReasoningContextProvider(systemSignalsManager, enricher)
            
            // Create loop manager
            loopManager = DefaultCognitionLoopManager(
                context = context,
                autonomyController = autonomyController,
                contextProvider = contextProvider,
                systemSignalsManager = systemSignalsManager,
                config = currentConfig,
                scope = scope
            )
            
            // Create monitor
            loopMonitor = CognitionLoopMonitor(loopManager!!)
            
            // Start the loop
            loopManager!!.startContinuousCognition()
            
            isInitialized = true
            Timber.i("Continuous cognition system initialized successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize continuous cognition")
            isInitialized = false
            throw e
        }
    }
    
    override suspend fun shutdown() {
        if (!isInitialized) {
            Timber.w("Not initialized")
            return
        }
        
        if (isShuttingDown) {
            Timber.w("Already shutting down")
            return
        }
        
        try {
            isShuttingDown = true
            Timber.i("Shutting down continuous cognition...")
            
            loopManager?.stopContinuousCognition()
            loopMonitor = null
            loopManager = null
            
            isInitialized = false
            isShuttingDown = false
            
            Timber.i("Continuous cognition shutdown complete")
            
        } catch (e: Exception) {
            Timber.e(e, "Error during shutdown")
            isShuttingDown = false
            throw e
        }
    }
    
    override fun getStatus(): CognitionLoopStatus {
        return loopManager?.getLoopStatus() ?: CognitionLoopStatus(
            isRunning = false,
            isPaused = true,
            isBackgroundMode = false,
            currentIntervalMs = 0,
            nextCognitionInMs = 0,
            cyclesCompletedThisSession = 0,
            averageCycleTimeMs = 0,
            lastCognitionTimestamp = 0,
            lastError = "Not initialized"
        )
    }
    
    override fun getMetrics(): SchedulingMetrics {
        return loopManager?.getSchedulingMetrics() ?: SchedulingMetrics()
    }
    
    override suspend fun triggerCognitionCycle() {
        if (!isInitialized) {
            Timber.w("Not initialized")
            return
        }
        
        try {
            Timber.d("Manually triggering cognition cycle")
            // TODO: Implement manual trigger (would need to refactor loop to support this)
        } catch (e: Exception) {
            Timber.e(e, "Error triggering cognition cycle")
        }
    }
    
    override fun updateConfig(config: CognitionLoopConfig) {
        currentConfig = config
        
        // TODO: Implement dynamic config update
        // For now, just log it
        Timber.i("Config updated: $config (requires restart to apply)")
    }
    
    override suspend fun setBackgroundEnabled(enabled: Boolean) {
        if (!isInitialized) {
            Timber.w("Not initialized")
            return
        }
        
        try {
            Timber.i("Background cognition ${if (enabled) "enabled" else "disabled"}")
            // TODO: Implement background enable/disable toggle
        } catch (e: Exception) {
            Timber.e(e, "Error changing background cognition")
        }
    }
    
    override suspend fun setPaused(paused: Boolean) {
        if (!isInitialized) {
            Timber.w("Not initialized")
            return
        }
        
        try {
            if (paused) {
                loopManager?.pauseCognition()
            } else {
                loopManager?.resumeCognition()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error pausing/resuming cognition")
        }
    }
    
    // ========== LIFECYCLE INTEGRATION ==========
    
    override fun onStart(owner: LifecycleOwner) {
        Timber.d("Lifecycle: onStart")
        // Foreground loop will handle this via DefaultCognitionLoopManager
    }
    
    override fun onStop(owner: LifecycleOwner) {
        Timber.d("Lifecycle: onStop")
        // Background loop will handle this via DefaultCognitionLoopManager
    }
    
    // ========== DEBUGGING HELPERS ==========
    
    /**
     * Get debug information about the cognition loop
     */
    fun getDebugInfo(): String {
        val status = getStatus()
        val metrics = getMetrics()
        
        return """
            ╔════════════════════════════════════════════════╗
            ║     CONTINUOUS COGNITION LOOP DEBUG INFO       ║
            ╚════════════════════════════════════════════════╝
            
            ${CognitionLoopDebugUtils.formatStatus(status)}
            
            ${CognitionLoopDebugUtils.formatMetrics(metrics)}
            
            Configuration:
            ├─ ForegroundInterval: ${currentConfig.foregroundPreferredIntervalMs}ms
            ├─ BackgroundInterval: ${currentConfig.backgroundPreferredIntervalMs}ms
            ├─ EnvironmentAwareTuning: ${currentConfig.enableEnvironmentAwareTuning}
            └─ BackgroundWorkerEnabled: ${currentConfig.enableBackgroundWorkerSync}
        """.trimIndent()
    }
    
    /**
     * Log debug info
     */
    fun logDebugInfo() {
        Timber.i(getDebugInfo())
    }
    
    /**
     * Reset all metrics
     */
    fun resetMetrics() {
        loopManager?.resetMetrics()
        Timber.i("Metrics reset")
    }
}

/**
 * Application-level initialization helper
 * Call this in your Application.onCreate() or in your DI setup
 */
object CognitionLoopInitializer {
    
    private var safeCognitionController: SafeCognitionController? = null
    
    /**
     * Initialize cognition loop in application
     * Typically called in Application.onCreate() or module setup
     */
    suspend fun init(
        context: Context,
        autonomyController: AutonomyController,
        systemSignalsManager: SystemSignalsManager,
        config: CognitionLoopConfig = CognitionLoopConfig()
    ): SafeCognitionController {
        if (safeCognitionController != null) {
            Timber.w("Already initialized")
            return safeCognitionController!!
        }
        
        Timber.i("Initializing CognitionLoop for application...")
        
        val controller = DefaultSafeCognitionController(
            context = context,
            autonomyController = autonomyController,
            systemSignalsManager = systemSignalsManager,
            initialConfig = config
        )
        
        controller.initialize()
        safeCognitionController = controller
        
        Timber.i("CognitionLoop initialized and ready")
        
        return controller
    }
    
    /**
     * Get the current cognition controller (or null if not initialized)
     */
    fun get(): SafeCognitionController? = safeCognitionController
    
    /**
     * Shutdown the cognition controller
     */
    suspend fun shutdown() {
        safeCognitionController?.shutdown()
        safeCognitionController = null
        Timber.i("CognitionLoop shutdown")
    }
}
