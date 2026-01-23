# AI Motion Intelligence: Integration Guide

This guide shows how to integrate the AI Motion Intelligence system with your existing AI core and autonomy loop.

## 🔌 Quick Integration Checklist

- [ ] Add `AIMotionIntelligence.kt` to your AI package
- [ ] Add `AIStateBroadcaster.kt` to your AI package
- [ ] Add `Three3DAIBridge.kt` to your UI package
- [ ] Hook broadcaster into your autonomy loop
- [ ] Connect broadcaster to your 3D WebView
- [ ] Test AI state changes in 3D

---

## 📦 Step 1: Set Up Broadcaster in Your DI Module

### In your Hilt Module or DI setup:

```kotlin
import com.aihos.ai.motion.AIMotionController
import com.aihos.ai.motion.AIStateBroadcaster

@Module
@InstallIn(SingletonComponent::class)
object AIMotionModule {
    
    @Provides
    @Singleton
    fun provideAIMotionController(): AIMotionController {
        return AIMotionController()
    }
    
    @Provides
    @Singleton
    fun provideAIStateBroadcaster(
        autonomyController: AutonomyController,
        reflectionEngine: ReflectionEngine,
        motionController: AIMotionController
    ): AIStateBroadcaster {
        return AIStateBroadcaster(
            autonomyController = autonomyController,
            reflectionEngine = reflectionEngine,
            motionController = motionController
        )
    }
}
```

---

## 🎯 Step 2: Hook Into Autonomy Controller

### In your `AutonomyController.triggerDecisionCycle()` method:

```kotlin
override suspend fun triggerDecisionCycle(context: ReasoningContext): DecisionOutcome {
    // ... existing decision logic ...
    
    val decision = /* your decision */
    val outcome = /* execute decision */
    
    // NOTIFY MOTION INTELLIGENCE: Decision was made
    if (this::broadcaster.isInitialized) {
        broadcaster.onDecisionMade(decision)
    }
    
    return outcome
}
```

### When reporting outcome:

```kotlin
override suspend fun reportOutcome(
    decisionId: String, 
    outcome: Outcome, 
    feedback: String
) {
    // ... existing outcome logic ...
    
    // NOTIFY MOTION INTELLIGENCE: Decision succeeded or failed
    val isSuccess = outcome == Outcome.SUCCESS || outcome == Outcome.PARTIAL
    if (this::broadcaster.isInitialized) {
        broadcaster.onDecisionOutcome(isSuccess)
    }
}
```

### Inject broadcaster:

```kotlin
class DefaultAutonomyController(
    private val memoryRepository: MemoryRepository,
    private val reasoningEngine: ReasoningEngine,
    private val reflectionEngine: ReflectionEngine,
    private val evolutionEngine: EvolutionEngine,
    private val contextProvider: ContextProvider,
    private val actionExecutor: ActionExecutor,
    @Inject private val broadcaster: AIStateBroadcaster  // ← Add this
) : AutonomyController {
```

---

## 🧠 Step 3: Hook Into Evolution Events

### In your `EvolutionEngine` implementation:

```kotlin
override suspend fun learnFromFeedback(feedback: String, context: String): LearningResult {
    // ... learning logic ...
    val result = /* process feedback */
    
    // Get updated metrics
    val metrics = getEvolutionMetrics()
    
    // NOTIFY MOTION INTELLIGENCE: Evolution event occurred
    if (this::broadcaster.isInitialized) {
        broadcaster.onEvolutionMetricsUpdated(metrics)
    }
    
    return result
}
```

Inject broadcaster similarly or access through a global reference.

---

## 🌊 Step 4: Connect Broadcaster to WebView

### In your 3D Screen or Activity:

