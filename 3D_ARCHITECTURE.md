# SA-AIHOS 3D Scene Architecture Guide

## Overview

This document provides a comprehensive guide to the ultra-advanced 3D animated UI system for SA-AIHOS (Self-Evolving Autonomous AI-Human OS). The system leverages **Three.js** for real-time 3D rendering and seamlessly integrates with Android via **WebView** and a **JavaScript-Kotlin bridge**.

---

## 🎯 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Android Application                    │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Compose UI Layer                                      │ │
│  │  - Three3DScreen (Composable)                          │ │
│  │  - SAIHOSViewModel                                     │ │
│  └────────────────┬─────────────────────────────────────┘ │
│                   │                                         │
│  ┌────────────────▼─────────────────────────────────────┐ │
│  │  Three3DWebView (Custom WebView)                     │ │
│  │  - JavaScript Interface Bridge                        │ │
│  │  - Message Handling (Scene ↔ Android)                │ │
│  │  - Lifecycle Management                              │ │
│  └────────────────┬─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                     │
                     │ (WebView Bridge)
                     │
┌─────────────────────────────────────────────────────────────┐
│                   3D Scene (WebGL/Three.js)                 │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Scene Manager (Scene.js)                              │ │
│  │  - Three.js Initialization                             │ │
│  │  - Rendering Loop                                      │ │
│  │  - Component Management                                │ │
│  └────────────────┬─────────────────────────────────────┘ │
│                   │                                         │
│  ┌────────────────▼─────────────────────────────────────┐ │
│  │  AI-Core Component                                    │ │
│  │  - Crystal Geometry (Icosahedron)                    │ │
│  │  - Neural Network Lines                              │ │
│  │  - Energy Field Aura                                 │ │
│  │  - Particle System                                   │ │
│  └────────────────────────────────────────────────────┘ │
│                   │                                        │
│  ┌────────────────▼──────────────────────────────────┐ │
│  │  Animation System                                  │ │
│  │  - Breathing Effect                               │ │
│  │  - Rotation Animation                             │ │
│  │  - Pulse Effect                                   │ │
│  │  - Color Cycling                                  │ │
│  └────────────────────────────────────────────────┘ │
│                   │                                    │
│  ┌────────────────▼──────────────────────────────┐ │
│  │  Lighting System                               │ │
│  │  - Primary Point Light (Cyan)                 │ │
│  │  - Secondary Point Light (Magenta)            │ │
│  │  - Accent Light (Blue)                        │ │
│  │  - Orbital Light Motion                       │ │
│  └────────────────────────────────────────────┘ │
│                   │                              │
│  ┌────────────────▼──────────────────────────┐ │
│  │  Effects Manager                           │ │
│  │  - Film Grain                              │ │
│  │  - Tone Mapping (ACES Filmic)             │ │
│  │  - Post-Processing                         │ │
│  └────────────────────────────────────────┘ │
│                   │                           │
│  ┌────────────────▼──────────────────────┐ │
│  │  Android Bridge                        │ │
│  │  - Message Serialization/Deserialization│ │
│  │  - Event System                        │ │
│  │  - Message Queue                       │ │
│  │  - Android Native Integration          │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

---

## 📁 Folder Structure

```
3d-scene/
├── index.html                          # Main HTML entry point
├── assets/                             # 3D models and textures (future)
│   ├── models/
│   ├── textures/
│   └── shaders/
│
├── src/
│   ├── Scene.js                        # Core scene manager
│   │
│   ├── components/
│   │   └── AICore.js                   # AI-core visual component
│   │
│   ├── animations/
│   │   └── AnimationController.js      # Animation system
│   │
│   ├── lighting/
│   │   └── LightingSystem.js           # Multi-light setup
│   │
│   ├── effects/
│   │   └── EffectsManager.js           # Post-processing effects
│   │
│   ├── utils/
│   │   └── (Math, geometry, helpers)
│   │
│   └── bridge/
│       └── AndroidBridge.js            # Android communication

app/src/main/kotlin/com/aihos/ui/
├── three_d/
│   └── Three3DWebView.kt               # Custom WebView wrapper
│
└── screens/
    └── Three3DScreen.kt                # Compose screen integration
```

---

## 🔧 Core Components

### 1. **Scene Manager (Scene.js)**

**Responsibility**: Orchestrates the entire 3D scene lifecycle, rendering loop, and component coordination.

**Key Methods**:
- `initialize()` - Setup Three.js, camera, renderer, and subsystems
- `render()` - Main rendering loop with frame-based delta time
- `_updateCameraOrbit()` - Smooth orbital camera motion
- `setAnimationIntensity(intensity)` - Control global animation intensity
- `setColorTheme(themeId)` - Change visual theme

