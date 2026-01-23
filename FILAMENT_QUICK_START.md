# Filament 3D Engine - Quick Start Reference

## Get Started in 3 Steps

### 1. View the 3D Core
Navigate to the "AI Core 3D" tab in the app. The system automatically renders and updates based on AI state.

### 2. Monitor State Changes
Watch how visuals respond to AI state:
```
Idle     → Cool blue glow
Thinking → Bright cyan, strong pulse
Evolving → Green with intense emission
Error    → Red flash warning
```

### 3. Customize (Optional)
All visual parameters are in **AICoreMaterial.kt** and easily modified:
```kotlin
AIState.Thinking -> {
    baseColor = Color(0.2f, 0.8f, 1.0f)  // Cyan
    emission = 0.5f
    pulseAmplitude = 0.4f
}
```

---

## Key Files Reference

| File | Purpose | Key Classes |
|------|---------|------------|
| Native3DEngine.kt | Rendering context & lifecycle | `Native3DEngine(context, surfaceView, scope)` |
| AICoreMaterial.kt | Material properties & state | `AICoreMaterial`, `enum AIState` |
| AI3DBridge.kt | AI-to-3D mapping | `AI3DBridge(engine, scope)` |
| AICore3DScreen.kt | Compose UI | `Filament3DView()`, `AICore3DScreen()` |

---

## AI State Visual Mapping

```
Idle         → Cool Blue (0.1, 0.4, 0.6)
Thinking     → Cyan (0.2, 0.8, 1.0) - Pulses
Reflecting   → Purple (0.7, 0.3, 1.0)
Evolving     → Green (0.3, 1.0, 0.3) - Strong pulse
Error        → Red (1.0, 0.0, 0.0) - Flashing
```

---

## Common Tasks

### Change Material Color
```kotlin
// In AICoreMaterial.kt, setAIState() method
AIState.Thinking -> {
    baseColor = Color(0.2f, 0.8f, 1.0f)  // Your color
    updateMaterialProperties()
}
```

### Adjust Rotation Speed
```kotlin
// In AI3DBridge.kt, updateAnimationFromMetrics()
val rotationSpeed = 16.67f / lastCycleTimeMs  // Adjust multiplier
```

### Modify Light Intensity
```kotlin
// In AI3DBridge.kt
engine.setLightIntensity(50000f * healthModulation)
```

### Add New AI State
```kotlin
// 1. In AICoreMaterial.kt
enum class AIState {
    // ... existing states
    MyNewState  // Add here
}

// 2. In setAIState()
MyNewState -> {
    baseColor = Color(...)
    metallic = 0.x
    // ... other properties
}

// 3. In AISystemController (map from app state)
AISystemState.MyState -> AIState.MyNewState
```

---

## Performance Tips

1. **Monitor Frame Time**
   ```kotlin
   Timber.d("Frame: ${frameTimeNanos}ns")
   ```

2. **Check Memory Usage**
   - Use Android Profiler → Memory tab
   - Target: ~57 MB total

3. **Optimize Geometry**
   - Sphere: 32x32 segments (good balance)
   - Reduce if needed: `32 * 32 / 2 = 512 vertices`

4. **Light Efficiency**
   - Use 1-2 lights max
   - Disable shadows for budget devices

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Rendering blank | Check surfaceView lifecycle, ensure engine.initialize() called |
| Slow performance | Reduce sphere segments or disable dynamic lighting |
| Materials not updating | Verify materialInstance initialized before setParameter() |
| App crashes on exit | Check destroy() method order |

---

## Code Examples

### Get Material Reference
```kotlin
val material = engine.getAICoreMaterial()
```

### Update from AI State
```kotlin
bridge.updateFromAIState(
    aiState = AIState.Thinking,
    cycleMetrics = CycleMetrics(...),
    lastDecision = "...",
    lastInsight = "..."
)
```

### Custom Rendering Update
```kotlin
LaunchedEffect(aiState) {
    when (aiState) {
        AIState.Thinking -> {
            engine.setRotationTarget(0f, 0.5f, 0f)
            engine.setScaleTarget(1.1f)
        }
        // ... other states
    }
}
```

---

## Next Steps

- [ ] Profile on actual device
- [ ] Add gesture-driven camera control
- [ ] Implement particle effects for evolution
- [ ] Add temporal visualization
- [ ] Export 3D models for customization

---

**Version**: 1.0  
**Last Updated**: January 24, 2026  
**Status**: Production Ready
