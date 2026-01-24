# Android System Signals - Hardening & Lifecycle Safety

**Status**: Design & Implementation Phase  
**Scope**: All system signal integrations used by AI cognition  
**Focus**: Reliability, safety, compliance, battery efficiency

---

## Current State Analysis

### Issues Identified in Current Implementation

#### 1. BatterySignalImpl - Unsafe Battery Intent Registration
```kotlin
// PROBLEM: registerReceiver(null, ...) on UI thread
// - Registers broadcast receiver without proper cleanup
// - IntentFilter created locally but receiver never unregistered
// - Sticky broadcast may not update if called once
```
**Risks**:
- Memory leak from unreleased broadcast receiver
- Stale battery data if not updated regularly
- No lifecycle binding

#### 2. SignalCollectorImpl - Incomplete Implementation
```kotlin
// TODO: Set up periodic update task
// TODO: Get from actual system (Screen state, foreground app, network)
```
**Risks**:
- Missing signal implementations (screen, app, network)
- No background service integration
- No listener lifecycle management
- Hard-coded mock data

#### 3. Missing Lifecycle Safety
**Risks**:
- Listeners registered but never unregistered
- No Activity/Fragment lifecycle binding
- Background service may run indefinitely
- No cleanup on app pause/stop

#### 4. No Throttling/Debouncing
**Risks**:
- Every signal update emits to all subscribers
- Excessive CPU/battery usage
- Unnecessary flow emissions
- No rate limiting on frequent signals

#### 5. Android Background Execution Policy
**Risks**:
- May violate Doze mode restrictions
- No WorkManager or JobScheduler integration
- Foreground service not declared
- Update frequency not optimized for battery

---

## Hardening Strategy

### Phase 1: Lifecycle-Safe Signal Collection

**Goals**:
- Proper registration/unregistration of all listeners
- Activity/Fragment lifecycle binding
- Clean shutdown on app background
- Zero memory leaks

**Architecture**:
```
┌─────────────────────────────────────────────────┐
│ SystemSignalsManager (Lifecycle-Aware)          │
├─────────────────────────────────────────────────┤
│                                                  │
│  Lifecycle: Binding (onCreate, onStart, onStop) │
│  Ownership: Activity/ViewModel (not singleton)   │
│                                                  │
│  Responsibilities:                              │
│  - Register listeners on lifecycle start        │
│  - Unregister listeners on lifecycle stop       │
│  - Manage background signal collection          │
│  - Throttle/debounce updates                    │
│  - Track active listeners                       │
│                                                  │
│  ┌─────────────────────────────────────────┐   │
│  │ Signal Providers                         │   │
│  ├─────────────────────────────────────────┤   │
│  │ • BatteryProvider (BroadcastReceiver)    │   │
│  │ • ScreenStateProvider (BroadcastReceiver)│   │
│  │ • ForegroundAppProvider (AccessibilityService)   │
│  │ • NetworkProvider (ConnectivityManager)  │   │
│  │ • TimeOfDayProvider (Timer-based)        │   │
│  │ • TemperatureProvider (BatteryManager)   │   │
│  │ • UsageIntensityProvider (Calculated)    │   │
│  └─────────────────────────────────────────┘   │
│                                                  │
│  ┌─────────────────────────────────────────┐   │
│  │ Signal Aggregation Layer                 │   │
│  ├─────────────────────────────────────────┤   │
│  │ • Throttling (10s for battery, 1s screen)   │
│  │ • Debouncing (200ms for network changes)     │
│  │ • Normalization (0-1 ranges)             │   │
│  │ • Timestamp tracking                     │   │
│  │ • Confidence scoring                     │   │
│  └─────────────────────────────────────────┘   │
│                                                  │
│  Output: DeviceContext StateFlow (Observable)   │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Phase 2: Listener Ownership & Cleanup

**Pattern: Lifecycle Component Binding**

```kotlin
// BEFORE: Global listener, never unregistered
class BadSignalCollector {
    init {
        context.registerReceiver(batteryReceiver, IntentFilter(...))
        // Never unregistered → memory leak
    }
}

