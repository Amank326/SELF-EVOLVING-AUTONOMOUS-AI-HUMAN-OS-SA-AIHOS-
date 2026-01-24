package com.aihos.ai.energy

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aihos.ai.autonomy.AutonomyController
import com.aihos.ai.cognition.CognitionLoopManager
import com.aihos.ai.perception.EnvironmentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * EnergyAwareCognitionIntegration: Bridges energy awareness with cognition loop
 *
 * This is the critical integration point that makes the AI energy-aware:
 * - Adapts cognition frequency based on energy state
 * - Reduces reasoning intensity under power constraints
 * - Pauses reflection/evolution when energy-critical
 * - Scales down ML inference frequency during low power
 * - Defers non-critical thinking to save energy
 * - Recovers full cognition when power stabilizes
 *
 * The integration works by:
 * 1. Monitoring energy and thermal states continuously
 * 2. Computing adjusted cognition parameters for current state
 * 3. Passing these to CognitionLoopManager for interval adjustment
 * 4. Guiding AutonomyController on reflection/evolution intensity
 * 5. Providing ML inference hints for batch size and frequency
 *
 * Result: AI that "knows" when it has energy to think deeply, and when
 * it needs to make quick decisions to conserve power.
 */
interface EnergyAwareCognitionBridge {
    /**
     * Get energy-adjusted cognition parameters for the current moment
     */
    suspend fun getEnergyAdjustedCognitionParams(): EnergyAdjustedParams

    /**
     * Get energy-adjusted reflection intensity (0.0 to 1.0)
     */
    suspend fun getReflectionIntensity(): Float

    /**
     * Get energy-adjusted evolution intensity (0.0 to 1.0)
     */
    suspend fun getEvolutionIntensity(): Float

    /**
     * Get energy-adjusted ML inference batch size multiplier
     */
    suspend fun getMlBatchSizeMultiplier(): Float

    /**
     * Get energy-adjusted graphics quality level
     */
    suspend fun getGraphicsQuality(): GraphicsQuality

    /**
     * Check if AI should defer expensive cognition
     */
    suspend fun shouldDeferExpensiveCognition(): Boolean

    /**
     * Get integration status and metrics
     */
    fun getIntegrationStatus(): EnergyAwarenessStatus
}

/**
 * Energy-adjusted parameters for cognition
 */
@Serializable
data class EnergyAdjustedParams(
    val cognitiveIntervalMultiplier: Float,     // Apply to base cognition interval
    val reflectionIntensity: Float,             // 0.0 (skip) to 1.0 (full)
    val evolutionIntensity: Float,              // 0.0 (skip) to 1.0 (full)
    val mlInferenceFrequencyMultiplier: Float,  // Apply to inference frequency
    val mlBatchSizeMultiplier: Float,           // Adjust inference batch size
    val graphicsQuality: GraphicsQuality,       // Rendering quality
    val shouldPauseBackgroundWork: Boolean,    // Pause ML, reflection, evolution
    val recommendation: String = ""             // Human-readable explanation
)

/**
 * Status information about energy-aware integration
 */
@Serializable
data class EnergyAwarenessStatus(
    val isOperational: Boolean = false,
    val energyState: EnergyState = EnergyState.UNKNOWN,
    val thermalState: ThermalState = ThermalState.UNKNOWN,
    val lastUpdateMs: Long = System.currentTimeMillis(),
    val activeCognitionReductions: List<String> = emptyList(),
    val currentEnergyProfile: String = "unknown",
    val estimatedCognitivePerformancePercent: Int = 100,
    val estimatedBatteryImpactPercentPerHour: Float = 0.1f
)

/**
 * Default implementation
 */
