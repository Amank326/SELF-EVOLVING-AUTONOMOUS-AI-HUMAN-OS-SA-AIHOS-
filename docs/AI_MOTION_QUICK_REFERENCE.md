# AI Motion Intelligence: Quick Reference

## File Structure

```
app/src/main/kotlin/com/aihos/ai/motion/
├── AIMotionIntelligence.kt       # State enums, data classes, mapping logic
└── AIStateBroadcaster.kt         # Real-time AI monitoring and broadcasting

app/src/main/kotlin/com/aihos/ui/three_d/
└── Three3DAIBridge.kt            # WebView ↔ Broadcaster connection

3d-scene/src/animations/
└── ProceduralAnimationController.js  # Real-time animation computation

3d-scene/src/components/
└── AIResponsiveComponentManager.js   # Apply animations to 3D objects

docs/
├── AI_MOTION_INTELLIGENCE.md     # Complete system documentation
└── AI_MOTION_INTEGRATION_GUIDE.md # How to integrate with your AI
```

---

## Cognitive States Quick Map

| State | Rate | Speed | Color | Particles | Lighting | When |
|-------|------|-------|-------|-----------|----------|------|
| **IDLE** | 0.5 Hz | 0.1× | Cyan | Settling | Normal | Waiting for input |
| **THINKING** | 1.0 Hz | 0.4× | Cyan | Orbital | Normal | Active reasoning |
| **DELIBERATING** | 1.8 Hz | 1.0× | Purple | Pulsing | Bright | Intensive weighing |
| **REFLECTING** | 0.7 Hz | 0.2× | Blue | Converging | Dimmed | Analyzing past |
| **EVOLVING** | 1.5 Hz | 0.7× | Green | Bursting | Pulsing | Learning/adapting |
| **UNCERTAIN** | 1.3 Hz | 0.6× | Amber | Chaotic | Pulsing | Exploring options |
| **EXECUTING** | 1.2 Hz | 0.5× | Cyan | Streaming | Strong | Implementing |
| **ERROR** | 2.5 Hz | 1.5× | Red | Scattering | Flashing | Problem detected |

---

## Parameter Ranges

```
breathingRate:       0.1 - 3.0 Hz
rotationSpeed:       0.0 - 2.0 multiplier
glowIntensity:       0.0 - 2.0
particleEmissionRate: 0.0 - 2.0
morphingIntensity:   0.0 - 1.0
confidence:          0.0 - 1.0 (affects glow, particle behavior)
```

---

## Integration Steps (TL;DR)

1. **Inject broadcaster** into your AI core
2. **Call** `broadcaster.onDecisionMade(decision)` when AI decides
3. **Call** `broadcaster.onDecisionOutcome(success)` when decision resolves
4. **Create** `Three3DAIBridge(webView, broadcaster)`
5. **Call** `broadcaster.startBroadcasting()`
6. **Watch** 3D core respond to AI state in real-time

---

## Key Classes

### Kotlin

**AIMotionState**: Complete snapshot of AI animation state
```kotlin
data class AIMotionState(
    val primaryState: AICognitiveState,
    val confidence: ConfidenceMetrics,
    val processing: AIProcessingMetrics,
    val breathingRate: Float,
    val rotationSpeed: Float,
    val colorTheme: AIColorTheme,
    val glowIntensity: Float,
    val particleEmissionRate: Float,
    val morphingIntensity: Float
)
```

**AIMotionController**: Computes parameters from state
```kotlin
fun computeMotionState(
    cognitiveState: AICognitiveState,
    confidence: ConfidenceMetrics,
    processing: AIProcessingMetrics
): AIMotionState
```

**AIStateBroadcaster**: Monitors AI and sends to 3D
```kotlin
fun startBroadcasting()  // Start monitoring
fun stopBroadcasting()   // Stop monitoring
fun onDecisionMade(decision: DecisionRecord)
fun onDecisionOutcome(isSuccess: Boolean)
fun onEvolutionMetricsUpdated(metrics: EvolutionMetrics)
```

### JavaScript

**ProceduralAnimationController**: Computes animations
```javascript
setAIMotionState(aiState)  // Receive AI state
update(deltaTime)          // Compute animation frame
```

**AIResponsiveComponentManager**: Applies to 3D
```javascript
applyAnimationFrame(frame)  // Update 3D objects
```

---

## Common Customizations

### Change idle breathing rate:
```kotlin
// In AIMotionController.kt
AICognitiveState.IDLE -> 0.3f  // ← Change this
```

