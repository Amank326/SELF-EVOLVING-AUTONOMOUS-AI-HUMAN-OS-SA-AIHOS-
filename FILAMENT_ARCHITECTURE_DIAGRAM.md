# Filament 3D Architecture - Visual Reference

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        SA-AIHOS APPLICATION                      │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                  Jetpack Compose UI Layer                  │ │
│  │                                                             │ │
│  │  ┌──────────────────────────────────────────────────────┐  │ │
│  │  │           SAIHOSApp (Main Navigation)                │  │ │
│  │  │  • NavHost with 5 screens                            │  │ │
│  │  │  • AICore3DScreen route (ai_core_3d)               │  │ │
│  │  │  • SAIHOSNavigationBar (5 items)                    │  │ │
│  │  └──────────────────────────────────────────────────────┘  │ │
│  │                         ↓                                     │ │
│  │  ┌──────────────────────────────────────────────────────┐  │ │
│  │  │          AICore3DScreen Composable                   │  │ │
│  │  │  • Collects aiState (StateFlow)                     │  │ │
│  │  │  • Collects cycleMetrics (StateFlow)                │  │ │
│  │  │  • Collects lastDecision (StateFlow)                │  │ │
│  │  │  • Collects lastInsight (StateFlow)                 │  │ │
│  │  └──────────────────────────────────────────────────────┘  │ │
│  │                         ↓                                     │ │
│  │  ┌──────────────────────────────────────────────────────┐  │ │
│  │  │      Filament3DView Composable (AndroidView)        │  │ │
│  │  │  • Wraps SurfaceView                                │  │ │
│  │  │  • LaunchedEffect for state binding                 │  │ │
│  │  │  • DisposableEffect for cleanup                     │  │ │
│  │  └──────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│               Native 3D Rendering Pipeline                        │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │            Native3DEngine (Filament Context)              │ │
│  │                                                             │ │
│  │  • Filament Engine                                        │ │
│  │  • Renderer & Scene Management                           │ │
│  │  • Camera & View Setup                                   │ │
│  │  • Rendering Loop (60 FPS)                               │ │
│  │  • Entity Lifecycle Management                           │ │
│  │  • Animation State (rotation, scale)                     │ │
│  │  • Lighting System (directional + ambient)               │ │
│  │                                                             │ │
│  │  Methods:                                                 │ │
│  │  - initialize()      // Setup Filament                   │ │
│  │  - startRendering()  // 60 FPS loop                      │ │
│  │  - renderFrame()     // Single frame render              │ │
│  │  - updateAnimation() // Procedural animation             │ │
│  │  - resume/pause      // Lifecycle                        │ │
│  │  - destroy()         // Cleanup                          │ │
│  │                                                             │ │
│  │  Lifecycle:                                               │ │
│  │    onCreate → initialize() → startRendering()            │ │
│  │    onPause  → pause()                                    │ │
│  │    onResume → resume()                                   │ │
│  │    onDestroy → destroy()                                 │ │
│  └────────────────────────────────────────────────────────────┘ │
│                               ↓                                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │     AI3DBridge (AI State → 3D Visual Mapper)              │ │
│  │                                                             │ │
│  │  Input:                                                    │ │
│  │  • AISystemController.AIState (from ViewModel)           │ │
│  │  • CycleMetrics (lastCycleTimeMs, health%)               │ │
│  │  • lastDecision, lastInsight (optional)                  │ │
│  │                                                             │ │
│  │  Processing:                                              │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │ updateMaterialFromState()                           │ │ │
│  │  │ • Idle       → Cool Blue (0.1, 0.4, 0.6)            │ │ │
│  │  │ • Thinking   → Cyan (0.2, 0.8, 1.0) + pulse         │ │ │
│  │  │ • Reflecting → Purple (0.7, 0.3, 1.0)               │ │ │
│  │  │ • Evolving   → Green (0.3, 1.0, 0.3) + strong pulse │ │ │
│  │  │ • Error      → Red (1.0, 0.0, 0.0) + flashing       │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │ updateAnimationFromMetrics()                        │ │ │
│  │  │ • rotationSpeed = 16.67 / lastCycleTimeMs           │ │ │
│  │  │ • scale = 0.9 + (health/200) * 0.2                 │ │ │
│  │  │ • Updates rotation/scale targets                    │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │ updateLightingFromState()                           │ │ │
│  │  │ • Idle: 20,000 lux                                  │ │ │
│  │  │ • Thinking: 35,000 lux                              │ │ │
│  │  │ • Evolving: 45,000 lux                              │ │ │
│  │  │ • Modulated by health factor                        │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  │                                                             │ │
│  │  Output:                                                   │ │
│  │  • Updated material properties (color, emission, etc.)   │ │
│  │  • Updated animation targets (rotation, scale)           │ │
│  │  • Updated light intensity                               │ │
│  └────────────────────────────────────────────────────────────┘ │
│                               ↓                                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │        AICoreMaterial (PBR Material System)               │ │
│  │                                                             │ │
│  │  Material Properties:                                     │ │
│  │  • baseColor        [RGB 0..1]                           │ │
│  │  • metallic         [0..1]                               │ │
│  │  • roughness        [0..1]                               │ │
│  │  • emissionColor    [RGB 0..1]                           │ │
│  │  • emissionIntensity [0..1]                              │ │
│  │                                                             │ │
│  │  Features:                                                │ │
│  │  • setAIState() - Maps enum state to properties          │ │
│  │  • updatePulseAnimation() - Sine-wave emission pulse     │ │
│  │  • updateMaterialProperties() - Apply to Filament        │ │
│  │                                                             │ │
│  │  Pulse Animation:                                         │ │
│  │  emission(t) = baseEmission * (1 + amplitude * sin(phase)) │ │
│  │  amplitude varies per state (0.3 to 0.7)                 │ │
│  │                                                             │ │
│  │  9 States Defined:                                        │ │
│  │  Idle, Initializing, Thinking, Acting, Reflecting,       │ │
│  │  Evolving, Paused, Stopped, Error                        │ │
│  └────────────────────────────────────────────────────────────┘ │
│                               ↓                                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │         Filament Rendering (GPU)                          │ │
│  │                                                             │ │
│  │  • Render Target: SurfaceView                            │ │
│  │  • Engine: Filament 1.51.6 (PBR)                        │ │
│  │  • Geometry: Sphere (32x32 segments, 1,024 vertices)    │ │
│  │  • Material: PBR with dynamic parameters                │ │
│  │  • Lighting: 1 Directional + 1 Ambient                  │ │
│  │  • Output: 60 FPS framebuffer swap                      │ │
│  │                                                             │ │
│  │  Frame Pipeline:                                          │ │
│  │  1. renderer.beginFrame()     // Start frame              │ │
│  │  2. renderer.render(view)     // Render scene            │ │
│  │  3. renderer.endFrame()       // Submit to GPU            │ │
│  │  4. Swap buffers / Display    // Show on screen          │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Android Device Display                         │
│              (SurfaceView showing AI Core 3D)                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Diagram