class DefaultEnergyAwareCognitionBridge(
    private val context: Context,
    private val cognitionLoopManager: CognitionLoopManager,
    private val autonomyController: AutonomyController?,
    private val energyManager: EnergyAwarenessManager,
    private val thermalManager: ThermalManager,
    private val metaCognition: MetaCognitionController,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : EnergyAwareCognitionBridge, DefaultLifecycleObserver {

    private var lastReflectionIntensity = 1.0f
    private var lastEvolutionIntensity = 0.5f
    private var lastMlBatchMultiplier = 1.0f
    private var updateCounter = 0
    private val statusHistory = mutableListOf<EnergyAwarenessStatus>()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        Timber.i("⚡ EnergyAwareCognitionBridge created")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Timber.i("⚡ EnergyAwareCognitionBridge destroyed")
    }

    override suspend fun getEnergyAdjustedCognitionParams(): EnergyAdjustedParams {
        val energyState = energyManager.getEnergyState()
        val energyProfile = energyManager.getCurrentEnergyProfile()
        val thermalConstraint = thermalManager.getThermalState()
        val metaCognitionAllows = !metaCognition.shouldAttemptExpensiveCognition()

        // Base parameters from energy profile
        var intervalMultiplier = 1.0f / energyProfile.cognitiveFrequency
        var reflectionIntensity = energyProfile.reflectionIntensity
        var evolutionIntensity = energyProfile.evolutionIntensity
        var mlMultiplier = energyProfile.mlInferenceFrequency
        var mlBatchMultiplier = 1.0f
        var graphicsQuality = energyProfile.graphicsQuality
        var shouldPauseBackground = !energyProfile.allowBackgroundOperations

        val reductions = mutableListOf<String>()

        // Apply thermal constraints
        if (thermalConstraint.shouldPauseCognition) {
            intervalMultiplier *= 3.0f
            reflectionIntensity = 0.0f
            evolutionIntensity = 0.0f
            mlMultiplier = 0.0f
            shouldPauseBackground = true
            reductions.add("Thermal: Pausing cognition due to high temperature (${thermalConstraint.temperatureCelsius}°C)")
        } else if (thermalConstraint.isThrottling) {
            intervalMultiplier *= 2.0f
            reflectionIntensity *= 0.3f
            evolutionIntensity *= 0.1f
            mlMultiplier *= 0.5f
            mlBatchMultiplier *= 0.5f
            reductions.add("Thermal: Reducing cognition due to throttling")
        } else if (thermalConstraint.recommendedCognitionIntensity < 0.7f) {
            val factor = thermalConstraint.recommendedCognitionIntensity
            reflectionIntensity *= factor
            evolutionIntensity *= factor
            mlMultiplier *= factor
            reductions.add("Thermal: Adjusting cognition by ${(factor * 100).toInt()}%")
        }

        // Apply meta-cognition constraints
        if (metaCognitionAllows) {
            reflectionIntensity *= 0.5f
            evolutionIntensity *= 0.3f
            reductions.add("MetaCognition: Deferring expensive operations")
        }

        // Update learning about energy costs
        scope.launch {
            metaCognition.recordCognitionEnergyCost(
                CognitionOperation.DECISION_CYCLE,
                energyProfile.estimatePowerCostMilliwatts()
            )
        }

        lastReflectionIntensity = reflectionIntensity
        lastEvolutionIntensity = evolutionIntensity
        lastMlBatchMultiplier = mlBatchMultiplier

        val recommendation = buildString {
            append("Energy State: ${energyState.energyState}")
            append(" | Thermal: ${thermalConstraint.thermalState}")
            append(" | Cognitive Frequency: ${(1f / intervalMultiplier * 100).toInt()}%")
            if (reductions.isNotEmpty()) {
                append(" | Reductions: ${reductions.joinToString(", ")}")
            }
        }

        return EnergyAdjustedParams(
            cognitiveIntervalMultiplier = min(10.0f, intervalMultiplier),  // Cap at 10x slowdown
            reflectionIntensity = max(0f, reflectionIntensity),
            evolutionIntensity = max(0f, evolutionIntensity),
            mlInferenceFrequencyMultiplier = max(0f, mlMultiplier),
            mlBatchSizeMultiplier = max(0f, mlBatchMultiplier),
            graphicsQuality = graphicsQuality,
            shouldPauseBackgroundWork = shouldPauseBackground,
            recommendation = recommendation
        )
    }

    override suspend fun getReflectionIntensity(): Float {
        return getEnergyAdjustedCognitionParams().reflectionIntensity
    }

    override suspend fun getEvolutionIntensity(): Float {
        return getEnergyAdjustedCognitionParams().evolutionIntensity
    }

    override suspend fun getMlBatchSizeMultiplier(): Float {
        return getEnergyAdjustedCognitionParams().mlBatchSizeMultiplier
    }

    override suspend fun getGraphicsQuality(): GraphicsQuality {
        return getEnergyAdjustedCognitionParams().graphicsQuality
    }

    override suspend fun shouldDeferExpensiveCognition(): Boolean {
        return metaCognition.shouldAttemptExpensiveCognition().not()
    }

    override fun getIntegrationStatus(): EnergyAwarenessStatus {
        val energyState = energyManager.getEnergyState()
        val energyProfile = energyManager.getCurrentEnergyProfile()
        val thermalState = thermalManager.getThermalMetrics()

        val status = EnergyAwarenessStatus(
            isOperational = true,
            energyState = energyState.energyState,
            thermalState = energyState.thermalInfo.thermalState,
            lastUpdateMs = System.currentTimeMillis(),
            currentEnergyProfile = when (energyState.energyState) {
                EnergyState.ABUNDANT -> "Abundant"
                EnergyState.NORMAL -> "Normal"
                EnergyState.LOW -> "Low Power"
                EnergyState.CRITICAL -> "Critical"
                EnergyState.UNKNOWN -> "Unknown"
            },
            estimatedCognitivePerformancePercent = (lastReflectionIntensity * 100).toInt(),
            estimatedBatteryImpactPercentPerHour = energyProfile.estimateBatteryDrainPerHour()
        )

        statusHistory.add(status)
        if (statusHistory.size > 100) statusHistory.removeAt(0)

        return status
    }
}

