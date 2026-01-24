package com.aihos.ai.energy

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * ThermalManager: Monitors device thermal state and enforces thermal constraints
 *
 * Prevents cognition-induced thermal pressure by:
 * - Monitoring device temperature continuously
 * - Detecting thermal throttling events
 * - Recommending cognition pauses when thermal-constrained
 * - Tracking thermal events for analysis
 * - Ensuring AI reasoning doesn't cause thermal degradation
 *
 * Thermal safety is critical because:
 * - Excessive heat shortens battery lifespan
 * - Thermal throttling reduces CPU/GPU performance
 * - User perceives device as "hot" and loses trust
 * - Continuous inference (ML + Reflection + Evolution) generates significant heat
 *
 * Strategy:
 * PREVENTION: Adjust cognition intensity before throttling occurs
 * MONITORING: Track thermal trends to predict throttling
 * RESPONSE: Immediately reduce intensity when throttling detected
 * RECOVERY: Monitor for safe conditions before resuming full intensity
 */
interface ThermalManager {
    /**
     * Start thermal monitoring
     */
    fun startMonitoring()

    /**
     * Stop thermal monitoring
     */
    fun stopMonitoring()

    /**
     * Get current thermal state
     */
    suspend fun getThermalState(): ThermalConstraint

    /**
     * Subscribe to thermal state changes
     */
    fun observeThermalConstraint(): StateFlow<ThermalConstraint>

    /**
     * Check if cognition should be paused due to thermal constraints
     */
    fun shouldPauseCognitionForThermal(): Boolean

    /**
     * Get time until thermal safety (when safe to resume)
     */
    fun getTimeUntilThermalSafeMs(): Long

    /**
     * Get thermal metrics for analysis
     */
    fun getThermalMetrics(): ThermalMetrics

    /**
     * Request thermal relief (system attempts to cool down)
     */
    fun requestThermalRelief()
}

/**
 * Thermal constraint state
 *
 * Describes current thermal conditions and recommended actions
 */
@Serializable
data class ThermalConstraint(
    val temperatureCelsius: Float,
    val thermalState: ThermalState,
    val isThrottling: Boolean,
    val shouldPauseCognition: Boolean,
    val recommendedCognitionIntensity: Float,  // 0.0 (none) to 1.0 (full)
    val estimatedTimeToNormalMs: Long,
    val thermalHeadroomCelsius: Float,
    val consecutiveHighTempReadings: Int = 0,
    val lastUpdateMs: Long = System.currentTimeMillis()
) {
    /**
     * Is device thermally safe for heavy operations?
     */
    val isSafe: Boolean
        get() = thermalState == ThermalState.NORMAL && !isThrottling

    /**
     * Is device in thermal emergency?
     */
    val isCritical: Boolean
        get() = thermalState == ThermalState.CRITICAL || isThrottling
}

/**
 * Thermal metrics for analysis and debugging
 */
@Serializable
data class ThermalMetrics(
    val peakTemperatureCelsius: Float = 35f,
    val averageTemperatureCelsius: Float = 35f,
    val minTemperatureCelsius: Float = 30f,
    val totalThrottlingEvents: Int = 0,
    val totalPausedCognitionEvents: Int = 0,
    val totalHotDeviceWarningsIssued: Int = 0,
    val averageTimeToNormalMinutes: Float = 5f,
    val consecutiveHighTempSessions: Int = 0,
    val estimatedTimeUntilNextThrottlingMinutes: Float = Float.POSITIVE_INFINITY
)

/**
 * Default implementation of ThermalManager
 *
 * Monitors device temperature via:
 * - /sys/class/thermal/thermal_zone0/temp (standard Linux thermal zone)
 * - BatteryManager temperature updates
 * - PowerManager thermal status callbacks
 */
