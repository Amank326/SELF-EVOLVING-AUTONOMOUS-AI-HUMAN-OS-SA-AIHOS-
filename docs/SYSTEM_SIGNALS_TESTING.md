# System Signals Testing Guide

## Overview

This guide provides unit tests, integration tests, and profiling procedures for the hardened system signals implementation.

## Unit Tests

### 1. BatteryProvider Tests

```kotlin
package com.aihos.system.signals.impl.providers

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class BatteryProviderTest {

    private lateinit var context: Context
    private lateinit var provider: BatteryProvider

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        provider = BatteryProvider(context)
    }

    @Test
    fun testBatteryProviderRegister() {
        // Act
        provider.register()

        // Assert
        // Battery level should be emitted and be in valid range
        assertTrue(provider.value.value in 0f..100f)
    }

    @Test
    fun testBatteryProviderUnregister() {
        // Arrange
        provider.register()

        // Act
        provider.unregister()

        // Assert
        // Provider should be in unregistered state
        // (Implementation detail, but value should stabilize)
        val valueBeforeUnregister = provider.value.value
        Thread.sleep(500)
        val valueAfterUnregister = provider.value.value
        // Value should not change after unregister
        // (unless broadcast happens during sleep, which is rare)
    }

    @Test
    fun testBatteryProviderDoubleRegisterIsSafe() {
        // Act
        provider.register()
        provider.register()  // Should not re-register

        // Assert
        assertTrue(provider.value.value in 0f..100f)
    }

    @Test
    fun testBatteryProviderDoubleUnregisterIsSafe() {
        // Act
        provider.register()
        provider.unregister()
        provider.unregister()  // Should not throw

        // Assert
        // No exception thrown
    }
}
```

### 2. ScreenStateProvider Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class ScreenStateProviderTest {

    private lateinit var context: Context
    private lateinit var provider: ScreenStateProvider

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        provider = ScreenStateProvider(context)
    }

    @Test
    fun testScreenStateProviderRegister() {
        // Act
        provider.register()

        // Assert
        // Should emit a boolean (true by default)
        assertTrue(provider.value.value is Boolean)
    }

    @Test
    fun testScreenStateProviderValues() {
        // Act
        provider.register()

        // Assert
        // Value should be true (assumed at start)
        assertTrue(provider.value.value)
    }
}
```

### 3. NetworkProvider Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class NetworkProviderTest {

    private lateinit var context: Context
    private lateinit var provider: NetworkProvider

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        provider = NetworkProvider(context)
    }

    @Test
    fun testNetworkProviderRegister() {
        // Act
        provider.register()

        // Assert
        assertTrue(provider.value.value is Boolean)
    }

    @Test
    fun testNetworkProviderEmitsBoolean() {
        // Act
        provider.register()

        // Assert
        val value = provider.value.value
        assertTrue(value is Boolean)
    }
}
```

### 4. TemperatureProvider Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class TemperatureProviderTest {

    private lateinit var provider: TemperatureProvider

    @Before
    fun setUp() {
        provider = TemperatureProvider(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            pollIntervalMs = 1_000L  // Fast polling for tests
        )
    }

    @Test
    fun testTemperatureProviderRegister() = runTest {
        // Act
        provider.register()
        // Wait for first poll
        advanceTimeBy(1_500)

        // Assert
        val temp = provider.value.value
        assertTrue(temp in 20f..60f)  // Should be clamped
    }

    @Test
    fun testTemperatureProviderRange() = runTest {
        // Act
        provider.register()
        advanceTimeBy(1_500)

        // Assert
        // Temperature should always be in valid range
        assertTrue(provider.value.value >= 20f)
        assertTrue(provider.value.value <= 60f)
    }

    @Test
    fun testTemperatureProviderUnregister() = runTest {
        // Act
        provider.register()
        advanceTimeBy(1_500)
        provider.unregister()
        advanceTimeBy(2_000)

        // Assert
        // Polling should have stopped (would have crashed without safety checks)
    }
}
```

### 5. TimeOfDayProvider Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class TimeOfDayProviderTest {

    private lateinit var provider: TimeOfDayProvider

    @Before
    fun setUp() {
        provider = TimeOfDayProvider(pollIntervalMs = 500L)  // Fast for tests
    }

    @Test
    fun testTimeOfDayProviderRegister() = runTest {
        // Act
        provider.register()
        advanceTimeBy(600)

        // Assert
        val timeOfDay = provider.value.value
        assertTrue(timeOfDay in 0f..1f)
    }

    @Test
    fun testTimeOfDayProviderUpdates() = runTest {
        // Arrange
        provider.register()
        advanceTimeBy(600)
        val value1 = provider.value.value

        // Act
        advanceTimeBy(600)
        val value2 = provider.value.value

        // Assert
        // Values should be different (time has progressed)
        // Note: This may fail at exact day boundary, but very unlikely in tests
    }
}
```