// AFTER: Lifecycle-bound listeners, auto-cleanup
class GoodSignalManager(
    context: Context,
    lifecycleOwner: LifecycleOwner
) {
    init {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> registerAllListeners()
                    Lifecycle.Event.ON_STOP -> unregisterAllListeners()
                    else -> {}
                }
            }
        })
    }
    
    private fun registerAllListeners() {
        // Register each with try-catch for robustness
    }
    
    private fun unregisterAllListeners() {
        // Unregister all listeners, handle already-unregistered
    }
}
```

### Phase 3: Throttling & Efficient Sampling

**Update Frequencies** (optimize for battery):

| Signal | Frequency | Reason |
|--------|-----------|--------|
| Battery | Every 10s | Changes slowly, no need for frequent updates |
| Temperature | Every 30s | Thermal changes are gradual |
| Screen State | Event-driven | Only changes when screen toggled |
| Network | Event-driven | Only changes when connection state changes |
| Foreground App | Every 5s | User app changes are observable |
| Time of Day | Every 60s | Changes only on minute boundaries |
| Usage Intensity | Every 5s | Calculated from other signals |

**Implementation**:
```kotlin
// Throttle: Emit at most once per interval
val batterySignal = batteryProvider
    .throttleTime(10.seconds)  // Max 1 emission per 10s
    .distinctUntilChanged()    // Skip if same value

// Debounce: Wait for stability before emitting
val networkSignal = networkProvider
    .debounce(200.millis)      // Wait 200ms for stability
    .distinctUntilChanged()    // Skip if same value
```

### Phase 4: Android Compliance

**Doze Mode Compliance**:
- ✅ No wakelocks
- ✅ Listeners unregistered on pause
- ✅ Updates throttled to battery-safe intervals
- ✅ No aggressive background polling

**Background Execution Limits** (Android 12+):
- ✅ Signal updates only when app is foreground
- ✅ No persistent background service solely for signals
- ✅ Listeners auto-unregister when app paused
- ✅ No WorkManager jobs for signal updates (too frequent)

**Foreground vs Background**:
- Foreground: All signals updated normally (high frequency)
- Background: Minimal updates, listeners mostly unregistered
- App Pause: ALL listeners unregistered, zero updates

---

## Implementation Plan

### File Structure

```
system/signals/
├── Signal.kt                          # Interface definitions
├── impl/
│   ├── SystemSignalsManager.kt        # Main lifecycle-aware manager
│   ├── providers/
│   │   ├── BatteryProvider.kt         # Battery + charging
│   │   ├── ScreenStateProvider.kt     # Screen on/off
│   │   ├── ForegroundAppProvider.kt   # Current foreground app
│   │   ├── NetworkProvider.kt         # Network connectivity
│   │   ├── TemperatureProvider.kt     # Device temperature
│   │   ├── TimeOfDayProvider.kt       # Time-based context
│   │   └── UsageIntensityProvider.kt  # Calculated intensity
│   ├── aggregators/
│   │   └── DeviceContextAggregator.kt # Merge signals to context
│   └── SignalCollectorImpl.kt          # Legacy interface impl
└── README.md                          # Signal integration guide
```

### Key Implementation Points

**1. SystemSignalsManager - Main Entry Point**

```kotlin
class SystemSignalsManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : SignalCollector {
    
    // Track active listeners (for debugging)
    private val activeListeners = mutableSetOf<String>()
    
    // Mutex for thread-safe listener management
    private val listenerMutex = Mutex()
    
    // All signal providers
    private val batteryProvider = BatteryProvider(context)
    private val screenProvider = ScreenStateProvider(context)
    private val networkProvider = NetworkProvider(context)
    // ... etc
    
    // Output: merged device context
    private val _deviceContext = MutableStateFlow<DeviceContext>(...)
    override val deviceContext: StateFlow<DeviceContext> = ...
    
    init {
        // Bind to lifecycle
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }
    