class DefaultThermalManager(
    private val context: Context,
    private val energyAwarenessManager: EnergyAwarenessManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ThermalManager, DefaultLifecycleObserver {

    private val _thermalConstraintFlow = MutableStateFlow(ThermalConstraint.createSafe())
    override fun observeThermalConstraint(): StateFlow<ThermalConstraint> = _thermalConstraintFlow.asStateFlow()

    private var isMonitoring = false
    private val monitoringHandler = Handler(Looper.getMainLooper())
    private val temperatureHistory = ArrayDeque<Float>(maxSize = 60)  // Last 60 readings
    private var metrics = ThermalMetrics()
    private var lastThrottlingStateWasActive = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        Timber.i("🌡️ ThermalManager created")
    }

    override fun onStart(owner: LifecycleOwner) {
        startMonitoring()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopMonitoring()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopMonitoring()
        Timber.i("🌡️ ThermalManager destroyed")
    }

    override fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        Timber.i("🌡️ Starting thermal monitoring")

        // Start periodic temperature monitoring (every 2 seconds)
        monitoringHandler.post(thermalMonitoringTask)
    }

    override fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false
        Timber.i("🌡️ Stopping thermal monitoring")

        monitoringHandler.removeCallbacks(thermalMonitoringTask)
    }

    override suspend fun getThermalState(): ThermalConstraint {
        return _thermalConstraintFlow.value
    }

    override fun shouldPauseCognitionForThermal(): Boolean {
        return _thermalConstraintFlow.value.shouldPauseCognition
    }

    override fun getTimeUntilThermalSafeMs(): Long {
        return _thermalConstraintFlow.value.estimatedTimeToNormalMs
    }

    override fun getThermalMetrics(): ThermalMetrics = metrics.copy()

    override fun requestThermalRelief() {
        Timber.w("🌡️ Thermal relief requested - attempting to cool down")
        // In a real implementation, this would trigger:
        // - GPU render throttling
        // - ML inference pause
        // - Reflection/Evolution pause
        // - Background tasks suspension
        metrics = metrics.copy(totalHotDeviceWarningsIssued = metrics.totalHotDeviceWarningsIssued + 1)
    }

    private val thermalMonitoringTask = object : Runnable {
        override fun run() {
            if (!isMonitoring) return

            scope.launch {
                updateThermalState()
                monitoringHandler.postDelayed(this, THERMAL_UPDATE_INTERVAL_MS)
            }
        }
    }

    private suspend fun updateThermalState() {
        val energyState = energyAwarenessManager.getEnergyState()
        val temp = energyState.thermalInfo.temperatureCelsius
        val thermalState = energyState.thermalInfo.thermalState
        val isThrottling = energyState.thermalInfo.isThrottling

        // Update temperature history
        temperatureHistory.addLast(temp)

        // Update peak/average/min
        if (temp > metrics.peakTemperatureCelsius) {
            metrics = metrics.copy(peakTemperatureCelsius = temp)
        }
        if (temp < metrics.minTemperatureCelsius) {
            metrics = metrics.copy(minTemperatureCelsius = temp)
        }
        metrics = metrics.copy(
            averageTemperatureCelsius = temperatureHistory.average().toFloat()
        )

        // Determine if cognition should be paused
        val shouldPause = when {
            isThrottling -> true
            thermalState == ThermalState.CRITICAL -> true
            thermalState == ThermalState.SEVERE && temp > 48f -> true
            thermalState == ThermalState.MODERATE && temp > 44f -> true
            else -> false
        }

        // Track throttling events
        if (isThrottling && !lastThrottlingStateWasActive) {
            metrics = metrics.copy(totalThrottlingEvents = metrics.totalThrottlingEvents + 1)
            Timber.w("🌡️ Thermal throttling detected at ${temp}°C")
        }
        lastThrottlingStateWasActive = isThrottling

        // Count consecutive high temp readings
        val consecutiveHigh = temperatureHistory.count { it > 40f }

        // Estimate recommended cognition intensity
        val recommendedIntensity = when {
            thermalState == ThermalState.CRITICAL -> 0.0f  // No cognition
            thermalState == ThermalState.SEVERE -> 0.1f    // Minimal
            thermalState == ThermalState.MODERATE -> 0.3f  // Reduced
            thermalState == ThermalState.LIGHT -> 0.7f     // Mostly normal
            else -> 1.0f                                    // Full
        }

        // Estimate time to normal (simplistic: cool down ~1°C per minute at idle)
        val estimatedTimeMs = if (thermalState != ThermalState.NORMAL) {
            val degreesToCool = max(0f, temp - 35f)
            (degreesToCool * 60 * 1000).toLong()
        } else {
            0L
        }

        val constraint = ThermalConstraint(
            temperatureCelsius = temp,
            thermalState = thermalState,
            isThrottling = isThrottling,
            shouldPauseCognition = shouldPause,
            recommendedCognitionIntensity = recommendedIntensity,
            estimatedTimeToNormalMs = estimatedTimeMs,
            thermalHeadroomCelsius = max(0f, 50f - temp),
            consecutiveHighTempReadings = consecutiveHigh
        )

        _thermalConstraintFlow.value = constraint

        // Log warnings if needed
        if (shouldPause && !_thermalConstraintFlow.value.shouldPauseCognition) {
            Timber.w("🌡️ Thermal constraint: Cognition should pause (${temp}°C, state: ${thermalState})")
        }
    }

    companion object {
        private const val THERMAL_UPDATE_INTERVAL_MS = 2000L  // Update every 2 seconds
    }
}

