# Energy-Aware AI Cognition - Quick Start Guide

## 30-Second Setup

```kotlin
// In Application.onCreate()
override fun onCreate() {
    super.onCreate()
    
    // Initialize energy-aware cognition system
    lifecycleScope.launch {
        EnergyAwareCognitionSystem.initialize(
            context = this@MyApp,
            cognitionLoopManager = autonomyController.getCognitionLoopManager(),
            autonomyController = autonomyController
        )
    }
}
```

**That's it!** The system automatically:
- ✅ Monitors battery, charging, power saver mode
- ✅ Monitors device temperature and thermal throttling
- ✅ Adapts cognition frequency based on energy state
- ✅ Reduces reflection/evolution when energy-constrained
- ✅ Pauses learning during thermal pressure
- ✅ Resumes full cognition when power stabilizes
- ✅ Teaches AI when NOT to think

---

## Getting Status

### Quick Status Check
```kotlin
// Get complete system status
val status = EnergyAwareCognitionSystem.get()?.getSystemStatus()

println("Energy: ${status?.energyState}")
println("Thermal: ${status?.thermalState}")
println("Cognitive Performance: ${status?.cognitivePerformancePercent}%")
println("Battery Health: ${status?.batteryHealthScore}/100")
```

### Energy Manager
```kotlin
val energyManager = EnergyAwareCognitionSystem.get()?.getEnergyManager()

// Get current energy state
val energyState = energyManager?.getEnergyState()
println("Battery: ${energyState?.batteryInfo?.levelPercent}%")
println("Charging: ${energyState?.batteryInfo?.isCharging}")
println("Energy State: ${energyState?.energyState}")

// Subscribe to changes
energyManager?.observeEnergyState()?.collect { state ->
    Timber.i("Energy health: ${state.energyHealthScore}/100")
}
```

### Thermal Manager
```kotlin
val thermalManager = EnergyAwareCognitionSystem.get()?.getThermalManager()

// Check if cognition should pause
val shouldPause = thermalManager?.shouldPauseCognitionForThermal() ?: false

// Get thermal state
val thermal = thermalManager?.getThermalState()
println("Temperature: ${thermal?.temperatureCelsius}°C")
println("Throttling: ${thermal?.isThrottling}")
println("Time to safe: ${thermal?.estimatedTimeToNormalMs}ms")
```

### Meta-Cognition / AI Wisdom
```kotlin
val metaCognition = EnergyAwareCognitionSystem.get()?.getMetaCognition()

// Get AI wisdom metrics
val metrics = metaCognition?.getMetaCognitionMetrics()
println("Wisdom Score: ${metrics?.estimatedAiWisdomScore}/100")
println("AI understands energy? ${metrics?.understoodEnergyAwareness}")
println("AI understands thermal? ${metrics?.understandsThermalPressure}")
```

---

## Runtime Control

### Pause/Resume Energy Monitoring
```kotlin
val energyManager = EnergyAwareCognitionSystem.get()?.getEnergyManager()

// Pause monitoring (if needed)
energyManager?.stopMonitoring()

// Resume monitoring
energyManager?.startMonitoring()
```

### Trigger Catch-Up Cognition
```kotlin
val metaCognition = EnergyAwareCognitionSystem.get()?.getMetaCognition()

// When charging (have energy), run deferred cognition
metaCognition?.prioritizeCatchupCognition()
```

### Check if Safe for Expensive Operations
```kotlin
val bridge = EnergyAwareCognitionSystem.get()?.getCognitionBridge()

// Before running expensive ML/reflection/evolution
val canRunExpensive = !bridge?.shouldDeferExpensiveCognition() ?: true

if (canRunExpensive) {
    // Safe to run expensive operations
    performDeepReflection()
    runML() 
} else {
    // Defer expensive operations
    queueForLater()
}
```

---

## Configuration Presets

### Conservative Mode (Maximum Battery Life)
```kotlin
// Use when battery is precious
// Estimated drain: 0.05% per hour

val conservativeProfile = EnergyProfile.createLow()
// Cognition: 50% speed
// Reflection: 40% intensity
// Evolution: 10% intensity
// ML inference: 50% frequency
// Graphics: MEDIUM quality
```

### Balanced Mode (Recommended)
```kotlin
// Default behavior with good balance
// Estimated drain: 0.1-0.2% per hour

val balancedProfile = EnergyProfile.createNormal()
// Cognition: 100% speed
// Reflection: 80% intensity
// Evolution: 60% intensity
// ML inference: 90% frequency
// Graphics: HIGH quality
```