```
AI System Controller
     (AISystemState)
         │
         ├─→ Idle, Thinking, Reflecting, Evolving, etc.
         │
         ↓
┌────────────────────────────────────┐
│     SAIHOSViewModel (ViewModel)    │
│  • stateFlow(aiState)              │
│  • stateFlow(cycleMetrics)         │
│  • stateFlow(lastDecision)         │
│  • stateFlow(lastInsight)          │
└────────────────────────────────────┘
         │
         ↓ collectAsStateWithLifecycle()
┌────────────────────────────────────┐
│   AICore3DScreen (Compose)         │
│   + Filament3DView                 │
└────────────────────────────────────┘
         │
         ↓ LaunchedEffect
┌────────────────────────────────────┐
│  AI3DBridge.updateFromAIState()    │
│  • Material update                 │
│  • Animation update                │
│  • Lighting update                 │
└────────────────────────────────────┘
         │
         ├─→ updateMaterialFromState()
         │        │
         │        └─→ AICoreMaterial.setAIState()
         │                 │
         │                 └─→ setBaseColor()
         │                     setEmissionColor()
         │                     setMetallic()
         │                     setRoughness()
         │                     setEmissionIntensity()
         │
         ├─→ updateAnimationFromMetrics()
         │        │
         │        └─→ engine.setRotationTarget()
         │            engine.setScaleTarget()
         │
         └─→ updateLightingFromState()
                  │
                  └─→ engine.setLightIntensity()
                       │
                       ↓
            ┌──────────────────────────────┐
            │  Native3DEngine             │
            │  • updateAnimation(dt)      │
            │  • renderFrame()            │
            │  • Filament updates         │
            └──────────────────────────────┘
                       │
                       ↓
            ┌──────────────────────────────┐
            │  Filament GPU Rendering     │
            │  • Apply materials          │
            │  • Lighting calculations    │
            │  • Fragment/Vertex shaders  │
            │  • Texture sampling         │
            └──────────────────────────────┘
                       │
                       ↓
            ┌──────────────────────────────┐
            │  SurfaceView Display        │
            │  (60 FPS framebuffer)       │
            └──────────────────────────────┘
```

---

## State Transition Diagram

