# SA-AIHOS Architecture Quick Reference

## The Three Layers

```
┌─────────────────────────────────────────────────────┐
│  COMPOSE UI (SAIHOSApp.kt)                         │
│  - Observes aiState & cycleMetrics via StateFlow   │
│  - Displays real-time AI visualization             │
│  - Responsive to state changes (animated)          │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│  VIEWMODEL (SAIHOSViewModel.kt)                    │
│  - Exposes aiState and cycleMetrics as StateFlow   │
│  - Manages lifecycle (startAI, pauseAI, etc.)      │
│  - Delegates to AISystemController                 │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│  AI SYSTEM CONTROLLER (AISystemController.kt)     │
│  - Manages Think/Act/Reflect/Evolve state machine  │
│  - Tracks performance metrics                      │
│  - Handles lifecycle transitions                   │
│  - Updates StateFlow in real-time                  │
└─────────────────────────────────────────────────────┘
```

---

## Using the ViewModel in Compose

```kotlin
@Composable
fun MyScreen() {
    val viewModel: SAIHOSViewModel = hiltViewModel()
    
    // Collect state with lifecycle awareness
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val metrics by viewModel.cycleMetrics.collectAsStateWithLifecycle()
    
    // Use state in UI
    Text("Current state: ${getStateDescription(aiState)}")
    Text("Cycle time: ${metrics.lastCycleTimeMs}ms")
    
    // Control AI
    Button(onClick = { viewModel.startAI() }) {
        Text("Start AI")
    }
}
```

---

## State Machine

```
Idle ──[start()]──→ Initializing
                        ↓
                    Thinking
                        ↓
                    Acting
                        ↓
                    Reflecting
                        ↓
                    Evolving
                        ↓
                    (loop back to Thinking)

Any state ──[pause()]──→ Paused
Paused ──[resume()]──→ (returns to previous state)
Any state ──[stop()]──→ Stopped
Any error ──[error()]──→ Error
```

---

## Available StateFlows

### From SAIHOSViewModel

```kotlin
val aiState: StateFlow<AIState>
// Current state: Idle, Initializing, Thinking, Acting, Reflecting, Evolving, Paused, Stopped, Error

val executionPhase: StateFlow<ExecutionPhase>
// Current phase: Think, Act, Reflect, Evolve

val lastDecision: StateFlow<CognitiveDecision?>
// Last made decision with reasoning

val lastInsight: StateFlow<ReflectionInsight?>
// Last reflection insight from learning

val cycleMetrics: StateFlow<CycleMetrics>
// Performance metrics: cycle time, phase times, health %
```

---

## Performance Metrics

```kotlin
data class CycleMetrics(
    val lastCycleTimeMs: Long,      // Time for last full cycle
    val averageCycleTimeMs: Long,   // Moving average
    val targetCycleTimeMs: Long,    // 16ms (60 FPS target)
    val thinkPhaseMs: Long,         // Reasoning phase time
    val actPhaseMs: Long,           // Action execution time
    val reflectPhaseMs: Long,       // Reflection phase time
    val evolvePhaseMs: Long         // Evolution phase time
)
```

### Calculate Health

```kotlin
val healthPercent = if (metrics.targetCycleTimeMs == 0) 100
else {
    ((metrics.targetCycleTimeMs.toFloat() / 
      metrics.lastCycleTimeMs.coerceAtLeast(1)) * 100).toInt()
        .coerceIn(0, 200)
}
// 100% = meeting target
// >100% = slower than target
// Color coding: Green (≥100%), Yellow (75-99%), Red (<75%)
```

---

## Adding New AI Screens

1. **Create screen Composable**
```kotlin
@Composable
fun MyAIScreen(viewModel: SAIHOSViewModel) {
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    
    // Your UI here
}
```

2. **Add to navigation**
```kotlin
NavHost(...) {
    composable("my_screen") {
        MyAIScreen(viewModel = hiltViewModel<SAIHOSViewModel>())
    }
}
```