### Aggressive Mode (Maximum Learning)
```kotlin
// Use when plugged in
// Estimated drain: 0.3-0.5% per hour

val aggressiveProfile = EnergyProfile.createAbundant()
// Cognition: 100% speed
// Reflection: 100% intensity
// Evolution: 100% intensity
// ML inference: 100% frequency
// Graphics: ULTRA quality
```

---

## Energy States at a Glance

### ABUNDANT (>80% or charging)
```
✅ Full thinking enabled
✅ All learning active
✅ Max graphics quality
⏱️ Cognition every 10 seconds
🔋 Drain: 0.3-0.5% per hour
```

### NORMAL (30-80%)
```
✅ Standard thinking
✅ Most learning active (80%)
✅ Some evolution (60%)
⏱️ Cognition every 10 seconds
🔋 Drain: 0.1-0.2% per hour
```

### LOW (<30%)
```
⚠️ Reduced thinking (50% speed)
⚠️ Minimal learning (40%)
⚠️ Minimal evolution (10%)
⏱️ Cognition every 20 seconds
🔋 Drain: 0.05-0.1% per hour
```

### CRITICAL (<15% or throttling)
```
❌ Emergency mode only
❌ No learning
❌ No evolution
⏱️ Cognition every 100 seconds
🔋 Drain: 0.01-0.05% per hour
```

---

## Thermal States at a Glance

| State | Temp | Action |
|-------|------|--------|
| **NORMAL** | <35°C | Full cognition |
| **LIGHT** | 35-40°C | Monitor, continue |
| **MODERATE** | 40-45°C | Reduce to 30% intensity |
| **SEVERE** | 45-50°C | Reduce to 10% intensity, pause evolution |
| **CRITICAL** | >50°C | Pause all cognition |

---

## Common Integration Points

### CognitionLoopManager Integration
```kotlin
// In CognitionLoopManager.runCognitionLoop()
val bridge = EnergyAwareCognitionSystem.get()?.getCognitionBridge()
val params = bridge?.getEnergyAdjustedCognitionParams()

// Apply energy adjustments
val adjustedInterval = baseIntervalMs * params.cognitiveIntervalMultiplier

// Pass to cognition cycle
autonomyController.triggerDecisionCycle(
    reflectionIntensity = params.reflectionIntensity,
    evolutionIntensity = params.evolutionIntensity
)
```

### ML Inference Integration
```kotlin
// In TensorFlow Lite inference
val bridge = EnergyAwareCognitionSystem.get()?.getCognitionBridge()
val batchSizeMultiplier = bridge?.getMlBatchSizeMultiplier() ?: 1.0f

// Adjust batch size
val adjustedBatchSize = (defaultBatchSize * batchSizeMultiplier).toInt()

// Skip some inferences if needed
val inferenceFrequency = bridge?.getEnergyAdjustedCognitionParams()?.mlInferenceFrequencyMultiplier ?: 1.0f
if (Random.nextFloat() > inferenceFrequency) return  // Skip this cycle
```

### Graphics Integration  
```kotlin
// In Filament renderer
val bridge = EnergyAwareCognitionSystem.get()?.getCognitionBridge()
val quality = bridge?.getGraphicsQuality() ?: GraphicsQuality.HIGH

when (quality) {
    GraphicsQuality.OFF -> disableRendering()
    GraphicsQuality.LOW -> setFrameRate(15)
    GraphicsQuality.MEDIUM -> setFrameRate(30)
    GraphicsQuality.HIGH -> setFrameRate(60)
    GraphicsQuality.ULTRA -> setFrameRate(120)
}
```

---

## Monitoring & Debugging

### Print Full Status
```kotlin
val coordinator = EnergyAwareCognitionSystem.get()
    ?: return println("Energy system not initialized")

val status = coordinator.getSystemStatus()

println("""
╔════════════════════════════════════════╗
║   ENERGY-AWARE AI COGNITION STATUS    ║
╠════════════════════════════════════════╣
║ Energy State: ${status.energyState}
║ Thermal State: ${status.thermalState}
║ Battery Health: ${status.batteryHealthScore}/100
║ Thermal Health: ${status.thermalHealthScore}/100
║ Cognitive Performance: ${status.cognitivePerformancePercent}%
║ Est. Battery Drain: ${status.estimatedBatteryDrainPercentPerHour}%/hour
║ AI Wisdom Score: ${status.metaCognitionWisdomScore}/100
║ Ready for Full Cognition: ${status.systemReadyForFullCognition}
╠════════════════════════════════════════╣
║ Recommendations:
${status.recommendedActions.joinToString("\n║ ")}
╚════════════════════════════════════════╝
""")
```