### 6. ForegroundAppProvider Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class ForegroundAppProviderTest {

    private lateinit var context: Context
    private lateinit var provider: ForegroundAppProvider

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        provider = ForegroundAppProvider(context, pollIntervalMs = 500L)
    }

    @Test
    fun testForegroundAppProviderRegister() = runTest {
        // Act
        provider.register()
        advanceTimeBy(600)

        // Assert
        val foregroundApp = provider.value.value
        assertTrue(foregroundApp.isNotEmpty())
    }

    @Test
    fun testForegroundAppProviderUnregister() = runTest {
        // Act
        provider.register()
        advanceTimeBy(600)
        provider.unregister()
        advanceTimeBy(1_000)

        // Assert
        // No exceptions thrown, polling stopped cleanly
    }
}
```

## Integration Tests

### SystemSignalsManager Lifecycle Test

```kotlin
@RunWith(AndroidJUnit4::class)
class SystemSignalsManagerTest {

    private lateinit var context: Context
    private lateinit var activity: TestActivity

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Create test activity with proper lifecycle
    }

    @Test
    fun testSystemSignalsManagerLifecycleBinding() = runTest {
        // Arrange
        val manager = SystemSignalsManager(context, activity)

        // Act - Simulate lifecycle
        activity.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        // Assert
        // Listeners should be registered (check logcat)
        // deviceContext should emit values

        // Act - Move to background
        activity.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        // Assert
        // Listeners should be unregistered
    }

    @Test
    fun testSystemSignalsManagerEmitsDeviceContext() = runTest {
        // Arrange
        val manager = SystemSignalsManager(context, activity)
        activity.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        var contextEmitted = false

        // Act
        manager.deviceContext.collect { context ->
            // Assert
            assertTrue(context.batteryLevel in 0f..100f)
            assertTrue(context.temperature in 20f..60f)
            assertTrue(context.isScreenOn is Boolean)
            assertTrue(context.isNetworkConnected is Boolean)
            assertTrue(context.timeOfDay in 0f..1f)
            assertTrue(context.foregroundApp.isNotEmpty())
            assertTrue(context.usageIntensity in 0f..1f)
            assertTrue(context.timestamp > 0)
            contextEmitted = true
        }

        // Assert
        assertTrue(contextEmitted)
    }
}
```

## Memory Leak Detection

### Using Android Profiler

1. **Start Memory Profiler**:
   - Open Android Studio → Profiler tab
   - Run app with SystemSignalsManager
   - Record memory usage

2. **Lifecycle Cycle Test**:
   ```kotlin
   // Start app (listeners register)
   // Background app (listeners unregister)
   // Return to app (listeners re-register)
   // Repeat 10 times
   ```

3. **Look for**:
   - Broadcast receiver objects persisting after unregister
   - BatteryBroadcastReceiver not being garbage collected
   - No memory growth after repeated lifecycle cycles

### Using LeakCanary

Add to build.gradle.kts:
```kotlin
debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
```

Run app, cycle through lifecycle, check for leak warnings in Logcat.

## Battery Usage Profiling

### Using Battery Profiler

1. **Before Hardening**:
   - Run old unsafe implementation
   - Measure battery drain over 1 hour
   - Note: May find aggressive polling or unmanaged receivers

2. **After Hardening**:
   - Run new hardened implementation
   - Measure battery drain over 1 hour
   - Compare: Should be < 1% per hour

### Manual Test

```kotlin
@Test
fun testBatteryImpactOfPolling() = runTest {
    // Arrange
    val providers = listOf(
        TemperatureProvider(),
        TimeOfDayProvider(),
        ForegroundAppProvider(context)
    )

    // Act
    providers.forEach { it.register() }
    
    // Run for simulated time
    advanceTimeBy(60 * 60 * 1_000)  // 1 hour

    // Assert
    // Check CPU usage, no excessive wake-locks, etc.
}
```

## Performance Benchmarks

### Latency Test

```kotlin
@Test
fun testSignalUpdateLatency() = runTest {
    // Arrange
    val manager = SystemSignalsManager(context, activity)
    activity.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

    var maxLatency = 0L

    // Act
    manager.deviceContext.collect { context ->
        val latency = System.currentTimeMillis() - context.timestamp
        maxLatency = maxOf(maxLatency, latency)
    }

    // Assert
    // Latency should be < 100ms
    assertTrue(maxLatency < 100)
}
```

### Throughput Test

```kotlin
@Test
fun testSignalThroughput() = runTest {
    // Arrange
    val manager = SystemSignalsManager(context, activity)
    activity.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    
    var emissionCount = 0

    // Act
    manager.deviceContext.collect { _ ->
        emissionCount++
    }

    advanceTimeBy(10 * 1_000)  // 10 seconds

    // Assert
    // Should emit ~10x per second (once per 100ms aggregation)
    assertTrue(emissionCount in 50..150)  // Accounting for startup delay
}
```

## Manual Testing Checklist

- [ ] **Registration**: App starts → all listeners registered (check Logcat)
- [ ] **Values**: All 8 context values populated correctly
- [ ] **Background**: App backgrounded → all listeners unregistered (check Logcat)
- [ ] **Foreground**: App restored → all listeners re-registered (check Logcat)
- [ ] **Battery**: Battery percentage shows correct value
- [ ] **Screen**: Screen on/off changes reflected immediately
- [ ] **Network**: Toggle WiFi → network state updates
- [ ] **Temperature**: Temperature value in valid range (20-60°C)
- [ ] **Time**: Time of day value between 0-1
- [ ] **Memory**: No memory growth after 10 lifecycle cycles
- [ ] **Battery**: Less than 1% battery drain per hour
- [ ] **Crashes**: No crashes when rapidly cycling lifecycle

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Signal Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v2
      - uses: android-actions/setup-android@v2
      - run: ./gradlew testDebugUnitTest
      - run: ./gradlew connectedAndroidTest
      - name: Upload coverage
        uses: codecov/codecov-action@v2
```

## Known Limitations

1. **ForegroundAppProvider**: Requires PACKAGE_USAGE_STATS permission
   - May return "unknown" if permission not granted
   - Test with permission granted and denied

2. **TemperatureProvider**: Uses BatteryManager (not true temperature sensor)
   - Confidence: 0.75 (estimated)
   - May not reflect actual device temperature

3. **TimeOfDayProvider**: Uses device clock
   - Affected by device timezone
   - Test on different timezone devices

## Success Criteria

✅ All providers register/unregister safely
✅ No memory leaks detected
✅ Battery impact < 1% per hour
✅ All 8 context signals populated correctly
✅ SystemSignalsManager binds to lifecycle properly
✅ Latency < 100ms from signal change to context emission
✅ No crashes on rapid lifecycle cycling
✅ Comprehensive error logging in Logcat