**Architecture**:
```javascript
// Scene structure
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, aspect, 0.1, 10000);
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });

// Component hierarchy
this.aiCore = new AICore(scene);           // Main visual
this.animationController = new AnimationController();  // Animation system
this.lightingSystem = new LightingSystem(scene);       // Lighting
this.effectsManager = new EffectsManager(scene, renderer); // Effects
```

---

### 2. **AI-Core Component (AICore.js)**

**Responsibility**: Represents the visual heart of SA-AIHOS with four interconnected layers.

**Visual Composition**:

#### a) **Core Crystal** (Icosahedron)
- Geometry: `IcosahedronGeometry(radius=0.8, subdivisions=5)`
- Material: Custom ShaderMaterial with fresnel effect
- Animation: Breathing scale, rotation
- Features: Edge glow, pulsing brightness

```glsl
// Crystal Fragment Shader
vec3 viewDir = normalize(cameraPosition - vPosition);
float fresnel = pow(1.0 - dot(viewDir, vNormal), 3.0);
vec3 color = mix(baseColor * 0.3, baseColor, fresnel);
color += baseColor * glowIntensity * fresnel * pulse;
```

#### b) **Neural Network** (Particle Connection Lines)
- 40 particles distributed in spherical pattern
- Lines connecting adjacent particles
- Wobbling animation based on time
- Color: Magenta (#ff0088) at 40% opacity

#### c) **Energy Field** (Aura Layer)
- Icosahedron geometry (larger scale: 1.3x)
- BackSide rendering for inner glow
- Additive blending for luminous effect
- Pulsing intensity based on animation state

#### d) **Particle System** (Floating Lights)
- 100 floating energy particles
- Random velocities and colors
- Boundary-based position reset
- 3D velocity-based physics

**Animation Methods**:
```javascript
updateAnimation(deltaTime, animationState) {
  // Apply animations
  rotationX, rotationY, rotationZ    // From AnimationController
  breathIntensity                     // Pulse breathing
  pulseIntensity                      // Glow intensity
  colorPhase                          // Color cycling
}
```

---

### 3. **Animation Controller (AnimationController.js)**

**Responsibility**: Manages all micro-animations with smooth composition and intensity control.

**Animation Types**:

| Animation | Frequency | Range | Purpose |
|-----------|-----------|-------|---------|
| **Breathing** | 2.0 Hz | ±10% scale | Subtle expansion/contraction |
| **Rotation** | Continuous | X: 0.3°/frame, Y: 0.5°/frame, Z: 0.1°/frame | Constant spin |
| **Pulse** | 1.5 Hz | 0.5-1.0 intensity | Glow breathing |
| **Color Cycle** | 0.5 Hz | 0-2π | Shader-driven hue shifts |

**Core Loop**:
```javascript
update(deltaTime) {
  this.state.time += deltaTime;
  this._updateAnimationState();
  
  for (animatable of this.animatables) {
    animatable.updateAnimation(deltaTime, this.currentAnimationState);
  }
}
```

**Intensity Control** (0.0 - 1.0):
- 0.0 = Paused (no animations)
- 0.5 = Normal (all animations at 50% amplitude)
- 1.0 = Maximum (full animation intensity)

---

### 4. **Lighting System (LightingSystem.js)**

**Responsibility**: Provides dynamic, multi-light illumination with orbital motion.

**Light Configuration**:

| Light | Type | Color | Position | Purpose |
|-------|------|-------|----------|---------|
| **Primary** | PointLight | Cyan (#00ffaa) | Orbital (3.5m radius) | Key lighting |
| **Secondary** | PointLight | Magenta (#ff0055) | Counter-orbital | Fill lighting |
| **Accent** | PointLight | Blue (#0088ff) | Top-rear | Rim/accent |
| **Rim** | DirectionalLight | White | (5, 5, 5) | Global illumination |
| **Ambient** | AmbientLight | Theme-based | Omnidirectional | Base light |

**Shadow Configuration**:
```javascript
// High-quality shadow mapping
shadowMap.mapSize = 2048x2048
shadowMap.type = PCFShadowShadowMap
shadowMap.radius = 4
camera.near = 0.1
camera.far = 20
```

**Dynamic Updates**:
- Lights orbit at `0.5 rad/second`
- Intensity pulsates based on animation state
- Colors change with theme selection
- Vertical oscillation for vertical accent light

---

### 5. **Effects Manager (EffectsManager.js)**

**Responsibility**: Post-processing and visual enhancements.

**Current Effects**:
- **Tone Mapping**: ACES Filmic tone mapping for cinematic look
- **Film Grain**: Subtle noise overlay for vintage feel
- **Color Space**: sRGB color space for accurate color reproduction

**Future Effects** (Ready for implementation):
- Bloom/Glow (built into shaders currently)
- Depth of Field
- Motion Blur
- Chromatic Aberration

---

### 6. **Android Bridge (AndroidBridge.js)**

**Responsibility**: Bidirectional communication between JavaScript and Android.

**Message Protocol**:
```javascript
{
  method: string,           // Command name
  data: Map<string, string>, // Payload
  timestamp: number         // Unix timestamp
}
```

**Supported Methods**:

**Android → JavaScript**:
- `setTheme(themeId)` - Change visual theme
- `setAnimationIntensity(intensity)` - Control animation speed
- `pause()` - Pause rendering
- `resume()` - Resume rendering
- `getMetrics()` - Request performance data

**JavaScript → Android**:
- `sceneInitialized` - Scene ready event
- `error` - Error notification
- `mouseMoved(x, y)` - Mouse position (0-1 normalized)
- `clicked` - Click event
- `resized(width, height)` - Window resize event
- `metrics` - Performance metrics update

**Features**:
- Message queue for offline scenarios
- Event callback system
- Automatic message batching to reduce overhead

---

## 🎨 Visual Themes

Four pre-configured color themes with coordinated lighting:

### **Cyan** (Default)
```
Primary:   #00ff88 (Bright Cyan-Green)
Secondary: #ff0055 (Magenta)
Tertiary:  #0088ff (Bright Blue)
Ambient:   #0a1a2e (Dark Blue)
```

### **Purple**
```
Primary:   #aa00ff (Purple)
Secondary: #ff00aa (Hot Pink)
Tertiary:  #00ffff (Cyan)
Ambient:   #1a0a2e (Dark Purple)
```

### **Red**
```
Primary:   #ff0055 (Red)
Secondary: #00ffaa (Cyan)
Tertiary:  #ff8800 (Orange)
Ambient:   #2e0a0a (Dark Red)
```

### **Blue**
```
Primary:   #0088ff (Blue)
Secondary: #00ffff (Cyan)
Tertiary:  #0055ff (Bright Blue)
Ambient:   #0a0a2e (Very Dark Blue)
```

---

## 📱 Android Integration

### **Three3DWebView (Three3DWebView.kt)**

Custom WebView subclass with:
- JavaScript interface for two-way communication
- Message serialization/deserialization (Kotlin Serialization)
- Lifecycle management (pause/resume)
- Settings optimization for 3D performance
- WebGL support

**Key Methods**:
```kotlin
fun load3DScene(assetsPath: String)          // Load scene HTML
fun sendMessage(method: String, data: Map)   // Send to JS
fun setTheme(themeId: String)                // Change theme
fun setAnimationIntensity(intensity: Double) // Control animation
fun pauseScene()                              // Pause rendering
fun resumeScene()                             // Resume rendering
fun requestMetrics()                          // Get performance data
fun takeScreenshot()                          // Capture frame
```

### **Three3DScreen (Three3DScreen.kt)**

Jetpack Compose screen composable:
- Wraps WebView in `AndroidView`
- Handles initialization loading
- Manages error states
- Integrates with ViewModel
- Provides message handling callback

---

## 🚀 Performance Optimization

### **Rendering Optimization**
- Pixel ratio scaling: Adapts to device DPI
- Shadow map resolution: 2048×2048 (balanced quality/performance)
- Particle count: 100 particles (tunable)
- Frame rate: Uncapped (uses requestAnimationFrame)

### **Memory Optimization**
- Geometry reuse where possible
- Shader compilation once at startup
- Automatic garbage collection via Three.js
- WebView lifecycle pause/resume to stop rendering

### **Network Optimization**
- Single HTML file with inline CSS
- Three.js loaded from CDN
- Minimal message frequency (random sampling for mouse events)
- Message batching support

---

## 🎮 Interaction Design

### **Current Interactions**
1. **Automatic Orbital Motion**: Camera circles the AI-Core
2. **Color Themes**: 4 pre-set visual themes
3. **Animation Intensity**: Control breathing, pulse, rotation
4. **Pause/Resume**: Stop/start animations

### **Future Interactions** (Ready for Implementation)
```javascript
// Potential extensions
- Drag to rotate AI-Core
- Click to trigger effects
- Voice command integration
- Gesture control
- Real-time data visualization binding
```

---

## 📊 Performance Metrics

The scene provides real-time metrics:
- **Frame Count**: Total frames rendered
- **Delta Time**: Time since last frame
- **Is Animating**: Boolean animation state
- **Scene Object Count**: Number of Three.js objects
- **Camera Position/Rotation**: Current camera state

**Monitor via**:
```javascript
const metrics = scene.getMetrics();
console.log(`FPS: ${1/metrics.deltaTime}`);
console.log(`Objects: ${metrics.sceneObjectCount}`);
```

---

## 🔄 Lifecycle

### **Initialization Flow**
```
1. HTML loads index.html
2. JavaScript imports Scene.js
3. Scene initializes:
   - Three.js scene/camera/renderer
   - AICore component
   - AnimationController
   - LightingSystem
   - EffectsManager
   - AndroidBridge
4. Android bridge sends sceneInitialized
5. Render loop starts (requestAnimationFrame)
```

### **Pause/Resume Flow**
```
pause():
- Set isAnimating = false
- Stop requestAnimationFrame
- Notify Android

resume():
- Set isAnimating = true
- Restart requestAnimationFrame
- Notify Android
```

### **Cleanup Flow**
```
dispose():
- Cancel animation frame
- Dispose all geometries
- Dispose all materials
- Remove canvas from DOM
- Clear scene graph
- Notify Android
```

---

## 🛠️ Extension Guide

### **Adding New Components**

1. Create new file in `src/components/YourComponent.js`:
```javascript
export class YourComponent {
  constructor(scene) {
    this.scene = scene;
    this.group = new THREE.Group();
    this.scene.add(this.group);
  }

  initialize() {
    // Setup geometry, materials, meshes
  }

  updateAnimation(deltaTime, animationState) {
    // Called every frame from AnimationController
  }

  dispose() {
    // Cleanup resources
  }
}
```

2. Register in `Scene.js`:
```javascript
this.yourComponent = new YourComponent(this.scene);
await this.yourComponent.initialize();
this.animationController.registerAnimatable(this.yourComponent);
```

### **Adding New Animations**

1. Add to `AnimationController.animations`:
```javascript
this.animations.yourAnimation = { enabled: true, frequency: 1.0, amplitude: 1.0 }
```

2. Calculate in `_updateAnimationState()`:
```javascript
this.currentAnimationState.yourValue = 
  Math.sin(t * frequency) * amplitude * intensity;
```

3. Use in `updateAnimation()`:
```javascript
updateAnimation(deltaTime, state) {
  this.mesh.rotation.z = state.yourValue;
}
```

### **Adding New Themes**

1. Add to `LightingSystem.colorThemes`:
```javascript
newTheme: {
  primary: 0x123456,
  secondary: 0x654321,
  accent: 0xabcdef,
  ambient: 0x000000
}
```

2. Add to `AICore.config.colors` if needed

---

## 📚 Dependencies

- **Three.js** (r128): `https://cdn.jsdelivr.net/npm/three@r128/build/three.module.js`
- **Android WebView API** (API 21+)
- **Kotlin Serialization**: For message serialization
- **Jetpack Compose**: For UI integration

---

## 🐛 Debugging

### **JavaScript Console**
```javascript
// Enable detailed logging
window.DEBUG = true;

// Access scene instance
const scene = window.SAIHOSSceneInstance;

// Get metrics
console.log(scene.getMetrics());

// Test communication
scene.bridge.sendToAndroid('test', { message: 'Hello Android' });
```

### **Android Logcat**
```bash
# Watch WebView messages
adb logcat | grep "Three3DWebView"

# Watch all AIHOS logs
adb logcat | grep "com.aihos"
```

---

## 📖 File Reference

| File | Purpose | Lines |
|------|---------|-------|
| `Scene.js` | Core scene manager | ~450 |
| `AICore.js` | AI-core visual | ~380 |
| `AnimationController.js` | Animation system | ~150 |
| `LightingSystem.js` | Lighting setup | ~200 |
| `EffectsManager.js` | Post-processing | ~120 |
| `AndroidBridge.js` | Android communication | ~200 |
| `Three3DWebView.kt` | Android WebView wrapper | ~250 |
| `Three3DScreen.kt` | Compose integration | ~150 |
| `index.html` | HTML entry point | ~350 |

**Total**: ~2,250 lines of code + documentation

---

## 🎯 Next Steps

1. **Test on Device**: Run on Android device to verify WebView performance
2. **Add Interactions**: Implement drag-to-rotate, click effects
3. **Sound Integration**: Add audio visualization tied to animations
4. **Data Binding**: Connect AI metrics to visual representation
5. **Advanced Shaders**: Implement more sophisticated vertex/fragment shaders
6. **Bloom Pass**: Add proper post-processing bloom effect
7. **Recording**: Add screen recording capability

---

## 📞 Support

For issues or questions:
1. Check browser console for JavaScript errors
2. Check Android logcat for bridge errors
3. Verify WebView settings in `Three3DWebView.kt`
4. Test with different Three.js versions if needed

---

**Created**: January 24, 2026  
**Version**: 1.0.0  
**Status**: Production-Ready Foundation

The 3D scene architecture is designed to be **modular**, **extensible**, and **performant**, ready for integration of advanced features and real-world data visualization.
