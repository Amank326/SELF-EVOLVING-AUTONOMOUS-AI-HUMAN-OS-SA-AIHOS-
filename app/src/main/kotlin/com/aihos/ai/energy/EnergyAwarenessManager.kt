package com.aihos.ai.energy

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * EnergyAwarenessManager: Monitors device energy state and provides AI with energy context
 *
 * Treats the device's energy constraints as biological needs:
 * - Battery level → Energy reserves
 * - Charging state → Energy input
 * - Thermal state → Body temperature
 * - CPU/GPU load → Physical exertion
 *
 * The AI uses this context to:
 * - Reduce cognition intensity when energy is scarce
 * - Defer non-critical thinking when thermally constrained
 * - Resume full cognition when power stabilizes
 * - Develop meta-cognitive awareness (know when NOT to think)
 *
 * Energy states are classified into 4 primary modes:
 * ABUNDANT (>80% battery, charging or stable): Full cognition
 * NORMAL (30-80%): Standard cognition with environment awareness
 * LOW (<30%): Reduced intensity, defer non-critical tasks
 * CRITICAL (<15%): Minimal cognition, emergency-only reasoning
 */
interface EnergyAwarenessManager {
    /**
     * Start monitoring device energy state
     */
    fun startMonitoring()

    /**
     * Stop monitoring device energy state
     */
    fun stopMonitoring()

    /**
     * Get current energy state
     */
    suspend fun getEnergyState(): EnergyState

    /**
     * Subscribe to energy state changes (reactive)
     */
    fun observeEnergyState(): StateFlow<EnergyState>

    /**
     * Get current energy metrics
     */
    fun getEnergyMetrics(): EnergyMetrics

    /**
     * Get energy profile for current state
     * Profile contains cognition recommendations
     */
    fun getCurrentEnergyProfile(): EnergyProfile

    /**
     * Check if device is thermally constrained
     */
    fun isThermallyConstrained(): Boolean

    /**
     * Get estimated time until energy critical state
     */
    fun getEstimatedTimeToLowPowerSeconds(): Long
}

/**
 * Primary energy states
 */
@Serializable
enum class EnergyState {
    ABUNDANT,     // >80% battery, charging or stable | Full cognition
    NORMAL,       // 30-80% battery, not under stress | Standard cognition
    LOW,          // <30% battery, not critical | Reduced cognition
    CRITICAL,     // <15% battery or heavy thermal load | Emergency cognition only
    UNKNOWN       // Unable to determine state
}

/**
 * Fine-grained battery information
 */
@Serializable
data class BatteryInfo(
    val levelPercent: Int,              // 0-100
    val isCharging: Boolean,            // Is plugged in?
    val chargingVia: ChargingSource = ChargingSource.UNKNOWN,
    val isLowPowerModeEnabled: Boolean, // Android 11+ battery saver
    val temperatureCelsius: Float,      // Device temperature
    val healthStatus: BatteryHealth = BatteryHealth.UNKNOWN
) {
    val isCritical: Boolean
        get() = levelPercent < 15 && !isCharging

    val isLow: Boolean
        get() = levelPercent < 30

    val isAbundant: Boolean
        get() = levelPercent > 80 || isCharging

    val estimatedMinutesRemaining: Int
        get() = when {
            isCharging -> Int.MAX_VALUE
            levelPercent > 80 -> 480 // 8 hours estimate
            levelPercent > 50 -> 240 // 4 hours
            levelPercent > 30 -> 120 // 2 hours
            levelPercent > 15 -> 60  // 1 hour
            else -> 15               // 15 minutes critical
        }
}

/**
 * Charging source identification
 */
@Serializable
enum class ChargingSource {
    AC_CHARGER,
    USB_CHARGER,
    WIRELESS_CHARGER,
    UNKNOWN,
    NOT_CHARGING
}

/**
 * Battery health status
 */
@Serializable
enum class BatteryHealth {
    GOOD,
    OVERHEAT,
    COLD,
    OVERVOLTAGE,
    UNSPECIFIED_FAILURE,
    UNKNOWN
}

/**
 * Thermal constraint information
 */