```kotlin
@Composable
fun Three3DVisualizationScreen(
    @Inject broadcaster: AIStateBroadcaster
) {
    var webView by remember { mutableStateOf<Three3DWebView?>(null) }
    var bridge by remember { mutableStateOf<Three3DAIBridge?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                Three3DWebView(context).apply {
                    webView = this
                    // Load your 3D scene HTML
                    loadUrl("file:///android_asset/3d-scene/index.html")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
    
    // Initialize bridge when both components ready
    LaunchedEffect(webView) {
        if (webView != null && bridge == null) {
            bridge = Three3DAIBridge(
                webView = webView!!,
                broadcaster = broadcaster
            ).also {
                // Start broadcasting AI state to 3D
                broadcaster.startBroadcasting()
            }
        }
    }
}
```

Or in an Activity:

```kotlin
class Three3DActivity : AppCompatActivity() {
    
    @Inject
    lateinit var broadcaster: AIStateBroadcaster
    
    private lateinit var webView: Three3DWebView
    private var bridge: Three3DAIBridge? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup DI
        (application as SAIHOSApplication).appComponent.inject(this)
        
        // Setup WebView
        webView = Three3DWebView(this)
        setContentView(webView)
        webView.loadUrl("file:///android_asset/3d-scene/index.html")
        
        // Connect broadcaster to WebView
        bridge = Three3DAIBridge(webView, broadcaster)
        
        // Start broadcasting
        broadcaster.startBroadcasting()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bridge?.dispose()
        broadcaster.stopBroadcasting()
    }
}
```

---

## 📊 Step 5: Provide Rich Metrics to Broadcaster

### Extract metrics from your AI systems and feed to broadcaster:

The broadcaster automatically extracts what it can, but you can enhance it by calling:

```kotlin
// When you have more specific metrics
broadcaster.onDecisionMade(
    DecisionRecord(
        id = decision.id,
        timestamp = System.currentTimeMillis(),
        context = context,
        chosenOption = selectedOption,
        allOptions = options,
        reasoning = reasoningText,
        confidenceLevel = 0.85f,  // Your confidence score
        // ... other fields
    )
)

broadcaster.onDecisionOutcome(isSuccess = true)  // or false
```

---

## 🎮 Step 6: Test the Integration

### In Android Emulator/Device:

1. Start the app with 3D visualization
2. Watch the 3D core as AI makes decisions
3. Observe state changes:
   - **IDLE**: Calm cyan, slow breathing
   - **THINKING**: Bright cyan, medium speed
   - **DELIBERATING**: Purple, fast rotation
   - **EVOLVING**: Green, morphing geometry
   - **UNCERTAIN**: Amber pulsing
   - **REFLECTING**: Blue, inward particles
   - **ERROR**: Red flashing

### In Browser (for testing without Android):

```html
<!-- In 3d-scene/index.html or your test page -->
<script>
  // Manually test different AI states
  async function testAIStates() {
    const testStates = [
      {
        primaryState: 'THINKING',
        confidence: { averageConfidence: 0.7 },
        processing: { cognitiveLoad: 0.5 },
      },
      {
        primaryState: 'EVOLVING',
        confidence: { averageConfidence: 0.9 },
        processing: { adaptationIntensity: 0.8 },
      },
      // ... more states ...
    ];
    
    for (const state of testStates) {
      window.SAIHOSSceneInstance.setAIMotionState(state);
      await new Promise(r => setTimeout(r, 3000)); // 3 seconds per state
    }
  }
</script>
```

---

## 🔌 Advanced: Custom State Extraction

If your AI system has different structure, customize the broadcaster:

```kotlin
class CustomAIStateBroadcaster(
    private val myCustomAI: MyAISystem,
    private val motionController: AIMotionController = AIMotionController()
) {
    
    // Override to extract metrics from your specific AI
    private suspend fun extractConfidenceMetrics(): ConfidenceMetrics {
        val myMetrics = myCustomAI.getMetrics()
        return AIMetricsBuilder.buildConfidenceMetrics(
            decisionConfidence = myMetrics.myConfidence,
            predictionConfidence = myMetrics.myPredictionScore,
            knowledgeConfidence = myMetrics.myKnowledgeLevel
        )
    }
    
    private suspend fun extractProcessingMetrics(): AIProcessingMetrics {
        val myMetrics = myCustomAI.getMetrics()
        return AIMetricsBuilder.buildProcessingMetrics(
            cognitiveLoad = myMetrics.processingIntensity,
            decisionComplexity = myMetrics.optionCount / 5f,
            uncertaintyLevel = myMetrics.uncertaintyScore,
            learningRate = myMetrics.learningRate,
            successRate = myMetrics.successRate,
            memoryLoad = myMetrics.activeMemoryCount / 100f,
            adaptationIntensity = myMetrics.ruleChangeRate
        )
    }
}
```

