# TensorFlow Lite ML Integration - Testing & Integration Guide

**Prepared by**: Senior On-Device ML Engineer  
**Date**: January 24, 2026  
**Scope**: Comprehensive testing, verification, and integration procedures  

---

## 📋 Table of Contents

1. [Unit Testing Guide](#unit-testing)
2. [Integration Testing](#integration-testing)
3. [Performance Testing](#performance-testing)
4. [Memory Profiling](#memory-profiling)
5. [Battery Impact Analysis](#battery-impact)
6. [Integration with Existing Layers](#integration)
7. [Troubleshooting](#troubleshooting)
8. [Deployment Checklist](#deployment)

---

## 🧪 Unit Testing

### 1. MLInterpreterManager Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class MLInterpreterManagerTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var context: Context
    private lateinit var manager: MLInterpreterManagerImpl
    private lateinit var lifecycleOwner: LifecycleOwner
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Create lifecycle owner for testing
        lifecycleOwner = TestLifecycleOwner()
    }
    
    @After
    fun tearDown() {
        runBlocking {
            manager.getStatus() // Ensure cleanup completed
        }
    }
    
    @Test
    fun testInitializationOnLifecycleStart() = runBlocking {
        manager = MLInterpreterManagerImpl(context, lifecycleOwner)
        
        assertEquals(MLManagerStatus.UNINITIALIZED, manager.getStatus())
        
        // Trigger ON_START
        lifecycleOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        delay(500)  // Wait for async initialization
        
        assertEquals(MLManagerStatus.READY, manager.getStatus())
        assertTrue(manager.isAvailable())
    }
    
    @Test
    fun testInferenceReturnsValidRange() = runBlocking {
        manager = MLInterpreterManagerImpl(context, lifecycleOwner)
        lifecycleOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        delay(500)
        
        val context = DeviceContext(
            batteryPercent = 75,
            screenOn = true,
            networkConnected = true,
            temperatureCelsius = 35f,
            timeOfDay = 0.5f,
            foregroundApp = "messaging",
            idleTimeSeconds = 30,
            recentDecisionCount = 5
        )
        
        val adjustment = manager.inferConfidenceAdjustment(context, MLModelType.BEHAVIOR_CLASSIFIER)
        
        // Verify adjustment is bounded
        assertTrue(adjustment >= -0.3f)
        assertTrue(adjustment <= 0.3f)
    }
    
    @Test
    fun testGracefulFallbackOnMLUnavailable() = runBlocking {
        // Create manager without interpreters
        manager = MLInterpreterManagerImpl(
            context,
            lifecycleOwner,
            interpreterProviders = emptyMap()
        )
        lifecycleOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        delay(500)
        
        val context = DeviceContext(
            batteryPercent = 75, screenOn = true, networkConnected = true,
            temperatureCelsius = 35f, timeOfDay = 0.5f,
            foregroundApp = "messaging", idleTimeSeconds = 30,
            recentDecisionCount = 5
        )
        
        val adjustment = manager.inferConfidenceAdjustment(context, MLModelType.BEHAVIOR_CLASSIFIER)
        
        // Should return neutral adjustment (0.0f) if ML unavailable
        assertEquals(0.0f, adjustment)
    }
    
    @Test
    fun testMetricsTracking() = runBlocking {
        manager = MLInterpreterManagerImpl(context, lifecycleOwner)
        lifecycleOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        delay(500)
        
        val context = DeviceContext(
            batteryPercent = 75, screenOn = true, networkConnected = true,
            temperatureCelsius = 35f, timeOfDay = 0.5f,
            foregroundApp = "messaging", idleTimeSeconds = 30,
            recentDecisionCount = 5
        )
        
        // Execute several inferences
        repeat(5) {
            manager.inferConfidenceAdjustment(context, MLModelType.BEHAVIOR_CLASSIFIER)
        }
        
        val metrics = manager.getMetrics()
        assertTrue(metrics.inferencesExecuted + metrics.inferencesFromCache >= 5)
    }
}
```

### 2. SignalInputFormatter Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class SignalInputFormatterTest {
    
    private val formatter = DefaultSignalInputFormatter()
    
    @Test
    fun testFormatInputNormalization() {
        val context = DeviceContext(
            batteryPercent = 50,
            screenOn = true,
            networkConnected = false,
            temperatureCelsius = 40f,
            timeOfDay = 0.5f,
            foregroundApp = "messaging",
            idleTimeSeconds = 1800,
            recentDecisionCount = 10
        )
        
        val input = formatter.formatInput(context)
        
        // Should have 8 values
        assertEquals(8, input.size)
        
        // All values should be in [0, 1] range
        for (value in input) {
            assertTrue(value >= 0f)
            assertTrue(value <= 1f)
        }
        
        // Verify specific mappings
        assertEquals(0.5f, input[0])  // Battery 50% → 0.5
        assertEquals(1f, input[1])    // Screen on → 1.0
        assertEquals(0f, input[2])    // Network off → 0.0
        assertEquals(0.5f, input[4])  // Time 0.5 → 0.5
    }
    
    @Test
    fun testParseOutputRange() {
        // Test softmax output (3 classes)
        val softmaxOutput = floatArrayOf(0.1f, 0.6f, 0.3f)
        val adjustment1 = formatter.parseOutput(softmaxOutput)
        assertTrue(adjustment1 >= -0.3f && adjustment1 <= 0.3f)
        
        // Test sigmoid output (binary)
        val sigmoidOutput = floatArrayOf(0.8f)
        val adjustment2 = formatter.parseOutput(sigmoidOutput)
        assertTrue(adjustment2 >= -0.3f && adjustment2 <= 0.3f)
    }
}
```

### 3. InferenceThrottler Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class InferenceThrottlerTest {
    
    private val throttler = DefaultInferenceThrottler(targetFrequencyHz = 2)
    
    @Test
    fun testThrottlesConcurrentRequests() = runBlocking {
        val context = DeviceContext(
            batteryPercent = 75, screenOn = true, networkConnected = true,
            temperatureCelsius = 35f, timeOfDay = 0.5f,
            foregroundApp = "messaging", idleTimeSeconds = 30,
            recentDecisionCount = 5
        )
        
        val executionCount = AtomicInteger(0)
        
        // Submit rapid requests
        repeat(10) {
            throttler.throttledInfer(context) {
                executionCount.incrementAndGet()
                InferenceResult(confidence = 0.0f)
            }
            delay(10)
        }
        
        // Should have executed only subset due to throttling
        assertTrue(executionCount.get() <= 3)  // 500ms window at 2Hz
    }
    
    @Test
    fun testCachesIdenticalInputs() = runBlocking {
        val context1 = DeviceContext(
            batteryPercent = 75, screenOn = true, networkConnected = true,
            temperatureCelsius = 35f, timeOfDay = 0.5f,
            foregroundApp = "messaging", idleTimeSeconds = 30,
            recentDecisionCount = 5
        )
        
        var executionCount = 0
        
        // First execution
        throttler.throttledInfer(context1) {
            executionCount++
            InferenceResult(confidence = 0.0f)
        }
        
        // Immediate second request with same context
        val result2 = throttler.throttledInfer(context1) {
            executionCount++
            InferenceResult(confidence = 0.0f)
        }
        
        // Should use cache
        assertEquals(1, executionCount)
        assertTrue(result2.cached)
    }
}
```

### 4. MLConfidenceAugmenter Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class MLConfidenceAugmenterTest {
    
    @Test
    fun testAugmentationBounds() = runBlocking {
        val mockManager = mockk<MLInterpreterManager>()
        coEvery { mockManager.isAvailable() } returns true
        coEvery { 
            mockManager.inferConfidenceAdjustment(any(), any()) 
        } returns 0.2f
        
        val augmenter = DefaultMLConfidenceAugmenter(mockManager)
        
        val context = DeviceContext(
            batteryPercent = 75, screenOn = true, networkConnected = true,
            temperatureCelsius = 35f, timeOfDay = 0.5f,
            foregroundApp = "messaging", idleTimeSeconds = 30,
            recentDecisionCount = 5
        )
        
        val augmented = augmenter.augmentConfidence(0.7f, context)
        
        // Final confidence should be bounded [0, 1]
        assertTrue(augmented.finalConfidence >= 0f)
        assertTrue(augmented.finalConfidence <= 1f)
        
        // ML adjustment should be bounded [-0.3, 0.3]
        assertTrue(augmented.mlAdjustment >= -0.3f)
        assertTrue(augmented.mlAdjustment <= 0.3f)
    }
}
```

---

## 🔗 Integration Testing

### Full System Integration Test

```kotlin
@RunWith(AndroidJUnit4::class)
class MLReasoningIntegrationTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var context: Context
    private lateinit var lifecycleOwner: TestLifecycleOwner
    private lateinit var mlManager: MLInterpreterManager
    private lateinit var mlAugmenter: MLConfidenceAugmenter
    private lateinit var reasoning: MLEnhancedReasoningEngine
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        lifecycleOwner = TestLifecycleOwner()
        
        mlManager = MLInterpreterManagerImpl(context, lifecycleOwner)
        mlAugmenter = DefaultMLConfidenceAugmenter(mlManager)
        reasoning = MLEnhancedReasoningEngine(
            baseReasoner = HeuristicReasoningEngine(),
            mlManager = mlManager,
            mlAugmenter = mlAugmenter
        )
    }
    
    @Test
    fun testFullDecisionWithMLAugmentation() = runBlocking {
        // Initialize ML
        lifecycleOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        delay(500)
        
        // Create reasoning context
        val context = ReasoningContext(
            currentTime = "14:30",
            dayOfWeek = "FRIDAY",
            appUsageDurationMinutes = 45,
            recentInteractionCount = 12,
            userIsFocused = true,
            batteryPercent = 75,
            isCharging = false
        )
        
        // Generate options
        val options = reasoning.generateOptions(context)
        assertTrue(options.isNotEmpty())
        
        // Score options
        val scores = options.map { option ->
            option to reasoning.scoreOption(option, context)
        }
        
        // Should have different scores
        val uniqueScores = scores.map { it.second }.distinct()
        assertTrue(uniqueScores.size > 1)
        
        // Get explanation
        val chosen = scores.maxByOrNull { it.second }!!.first
        val explanation = reasoning.explainDecision(chosen, options, context)
        
        // Should contain both rule and ML explanations
        assertTrue(explanation.contains("PRIMARY"))
        assertTrue(explanation.contains("SECONDARY"))
    }
}
```

---

## ⚡ Performance Testing

### Inference Latency Benchmarking

```kotlin
@RunWith(AndroidJUnit4::class)
class MLPerformanceTest {
    
    @Test
    fun benchmarkInferenceLatency() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = MLInterpreterManagerImpl(context, TestLifecycleOwner())
        
        val deviceContext = DeviceContext(
            batteryPercent = 75, screenOn = true, networkConnected = true,
            temperatureCelsius = 35f, timeOfDay = 0.5f,
            foregroundApp = "messaging", idleTimeSeconds = 30,
            recentDecisionCount = 5
        )
        
        val latencies = mutableListOf<Long>()
        
        // Warm up
        repeat(5) {
            manager.inferConfidenceAdjustment(deviceContext, MLModelType.BEHAVIOR_CLASSIFIER)
        }
        
        // Measure
        repeat(100) {
            val start = System.currentTimeMillis()
            manager.inferConfidenceAdjustment(deviceContext, MLModelType.BEHAVIOR_CLASSIFIER)
            val elapsed = System.currentTimeMillis() - start
            latencies.add(elapsed)
        }
        
        val p50 = latencies.sorted()[latencies.size / 2]
        val p95 = latencies.sorted()[(latencies.size * 0.95).toInt()]
        val p99 = latencies.sorted()[(latencies.size * 0.99).toInt()]
        
        Log.d("MLPerformance", "P50 latency: ${p50}ms")
        Log.d("MLPerformance", "P95 latency: ${p95}ms")
        Log.d("MLPerformance", "P99 latency: ${p99}ms")
        
        // P99 should be under 100ms
        assertTrue(p99 < 100)
    }
}
```

---

## 💾 Memory Profiling

### Memory Leak Detection

**Procedure**:
1. Open Android Profiler (View → Tool Windows → Profiler)
2. Select "Memory" tab
3. Run "MLInterpreterManagerTest.testInitializationOnLifecycleStart"
4. Trigger garbage collection (GC icon)
5. Observe native memory usage
6. Repeat test 10 times
7. If memory increases linearly, investigate leak

**Expected Behavior**:
- Initial allocation: ~50-80 MB (models loaded)
- After 10 iterations: No significant increase
- After GC: Memory returns to baseline

### Memory Usage Limits

```
Model Loading Phase:
├─ Model files: 1.2 + 0.8 + 0.6 = 2.6 MB (cached)
├─ Interpreters: 3 × ~5 MB = 15 MB
├─ Tensor buffers: 3 × ~1 MB = 3 MB
└─ Total: ~20-25 MB

Inference Phase:
├─ Input tensor: ~32 bytes (8 float32)
├─ Output tensor: ~32 bytes (1-5 float32)
└─ Workspace: ~10 MB (reused per inference)

Peak Memory: ~35 MB (acceptable)
Stable State: ~20 MB
```

---

## 🔋 Battery Impact Analysis

### Battery Profiler Procedure

1. Enable Device Battery Historian (Android 5.0+)
2. Connect device via USB
3. Run test app for 1 hour with ML enabled
4. Export battery statistics
5. Compare with baseline (no ML)

### Expected Battery Impact

```
Baseline (no ML):
├─ Screen on: ~15% per hour
├─ Screen off: ~2% per hour
└─ Idle: ~0.5% per hour

With ML (2 Hz inference):
├─ Screen on: ~15.4% per hour (+0.4% increase)
├─ Screen off: ~2.1% per hour (+0.1% increase)
└─ Idle: ~0.5% per hour (negligible)

Justification:
├─ 2 Hz = 2 inferences/second
├─ Each inference: ~50ms @ 200mW = 10mJ
├─ Per hour: 7200 inferences × 10mJ = 72J
├─ Battery capacity: ~15 Wh = 54000J
├─ Impact: 72J / 54000J = 0.13% (conservative estimate)
└─ Margin: <0.5% target safely met
```

---

## 🔌 Integration with Existing Layers

### Integrating with SAIHOSViewModel

```kotlin
// In SAIHOSViewModel.kt

@HiltViewModel
class SAIHOSViewModel @Inject constructor(
    private val reasoningEngine: ReasoningEngine,
    private val memoryLayer: MemoryLayer,
    private val evolutionEngine: EvolutionEngine,
    private val autonomyController: AutonomyController,
    // New: ML system
    @ApplicationContext private val context: Context,
    private val mlManager: MLInterpreterManager
) : ViewModel(), LifecycleEventObserver {
    
    // ... existing code ...
    
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        // ML manager lifecycle is handled automatically via LifecycleEventObserver
        // No additional code needed
    }
    
    /**
     * Make decision with ML augmentation
     */
    fun makeDecision(context: ReasoningContext) {
        viewModelScope.launch {
            try {
                // Reasoning engine now includes ML augmentation
                val options = reasoningEngine.generateOptions(context)
                
                val scores = options.map { option ->
                    option to reasoningEngine.scoreOption(option, context)
                }
                
                val (chosenOption, confidence) = scores.maxByOrNull { it.second }
                    ?: return@launch
                
                // Get explanation (includes both rule and ML paths)
                val explanation = reasoningEngine.explainDecision(
                    chosenOption, options, context
                )
                
                // Execute decision
                autonomyController.executeAction(chosenOption, confidence)
                
                // Store for learning
                memoryLayer.recordDecision(
                    chosenOption, confidence, explanation, success = true
                )
                
                // Evolve rules based on outcome
                evolutionEngine.analyzeFeedback(chosenOption, explanation)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in decision-making", e)
            }
        }
    }
}
```

### Setting up ML in Hilt Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object MLModule {
    
    @Provides
    @Singleton
    fun provideMLInterpreterManager(
        @ApplicationContext context: Context,
        @ActivityContext lifecycleOwner: LifecycleOwner
    ): MLInterpreterManager {
        return MLInterpreterManagerImpl(
            context = context,
            lifecycleOwner = lifecycleOwner,
            interpreterProviders = mapOf(
                MLModelType.BEHAVIOR_CLASSIFIER to TFLiteInterpreterProviderImpl(
                    context,
                    ModelMetadata(
                        type = "BEHAVIOR_CLASSIFIER",
                        version = "1.0",
                        filePath = "models/behavior_classifier_v1.tflite",
                        inputShape = listOf(1, 8),
                        outputSize = 3,
                        typicalInferenceTimeMs = 20
                    )
                ),
                MLModelType.PRIORITY_SCORER to TFLiteInterpreterProviderImpl(
                    context,
                    ModelMetadata(
                        type = "PRIORITY_SCORER",
                        version = "1.0",
                        filePath = "models/priority_scorer_v1.tflite",
                        inputShape = listOf(1, 8),
                        outputSize = 5,
                        typicalInferenceTimeMs = 15
                    )
                ),
                // ... additional models ...
            )
        )
    }
    
    @Provides
    @Singleton
    fun provideMLConfidenceAugmenter(
        manager: MLInterpreterManager
    ): MLConfidenceAugmenter {
        return DefaultMLConfidenceAugmenter(manager)
    }
    
    @Provides
    @Singleton
    fun provideReasoningEngine(
        mlManager: MLInterpreterManager,
        mlAugmenter: MLConfidenceAugmenter
    ): ReasoningEngine {
        return MLEnhancedReasoningEngine(
            baseReasoner = HeuristicReasoningEngine(),
            mlManager = mlManager,
            mlAugmenter = mlAugmenter
        )
    }
}
```

---

## 🐛 Troubleshooting

### Issue: "Model file not found"

**Cause**: Model not in assets directory  
**Solution**:
1. Add model files to `app/src/main/assets/models/`
2. Verify file paths in ModelMetadata match exactly
3. Check asset filePath: `"models/model_name_v1.tflite"`

### Issue: "Inference returning null repeatedly"

**Cause**: Interpreter crash or invalid input  
**Solution**:
1. Check input tensor shape matches model expectations
2. Verify all input values normalized to [0, 1]
3. Check model compatibility (TFLite version)
4. Review logcat for detailed error messages

### Issue: "High latency (>200ms) on inference"

**Cause**: Model too complex or NNAPI not available  
**Solution**:
1. Quantize model to INT8
2. Reduce input feature count
3. Enable NNAPI delegate if available
4. Use smaller model variant

### Issue: "Memory leak in Android Profiler"

**Cause**: Interpreter not closed properly  
**Solution**:
1. Ensure cleanup() called on ON_DESTROY
2. Check try-finally blocks exist
3. Verify no circular references
4. Use weakref for context if needed

### Issue: "ANR (Application Not Responding)"

**Cause**: Inference running on main thread  
**Solution**:
1. Always use `withContext(Dispatcher.Default)` for inference
2. Verify timeout is set to 100ms max
3. Check for blocking coroutine operations
4. Review main thread profile in Android Profiler

---

## ✅ Deployment Checklist

### Pre-Deployment Verification

- [ ] All unit tests passing
- [ ] Integration tests passing
- [ ] Performance benchmarks acceptable (P99 < 100ms)
- [ ] Memory profiling shows no leaks
- [ ] Battery impact < 0.5% per hour
- [ ] No ANRs during 1-hour stress test
- [ ] Graceful fallback to rule-based works
- [ ] Both models load successfully
- [ ] Inference throttling prevents excessive calls
- [ ] Lifecycle binding correct

### Model Deployment

- [ ] All three model files copied to assets/models/
- [ ] Model file sizes verified (< 5 MB each)
- [ ] Model versions documented
- [ ] ModelMetadata updated with correct paths
- [ ] NNAPI delegate enabled (optional but recommended)

### Production Monitoring

**Add to analytics**:
- ML inference execution count
- ML inference cache hit rate
- ML adjustment distribution
- ML confidence vs rule confidence
- ML inference failure rate
- Average inference latency

**Set alerts**:
- ML failure rate > 5%
- Average latency > 100ms
- Memory usage > 100 MB
- Battery impact > 1% per hour

---

## 📊 Success Criteria

**All of the following must be true for production release**:

✅ Code compiles with zero errors  
✅ All 20+ unit tests passing  
✅ Integration tests passing  
✅ P99 inference latency < 100ms  
✅ No memory leaks detected  
✅ Battery impact < 0.5% per hour  
✅ No ANRs in stress testing  
✅ Graceful fallback verified  
✅ Documentation complete  
✅ Code review approved  

---

**End of Testing & Integration Guide**

Next: Deploy to production with monitoring in place.