@Serializable
data class ThermalInfo(
    val temperatureCelsius: Float,
    val thermalState: ThermalState = ThermalState.NORMAL,
    val isThrottling: Boolean = false,
    val cpuLoadPercent: Float = 0f,    // 0-100
    val gpuLoadPercent: Float = 0f,    // 0-100
    val thermalHeadroomCelsius: Float = 0f  // Degrees before throttling
) {
    val isCritical: Boolean
        get() = temperatureCelsius > 45f || thermalState == ThermalState.CRITICAL

    val isConstrained: Boolean
        get() = thermalState in listOf(ThermalState.MODERATE, ThermalState.CRITICAL) || cpuLoadPercent > 80f
}

/**
 * Device thermal states (Android 11+)
 */
@Serializable
enum class ThermalState {
    NORMAL,        // <35°C, no constraints
    LIGHT,         // 35-40°C, minor constraints
    MODERATE,      // 40-45°C, noticeable constraints
    SEVERE,        // 45-50°C, heavy constraints
    CRITICAL,      // >50°C, emergency thermal throttling
    UNKNOWN        // Unable to determine
}

/**
 * Complete energy state snapshot
 */
@Serializable
data class EnergyState(
    val batteryInfo: BatteryInfo,
    val thermalInfo: ThermalInfo,
    val energyState: EnergyState,
    val lastUpdateTimeMs: Long = System.currentTimeMillis(),
    val consecutiveHighLoadSeconds: Int = 0,  // For trend tracking
    val recentPowerChanges: List<PowerChange> = emptyList()  // Recent events
) {
    /**
     * Overall energy "health" score (0-100)
     * Lower = more constrained, fewer resources available
     */
    val energyHealthScore: Int
        get() {
            var score = 50  // Baseline

            // Battery contribution (40 points)
            when {
                batteryInfo.isCharging -> score += 40
                batteryInfo.levelPercent > 80 -> score += 35
                batteryInfo.levelPercent > 50 -> score += 25
                batteryInfo.levelPercent > 30 -> score += 15
                batteryInfo.levelPercent > 15 -> score += 5
                else -> score -= 10
            }

            // Thermal contribution (40 points)
            when (thermalInfo.thermalState) {
                ThermalState.NORMAL -> score += 40
                ThermalState.LIGHT -> score += 25
                ThermalState.MODERATE -> score += 10
                ThermalState.SEVERE -> score -= 10
                ThermalState.CRITICAL -> score -= 25
                ThermalState.UNKNOWN -> score += 0
            }

            // CPU/GPU load contribution (20 points)
            val avgLoad = (thermalInfo.cpuLoadPercent + thermalInfo.gpuLoadPercent) / 2
            score += (20 * (1f - avgLoad / 100f)).toInt()

            return score.coerceIn(0, 100)
        }

    /**
     * Determines if device is under significant energy pressure
     */
    val isUnderEnergyPressure: Boolean
        get() = energyHealthScore < 40

    /**
     * Determines if device can handle intensive operations
     */
    val canHandleIntensiveWork: Boolean
        get() = energyHealthScore > 70 && !thermalInfo.isThrottling
}

/**
 * Recent power change event (for trend analysis)
 */
@Serializable
data class PowerChange(
    val type: PowerChangeType,
    val timestampMs: Long = System.currentTimeMillis(),
    val batteryLevelChange: Int = 0,
    val reasonDescription: String = ""
)

@Serializable
enum class PowerChangeType {
    CHARGING_STARTED,
    CHARGING_STOPPED,
    LOW_POWER_MODE_ENABLED,
    LOW_POWER_MODE_DISABLED,
    THERMAL_THROTTLING_STARTED,
    THERMAL_THROTTLING_ENDED,
    UNEXPECTED_BATTERY_DROP,
    CRITICAL_BATTERY_REACHED
}

/**
 * Energy profile: Recommendations for AI behavior at current energy state
 *
 * Each energy state has a profile that guides:
 * - Cognition frequency (how often to think)
 * - Cognition intensity (how deeply to think)
 * - Learning intensity (how much to learn)
 * - ML inference frequency
 * - Graphics quality
 * - Background operations
 */