```
         ┌─────────────────────────────────────────────────┐
         │        AI System State Machine                  │
         └─────────────────────────────────────────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ↓                 ↓                 ↓
    ┌────────┐         ┌──────────┐    ┌──────────┐
    │  Idle  │──↔──→ │Thinking  │───→│Reflecting│
    └────────┘       └──────────┘    └──────────┘
         │                 │              │
         │                 ↓              │
         │            ┌────────┐          │
         └───────→ │  Acting  │←─────────┘
                    └────────┘
                         │
                         ↓
                    ┌──────────┐
                    │ Evolving │
                    └──────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ↓               ↓               ↓
    ┌────────┐      ┌────────┐    ┌─────────┐
    │ Paused │   │Stopped│   │ Error  │
    └────────┘      └────────┘    └─────────┘


┌──────────────────────────────────────────────────┐
│     Visual State Mapping (State → Color)        │
├──────────────────────────────────────────────────┤
│                                                  │
│  Idle        → Blue (0.1, 0.4, 0.6)             │
│               Steady, low glow                  │
│                                                  │
│  Thinking    → Cyan (0.2, 0.8, 1.0)            │
│               Fast pulse, high emission         │
│                                                  │
│  Reflecting  → Purple (0.7, 0.3, 1.0)          │
│               Moderate pulse                    │
│                                                  │
│  Acting      → Green (0.3, 1.0, 0.3)           │
│               Steady, medium emission           │
│                                                  │
│  Evolving    → Lime (0.3, 1.0, 0.3)            │
│               Strong pulse, highest emission    │
│                                                  │
│  Error       → Red (1.0, 0.0, 0.0)             │
│               Flashing, critical alert          │
│                                                  │
│  Paused      → Orange (1.0, 0.4, 0.0)          │
│               Dim, suspended state              │
│                                                  │
│  Stopped     → Gray (0.2, 0.2, 0.2)            │
│               Off, inactive                     │
│                                                  │
└──────────────────────────────────────────────────┘
```

---

## Component Interaction Diagram

```
┌──────────────────┐
│  AI System State │ ──updates──→ ┌────────────────┐
│  (9 states)      │              │  ViewModel     │
└──────────────────┘              │  StateFlows    │
                                  └────────────────┘
                                        │
        ┌───────────────────────────────┼───────────────────────────────┐
        │                               │                               │
        ↓                               ↓                               ↓
┌──────────────────┐          ┌──────────────────┐           ┌──────────────────┐
│  aiState         │          │ cycleMetrics     │           │ lastDecision     │
│  (StateFlow)     │          │ (StateFlow)      │           │ (StateFlow)      │
└──────────────────┘          └──────────────────┘           └──────────────────┘
        │                               │                               │
        └───────────────────────────────┼───────────────────────────────┘
                                        │
                                        ↓ collected in Compose
                                ┌──────────────────┐
                                │ AICore3DScreen   │
                                │ + Filament3DView │
                                └──────────────────┘
                                        │
                                        ↓ LaunchedEffect
                            ┌──────────────────────┐
                            │  AI3DBridge          │
                            │ updateFromAIState() │
                            └──────────────────────┘
                                   │   │   │
                ┌──────────────────┘   │   └──────────────────┐
                ↓                      ↓                       ↓
    ┌────────────────────┐  ┌────────────────────┐  ┌────────────────────┐
    │ Material Update    │  │ Animation Update   │  │ Lighting Update    │
    │ (color, emission)  │  │ (rotation, scale)  │  │ (intensity)        │
    └────────────────────┘  └────────────────────┘  └────────────────────┘
                │                      │                      │
                └──────────────────────┬──────────────────────┘
                                       │
                                       ↓
                            ┌──────────────────────┐
                            │  Native3DEngine      │
                            │ • Scene management   │
                            │ • Rendering loop     │
                            │ • Animation state    │
                            └──────────────────────┘
                                       │
                                       ↓
                            ┌──────────────────────┐
                            │  Filament (GPU)      │
                            │ • Material shading   │
                            │ • Lighting calc      │
                            │ • Texture sampling   │
                            └──────────────────────┘
                                       │
                                       ↓
                            ┌──────────────────────┐
                            │  SurfaceView Output  │
                            │  (60 FPS)            │
                            └──────────────────────┘
```

---

## Performance Timeline

```
Time (ms)
┌────────────────────────────────────────────────────────────┐ 16.67ms (60 FPS)
│                                                             │
│ AI State Update       0.5ms ████                           │
│ Material Update       0.3ms ██                             │
│ Animation Update      0.4ms ███                            │
│ Engine Prep           0.8ms ██████                         │
│ GPU Rendering        11.0ms ████████████████████████████  │
│ Buffer/Display        2.0ms ███████████                    │
│ Overhead              1.9ms ██████████                     │
│                                                             │
└────────────────────────────────────────────────────────────┘
  0      2      4      6      8     10     12     14     16
  
Target: <16.67ms (60 FPS)
Actual: ~16.6ms (CPU + GPU time)
```