---

## 🎨 Step 7: Customize Visual Behavior (Optional)

### Adjust breathing rates:

```kotlin
// In AIMotionController.kt
private fun computeBreathingRate(state: AICognitiveState, cognitiveLoad: Float): Float {
    val baseRate = when (state) {
        AICognitiveState.IDLE -> 0.3f  // Slower for idle
        AICognitiveState.THINKING -> 1.2f  // Faster thinking
        // ... adjust to your preference
    }
    return baseRate + (cognitiveLoad * 0.5f)
}
```

### Change color themes:

```javascript
// In ProceduralAnimationController.js
_themeToColor(theme) {
    switch (theme) {
        case 'CYAN':
            return { r: 0.2, g: 1.0, b: 0.8 }; // Adjust color
        // ... etc
    }
}
```

### Adjust morphing intensity:

```kotlin
// More dramatic evolution morphing
private fun computeMorphingIntensity(state: AICognitiveState, adaptationRate: Float): Float {
    val baseIntensity = when (state) {
        AICognitiveState.EVOLVING -> 1.2f  // More morphing
        // ... etc
    }
    return (baseIntensity + adaptationRate * 0.9f).coerceIn(0f, 1f)
}
```

---

## 🐛 Troubleshooting

### AI state not reaching 3D:
1. Check that broadcaster is started: `broadcaster.startBroadcasting()`
2. Verify Three3DAIBridge is created and connected
3. Check Android logcat for errors: `adb logcat | grep "SAIHOSBridge\|Broadcaster"`

### 3D not responding to AI:
1. Verify `setAIMotionState()` is being called in Scene.js
2. Check that ProceduralAnimationController is registered
3. Ensure ComponentManager is applying frames

### Animations jittery:
1. Increase `smoothingFactor` in broadcaster (0.2 → 0.3)
2. Reduce broadcast frequency (100ms → 200ms)
3. Check deltaTime calculation in render loop

### Performance issues:
1. Reduce particle count
2. Disable geometry morphing for certain states
3. Lower bloom intensity
4. Reduce broadcast frequency

---

## 📈 Monitoring Integration

### Check if broadcaster is working:

```kotlin
broadcaster.scope.launch {
    while (isActive) {
        val state = broadcaster.currentState
        Timber.i("Current AI State: ${state?.primaryState}, Breathing: ${state?.breathingRate}")
        delay(1000)
    }
}
```

### Monitor WebView messages:

```javascript
// In index.html console
window.addEventListener('aiStateReceived', (e) => {
    console.log('AI State:', e.detail);
});
```

---

## ✅ Verification Checklist

After integration, verify:

- [ ] Broadcaster starts without errors
- [ ] WebView receives messages from broadcaster
- [ ] 3D scene updates when broadcaster sends state
- [ ] Different AI states produce visually distinct animations
- [ ] Animations are smooth (no jitter)
- [ ] Performance is acceptable (60 FPS)
- [ ] State transitions are smooth
- [ ] Colors match the state (cyan=thinking, green=evolving, etc.)
- [ ] Particles respond to AI state
- [ ] Geometry morphs during evolution
- [ ] Lighting changes with AI state

---

## 🚀 You're Ready!

Your AI system is now connected to a visual representation of its own cognition. Watch as the 3D core animates in response to actual AI thinking, learning, and reasoning.

**The future of AI visualization is here: make consciousness visible.** ✨