@Serializable
data class EnergyProfile(
    val energyState: EnergyState,
    val cognitiveFrequency: Float,           // Multiplier: 0.1 (10% of normal) to 1.0 (full)
    val cognitiveIntensity: Float,           // Multiplier: 0.1 (minimal) to 1.0 (full)
    val learningIntensity: Float,            // Multiplier: 0.0 (no learning) to 1.0 (full)
    val reflectionIntensity: Float,          // Multiplier: 0.0 (skip) to 1.0 (full)
    val evolutionIntensity: Float,           // Multiplier: 0.0 (skip) to 1.0 (full)
    val mlInferenceFrequency: Float,         // Multiplier: 0.0 (disabled) to 1.0 (full)
    val graphicsQuality: GraphicsQuality,    // Visual rendering quality
    val allowBackgroundOperations: Boolean,  // Can background tasks run?
    val allowHeavyComputation: Boolean,      // Can run heavy inference/reflection?
    val maxCognitiveIntervalsPerHour: Int,   // Rate limit for thinking
    val description: String = ""             // Human-readable profile description
) {
    /**
     * Estimate power cost of this profile (mW)
     * Used for energy budgeting
     */
    fun estimatePowerCostMilliwatts(): Float {
        var cost = 50f  // Baseline monitoring

        // Cognitive frequency cost
        cost += cognitiveFrequency * 150f

        // Learning cost
        cost += learningIntensity * 100f

        // Reflection cost
        cost += reflectionIntensity * 80f

        // Evolution cost
        cost += evolutionIntensity * 120f

        // ML inference cost
        cost += mlInferenceFrequency * 200f

        // Graphics cost
        cost += when (graphicsQuality) {
            GraphicsQuality.OFF -> 0f
            GraphicsQuality.LOW -> 50f
            GraphicsQuality.MEDIUM -> 150f
            GraphicsQuality.HIGH -> 300f
            GraphicsQuality.ULTRA -> 400f
        }

        return cost
    }

    /**
     * Estimate battery drain per hour at this profile
     */
    fun estimateBatteryDrainPercentPerHour(currentBatteryPercent: Int = 50): Float {
        val powerCost = estimatePowerCostMilliwatts()
        // Rough estimate: typical phone battery ~3000mAh @ 3.8V = 11400mWh
        // Divide by 100 to get %/mWh, multiply by 3600s/h
        return (powerCost / 11400f) * 3600f
    }
}

/**
 * Graphics rendering quality levels
 */
@Serializable
enum class GraphicsQuality {
    OFF,      // No rendering
    LOW,      // 30fps, low LOD, reduced shaders
    MEDIUM,   // 30fps, medium LOD
    HIGH,     // 60fps, high LOD, full effects
    ULTRA     // 120fps, ultra LOD, max effects
}

/**
 * Energy metrics for debugging and analysis
 */
@Serializable
data class EnergyMetrics(
    val totalCyclesDeferredDueToEnergy: Long = 0L,
    val totalLearningEventsDeferredDueToEnergy: Long = 0L,
    val totalCriticalBatteryEvents: Long = 0L,
    val totalThermalThrottlingEvents: Long = 0L,
    val averageEnergyHealthScore: Float = 75f,
    val peakTemperatureCelsius: Float = 35f,
    val lowestBatteryLevelSeen: Int = 100,
    val averageChargingTimeMinutes: Float = 0f,
    val cognitiveOperationsThrottledCount: Int = 0,
    val mlInferencesThrottledCount: Int = 0
)

/**
 * Default implementation of EnergyAwarenessManager
 *
 * Monitors:
 * - BatteryManager broadcast for battery events
 * - PowerManager for thermal state and power saver mode
 * - Hardware sensors (if available) for CPU/GPU load
 */
class DefaultEnergyAwarenessManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : EnergyAwarenessManager, DefaultLifecycleObserver {

    private val _energyStateFlow = MutableStateFlow<EnergyState?>(null)
    override fun observeEnergyState(): StateFlow<EnergyState> = _energyStateFlow.asStateFlow() as StateFlow<EnergyState>

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
    private val batteryReceiver = BatteryBroadcastReceiver()
    private var isMonitoring = false

    private var lastBatteryLevel = 100
    private var lastThermalState = ThermalState.NORMAL
    private var lastEnergyState = EnergyState.NORMAL
    private var consecutiveHighLoadSeconds = 0
    private val recentPowerChanges = ArrayDeque<PowerChange>(maxSize = 20)