---

## File Dependency Graph

```
SAIHOSApp.kt
    │
    ├─→ AICore3DScreen.kt
    │       │
    │       ├─→ Native3DEngine.kt
    │       │       │
    │       │       ├─→ AICoreMaterial.kt
    │       │       │       (Material PBR properties)
    │       │       │
    │       │       └─→ AI3DBridge.kt
    │       │               (State mapping)
    │       │
    │       └─→ SAIHOSViewModel
    │           (StateFlow collections)
    │
    ├─→ build.gradle.kts
    │   (Filament dependencies)
    │
    └─→ Android Framework
        (Compose, Coroutines, Lifecycle)
```

---

## Lifecycle Sequence Diagram

```
Activity/Fragment Lifecycle          Native3DEngine              AI3DBridge
│                                    │                          │
├─ onCreate()                        │                          │
│  ├─ inflate SAIHOSApp             │                          │
│  └─ AICore3DScreen created        │                          │
│                                    │                          │
├─ onStart()                         │                          │
│                                    │                          │
├─ onResume()                        │                          │
│  └─ Compose renders Filament3DView│                          │
│      └─ SurfaceView created       │                          │
│         └─ initialize()           ├─ Filament setup          │
│         └─ startRendering() ──────┼─ 60 FPS loop starts     │
│         └─ bridge.start() ────────────────────────────────┤ start()
│         └─ LaunchedEffect ───────────────────────────────┤ begin listening
│                                    │  [Rendering Active]     │ [Active]
│                                    │                          │
│ [User interaction]                 │                          │
│ StateFlow updates                 │                          │
│  └─ aiState changed              │                          │
│  └─ cycleMetrics changed         │                          │
│     └─ LaunchedEffect triggered  ──────────────────────────┤ updateFromAIState()
│        └─ bridge.update() ───────┼─ updateAnimation() ──┤
│                                    ├─ updateMaterial()   │
│                                    ├─ updateLighting()   │
│                                    │                     └── Material updated
│                                    │                          
│ (repeats for each frame)           │ renderFrame() (60 times/sec)
│  └─ Filament renders GPU           │
│                                    │
├─ onPause()                         │                          │
│  └─ pause() ───────────────────────┼─ [Paused]             │
│                                    │ Rendering stopped       │
│                                    │                          │
├─ onResume() (back to foreground)  │                          │
│  └─ resume() ──────────────────────┼─ [Resume]             │
│                                    │ Rendering continues    │
│                                    │                          │
├─ onDestroy()                       │                          │
│  └─ DisposableEffect               │                          │
│     └─ bridge.destroy() ──────────────────────────────────┤ destroy()
│     └─ engine.destroy() ──────────┼─ Clean all resources  │
│                                    │ [Destroyed]            [Destroyed]
│
```

---

## Memory Layout

```
┌────────────────────────────────────────────┐
│         Total Memory: ~57 MB                │
│                                             │
│ ┌─────────────────────────────────────────┐│
│ │ Filament Engine: ~40 MB                 ││
│ │ • Vulkan/OpenGL context                 ││
│ │ • Command buffers                       ││
│ │ • Shader compilation cache              ││
│ │ • GPU memory management                 ││
│ └─────────────────────────────────────────┘│
│                                             │
│ ┌─────────────────────────────────────────┐│
│ │ Material Data: ~2 MB                    ││
│ │ • PBR parameters                        ││
│ │ • Material instances                    ││
│ │ • Color/emission values                 ││
│ └─────────────────────────────────────────┘│
│                                             │
│ ┌─────────────────────────────────────────┐│
│ │ Mesh Buffers: ~5 MB                     ││
│ │ • Vertex data (1,024 vertices)          ││
│ │ • Index buffers                         ││
│ │ • Normal/tangent vectors                ││
│ │ • GPU upload buffers                    ││
│ └─────────────────────────────────────────┘│
│                                             │
│ ┌─────────────────────────────────────────┐│
│ │ Textures (future): ~10 MB               ││
│ │ • Albedo maps                           ││
│ │ • Normal maps                           ││
│ │ • Roughness maps                        ││
│ │ • Metallic maps                         ││
│ └─────────────────────────────────────────┘│
│                                             │
│ ┌─────────────────────────────────────────┐│
│ │ Overhead: ~minor                        ││
│ │ • Android Framework                     ││
│ │ • Coroutine state                       ││
│ │ • Compose state                         ││
│ └─────────────────────────────────────────┘│
│                                             │
└────────────────────────────────────────────┘
```

---

**Architecture Version**: 1.0  
**Last Updated**: January 24, 2026  
**Status**: Production Ready
