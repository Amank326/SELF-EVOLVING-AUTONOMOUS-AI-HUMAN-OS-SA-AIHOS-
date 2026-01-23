# 3D Scene Integration with AI Metrics

## Overview

This guide shows how to bind real AI system metrics to the 3D visualization, making the 3D scene a **living dashboard** of AI cognition.

---

## 📊 Available AI Metrics

The SA-AIHOS system continuously tracks:

### Memory Metrics
```kotlin
data class MemoryMetrics(
    val totalEpisodes: Int,
    val episodesByType: Map<MemoryType, Int>, // Episodic, Semantic, Procedural, etc.
    val memoryUsageBytes: Long,
    val averageEpisodeSize: Int,
    val accessFrequency: Map<String, Int> // Most accessed memories
)
```

### Reasoning Metrics
```kotlin
data class ReasoningMetrics(
    val decisionsThisSession: Int,
    val averageDecisionTime: Long,  // ms
    val averageConfidence: Float,   // 0.0 - 1.0
    val topReasons: List<String>,   // Most common reasoning patterns
    val successRate: Float,         // Positive outcome ratio
    val failureRate: Float
)
```

### Evolution Metrics
```kotlin
data class EvolutionMetrics(
    val rulesCreated: Int,
    val rulesModified: Int,
    val rulesDeprecated: Int,
    val averageRuleWeight: Float,
    val generationNumber: Int,
    val adaptationRate: Float     // How quickly rules change
)
```

### Autonomy Metrics
```kotlin
data class AutonomyMetrics(
    val currentLevel: Float,      // 0.0 - 1.0
    val actionsApproved: Int,
    val actionsRejected: Int,
    val approvalRate: Float,
    val averageExecutionTime: Long
)
```

---

## 🎨 Visualization Bindings

### 1. Memory Load → AI-Core Size

**Concept**: Larger AI-Core = more memory loaded

```kotlin
// In ViewModel or Composable
observeMemoryMetrics { metrics ->
    // Scale the crystal size based on memory load
    val maxMemory = 100_000_000  // 100 MB
    val loadRatio = metrics.memoryUsageBytes / maxMemory
    val coreScale = 0.8 + (loadRatio * 0.5)  // Range: 0.8 - 1.3
    
    webView.evaluateJavascript("""
        if (window.SAIHOSSceneInstance && window.SAIHOSSceneInstance.aiCore) {
            window.SAIHOSSceneInstance.aiCore.group.scale.setScalar($coreScale);
        }
    """.trimIndent()) { }
}
```

### 2. Reasoning Confidence → Crystal Glow Intensity

**Concept**: Higher confidence = brighter glow

```kotlin
observeReasoningMetrics { metrics ->
    // Update glow intensity based on confidence
    webView.evaluateJavascript("""
        if (window.SAIHOSSceneInstance) {
            const confidence = ${metrics.averageConfidence};
            window.SAIHOSSceneInstance.setAnimationIntensity(confidence);
        }
    """.trimIndent()) { }
}
```

### 3. Evolution Rate → Rotation Speed

**Concept**: Faster rotation = faster evolution

```kotlin
observeEvolutionMetrics { metrics ->
    // Increase rotation speed based on adaptation rate
    val baseSpeed = 0.3
    val adaptiveSpeed = baseSpeed + (metrics.adaptationRate * 0.5)
    
    webView.evaluateJavascript("""
        if (window.SAIHOSSceneInstance && window.SAIHOSSceneInstance.animationController) {
            window.SAIHOSSceneInstance.animationController.setRotationSpeed(
                $adaptiveSpeed * 0.3,
                $adaptiveSpeed * 0.5,
                $adaptiveSpeed * 0.1
            );
        }
    """.trimIndent()) { }
}
```

### 4. Decision Success Rate → Color Theme

**Concept**: Different colors represent health states