    private var metrics = EnergyMetrics()

    init {
        // Auto-register with lifecycle
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        Timber.i("🔋 EnergyAwarenessManager created")
    }

    override fun onStart(owner: LifecycleOwner) {
        startMonitoring()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopMonitoring()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopMonitoring()
        Timber.i("🔋 EnergyAwarenessManager destroyed")
    }

    override fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        Timber.i("🔋 Starting energy monitoring")

        // Register for battery events
        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(batteryReceiver, batteryFilter)

        // Trigger initial update
        updateEnergyState()
    }

    override fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false
        Timber.i("🔋 Stopping energy monitoring")

        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Timber.w(e, "🔋 Failed to unregister battery receiver")
        }
    }

    override suspend fun getEnergyState(): EnergyState {
        return _energyStateFlow.value ?: updateEnergyState()
    }

    override fun getCurrentEnergyProfile(): EnergyProfile {
        val state = _energyStateFlow.value ?: return EnergyProfile.createDefault()

        return when (state.energyState) {
            EnergyState.ABUNDANT -> EnergyProfile.createAbundant()
            EnergyState.NORMAL -> EnergyProfile.createNormal()
            EnergyState.LOW -> EnergyProfile.createLow()
            EnergyState.CRITICAL -> EnergyProfile.createCritical()
            EnergyState.UNKNOWN -> EnergyProfile.createDefault()
        }
    }

    override fun isThermallyConstrained(): Boolean {
        val state = _energyStateFlow.value ?: return false
        return state.thermalInfo.isConstrained
    }

    override fun getEstimatedTimeToLowPowerSeconds(): Long {
        val state = _energyStateFlow.value ?: return Long.MAX_VALUE

        if (state.batteryInfo.isCharging) return Long.MAX_VALUE

        return (state.batteryInfo.estimatedMinutesRemaining * 60).toLong()
    }

    override fun getEnergyMetrics(): EnergyMetrics = metrics.copy()

    private fun updateEnergyState(): EnergyState {
        val batteryInfo = getBatteryInfo()
        val thermalInfo = getThermalInfo()

        val energyState = when {
            batteryInfo.isCritical || thermalInfo.isCritical -> EnergyState.CRITICAL
            batteryInfo.isLow || thermalInfo.isConstrained -> EnergyState.LOW
            batteryInfo.isAbundant && !thermalInfo.isConstrained -> EnergyState.ABUNDANT
            else -> EnergyState.NORMAL
        }

        val newState = EnergyState(
            batteryInfo = batteryInfo,
            thermalInfo = thermalInfo,
            energyState = energyState,
            consecutiveHighLoadSeconds = if (thermalInfo.isConstrained) {
                consecutiveHighLoadSeconds + 1
            } else {
                0
            },
            recentPowerChanges = recentPowerChanges.toList()
        )

        _energyStateFlow.value = newState

        // Track state transitions
        if (energyState != lastEnergyState) {
            Timber.i("🔋 Energy state transition: $lastEnergyState → $energyState (score: ${newState.energyHealthScore})")
            lastEnergyState = energyState
        }

        return newState
    }

    private fun getBatteryInfo(): BatteryInfo {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?

        // Get battery status
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)
            ?: return BatteryInfo(
                levelPercent = 50,
                isCharging = false,
                isLowPowerModeEnabled = false,
                temperatureCelsius = 0f
            )

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val levelPercent = (level * 100) / scale

        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val chargingSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingSource.AC_CHARGER
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingSource.USB_CHARGER
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingSource.WIRELESS_CHARGER
            else -> if (isCharging) ChargingSource.UNKNOWN else ChargingSource.NOT_CHARGING
        }

        val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val healthStatus = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            BatteryManager.BATTERY_HEALTH_OVERVOLTAGE -> BatteryHealth.OVERVOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
            else -> BatteryHealth.UNKNOWN
        }

        val isLowPowerMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            powerManager?.isPowerSaveMode ?: false
        } else {
            false
        }

        // Track level changes
        if (levelPercent != lastBatteryLevel) {
            if (levelPercent < lastBatteryLevel && (lastBatteryLevel - levelPercent) > 10) {
                recordPowerChange(PowerChangeType.UNEXPECTED_BATTERY_DROP, levelPercent - lastBatteryLevel)
            }
            if (levelPercent < 15 && lastBatteryLevel >= 15) {
                recordPowerChange(PowerChangeType.CRITICAL_BATTERY_REACHED)
                metrics = metrics.copy(totalCriticalBatteryEvents = metrics.totalCriticalBatteryEvents + 1)
            }
            lastBatteryLevel = levelPercent
        }

        return BatteryInfo(
            levelPercent = levelPercent,
            isCharging = isCharging,
            chargingVia = chargingSource,
            isLowPowerModeEnabled = isLowPowerMode,
            temperatureCelsius = temperature,
            healthStatus = healthStatus
        )
    }

    private fun getThermalInfo(): ThermalInfo {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?

        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }

        val thermalState = when (thermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalState.NORMAL
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState.CRITICAL
            else -> ThermalState.NORMAL
        }

        val temperature = getDeviceTemperature()
        val isThrottling = thermalStatus in listOf(
            PowerManager.THERMAL_STATUS_SEVERE,
            PowerManager.THERMAL_STATUS_CRITICAL
        )

        // Track thermal events
        if (thermalState != lastThermalState && thermalState != ThermalState.NORMAL) {
            recordPowerChange(PowerChangeType.THERMAL_THROTTLING_STARTED)
            metrics = metrics.copy(totalThermalThrottlingEvents = metrics.totalThermalThrottlingEvents + 1)
        } else if (lastThermalState != ThermalState.NORMAL && thermalState == ThermalState.NORMAL) {
            recordPowerChange(PowerChangeType.THERMAL_THROTTLING_ENDED)
        }
        lastThermalState = thermalState

        // Update peak temperature
        if (temperature > metrics.peakTemperatureCelsius) {
            metrics = metrics.copy(peakTemperatureCelsius = temperature)
        }

        return ThermalInfo(
            temperatureCelsius = temperature,
            thermalState = thermalState,
            isThrottling = isThrottling,
            cpuLoadPercent = estimateCpuLoad().toFloat(),
            gpuLoadPercent = estimateGpuLoad().toFloat(),
            thermalHeadroomCelsius = max(0f, 50f - temperature)
        )
    }

    private fun getDeviceTemperature(): Float {
        // Try to read from thermal zone files (common on Linux/Android)
        return try {
            val thermFile = "/sys/class/thermal/thermal_zone0/temp"
            val content = java.io.File(thermFile).readText().trim()
            val tempMicro = content.toLongOrNull() ?: return 35f
            (tempMicro / 1000f).coerceIn(0f, 80f)
        } catch (e: Exception) {
            // Fallback: battery temperature from battery manager
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            (temp / 10f).coerceIn(0f, 80f)
        }
    }

    private fun estimateCpuLoad(): Int {
        // Simplified CPU load estimate based on /proc/stat
        return try {
            val cpuFile = java.io.File("/proc/stat")
            if (!cpuFile.exists()) return 0
            
            // In production, you'd parse /proc/stat and calculate CPU usage
            // For now, return a safe default
            0
        } catch (e: Exception) {
            0
        }
    }

    private fun estimateGpuLoad(): Int {
        // GPU load is harder to estimate without specific GPU driver interfaces
        // Return 0 as safe default; can be overridden with actual GPU metrics
        return 0
    }

    private fun recordPowerChange(type: PowerChangeType, batteryChange: Int = 0) {
        val change = PowerChange(
            type = type,
            batteryLevelChange = batteryChange,
            reasonDescription = when (type) {
                PowerChangeType.CHARGING_STARTED -> "Device connected to charger"
                PowerChangeType.CHARGING_STOPPED -> "Device disconnected from charger"
                PowerChangeType.LOW_POWER_MODE_ENABLED -> "Low power mode activated"
                PowerChangeType.LOW_POWER_MODE_DISABLED -> "Low power mode deactivated"
                PowerChangeType.THERMAL_THROTTLING_STARTED -> "Device thermal throttling started"
                PowerChangeType.THERMAL_THROTTLING_ENDED -> "Device thermal throttling ended"
                PowerChangeType.UNEXPECTED_BATTERY_DROP -> "Unexpected battery drop: ${batteryChange}%"
                PowerChangeType.CRITICAL_BATTERY_REACHED -> "Battery critical: <15%"
            }
        )
        recentPowerChanges.addLast(change)
        Timber.i("🔋 Power event: ${change.type} - ${change.reasonDescription}")
    }

    /**
     * Broadcast receiver for battery change events
     */
    private inner class BatteryBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                updateEnergyState()
            }
        }
    }

    companion object {
        private const val MAX_POWER_CHANGES = 20
    }
}