    private val lifecycleObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Register all listeners
                }
                Lifecycle.Event.ON_STOP -> {
                    // Unregister all listeners - critical for cleanup
                }
                else -> {}
            }
        }
    }
}
```

**2. Each Provider - Lifecycle-Safe**

```kotlin
// Pattern for all providers
class BatteryProvider(private val context: Context) {
    private val _value = MutableStateFlow<Float>(getCurrentBattery())
    val value: StateFlow<Float> = _value.asStateFlow()
    
    private var receiver: BroadcastReceiver? = null
    
    fun register() {
        if (receiver != null) return  // Already registered
        
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                updateBattery()
            }
        }
        
        try {
            context.registerReceiver(
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                Context.RECEIVER_NOT_EXPORTED  // Android 12+
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to register battery receiver")
            receiver = null
        }
    }
    
    fun unregister() {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Already unregistered, safe to ignore
            }
            receiver = null
        }
    }
}
```

**3. Throttling & Aggregation**

```kotlin
// In SystemSignalsManager
private suspend fun aggregateSignals() {
    // Combine all signal flows with appropriate throttling
    combine(
        batteryProvider.value.throttleTime(10.seconds),
        screenProvider.value.debounce(100.millis),
        networkProvider.value.debounce(200.millis),
        // ... others
    ) { battery, screen, network, ... ->
        DeviceContextImpl(
            batteryLevel = battery,
            isScreenOn = screen,
            isNetworkConnected = network,
            // ... others
            timestamp = System.currentTimeMillis()
        )
    }
    .distinctUntilChanged()  // Skip duplicate contexts
    .collect { context ->
        _deviceContext.emit(context)
    }
}
```

---

## Safety Guarantees

### Memory Leak Prevention

- ✅ **No global receivers**: All receivers managed by lifecycle
- ✅ **Automatic cleanup**: Unregister in lifecycle.onStop
- ✅ **Try-catch protection**: Unregister failures don't crash
- ✅ **Listener tracking**: Debug log which listeners are active

### Lifecycle Safety

- ✅ **Binding to LifecycleOwner**: Automatic with lifecycle
- ✅ **No update after stop**: Listeners unregistered on STOP
- ✅ **Resume support**: Listeners re-register on START
- ✅ **Destroy cleanup**: All resources cleaned up

### Battery Efficiency

- ✅ **Throttling**: Max update frequency per signal type
- ✅ **Event-driven**: Screen/network use intent broadcasts
- ✅ **No wakelocks**: All updates use standard listeners
- ✅ **Background muting**: Minimal updates when app paused

### Android Compliance

- ✅ **No Doze violations**: Listeners unregistered in background
- ✅ **Foreground signal**: Updates work normally in foreground
- ✅ **Background limits**: App can be paused without crashes
- ✅ **Android 12+ ready**: RECEIVER_NOT_EXPORTED flags

---

## Testing Strategy

### Unit Tests
- [ ] Listener registration/unregistration
- [ ] Signal value updates
- [ ] Throttling/debouncing behavior
- [ ] Error handling in registration failures

### Integration Tests
- [ ] Lifecycle binding with Activity
- [ ] Cleanup on destroy
- [ ] Resume functionality after pause
- [ ] No memory leaks (profile with profiler)

### Manual Testing
- [ ] Toggle app foreground/background
- [ ] Monitor logcat for receiver errors
- [ ] Check battery impact with battery profiler
- [ ] Verify signals update correctly

---

## Success Criteria

✅ **All signals properly registered/unregistered**  
✅ **Zero memory leaks in lifecycle**  
✅ **Proper error handling for listener failures**  
✅ **Throttling reduces unnecessary updates**  
✅ **Battery impact < 2% in profiler**  
✅ **Android background execution compliance**  
✅ **Comprehensive documentation**  
✅ **All code committed and tested**

---

## Next Steps

1. Implement SystemSignalsManager with lifecycle binding
2. Create all signal providers with safe registration
3. Add throttling and aggregation logic
4. Document signal lifecycle and integration
5. Update README with system perception guide
6. Commit incrementally with clear messages
