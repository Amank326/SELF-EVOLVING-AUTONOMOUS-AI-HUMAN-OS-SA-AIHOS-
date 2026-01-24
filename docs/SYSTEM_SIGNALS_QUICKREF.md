# System Signals Quick Reference

## Quick Start: Using Signals in Your Code

### Basic Setup (2 minutes)

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var signalsManager: SystemSignalsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create signals manager (lifecycle binding is automatic)
        signalsManager = SystemSignalsManager(
            context = this,
            lifecycleOwner = this
        )
    }

    override fun onStart() {
        super.onStart()
        
        // Listen to device context changes
        // Listeners automatically register here
        lifecycleScope.launch {
            signalsManager.deviceContext.collect { context ->
                println("Battery: ${context.batteryLevel}%")
                println("Screen: ${context.isScreenOn}")
                println("Network: ${context.isNetworkConnected}")
            }
        }
    }
    
    // Listeners automatically unregister in onStop (lifecycle binding)
}
```

## Device Context Fields

```kotlin
data class DeviceContext(
    val batteryLevel: Float           // 0-100% - Current battery percentage
    val temperature: Float            // °C - Device temperature (20-60 clamped)
    val isScreenOn: Boolean           // true/false - Screen state
    val foregroundApp: String         // Package name - Current foreground app
    val usageIntensity: Float         // 0.0-1.0 - Calculated usage intensity
    val timeOfDay: Float              // 0.0-1.0 - Time as fraction of day
    val isNetworkConnected: Boolean   // true/false - Network availability
    val timestamp: Long               // Milliseconds - Snapshot time
)
```

## Common Patterns

### Pattern 1: Adjust Learning Based on Battery

```kotlin
signalsManager.deviceContext.collect { context ->
    val shouldLearnAggresively = context.batteryLevel > 30
    val shouldLearnConservatively = context.batteryLevel < 20
    
    if (shouldLearnAggresively) {
        ai.setLearningRate(0.9f)  // High learning
    } else if (shouldLearnConservatively) {
        ai.setLearningRate(0.1f)  // Minimal learning
    }
}
```

### Pattern 2: Check Network Before Syncing

```kotlin
signalsManager.deviceContext.collect { context ->
    if (context.isNetworkConnected) {
        syncData()  // Only sync when connected
    } else {
        useOfflineMode()
    }
}
```

### Pattern 3: Adjust Reasoning Load During Activity

```kotlin
signalsManager.deviceContext.collect { context ->
    val userActive = context.isScreenOn && context.usageIntensity > 0.5f
    
    if (userActive) {
        ai.setReasoningLoad(0.8f)  // Full reasoning while user watching
    } else {
        ai.setReasoningLoad(0.2f)  // Minimal reasoning when idle
    }
}
```

### Pattern 4: Reflect During Calm Moments

```kotlin
signalsManager.deviceContext.collect { context ->
    // Optimal reflection: night time, low battery stress, network available
    val reflectionConditionsMet = 
        context.timeOfDay < 0.25f &&  // Late night
        context.batteryLevel > 50 &&   // Not stressed
        context.isNetworkConnected &&
        !context.isScreenOn             // User sleeping
    
    if (reflectionConditionsMet) {
        ai.triggerDeepReflection()
    }
}
```

## Signal Update Frequencies

| Signal | Update Method | Frequency | Latency |
|--------|--------------|-----------|---------|
| Battery | Broadcast | On change | < 1s |
| Screen | Broadcast | Immediate | < 100ms |
| Network | Callback | On change | < 1s |
| Temperature | Polling | Every 10s | Up to 10s |
| Time of Day | Polling | Every 60s | Up to 60s |
| Foreground App | Polling | Every 2s | Up to 2s |
| Usage Intensity | Derived | Real-time | < 1s |

## Thread Safety

All signals are **thread-safe** via StateFlow:

```kotlin
// Safe to collect from multiple coroutines simultaneously
launch(Dispatchers.Main) {
    signalsManager.deviceContext.collect { context -> ... }
}

launch(Dispatchers.Default) {
    signalsManager.deviceContext.collect { context -> ... }
}
```

## Performance Tips

### ✅ DO: Use Flow Operators for Efficiency

```kotlin
signalsManager.deviceContext
    .throttleTime(500)                    // Emit at most every 500ms
    .distinctUntilChanged()               // Skip duplicates
    .collect { context -> ... }