/**
 * EnergyAwareCognitionCoordinator: Orchestrates all energy-aware components
 *
 * Acts as the central coordinator that:
 * - Initializes all energy managers
 * - Integrates with cognition loop
 * - Provides convenient API for entire system
 * - Handles lifecycle of all energy systems
 */
interface EnergyAwareCognitionCoordinator {
    /**
     * Initialize all energy-aware systems
     */
    suspend fun initialize()

    /**
     * Shutdown all energy-aware systems
     */
    suspend fun shutdown()

    /**
     * Get access to energy manager
     */
    fun getEnergyManager(): EnergyAwarenessManager

    /**
     * Get access to thermal manager
     */
    fun getThermalManager(): ThermalManager

    /**
     * Get access to meta-cognition controller
     */
    fun getMetaCognition(): MetaCognitionController

    /**
     * Get access to cognition bridge
     */
    fun getCognitionBridge(): EnergyAwareCognitionBridge

    /**
     * Get complete system status
     */
    suspend fun getSystemStatus(): EnergySystemStatus
}

/**
 * Complete system status
 */
@Serializable
data class EnergySystemStatus(
    val energyState: EnergyState = EnergyState.UNKNOWN,
    val thermalState: ThermalState = ThermalState.UNKNOWN,
    val batteryHealthScore: Int = 75,
    val thermalHealthScore: Int = 75,
    val cognitivePerformancePercent: Int = 100,
    val estimatedBatteryDrainPercentPerHour: Float = 0.1f,
    val metaCognitionWisdomScore: Float = 50f,
    val systemReadyForFullCognition: Boolean = true,
    val recommendedActions: List<String> = emptyList()
)

/**
 * Default implementation of coordinator
 */