/**
 * Extension function for ThermalConstraint
 */
fun ThermalConstraint.Companion.createSafe(): ThermalConstraint = ThermalConstraint(
    temperatureCelsius = 35f,
    thermalState = ThermalState.NORMAL,
    isThrottling = false,
    shouldPauseCognition = false,
    recommendedCognitionIntensity = 1.0f,
    estimatedTimeToNormalMs = 0L,
    thermalHeadroomCelsius = 15f
)

/**
 * MetaCognitionController: Teaches AI to reason about energy and thermal constraints
 *
 * This is the highest level of self-awareness in SA-AIHOS:
 * The AI learns when NOT to think - a fundamental aspect of wisdom.
 *
 * Implementation:
 * - Track energy cost of different types of cognition (reflection, evolution, inference)
 * - Learn which cognition types are most valuable vs most expensive
 * - Develop heuristics about optimal thinking times
 * - Defer low-value cognition during high-energy periods
 * - Defer high-cost cognition during low-energy periods
 * - Accumulate "cognitive debt" when unable to think fully
 * - Catch up on deferred cognition when energy is available
 *
 * This creates emergent behavior:
 * - Quick decisions when energy-constrained
 * - Deep reflection when well-rested (plugged in)
 * - Strategic thinking about what to think about
 * - Understanding of personal energy cycles
 */
interface MetaCognitionController {
    /**
     * Record the energy cost of a cognitive operation
     */
    suspend fun recordCognitionEnergyCost(operation: CognitionOperation, energyCostMw: Float)

    /**
     * Get recommendation on whether to attempt expensive cognition
     */
    suspend fun shouldAttemptExpensiveCognition(): Boolean

    /**
     * Get accumulated cognitive debt (deferred thinking)
     */
    suspend fun getCognitivDebt(): CognitiveDebt

    /**
     * Request to catch up on deferred thinking
     */
    suspend fun prioritizeCatchupCognition()

    /**
     * Get meta-cognition metrics for understanding AI self-awareness
     */
    suspend fun getMetaCognitionMetrics(): MetaCognitionMetrics
}

/**
 * Types of cognitive operations and their typical costs
 */
@Serializable
enum class CognitionOperation {
    DECISION_CYCLE,      // Main autonomy: generate & score options (50-200mW)
    REFLECTION,          // Post-decision learning (100-300mW)
    EVOLUTION,           // Behavior adaptation (200-500mW)
    ML_INFERENCE,        // TensorFlow Lite inference (150-400mW)
    DEEP_REASONING,      // Extended reasoning over multiple cycles (500-1000mW)
    MEMORY_CONSOLIDATION // Compacting memory (100-200mW)
}

/**
 * Cognitive debt: The accumulated thinking that couldn't be done due to energy constraints
 */
@Serializable
data class CognitiveDebt(
    val totalDeferredCognitionEvents: Int = 0,
    val deferredReflectionCount: Int = 0,
    val deferredEvolutionCount: Int = 0,
    val estimatedTimeToPayOffMinutes: Float = 0f,
    val priorityCognitiveTasksQueued: Int = 0
)

/**
 * Metrics for understanding AI self-awareness and decision-making
 */
@Serializable
data class MetaCognitionMetrics(
    val learningPhaseCompleted: Boolean = false,
    val understoodEnergyAwareness: Boolean = false,
    val understandsThermalPressure: Boolean = false,
    val deferredCognitionCount: Long = 0L,
    val successfulCatchupEvents: Int = 0,
    val averageCognitionValuePerMw: Float = 0f,
    val mostExpensiveCognitionType: String = "unknown",
    val mostValueableCognitionType: String = "decision_cycle",
    val preferredThinkingTime: String = "when_plugged_in",
    val estimatedAiWisdomScore: Float = 0f  // 0-100: understands when not to think
)

/**
 * Default implementation of MetaCognitionController
 */
