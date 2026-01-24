package com.aihos.ai.ml.impl

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.aihos.ai.ml.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong

/**
 * Default implementation of MLInterpreterManager
 * Lifecycle-aware coordinator for TensorFlow Lite interpreters
 *
 * Key Features:
 * - Automatic initialization on Activity.ON_START
 * - Automatic cleanup on Activity.ON_STOP
 * - Thread-safe async inference via Dispatcher.Default
 * - Graceful fallback on ML unavailable
 * - Memory leak prevention with proper resource cleanup
 * - Metrics tracking for monitoring
 *
 * Lifecycle Flow:
 * UNINITIALIZED → (ON_START) → LOADING → READY
 * READY → (ON_STOP) → SHUTDOWN
 * READY → (error) → ERROR
 */
class MLInterpreterManagerImpl(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val interpreterProviders: Map<MLModelType, TFLiteInterpreterProvider> = emptyMap(),
    private val throttlerFactory: (MLModelType) -> InferenceThrottler = { DefaultInferenceThrottler() },
    private val inputFormatterFactory: (MLModelType) -> SignalInputFormatter = { DefaultSignalInputFormatter() }
) : MLInterpreterManager {
    
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val interpreterLock = kotlinx.coroutines.sync.Mutex()
    
    // State management
    private val _status = MutableStateFlow(MLManagerStatus.UNINITIALIZED)
    override val statusFlow: StateFlow<MLManagerStatus> = _status.asStateFlow()
    
    // Interpreter instances per model type
    private val activeInterpreters = mutableMapOf<MLModelType, TFLiteInterpreterProvider>()
    private val throttlers = mutableMapOf<MLModelType, InferenceThrottler>()
    private val inputFormatters = mutableMapOf<MLModelType, SignalInputFormatter>()
    
    // Metrics tracking
    private val metricsState = MutableMetricsState()
    
    // Initialization flag
    private var initialized = false
    
    init {
        // Register as lifecycle observer
        lifecycleOwner.lifecycle.addObserver(this)
        Timber.d("MLInterpreterManager initialized, awaiting lifecycle events")
    }
    
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                Timber.d("Lifecycle: ON_START - initializing interpreters")
                scope.launch {
                    initializeInterpreters()
                }
            }
            Lifecycle.Event.ON_STOP -> {
                Timber.d("Lifecycle: ON_STOP - preparing for shutdown")
                scope.launch {
                    prepareShutdown()
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                Timber.d("Lifecycle: ON_DESTROY - cleaning up resources")
                cleanupResources()
            }
            else -> {} // Ignore other lifecycle events
        }
    }
    
    /**
     * Initialize all ML interpreters
     * Called automatically on Activity.ON_START
     */
    private suspend fun initializeInterpreters() {
        if (initialized) {
            Timber.w("Interpreters already initialized, skipping re-initialization")
            return
        }
        
        withContext(Dispatchers.Default) {
            interpreterLock.withLock {
                try {
                    _status.value = MLManagerStatus.LOADING
                    
                    // Initialize each model type
                    for ((modelType, provider) in interpreterProviders) {
                        try {
                            val success = provider.initialize()
                            if (success) {
                                activeInterpreters[modelType] = provider
                                throttlers[modelType] = throttlerFactory(modelType)
                                inputFormatters[modelType] = inputFormatterFactory(modelType)
                                Timber.d("Initialized interpreter for $modelType")
                                metricsState.recordModelLoaded()
                            } else {
                                Timber.w("Failed to initialize interpreter for $modelType")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error initializing $modelType interpreter")
                            metricsState.recordError(e.message ?: "Unknown error")
                        }
                    }
                    
                    initialized = true
                    _status.value = MLManagerStatus.READY
                    Timber.i("All interpreters initialized, status = READY")
                    
                } catch (e: Exception) {
                    Timber.e(e, "Fatal error during interpreter initialization")
                    _status.value = MLManagerStatus.ERROR
                    metricsState.recordError(e.message ?: "Initialization error")
                }
            }
        }
    }
    
    /**
     * Execute inference and return confidence adjustment
     * Safe to call from any thread
     */
    override suspend fun inferConfidenceAdjustment(
        context: DeviceContext,
        modelType: MLModelType
    ): Float {
        return withContext(Dispatchers.Default) {
            try {
                if (!isAvailable()) {
                    Timber.w("ML not available for inference, returning neutral")
                    return@withContext 0.0f
                }
                
                interpreterLock.withLock {
                    val interpreter = activeInterpreters[modelType]
                    if (interpreter == null || !interpreter.isInitialized()) {
                        Timber.w("No initialized interpreter for $modelType")
                        return@withContext 0.0f
                    }
                    
                    val formatter = inputFormatters[modelType] ?: return@withContext 0.0f
                    val throttler = throttlers[modelType] ?: return@withContext 0.0f
                    
                    // Format input
                    val input = formatter.formatInput(context)
                    
                    // Execute with throttling
                    val result = throttler.throttledInfer(context) {
                        executeInference(interpreter, formatter, input, modelType)
                    }
                    
                    // Track metrics
                    metricsState.recordInference(result)
                    
                    // Return bounded confidence adjustment
                    return@withContext result.confidence.coerceIn(-0.3f, 0.3f)
                }
            } catch (e: TimeoutCancellationException) {
                Timber.w("Inference timeout for $modelType")
                metricsState.recordFailure("Timeout")
                return@withContext 0.0f
            } catch (e: Exception) {
                Timber.e(e, "Error during inference for $modelType")
                metricsState.recordFailure(e.message ?: "Unknown error")
                return@withContext 0.0f
            }
        }
    }
    
    /**
     * Execute single inference operation
     */
    private suspend fun executeInference(
        interpreter: TFLiteInterpreterProvider,
        formatter: SignalInputFormatter,
        input: FloatArray,
        modelType: MLModelType
    ): InferenceResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val output = withTimeoutOrNull(100L) {
                interpreter.infer(input, timeoutMs = 100L)
            }
            
            if (output == null) {
                Timber.w("Inference returned null for $modelType")
                InferenceResult(
                    confidence = 0.0f,
                    modelType = modelType.name,
                    latencyMs = System.currentTimeMillis() - startTime,
                    fallback = true,
                    error = "Inference failed"
                )
            } else {
                val confidence = formatter.parseOutput(output)
                InferenceResult(
                    confidence = confidence.coerceIn(-0.3f, 0.3f),
                    modelType = modelType.name,
                    latencyMs = System.currentTimeMillis() - startTime,
                    fallback = false
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Inference exception for $modelType")
            InferenceResult(
                confidence = 0.0f,
                modelType = modelType.name,
                latencyMs = System.currentTimeMillis() - startTime,
                fallback = true,
                error = e.message
            )
        }
    }
    
    /**
     * Prepare for shutdown (ON_STOP)
     */
    private suspend fun prepareShutdown() {
        withContext(Dispatchers.Default) {
            interpreterLock.withLock {
                _status.value = MLManagerStatus.IDLE
                Timber.d("Prepared for shutdown, no longer accepting inference requests")
            }
        }
    }
    
    /**
     * Cleanup all resources (ON_DESTROY)
     */
    private fun cleanupResources() {
        scope.launch {
            try {
                interpreterLock.withLock {
                    // Close all interpreters
                    for ((modelType, interpreter) in activeInterpreters) {
                        try {
                            interpreter.cleanup()
                            Timber.d("Cleaned up interpreter for $modelType")
                        } catch (e: Exception) {
                            Timber.w(e, "Error cleaning up $modelType interpreter")
                        }
                    }
                    
                    // Clear state
                    activeInterpreters.clear()
                    throttlers.clear()
                    inputFormatters.clear()
                    initialized = false
                    
                    _status.value = MLManagerStatus.SHUTDOWN
                    Timber.i("All resources cleaned up, status = SHUTDOWN")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during resource cleanup")
            } finally {
                scope.cancel()
            }
        }
    }
    
    override fun getStatus(): MLManagerStatus {
        return _status.value
    }
    
    override suspend fun isAvailable(): Boolean {
        return withContext(Dispatchers.Default) {
            val status = getStatus()
            status == MLManagerStatus.READY || status == MLManagerStatus.INFERRING
        }
    }
    
    override fun getMetrics(): MLMetrics {
        return metricsState.toMetrics()
    }
}

/**
 * Mutable metrics state for tracking ML system health
 */
private class MutableMetricsState {
    private val executedCount = AtomicLong(0L)
    private val cachedCount = AtomicLong(0L)
    private val failedCount = AtomicLong(0L)
    private val latencySum = AtomicLong(0L)
    private val peakMemory = AtomicLong(0L)
    private val modelsCount = AtomicLong(0L)
    private var lastError: String? = null
    private val startTime = System.currentTimeMillis()
    
    fun recordInference(result: InferenceResult) {
        if (result.cached) {
            cachedCount.incrementAndGet()
        } else {
            executedCount.incrementAndGet()
            latencySum.addAndGet(result.latencyMs)
        }
    }
    
    fun recordFailure(message: String) {
        failedCount.incrementAndGet()
        lastError = message
    }
    
    fun recordModelLoaded() {
        modelsCount.incrementAndGet()
    }
    
    fun recordError(message: String) {
        lastError = message
    }
    
    fun toMetrics(): MLMetrics {
        val executed = executedCount.get()
        val avgLatency = if (executed > 0) {
            (latencySum.get() / executed).toFloat()
        } else {
            0f
        }
        
        return MLMetrics(
            inferencesExecuted = executed,
            inferencesFromCache = cachedCount.get(),
            inferencesFailedTotal = failedCount.get(),
            averageLatencyMs = avgLatency,
            peakMemoryUsageMb = peakMemory.get() / 1_000_000f,
            modelsLoaded = modelsCount.toInt(),
            lastErrorMessage = lastError,
            uptimeMs = System.currentTimeMillis() - startTime
        )
    }
}