```

### ❌ DON'T: Ignore Signal Changes

```kotlin
// Bad: Polling the signal value
launch {
    while (true) {
        val context = signalsManager.deviceContext.value
        println(context.batteryLevel)
        delay(1000)
    }
}
```

### ✅ DO: Collect Changes via Flow

```kotlin
// Good: React to actual changes
signalsManager.deviceContext.collect { context ->
    println(context.batteryLevel)  // Only prints when battery changes
}
```

## Debugging

### Enable Verbose Logging

```kotlin
Timber.plant(Timber.DebugTree())  // Enable Timber logging

// Watch logcat for signal updates:
// BatteryProvider: Battery level updated to 85%
// ScreenStateProvider: Screen turned ON
// NetworkProvider: Network became available
// TemperatureProvider: Temperature polled: 35°C
// TimeOfDayProvider: 14:30 (value: 0.60, isDaytime: true)
// ForegroundAppProvider: Foreground app: com.android.chrome
```

### Common Issues

**Issue: Signals not updating**
```kotlin
// Check if lifecycle is active
println("Lifecycle state: ${this.lifecycle.currentState}")
// Should be: STARTED
```

**Issue: Battery always 80%**
```kotlin
// Provider might not be registered
// Check logcat for "BatteryProvider: Registered successfully"
```

**Issue: High memory usage**
```kotlin
// Ensure listeners are unregistering on onStop
// Check logcat for "Unregistered successfully" messages
```

## Memory Leak Prevention Checklist

✅ **Always use lifecycle binding**:
```kotlin
val manager = SystemSignalsManager(context, lifecycleOwner)
// Not manually managing provider lifecycle
```

✅ **No manual listener registration**:
```kotlin
// SystemSignalsManager handles all registration/unregistration
// Don't do this:
provider.register()
// ... later ...
provider.unregister()
```

✅ **Use lifecycleScope for collection**:
```kotlin
// Good: Collection dies with activity
lifecycleScope.launch {
    signalsManager.deviceContext.collect { ... }
}

// Bad: Collection outlives activity
viewModelScope.launch {
    signalsManager.deviceContext.collect { ... }
}
```

## API Summary

### SystemSignalsManager

```kotlin
class SystemSignalsManager(
    context: Context,
    lifecycleOwner: LifecycleOwner
) {
    // Main API
    val deviceContext: StateFlow<DeviceContext>  // Subscribe to changes
    
    // Lifecycle automatically manages these:
    suspend fun start()    // Called on lifecycle.ON_START
    suspend fun stop()     // Called on lifecycle.ON_STOP
    fun destroy()          // Call if needed to cleanup early
}
```

### DeviceContext Interface

```kotlin
interface DeviceContext {
    val batteryLevel: Float           // 0-100
    val temperature: Float            // °C
    val isScreenOn: Boolean           // true/false
    val foregroundApp: String         // Package name
    val usageIntensity: Float         // 0-1
    val timeOfDay: Float              // 0-1
    val isNetworkConnected: Boolean   // true/false
    val timestamp: Long               // ms
}
```

## FAQ

**Q: Do I need to manually unregister listeners?**
A: No. SystemSignalsManager handles all registration/unregistration via lifecycle binding.

**Q: Will this drain my battery?**
A: No. Total battery impact is < 1% per hour.

**Q: Can I modify signal frequency?**
A: Yes. Pass `pollIntervalMs` to each provider's constructor. See [SYSTEM_SIGNALS_INTEGRATION.md](SYSTEM_SIGNALS_INTEGRATION.md) for details.

**Q: What if permissions are missing?**
A: Providers handle gracefully. ForegroundAppProvider returns "unknown" if PACKAGE_USAGE_STATS not granted.

**Q: Can multiple Activities listen to signals?**
A: Yes, but create separate SystemSignalsManager per Activity (each binds to its own lifecycle).

**Q: How do I test signals?**
A: See [SYSTEM_SIGNALS_INTEGRATION.md](SYSTEM_SIGNALS_INTEGRATION.md) for unit and integration test examples.

## Resources

- **Full Documentation**: [SYSTEM_SIGNALS_INTEGRATION.md](SYSTEM_SIGNALS_INTEGRATION.md)
- **Design & Safety**: [SYSTEM_SIGNALS_HARDENING.md](SYSTEM_SIGNALS_HARDENING.md)
- **Source Code**: `com.aihos.system.signals.impl.*`
- **Android Docs**: [Lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle) | [Broadcasts](https://developer.android.com/guide/components/broadcasts) | [ConnectivityManager](https://developer.android.com/reference/android/net/ConnectivityManager)