### Monitor Energy Events
```kotlin
val energyManager = EnergyAwareCognitionSystem.get()?.getEnergyManager()

energyManager?.observeEnergyState()?.collect { state ->
    if (state.energyHealthScore < 40) {
        Timber.w("⚠️ Energy pressure: ${state.energyHealthScore}/100")
    }
    
    // Print power changes
    state.recentPowerChanges.forEach { change ->
        Timber.i("🔋 ${change.type}: ${change.reasonDescription}")
    }
}
```

### Monitor Thermal Events
```kotlin
val thermalManager = EnergyAwareCognitionSystem.get()?.getThermalManager()

thermalManager?.observeThermalConstraint()?.collect { constraint ->
    Timber.i("🌡️ ${constraint.temperatureCelsius}°C (${constraint.thermalState})")
    
    if (constraint.shouldPauseCognition) {
        Timber.w("🌡️ PAUSING COGNITION - Too hot!")
    }
    
    if (constraint.isThrottling) {
        Timber.w("🌡️ THROTTLING DETECTED")
    }
}
```

---

## Testing Checklist

- [ ] **Battery Test**: Drain device to 25% → Should go to LOW state
- [ ] **Charging Test**: Plug in device → Should go to ABUNDANT state
- [ ] **Thermal Test**: Device reaches 45°C → Cognition should reduce
- [ ] **Low Power Mode**: Enable power saver → Cognition reduces
- [ ] **Critical Battery**: Let battery reach <15% → Emergency mode
- [ ] **Catch-Up**: Trigger when charging → Deferred ops run
- [ ] **Wisdom**: Check wisdom score after 10+ minutes → Should increase

---

## Metrics to Monitor

### Key Performance Indicators
```
Energy Health Score: 0-100 (higher is better)
Thermal Health Score: 0-100 (higher is better)
Cognitive Performance: 0-100% (percentage of full capacity)
AI Wisdom Score: 0-100 (higher = better at energy management)
Battery Drain Rate: %/hour (lower is better)
```

### Goals
```
✅ Energy health: >70 in normal conditions
✅ Thermal health: >80 (no throttling)
✅ Cognitive performance: >80% when plugged in
✅ AI wisdom: >70 after 30 minutes
✅ Battery drain: <0.2% per hour in NORMAL state
✅ Never thermal throttle (preventive)
```

---

## Troubleshooting

### Problem: Energy system not monitoring
```
Solution:
1. Check that initialize() was called
2. Verify EnergyAwareCognitionSystem.get() != null
3. Check logcat for "EnergyAwarenessManager created"
4. Ensure BATTERY permission in manifest
```

### Problem: Thermal throttling still occurring
```
Solution:
1. Lower thermal thresholds (be more conservative)
2. Reduce ML inference frequency more aggressively
3. Check if background tasks are causing heat
4. Verify thermal sensor is being read correctly
```

### Problem: Battery draining faster than expected
```
Solution:
1. Verify energy profile is being applied (check logs)
2. Check ML inference isn't running constantly
3. Verify graphics quality is scaling down
4. Check for other power-hungry apps
```

### Problem: AI not learning enough
```
Solution:
1. Trigger catch-up when plugged in: metaCognition.prioritizeCatchupCognition()
2. Increase NORMAL profile reflection/evolution intensity
3. Spend more time in ABUNDANT/NORMAL states
4. Check that MetaCognition is enabled
```

---

## Summary

Energy-aware cognition makes SA-AIHOS truly sustainable:

| Aspect | Before | After |
|--------|--------|-------|
| **Thinking** | Constant 100% | Adapts to energy |
| **Thermal** | Risk of throttling | Prevented |
| **Battery** | 0.5-1.0%/hour | 0.1-0.2%/hour |
| **Wisdom** | None | Learns when not to think |
| **Adaptation** | Manual config | Automatic |

**Result**: An AI that thinks responsibly about its energy, a device that stays cool, and a user who sees extended battery life.