```kotlin
observeReasoningMetrics { metrics ->
    val theme = when {
        metrics.successRate > 0.8 -> "cyan"      // Healthy
        metrics.successRate > 0.6 -> "blue"      // Good
        metrics.successRate > 0.4 -> "purple"    // Fair
        else -> "red"                             // Struggling
    }
    
    webView.setTheme(theme)
}
```

### 5. Memory Type Distribution → Particle Colors

**Concept**: Particle colors reflect which memory types are active

```kotlin
observeMemoryMetrics { metrics ->
    // Calculate dominant memory types
    val episodic = metrics.episodesByType[MemoryType.EPISODIC] ?: 0
    val semantic = metrics.episodesByType[MemoryType.SEMANTIC] ?: 0
    val procedural = metrics.episodesByType[MemoryType.PROCEDURAL] ?: 0
    
    val script = """
        if (window.SAIHOSSceneInstance && window.SAIHOSSceneInstance.aiCore.particleSystem) {
            const colors = window.SAIHOSSceneInstance.aiCore.particleSystem.geometry.attributes.color;
            const total = $episodic + $semantic + $procedural;
            
            for (let i = 0; i < colors.count; i++) {
                const r = Math.random();
                if (r < $episodic / total) {
                    colors.setXYZ(i, 0, 1, 1);      // Episodic: Cyan
                } else if (r < ($episodic + $semantic) / total) {
                    colors.setXYZ(i, 1, 0, 1);      // Semantic: Magenta
                } else {
                    colors.setXYZ(i, 0.5, 0.8, 1);  // Procedural: Light Blue
                }
            }
            colors.needsUpdate = true;
        }
    """.trimIndent()
    
    webView.evaluateJavascript(script) { }
}
```

### 6. Autonomy Level → Camera Distance

**Concept**: Higher autonomy = closer camera (more immersive)

```kotlin
observeAutonomyMetrics { metrics ->
    // Adjust camera distance based on autonomy level
    val distance = 5.0 - (metrics.currentLevel * 1.5)  // Range: 3.5 - 5.0
    
    webView.evaluateJavascript("""
        if (window.SAIHOSSceneInstance) {
            window.SAIHOSSceneInstance.config.camera.orbitRadius = $distance;
        }
    """.trimIndent()) { }
}
```

---

## 🔄 Real-Time Dashboard Example

Complete Composable screen showing both 3D visualization and metrics:

```kotlin
@Composable
fun ThreeDDashboardScreen(
    viewModel: SAIHOSViewModel = hiltViewModel()
) {
    val memoryMetrics by viewModel.memoryMetrics.collectAsState()
    val reasoningMetrics by viewModel.reasoningMetrics.collectAsState()
    val evolutionMetrics by viewModel.evolutionMetrics.collectAsState()
    val autonomyMetrics by viewModel.autonomyMetrics.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 3D Scene
        Three3DScreen(
            modifier = Modifier.fillMaxSize()
        )

        // Metrics Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Memory: ${memoryMetrics?.totalEpisodes ?: 0} episodes")
            Text("Confidence: ${String.format("%.1f", (reasoningMetrics?.averageConfidence ?: 0f) * 100)}%")
            Text("Success Rate: ${String.format("%.1f", (reasoningMetrics?.successRate ?: 0f) * 100)}%")
            Text("Evolution: Gen ${evolutionMetrics?.generationNumber ?: 0}")
            Text("Autonomy: ${String.format("%.1f", autonomyMetrics?.currentLevel ?: 0f)}")
        }

        // Control Panel
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.pauseAI() }) {
                Text("Pause AI")
            }
            Button(onClick = { viewModel.resumeAI() }) {
                Text("Resume AI")
            }
            Button(onClick = { viewModel.triggerReflection() }) {
                Text("Reflect Now")
            }
            Button(onClick = { viewModel.triggerEvolution() }) {
                Text("Evolve Now")
            }
        }
    }

    // Observe metrics and update 3D scene
    LaunchedEffect(memoryMetrics, reasoningMetrics, evolutionMetrics, autonomyMetrics) {
        updateThreeDScene(
            memoryMetrics,
            reasoningMetrics,
            evolutionMetrics,
            autonomyMetrics
        )
    }
}

fun updateThreeDScene(
    memoryMetrics: MemoryMetrics?,
    reasoningMetrics: ReasoningMetrics?,
    evolutionMetrics: EvolutionMetrics?,
    autonomyMetrics: AutonomyMetrics?
) {
    // Implement bindings from sections above
}
```