/**
 * Extension functions for EnergyProfile
 */
fun EnergyProfile.Companion.createDefault(): EnergyProfile = EnergyProfile(
    energyState = EnergyState.NORMAL,
    cognitiveFrequency = 1.0f,
    cognitiveIntensity = 1.0f,
    learningIntensity = 1.0f,
    reflectionIntensity = 1.0f,
    evolutionIntensity = 0.5f,
    mlInferenceFrequency = 1.0f,
    graphicsQuality = GraphicsQuality.HIGH,
    allowBackgroundOperations = true,
    allowHeavyComputation = true,
    maxCognitiveIntervalsPerHour = 360,
    description = "Default profile"
)

fun EnergyProfile.Companion.createAbundant(): EnergyProfile = EnergyProfile(
    energyState = EnergyState.ABUNDANT,
    cognitiveFrequency = 1.0f,
    cognitiveIntensity = 1.0f,
    learningIntensity = 1.0f,
    reflectionIntensity = 1.0f,
    evolutionIntensity = 1.0f,
    mlInferenceFrequency = 1.0f,
    graphicsQuality = GraphicsQuality.ULTRA,
    allowBackgroundOperations = true,
    allowHeavyComputation = true,
    maxCognitiveIntervalsPerHour = 360,
    description = "Abundant power - Full cognition, max graphics"
)