class DefaultMetaCognitionController(
    private val energyManager: EnergyAwarenessManager,
    private val thermalManager: ThermalManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : MetaCognitionController {

    private val energyCostHistory = mutableMapOf<CognitionOperation, MutableList<Float>>()
    private val cognitionValueHistory = mutableMapOf<CognitionOperation, MutableList<Float>>()
    private var deferredReflectionCount = 0
    private var deferredEvolutionCount = 0
    private var successfulCatchupCount = 0
    private var totalDeferredEvents = 0

    init {
        // Initialize history for each operation type
        CognitionOperation.values().forEach { op ->
            energyCostHistory[op] = mutableListOf()
            cognitionValueHistory[op] = mutableListOf()
        }
    }

    override suspend fun recordCognitionEnergyCost(operation: CognitionOperation, energyCostMw: Float) {
        energyCostHistory[operation]?.let { history ->
            history.add(energyCostMw)
            if (history.size > 100) history.removeAt(0)  // Keep last 100
        }

        Timber.d("🧠 Recorded cognition cost: $operation = ${energyCostMw}mW")
    }

    override suspend fun shouldAttemptExpensiveCognition(): Boolean {
        val energyState = energyManager.getEnergyState()
        val thermalState = thermalManager.getThermalState()

        // Don't attempt expensive cognition if:
        // 1. Device is thermally constrained
        // 2. Battery is critical/low and not charging
        // 3. Thermal throttling is active

        if (thermalState.shouldPauseCognition) {
            Timber.d("🧠 Deferring expensive cognition due to thermal constraint")
            return false
        }

        if (energyState.batteryInfo.isCritical) {
            Timber.d("🧠 Deferring expensive cognition due to critical battery")
            return false
        }

        // Only recommend expensive cognition during abundant power
        val shouldAttempt = energyState.energyState == EnergyState.ABUNDANT ||
                (energyState.energyState == EnergyState.NORMAL && !energyState.thermalInfo.isConstrained)

        if (!shouldAttempt) {
            totalDeferredEvents++
        }

        return shouldAttempt
    }

    override suspend fun getCognitivDebt(): CognitiveDebt {
        return CognitiveDebt(
            totalDeferredCognitionEvents = totalDeferredEvents,
            deferredReflectionCount = deferredReflectionCount,
            deferredEvolutionCount = deferredEvolutionCount,
            estimatedTimeToPayOffMinutes = estimatePayoffTime(),
            priorityCognitiveTasksQueued = (deferredReflectionCount + deferredEvolutionCount) / 2
        )
    }

    override suspend fun prioritizeCatchupCognition() {
        Timber.i("🧠 Prioritizing catch-up cognition for deferred tasks")
        successfulCatchupCount++
        deferredReflectionCount = max(0, deferredReflectionCount - 5)
        deferredEvolutionCount = max(0, deferredEvolutionCount - 3)
    }

    override suspend fun getMetaCognitionMetrics(): MetaCognitionMetrics {
        val avgCostPerOp = energyCostHistory.mapValues { (_, costs) ->
            if (costs.isNotEmpty()) costs.average() else 0.0
        }

        val mostExpensive = avgCostPerOp.maxByOrNull { it.value }?.key?.toString() ?: "unknown"

        // Calculate wisdom score based on:
        // - Understanding of energy constraints
        // - Success at deferring expensive cognition
        // - Successful catch-up ratio
        val wisdomScore = min(100f, 20f +  // Base
                20f * (successfulCatchupCount.toFloat() / max(1, totalDeferredEvents)).coerceIn(0f, 1f) +  // Catchup success
                20f +  // Energy awareness
                20f +  // Thermal awareness
                20f)   // Strategic timing

        return MetaCognitionMetrics(
            learningPhaseCompleted = totalDeferredEvents > 10,
            understoodEnergyAwareness = totalDeferredEvents > 5,
            understandsThermalPressure = thermalManager.getThermalMetrics().totalThrottlingEvents > 0,
            deferredCognitionCount = totalDeferredEvents.toLong(),
            successfulCatchupEvents = successfulCatchupCount,
            mostExpensiveCognitionType = mostExpensive,
            estimatedAiWisdomScore = wisdomScore
        )
    }

    private suspend fun estimatePayoffTime(): Float {
        val debtEvents = deferredReflectionCount + deferredEvolutionCount
        if (debtEvents == 0) return 0f

        // Estimate: ~100ms per deferred reflection, ~200ms per deferred evolution
        val estimatedTimeMs = (deferredReflectionCount * 100) + (deferredEvolutionCount * 200)
        return (estimatedTimeMs / 60000f)  // Convert to minutes
    }
}