---

## 🎯 Advanced: Event-Driven Visualization

Trigger special effects when major AI events occur:

```kotlin
// In AndroidBridge.js
window.SAIHOSBridge.triggerDecisionEffect = function(confidence) {
    // Flash the crystal when a high-confidence decision is made
    if (confidence > 0.8) {
        const aiCore = window.SAIHOSSceneInstance.aiCore.coreCrystal;
        const originalScale = aiCore.scale.x;
        
        // Pulse animation
        let time = 0;
        const interval = setInterval(() => {
            time += 0.05;
            aiCore.scale.setScalar(originalScale + Math.sin(time) * 0.1);
            if (time > Math.PI) {
                aiCore.scale.setScalar(originalScale);
                clearInterval(interval);
            }
        }, 16);
    }
};

window.SAIHOSBridge.triggerErrorEffect = function() {
    // Red flash on error
    const original = window.SAIHOSSceneInstance.scene.background.getHex();
    window.SAIHOSSceneInstance.scene.background.setHex(0xff0000);
    setTimeout(() => {
        window.SAIHOSSceneInstance.scene.background.setHex(original);
    }, 200);
};

window.SAIHOSBridge.triggerLearningEffect = function() {
    // Color shift on learning
    window.SAIHOSSceneInstance.setColorTheme("purple");
    setTimeout(() => {
        window.SAIHOSSceneInstance.setColorTheme("cyan");
    }, 2000);
};
```

### Trigger from Kotlin:

```kotlin
// When high-confidence decision made
reasoningEngine.makeDecision(...).let { decision ->
    if (decision.confidence > 0.8) {
        webView.evaluateJavascript("""
            window.SAIHOSBridge.triggerDecisionEffect(${decision.confidence});
        """)
    }
}

// When learning event occurs
evolutionEngine.learnFromFeedback(...).let { result ->
    if (result.newRulesCreated > 0) {
        webView.evaluateJavascript("window.SAIHOSBridge.triggerLearningEffect();")
    }
}

// When error occurs
catch (e: Exception) {
    webView.evaluateJavascript("window.SAIHOSBridge.triggerErrorEffect();")
}
```

---

## 📡 Streaming Metrics to 3D

Continuous metric stream with minimal overhead:

```kotlin
// In ViewModel
val metricsFlow = combine(
    memoryRepository.getMetricsFlow(),
    reasoningEngine.getMetricsFlow(),
    evolutionEngine.getMetricsFlow(),
    autonomyController.getMetricsFlow()
) { memory, reasoning, evolution, autonomy ->
    MetricsSnapshot(memory, reasoning, evolution, autonomy)
}
.throttleLatest(250)  // Update 3D scene at ~4 Hz max
.collect { snapshot ->
    updateThreeDWithMetrics(snapshot)
}

private fun updateThreeDWithMetrics(snapshot: MetricsSnapshot) {
    val json = Json.encodeToString(snapshot)
    webView.evaluateJavascript("""
        if (window.SAIHOSSceneInstance) {
            window.SAIHOSSceneInstance.updateMetrics($json);
        }
    """)
}
```

### In JavaScript:

```javascript
updateMetrics(metricsJson) {
    const metrics = JSON.parse(metricsJson);
    
    // Scale crystal by memory
    const memScale = 0.8 + (metrics.memory.usage / metrics.memory.max) * 0.5;
    this.aiCore.group.scale.setScalar(memScale);
    
    // Adjust rotation by evolution rate
    this.animationController.setRotationSpeed(
        0.3 * metrics.evolution.adaptationRate,
        0.5 * metrics.evolution.adaptationRate,
        0.1 * metrics.evolution.adaptationRate
    );
    
    // Change theme by health
    if (metrics.reasoning.successRate > 0.8) {
        this.setColorTheme('cyan');
    } else if (metrics.reasoning.successRate < 0.4) {
        this.setColorTheme('red');
    }
}
```

---

## 🎬 Recording Session Visualization

Capture the AI's thinking process as a 3D visualization:

```kotlin
fun startRecordingSession(duration: Long) {
    val metricsSnapshots = mutableListOf<MetricsSnapshot>()
    
    // Collect metrics
    viewModelScope.launch {
        metricsFlow.take((duration / 250).toInt()).collect {
            metricsSnapshots.add(it)
        }
    }
    
    // After duration, export
    delay(duration)
    exportVisualization(metricsSnapshots)
}

fun exportVisualization(snapshots: List<MetricsSnapshot>) {
    // Generate HTML/JavaScript that replays the 3D scene
    // with the recorded metrics
    
    val replayScript = generateReplayScript(snapshots)
    val html = """
        <!DOCTYPE html>
        <html>
        <body>
            <div id="container" style="width: 100%; height: 100%"></div>
            <script src="https://cdn.jsdelivr.net/npm/three@r128/build/three.module.js"></script>
            <script>
                // Load scene
                const scene = new SAIHOSScene('#container');
                
                // Replay metrics
                $replayScript
            </script>
        </body>
        </html>
    """.trimIndent()
    
    // Save HTML file
    val file = File(context.getExternalFilesDir(null), "ai_session.html")
    file.writeText(html)
    
    // Share or open in browser
    shareFile(file)
}
```

---

## 🔧 Custom Metric Bindings

Template for adding your own metrics:

```kotlin
// 1. Define your metric
data class CustomMetric(
    val value: Float,
    val label: String,
    val unit: String
)

// 2. Create flow in your engine
val customMetricFlow: Flow<CustomMetric> = flow {
    while (true) {
        emit(CustomMetric(
            value = calculateCustomValue(),
            label = "Custom Metric",
            unit = "units"
        ))
        delay(250)
    }
}

// 3. Combine with other metrics
val metricsFlow = combine(
    memoryMetricsFlow,
    customMetricFlow
) { memory, custom ->
    Pair(memory, custom)
}

// 4. Update 3D scene
metricsFlow.collect { (memory, custom) ->
    webView.evaluateJavascript("""
        if (window.SAIHOSSceneInstance) {
            // Your custom visualization
            window.SAIHOSSceneInstance.updateCustomMetric(
                ${custom.value},
                "${custom.label}"
            );
        }
    """)
}

// 5. Implement in JavaScript
// Add method to Scene class
updateCustomMetric(value, label) {
    // Visualize your custom metric
    // e.g., change particle colors, lighting, etc.
}
```

---

## 📈 Performance Considerations

1. **Throttle Updates**: Limit 3D updates to 4-10 Hz max (250-100ms intervals)
2. **Use Batch Updates**: Send multiple metrics in one JavaScript call
3. **Avoid DOM Thrashing**: Minimize evaluateJavascript calls
4. **Cache Calculations**: Pre-compute scaling factors on Android side
5. **Async Rendering**: Don't block UI thread with large metric calculations

---

## 📚 Documentation

- [3D Architecture Guide](3D_ARCHITECTURE.md) - Technical details
- [3D Quick Start](3D_QUICK_START.md) - Get running quickly
- [AI System Docs](docs/ARCHITECTURE.md) - AI metrics and architecture

---

**With these bindings, your 3D scene becomes a real-time window into AI cognition.**

Watch your AI learn, evolve, and improve itself—visualized in stunning 3D. 🚀