fun EnergyProfile.Companion.createNormal(): EnergyProfile = EnergyProfile(
    energyState = EnergyState.NORMAL,
    cognitiveFrequency = 1.0f,
    cognitiveIntensity = 1.0f,
    learningIntensity = 0.8f,
    reflectionIntensity = 0.8f,
    evolutionIntensity = 0.6f,
    mlInferenceFrequency = 0.9f,
    graphicsQuality = GraphicsQuality.HIGH,
    allowBackgroundOperations = true,
    allowHeavyComputation = true,
    maxCognitiveIntervalsPerHour = 360,
    description = "Normal power - Balanced cognition"
)

fun EnergyProfile.Companion.createLow(): EnergyProfile = EnergyProfile(
    energyState = EnergyState.LOW,
    cognitiveFrequency = 0.5f,
    cognitiveIntensity = 0.7f,
    learningIntensity = 0.4f,
    reflectionIntensity = 0.3f,
    evolutionIntensity = 0.1f,
    mlInferenceFrequency = 0.5f,
    graphicsQuality = GraphicsQuality.MEDIUM,
    allowBackgroundOperations = false,
    allowHeavyComputation = false,
    maxCognitiveIntervalsPerHour = 120,
    description = "Low power - Reduced cognition, no background work"
)

fun EnergyProfile.Companion.createCritical(): EnergyProfile = EnergyProfile(
    energyState = EnergyState.CRITICAL,
    cognitiveFrequency = 0.1f,
    cognitiveIntensity = 0.3f,
    learningIntensity = 0.0f,
    reflectionIntensity = 0.0f,
    evolutionIntensity = 0.0f,
    mlInferenceFrequency = 0.0f,
    graphicsQuality = GraphicsQuality.LOW,
    allowBackgroundOperations = false,
    allowHeavyComputation = false,
    maxCognitiveIntervalsPerHour = 12,
    description = "Critical power - Emergency cognition only"
)

/**
 * Companion object factory methods
 */
object EnergyProfileFactory {
    fun createDefault() = EnergyProfile.createDefault()
    fun createAbundant() = EnergyProfile.createAbundant()
    fun createNormal() = EnergyProfile.createNormal()
    fun createLow() = EnergyProfile.createLow()
    fun createCritical() = EnergyProfile.createCritical()
}