3. **Add to bottom nav** (in SAIHOSNavigationBar)
```kotlin
NavigationBarItem(
    icon = { Icon(...) },
    label = { Text("My Screen") },
    selected = false,
    onClick = { navController.navigate("my_screen") }
)
```

---

## Lifecycle Hooks

### When Activity is Destroyed
```kotlin
// ViewModel.onCleared() is automatically called
// This calls: aiSystemController.stop()
// Result: AI system shuts down cleanly
```

### When App Goes to Background
```kotlin
// Call from activity: viewModel.pauseAI()
// AI system pauses (can resume later)
// State is preserved
```

### When App Comes to Foreground
```kotlin
// Call from activity: viewModel.resumeAI()
// AI system resumes from paused state
```

### Starting AI
```kotlin
// Call: viewModel.startAI()
// Sets state to Initializing → begins cycle loop
```

---

## Common Patterns

### Display Current State
```kotlin
Text(
    when (aiState) {
        AIState.Thinking → "🧠 Thinking..."
        AIState.Acting → "⚡ Acting"
        AIState.Reflecting → "🔍 Reflecting"
        AIState.Evolving → "🔄 Evolving"
        else → "⏸️ ${aiState.name}"
    }
)
```

### Conditional UI Based on State
```kotlin
if (aiState == AIState.Acting) {
    CircularProgressIndicator()  // Show loading during action
} else {
    Text("AI is ${getStateDescription(aiState)}")
}
```

### Listen to Metrics
```kotlin
LaunchedEffect(cycleMetrics) {
    if (cycleMetrics.lastCycleTimeMs > 20) {
        // Performance degradation detected
        Timber.w("Cycle time exceeds target: ${cycleMetrics.lastCycleTimeMs}ms")
    }
}
```

### Show Last Decision
```kotlin
val lastDecision by viewModel.lastDecision.collectAsStateWithLifecycle()
lastDecision?.let {
    Text("Last decision: ${it.action}")
    Text("Reasoning: ${it.reasoning}")
}
```

---

## Debugging

### Check Current State
```kotlin
// In ViewModel scope or Logcat
Timber.d("AI State: ${viewModel.aiState.value}")
Timber.d("Cycle time: ${viewModel.cycleMetrics.value.lastCycleTimeMs}ms")
```

### Monitor State Changes
```kotlin
// In a screen or activity
viewModel.aiState.collect { state ->
    Timber.d("State changed to: $state")
}
```

### Profile Performance
```kotlin
// Check metrics from AI state status bar
// Or access directly:
val metrics = viewModel.cycleMetrics.value
Timber.i(
    "Performance: cycle=${metrics.lastCycleTimeMs}ms, " +
    "think=${metrics.thinkPhaseMs}ms, " +
    "act=${metrics.actPhaseMs}ms"
)
```

---

## Key Principles

1. **UI Never Touches AI Directly**
   - Always go through ViewModel
   - All state comes from StateFlow

2. **State is Observable, Not Mutable**
   - Use `collectAsStateWithLifecycle()` in Compose
   - Automatic recomposition on state change

3. **Lifecycle is Managed Automatically**
   - ViewModel cleanup happens in onCleared()
   - No manual resource management needed

4. **Performance is Monitored**
   - Every cycle produces metrics
   - Health percentage shows performance vs. target

5. **Everything is Type-Safe**
   - Sealed AIState classes
   - No string state names
   - Compiler guarantees correctness

---

## File Locations

- **State Machine**: `app/src/main/kotlin/com/aihos/ai/AISystemController.kt`
- **ViewModel**: `app/src/main/kotlin/com/aihos/ui/viewmodel/SAIHOSViewModel.kt`
- **Root Composable**: `app/src/main/kotlin/com/aihos/ui/SAIHOSApp.kt`
- **Activity**: `app/src/main/kotlin/com/aihos/ui/MainActivity.kt`
- **Screens**: `app/src/main/kotlin/com/aihos/ui/screens/`

---

## Integration with 3D Scene

(Coming next phase)

Will update StateFlow consumers in the 3D rendering engine to visualize:
- Current AI state with visual markers
- Cycle time with performance indicator
- Individual phase times with phase visualization