### Change evolution color:
```javascript
// In ProceduralAnimationController.js
case 'GREEN':
    return { r: 0.2, g: 1.0, b: 0.4 }; // ← Change RGB
```

### Adjust thinking rotation speed:
```kotlin
// In AIMotionController.kt
AICognitiveState.THINKING -> 0.4f  // ← Change this
```

### Increase morphing during evolution:
```kotlin
// In AIMotionController.kt
AICognitiveState.EVOLVING -> 1.0f  // ← Change base intensity
```

---

## Testing Checklist

- [ ] Broadcaster starts with `startBroadcasting()`
- [ ] `onDecisionMade()` called when AI decides
- [ ] `onDecisionOutcome()` called when decision resolves
- [ ] Three3DAIBridge created and connected
- [ ] WebView receives AI state messages
- [ ] 3D core changes color with state
- [ ] Breathing rate changes with cognitive load
- [ ] Rotation speed changes with complexity
- [ ] Particles respond to particle rate
- [ ] Lighting dims for REFLECTING state
- [ ] Lights pulse for EVOLVING state
- [ ] Animation is smooth (no jitter)

---

## Performance Tips

1. **Reduce broadcast frequency** if CPU-bound: 100ms → 200ms
2. **Disable morphing** for low-end devices
3. **Reduce particle count** if needed
4. **Lower bloom intensity**
5. **Use `coerceIn(0f, 1f)`** on all float parameters

---

## Debug Commands

### Kotlin (Logcat):
```
adb logcat | grep "Broadcaster\|Bridge"
```

### JavaScript (Browser Console):
```javascript
// Check if state is being received
console.log(window.SAIHOSSceneInstance.currentAnimationFrame)

// Manually set state for testing
window.SAIHOSSceneInstance.setAIMotionState({
    primaryState: 'EVOLVING',
    confidence: { averageConfidence: 0.9 },
    processing: { cognitiveLoad: 0.8 },
    breathingRate: 1.5,
    rotationSpeed: 0.7,
    colorTheme: 'GREEN',
    glowIntensity: 0.9,
    particleEmissionRate: 1.8,
    morphingIntensity: 0.8
})
```

---

## Common Errors & Solutions

| Error | Cause | Fix |
|-------|-------|-----|
| "Bridge not available" | WebView not ready | Wait for `notifyInitialized()` |
| "AI state null" | Broadcaster not started | Call `broadcaster.startBroadcasting()` |
| Jittery animation | Smoothing too low | Increase `smoothingFactor` to 0.3 |
| No color change | State mapping wrong | Check `computeColorTheme()` |
| Particles not moving | Emission rate = 0 | Check `computeParticleRate()` |

---

## What's Happening Behind the Scenes

```
AI Decision Made
    ↓
Broadcaster detects (confidenceLevel = 0.85)
    ↓
Compute: breathing = 1.0 Hz, rotation = 0.4×, color = CYAN
    ↓
Create AIMotionState, serialize to JSON
    ↓
Send via WebView bridge
    ↓
ProceduralAnimationController receives state
    ↓
Each frame: compute breathing phase, rotation angle, particle behavior
    ↓
AIResponsiveComponentManager applies to 3D objects
    ↓
Three.js renders: cyan core with 1 Hz breathing, rotating at 0.4×/s
    ↓
User sees: "AI is thinking" (visual feedback)
```

---

## Architecture Overview

```
AI Core Engines         AIMotionController      ProceduralAnimation      3D Rendering
─────────────────────────────────────────────────────────────────────────────────
Memory Engine      ──┐
Reasoning Engine   ──├─→ AIStateBroadcaster ──→ AndroidBridge ──→ ProceduralAnimCtrl ──→ ComponentManager ──→ Mesh Update
Reflection Engine  ──├─→  (extracts metrics)     (JSON)              (compute frame)      (apply frame)        (render)
Evolution Engine   ──├─→
Autonomy Loop      ──┘
```

---

## Resources

- **Full System Docs**: `docs/AI_MOTION_INTELLIGENCE.md`
- **Integration Guide**: `docs/AI_MOTION_INTEGRATION_GUIDE.md`
- **Main Repo**: https://github.com/Amank326/SELF-EVOLVING-AUTONOMOUS-AI-HUMAN-OS-SA-AIHOS-

---

**Remember: Every animation = Real AI cognition. Make consciousness visible!** ✨

