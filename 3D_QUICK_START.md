# SA-AIHOS 3D Scene - Quick Start Guide

## 🚀 Getting Started in 5 Minutes

### Prerequisites
- Android Studio Iguana+
- Web browser with WebGL support (for testing)
- Asset folder at `app/src/main/assets/3d-scene/`

### Step 1: Copy 3D Assets

```bash
# Copy 3D scene files to Android assets
mkdir -p app/src/main/assets/3d-scene
cp 3d-scene/* app/src/main/assets/3d-scene/
```

### Step 2: Test in Browser (Optional)

Before running on Android, test locally:

```bash
# Navigate to 3d-scene folder
cd 3d-scene

# Start a simple HTTP server
python3 -m http.server 8000
# or
npx http-server

# Open browser to http://localhost:8000
```

### Step 3: Integrate into Android

Add the `Three3DScreen` to your navigation:

```kotlin
// In your navigation setup
composable(
    route = "three-d",
    content = { Three3DScreen() }
)
```

### Step 4: Build & Run

```bash
./gradlew build
./gradlew installDebug
```

---

## 🎮 Basic Usage

### In Compose

```kotlin
@Composable
fun MyScreen() {
    Three3DScreen(
        modifier = Modifier.fillMaxSize(),
        onSceneMessage = { message ->
            when (message.method) {
                "clicked" -> println("User clicked 3D scene")
                "mouseMoved" -> println("Mouse at: ${message.data}")
                else -> println("Received: ${message.method}")
            }
        }
    )
}
```

### Controlling the Scene

```kotlin
// Get reference to WebView (inside AndroidView factory)
val webView: Three3DWebView = /* ... */

// Set theme
webView.setTheme("purple")

// Control animations
webView.setAnimationIntensity(0.5)

// Pause/Resume
webView.pauseScene()
webView.resumeScene()

// Get metrics
webView.requestMetrics()

// Screenshot
webView.takeScreenshot()
```

---

## 🎨 Color Themes

Switch between 4 pre-configured themes:

```kotlin
webView.setTheme("cyan")      // Default
webView.setTheme("purple")    // Purple tones
webView.setTheme("red")       // Red/orange
webView.setTheme("blue")      // Blue tones
```

---

## 📊 HTML Controls

When loaded in browser, the index.html provides:

- **Pause Button**: Stop/resume animations
- **Theme Button**: Cycle through 4 themes
- **Intensity Slider**: Control animation intensity (0.0 - 1.0)
- **Metrics Display**: Real-time FPS, object count, theme

---

## 🔧 Configuration

### Adjust Animation Speed

In `AnimationController.js`:

```javascript
this.animations.rotation.speed = { x: 0.3, y: 0.5, z: 0.1 };
// Increase for faster rotation
// Decrease for slower rotation
```

### Adjust Particle Count

In `AICore.js`:

```javascript
// Neural network particles
const particleCount = 40;  // Change this

// Floating particles
const particleCount = 100; // Change this
```

### Adjust Camera Orbit

In `Scene.js`:

```javascript
const orbitSpeed = 0.2;      // radians per second
const orbitRadius = 3.5;     // distance from center
```

### Adjust Light Colors

In `LightingSystem.js`:

```javascript
this.lights.primary = new THREE.PointLight(0x00ffaa, 2.0, 10);
// First param: color (hex)
// Second param: intensity
// Third param: distance
```

---

## 📱 Lifecycle Handling

The WebView automatically handles:

- **Pause on Activity Pause**: `onPause()` pauses scene
- **Resume on Activity Resume**: `onResume()` resumes scene
- **Dispose on Activity Destroy**: Cleanup all resources

No additional lifecycle handling needed.

---

## 🎬 Animation States

### Current Animation Capabilities

```javascript
// Breathing - subtle pulsing scale
// Frequency: 2.0 Hz, Amplitude: ±10%

// Rotation - continuous spin
// X: 0.3°/frame, Y: 0.5°/frame, Z: 0.1°/frame

// Pulse - glow intensity oscillation
// Frequency: 1.5 Hz

// Color Cycle - shader-driven hue shifts
// Frequency: 0.5 Hz
```

### Control Intensity

```kotlin
// 0.0 = Paused
webView.setAnimationIntensity(0.0)

// 0.5 = Half speed/intensity
webView.setAnimationIntensity(0.5)

// 1.0 = Full speed/intensity
webView.setAnimationIntensity(1.0)
```

