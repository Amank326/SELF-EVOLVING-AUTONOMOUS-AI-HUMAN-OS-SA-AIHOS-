# TensorFlow Lite ML Integration - Complete Developer Guide

**Prepared by**: Senior On-Device ML Engineer  
**Date**: January 24, 2026  
**Audience**: Android developers integrating ML with SA-AIHOS  

---

## 📚 Quick Navigation

**For different needs**:
- 🚀 **Getting Started**: See [5-Minute Setup](#5-minute-setup)
- 🏗️ **Understanding Architecture**: See [Architecture Overview](#architecture-overview)
- 💻 **Implementation Details**: See [Implementation Reference](#implementation-reference)
- 🧪 **Testing**: See [Testing Procedures](#testing-procedures)
- 🔧 **Troubleshooting**: See [Troubleshooting Guide](#troubleshooting)
- 📊 **Monitoring**: See [Production Monitoring](#production-monitoring)

---

## 🚀 5-Minute Setup

### Step 1: Add Dependencies (1 min)

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.0")
    
    // Kotlin Serialization (for inference results)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Coroutines (already in project)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### Step 2: Add Model Files (2 min)

1. Create `app/src/main/assets/models/` directory
2. Copy three TFLite model files:
   - `behavior_classifier_v1.tflite` (1.2 MB)
   - `priority_scorer_v1.tflite` (0.8 MB)
   - `memory_confidence_v1.tflite` (0.6 MB)

### Step 3: Configure Hilt Module (1 min)

Create `app/src/main/kotlin/com/aihos/di/MLModule.kt`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object MLModule {
    
    @Provides
    @Singleton
    fun provideMLInterpreterManager(
        @ApplicationContext context: Context
    ): MLInterpreterManager {
        // Note: LifecycleOwner obtained from Activity
        // For now, create with app context
        return MLInterpreterManagerImpl(context, context as LifecycleOwner)
    }
    
    @Provides
    @Singleton
    fun provideMLConfidenceAugmenter(
        manager: MLInterpreterManager
    ): MLConfidenceAugmenter {
        return DefaultMLConfidenceAugmenter(manager)
    }
}
```

### Step 4: Update ReasoningEngine (1 min)

In your ViewModel or Activity:

```kotlin
// Before: Using base reasoning
val reasoning = HeuristicReasoningEngine()

// After: Using ML-enhanced reasoning
@Inject
lateinit var mlManager: MLInterpreterManager

@Inject
lateinit var mlAugmenter: MLConfidenceAugmenter

private val reasoning = MLEnhancedReasoningEngine(
    baseReasoner = HeuristicReasoningEngine(),
    mlManager = mlManager,
    mlAugmenter = mlAugmenter
)
```

**That's it!** ML inference is now active with automatic fallback.

---

## 🏗️ Architecture Overview

### Three-Layer ML Integration

```
Layer 3: AI Cognition
┌─────────────────────────────────────┐
│ ReasoningEngine                     │
│ - generateOptions()                 │
│ - scoreOption() ← ML ENHANCEMENT    │
│ - explainDecision() ← DUAL PATH    │
└─────────────────────────────────────┘
                ↑
                │ uses
                ↓
Layer 2: ML Augmentation
┌─────────────────────────────────────┐
│ MLConfidenceAugmenter               │
│ - augmentConfidence()               │
│ - explainAugmentation()             │
└─────────────────────────────────────┘
                ↑
                │ uses
                ↓
Layer 1: ML Core
┌─────────────────────────────────────┐
│ MLInterpreterManager                │
│ - Lifecycle-aware (ON_START/STOP)   │
│ - Thread-safe async inference       │
│ - Metrics tracking                  │
├─────────────────────────────────────┤
│ TFLiteInterpreterProvider (×3)      │
│ - Behavior Classifier               │
│ - Priority Scorer                   │
│ - Memory Confidence                 │
├─────────────────────────────────────┤
│ SignalInputFormatter                │
│ - DeviceContext → Tensor normalization
├─────────────────────────────────────┤
│ InferenceThrottler                  │
│ - Rate-limiting (2 Hz max)          │
│ - Result caching                    │
└─────────────────────────────────────┘
```

### Data Flow in Reasoning

```
ReasoningContext (from app state)
         ↓
    [Rule Engine]
         ↓
     Option + Confidence (e.g., 0.75)
         ↓
  [ML Augmentation Layer]
         ↓
   DeviceContext
         ↓
  MLInterpreterManager
         ↓
  TFLite Inference
         ↓
  Adjustment (-0.3 to +0.3)
         ↓
  Final Confidence (e.g., 0.71)
         ↓
    [Execution]
         ↓
  Decision Record (stores both paths)
```

---

## 💻 Implementation Reference

### 1. ML Interpreter Manager

**What it does**: Coordinates interpreter lifecycle, manages async inference, tracks metrics.

**Key methods**:

```kotlin
// Initialize on Activity.ON_START
lifecycleOwner.lifecycle.addObserver(mlManager)

// Async inference
val adjustment = mlManager.inferConfidenceAdjustment(
    context = deviceContext,
    modelType = MLModelType.BEHAVIOR_CLASSIFIER
)

// Check status
val status = mlManager.getStatus()  // READY, ERROR, etc.

// Get metrics
val metrics = mlManager.getMetrics()
println("Executed: ${metrics.inferencesExecuted}")
println("Latency: ${metrics.averageLatencyMs}ms")
```

**Thread safety**:
- Safe to call from any thread
- Dispatcher.Default used for inference
- Mutex protects interpreter state

### 2. TFLite Interpreter Provider

**What it does**: Encapsulates single model's inference execution.

**Key methods**:

```kotlin
// Initialize (called automatically by manager)
val success = provider.initialize()

// Execute inference
val output = provider.infer(
    input = floatArrayOf(0.5f, 1.0f, ...), // 8 values
    timeoutMs = 100L
)

// Cleanup (called automatically by manager)
provider.cleanup()
```

**Input/output**:
- Input: 8-feature float array (DeviceContext features)
- Output: Variable size based on model (logits, probabilities)

### 3. Signal Input Formatter

**What it does**: Converts DeviceContext to normalized tensor input.

**Features normalized**:

```
1. Battery     0-100%        → [0, 1]
2. Screen      on/off        → {0, 1}
3. Network     connected/not → {0, 1}
4. Temperature 20-60°C       → [0, 1]
5. TimeOfDay   0-24h         → [0, 1]
6. Foreground App           → [0, 1] (category match)
7. Idle Time   0-3600s      → [0, 1]
8. Recent Decisions 0-20    → [0, 1]
```

**Usage**:

```kotlin
val formatter = DefaultSignalInputFormatter()
val input = formatter.formatInput(deviceContext)
// Returns: FloatArray(8) with all values in [0, 1]
```

### 4. Inference Throttler

**What it does**: Prevents excessive inference calls, caches results.

**Configuration**:

```kotlin
// Max 2 inferences per second (500ms minimum interval)
val throttler = DefaultInferenceThrottler(targetFrequencyHz = 2)

// Also caches if inputs are identical
val result = throttler.throttledInfer(deviceContext) {
    // This block only executes if time elapsed or inputs changed
    executeExpensiveInference()
}
```

**Impact**:
- Reduces inference frequency from potentially 60+ Hz to 2 Hz
- Saves ~97% of inference cycles via caching
- Negligible impact on decision quality

### 5. ML Confidence Augmenter

**What it does**: Combines rule and ML confidence into final score.

**Formula**:

```
Rule Confidence:      0.75 (from ReasoningEngine)
ML Adjustment:       -0.15 (from TFLite models)
                     ────────
Multiplier:      1 + (-0.15 / 3) = 0.95
Final Confidence: 0.75 × 0.95 = 0.71
```

**Usage**:

```kotlin
val augmented = mlAugmenter.augmentConfidence(
    ruleConfidence = 0.75f,
    context = deviceContext
)

// Returns:
// AugmentedConfidence(
//   ruleConfidence = 0.75f,
//   mlAdjustment = -0.15f,
//   finalConfidence = 0.71f,
//   mlAvailable = true,
//   mlExplanation = "..."
// )
```

---

## 🧪 Testing Procedures

### Unit Testing Template

```kotlin
@RunWith(AndroidJUnit4::class)
class MyMLTest {
    
    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()
    
    private lateinit var context: Context
    private lateinit var mlManager: MLInterpreterManager
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        mlManager = MLInterpreterManagerImpl(context, TestLifecycleOwner())
    }
    
    @Test
    fun testInferenceReturnsValidRange() = runBlocking {
        val adjustment = mlManager.inferConfidenceAdjustment(
            context = DeviceContext(...),
            modelType = MLModelType.BEHAVIOR_CLASSIFIER
        )
        
        assertTrue(adjustment >= -0.3f)
        assertTrue(adjustment <= 0.3f)
    }
}
```

### Manual Testing Checklist

- [ ] Model files present in assets/models/
- [ ] First inference completes < 500ms
- [ ] Subsequent inferences cached properly
- [ ] No ANR on main thread
- [ ] Both rule and ML explanations in output
- [ ] Fallback works if model missing
- [ ] Memory usage stable after 1000+ inferences
- [ ] Battery impact imperceptible (< 0.5% per hour)

---

## 🔧 Troubleshooting

### Problem: "Cannot find symbol: MLInterpreterManager"

**Cause**: Import missing or class not found  
**Solution**:
```kotlin
import com.aihos.ai.ml.MLInterpreterManager
import com.aihos.ai.ml.impl.MLInterpreterManagerImpl
```

### Problem: "Model file not found in assets"

**Cause**: File not copied to correct location  
**Solution**:
1. Run: `mkdir -p app/src/main/assets/models`
2. Copy model files there
3. Rebuild project (`Build → Clean Project`, then `Run`)

### Problem: "Inference returns null"

**Cause**: Model incompatibility or interpreter crash  
**Solution**:
1. Check model format is TFLite (`.tflite` extension)
2. Verify model input/output shapes match ModelMetadata
3. Check all input values normalized to [0, 1]
4. Review logcat: `adb logcat | grep "com.aihos"`

### Problem: "High latency (> 100ms)"

**Cause**: Model complex or NNAPI not available  
**Solution**:
```kotlin
// Option 1: Use quantized model (faster)
// Replace .tflite file with _quantized.tflite version

// Option 2: Increase throttle interval
val throttler = DefaultInferenceThrottler(targetFrequencyHz = 1)

// Option 3: Check NNAPI availability
adb shell getprop ro.hardware.keystore
```

### Problem: "ML impact on battery 2%+"

**Cause**: Inference frequency too high or inefficient model  
**Solution**:
```kotlin
// Reduce inference frequency
val throttler = DefaultInferenceThrottler(targetFrequencyHz = 1)

// Or disable ML entirely
val reasoning = HeuristicReasoningEngine()  // Fall back to rules
```

---

## 📊 Production Monitoring

### Metrics to Track

```kotlin
// Add to analytics
analyticsProvider.logMLMetrics(
    executedInferences = metrics.inferencesExecuted,
    cachedInferences = metrics.inferencesFromCache,
    failedInferences = metrics.inferencesFailedTotal,
    averageLatencyMs = metrics.averageLatencyMs,
    peakMemoryMb = metrics.peakMemoryUsageMb
)

// Monitor decision confidence distribution
analyticsProvider.logDecisionConfidence(
    ruleConfidence = 0.75f,
    mlAdjustment = -0.15f,
    finalConfidence = 0.71f,
    mlAvailable = true
)
```

### Alerts to Set

```
IF ml.failureRate > 0.05:
    ALERT("ML inference failing frequently")
    
IF ml.avgLatencyMs > 100:
    ALERT("ML inference slower than expected")
    
IF ml.memoryUsageMb > 100:
    ALERT("ML memory usage abnormal")
    
IF battery.impact > 0.005 per_hour:
    ALERT("ML battery impact exceeds budget")
```

### Debugging Commands

```bash
# View ML logs
adb logcat | grep "MLInterpreter\|MLManager\|MLEnhanced"

# Profile memory
adb shell dumpsys meminfo com.aihos | grep -A 10 "TOTAL"

# Check inference execution trace
adb logcat | grep "Executed new inference\|Using cached"
```

---

## ✅ Acceptance Criteria for Integration

Before deploying, verify:

- ✅ Code compiles with zero errors
- ✅ All unit tests passing
- ✅ Inference latency P99 < 100ms
- ✅ No memory leaks detected
- ✅ Battery impact < 0.5% per hour
- ✅ Graceful fallback verified
- ✅ Both rule and ML explanations present
- ✅ No ANRs in stress testing
- ✅ Monitoring in place

---

## 📖 Further Reading

**Design Deep Dive**: [TFLITE_HARDENING_DESIGN.md](TFLITE_HARDENING_DESIGN.md)  
**Testing Guide**: [TFLITE_TESTING_INTEGRATION.md](TFLITE_TESTING_INTEGRATION.md)  
**API Reference**: See comments in MLInterfaces.kt  

---

## 🎯 Key Takeaways

1. **ML enhances, doesn't replace**: Rule-based reasoning is primary
2. **Bounded influence**: ML adjustment limited to ±0.3 confidence points
3. **Transparent reasoning**: Both paths explained in decision output
4. **Graceful degradation**: Works perfectly without ML
5. **Efficient execution**: Throttled to 2 Hz, cached when possible
6. **Safe lifecycle**: Automatic init/cleanup with Activity lifecycle

---

**ML Integration complete! Your AI system now has data-driven confidence augmentation.**

For support, reference the design document or review unit test examples.