class DefaultEnergyAwareCognitionCoordinator(
    private val context: Context,
    private val cognitionLoopManager: CognitionLoopManager,
    private val autonomyController: AutonomyController?,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : EnergyAwareCognitionCoordinator, DefaultLifecycleObserver {

    private lateinit var energyManager: EnergyAwarenessManager
    private lateinit var thermalManager: ThermalManager
    private lateinit var metaCognition: MetaCognitionController
    private lateinit var cognitionBridge: EnergyAwareCognitionBridge

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        Timber.i("⚡ EnergyAwareCognitionCoordinator created")
    }

    override fun onStart(owner: LifecycleOwner) {
        scope.launch {
            initialize()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        scope.launch {
            shutdown()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Timber.i("⚡ EnergyAwareCognitionCoordinator destroyed")
    }

    override suspend fun initialize() {
        Timber.i("⚡ Initializing energy-aware cognition system")

        energyManager = DefaultEnergyAwarenessManager(context, scope)
        thermalManager = DefaultThermalManager(context, energyManager, scope)
        metaCognition = DefaultMetaCognitionController(energyManager, thermalManager, scope)
        cognitionBridge = DefaultEnergyAwareCognitionBridge(
            context,
            cognitionLoopManager,
            autonomyController,
            energyManager,
            thermalManager,
            metaCognition,
            scope
        )

        energyManager.startMonitoring()
        thermalManager.startMonitoring()

        Timber.i("⚡ Energy-aware cognition system initialized")
    }

    override suspend fun shutdown() {
        Timber.i("⚡ Shutting down energy-aware cognition system")

        if (::energyManager.isInitialized) energyManager.stopMonitoring()
        if (::thermalManager.isInitialized) thermalManager.stopMonitoring()

        Timber.i("⚡ Energy-aware cognition system shutdown complete")
    }

    override fun getEnergyManager(): EnergyAwarenessManager {
        require(::energyManager.isInitialized) { "Energy manager not initialized" }
        return energyManager
    }

    override fun getThermalManager(): ThermalManager {
        require(::thermalManager.isInitialized) { "Thermal manager not initialized" }
        return thermalManager
    }

    override fun getMetaCognition(): MetaCognitionController {
        require(::metaCognition.isInitialized) { "Meta-cognition not initialized" }
        return metaCognition
    }

    override fun getCognitionBridge(): EnergyAwareCognitionBridge {
        require(::cognitionBridge.isInitialized) { "Cognition bridge not initialized" }
        return cognitionBridge
    }

    override suspend fun getSystemStatus(): EnergySystemStatus {
        val energyState = energyManager.getEnergyState()
        val thermalState = thermalManager.getThermalState()
        val params = cognitionBridge.getEnergyAdjustedCognitionParams()
        val metaMetrics = metaCognition.getMetaCognitionMetrics()

        val recommendations = mutableListOf<String>()
        when (energyState.energyState) {
            EnergyState.CRITICAL -> recommendations.add("⚠️ Critical battery - minimal cognition enabled")
            EnergyState.LOW -> recommendations.add("⚠️ Low battery - reduced cognition active")
            EnergyState.ABUNDANT -> recommendations.add("✅ Abundant power - full cognition enabled")
            else -> {}
        }

        if (thermalState.shouldPauseCognition) {
            recommendations.add("⚠️ Thermal constraint - cognition paused")
        }

        if (metaMetrics.estimatedAiWisdomScore > 70) {
            recommendations.add("✅ AI has learned to optimize energy usage")
        }

        return EnergySystemStatus(
            energyState = energyState.energyState,
            thermalState = energyState.thermalInfo.thermalState,
            batteryHealthScore = energyState.energyHealthScore,
            thermalHealthScore = (100f * (1f - (thermalState.temperatureCelsius - 30f) / 20f)).toInt().coerceIn(0, 100),
            cognitivePerformancePercent = (params.cognitiveIntervalMultiplier.let { 100f / it }).toInt().coerceIn(0, 100),
            estimatedBatteryDrainPercentPerHour = energyManager.getCurrentEnergyProfile().estimateBatteryDrainPerHour(),
            metaCognitionWisdomScore = metaMetrics.estimatedAiWisdomScore,
            systemReadyForFullCognition = energyState.energyState in listOf(EnergyState.ABUNDANT, EnergyState.NORMAL) &&
                    !thermalState.shouldPauseCognition,
            recommendedActions = recommendations
        )
    }
}

/**
 * Convenience object for accessing coordinator (singleton pattern)
 */
object EnergyAwareCognitionSystem {
    private var coordinator: EnergyAwareCognitionCoordinator? = null

    suspend fun initialize(
        context: Context,
        cognitionLoopManager: CognitionLoopManager,
        autonomyController: AutonomyController?
    ) {
        coordinator = DefaultEnergyAwareCognitionCoordinator(
            context,
            cognitionLoopManager,
            autonomyController
        )
        coordinator?.initialize()
    }

    fun get(): EnergyAwareCognitionCoordinator? = coordinator

    suspend fun shutdown() {
        coordinator?.shutdown()
        coordinator = null
    }
}