---

## 🔍 Debugging

### Browser Console

```javascript
// Access scene
const scene = window.SAIHOSSceneInstance;

// Check metrics
console.log(scene.getMetrics());

// Test theme change
scene.setColorTheme('purple');

// Test intensity
scene.setAnimationIntensity(0.3);

// Manual pause
scene.pause();

// Manual resume
scene.resume();
```

### Android Logcat

```bash
# Filter WebView logs
adb logcat | grep "Three3DWebView"

# Filter all AIHOS logs
adb logcat | grep "com.aihos"

# Watch JavaScript console output
adb logcat | grep "SAIHOSBridge"
```

---

## ⚡ Performance Tips

1. **Reduce Particle Count** if FPS drops below 30
2. **Lower Shadow Map Resolution** to 1024x1024 if memory-constrained
3. **Disable Film Grain** for faster rendering
4. **Pause When Off-Screen** using app lifecycle
5. **Use Lower Animation Intensity** for battery saving

---

## 🛠️ Customization Examples

### Change Crystal Color

In `AICore.js`:

```javascript
this.config.colors = {
  primary: new THREE.Color(0xff0055),  // Change to magenta
  secondary: new THREE.Color(0x0088ff),
  tertiary: new THREE.Color(0x00ff88),
  glow: new THREE.Color(0xff0088),
};
```

### Add New Animation

In `AnimationController.js`:

```javascript
// 1. Add to animations
this.animations.myAnimation = { enabled: true, frequency: 1.0 };

// 2. Calculate in _updateAnimationState()
this.currentAnimationState.myValue = 
  Math.sin(t * frequency);

// 3. Use in AICore.updateAnimation()
this.group.scale.y = 1 + animationState.myValue * 0.2;
```

### Change Light Positions

In `LightingSystem.js`:

```javascript
// Static position instead of orbital
this.lights.primary.position.set(3, 2, 2);

// Or custom orbital with different parameters
const radius = 5;  // Bigger orbit
const speed = 0.2; // Same speed
```

---

## 📚 File Structure

```
3d-scene/
├── index.html                    # Start here!
├── src/
│   ├── Scene.js                  # Main scene manager
│   ├── components/AICore.js      # AI-core visual
│   ├── animations/AnimationController.js
│   ├── lighting/LightingSystem.js
│   ├── effects/EffectsManager.js
│   └── bridge/AndroidBridge.js   # Android communication
└── assets/                        # For 3D models/textures
```

---

## 🚨 Common Issues

### WebView Shows Blank
- Check that files are in `app/src/main/assets/3d-scene/`
- Verify `index.html` can find `src/Scene.js`
- Check browser console for file not found errors

### Performance Issues
- Lower animation intensity with `setAnimationIntensity()`
- Reduce particle count in `AICore.js`
- Disable effects in `EffectsManager.js`

### Theme Not Changing
- Verify `webView.setTheme()` is called after scene loads
- Check that theme ID matches: `cyan`, `purple`, `red`, `blue`

### No Android Communication
- Verify `SAIHOSBridge` is available: `typeof window.SAIHOSBridge`
- Check logcat for JavaScript errors
- Ensure WebView JavaScript is enabled

---

## 📖 Learn More

- **Full Architecture**: See `3D_ARCHITECTURE.md`
- **Three.js Docs**: https://threejs.org/docs/
- **WebGL Fundamentals**: https://webglfundamentals.org/

---

## 🎯 What's Next

1. ✅ 3D scene working
2. ✅ Android integration complete
3. ⬜ Add interactions (drag, click, voice)
4. ⬜ Bind to AI metrics (memory, reasoning output)
5. ⬜ Add sound/music visualization
6. ⬜ Record/share scenes

---

## 💡 Pro Tips

- **Browser Testing**: Use Chrome DevTools for JavaScript debugging
- **Performance Profiling**: Use Chrome Performance tab to profile rendering
- **Mobile Testing**: Use `adb logcat` to monitor on real device
- **Hot Reload**: Modify JavaScript, refresh browser to see changes instantly
- **Multiple Themes**: Script theme cycling for demos

---

**Ready to visualize AI evolution in real-time!** 🚀

For detailed technical information, see `3D_ARCHITECTURE.md`.
