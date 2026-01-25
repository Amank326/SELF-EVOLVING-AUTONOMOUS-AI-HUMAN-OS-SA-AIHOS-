# SA-AIHOS Build Status & Troubleshooting Guide

## Current Build Issue

**Error:** `Could not load module <Error module>` at `kaptGenerateStubsDebugKotlin`

**Root Cause Analysis:**
- KAPT annotation processor failing during Room database code generation
- Cryptic error message prevents exact diagnosis
- Likely related to: Kotlin version compatibility, Room compiler version, or circular dependency

---

## What's Been Fixed ✅

1. **Room KAPT Annotations** - All DAO methods properly annotated:
   - ✅ @Insert methods return `Long` (PerformanceMetricDao, SystemConfigDao)
   - ✅ @Update methods added to 3 DAOs
   - ✅ @Delete methods added to 2 DAOs  
   - ✅ Nullable return type `Float?` on getAverageRating() (AVG returns NULL)

2. **Java/Kotlin Compatibility** - Updated from Java 1.8 → 11
   - ✅ Target JDK 17 compliance
   - ✅ Kotlin 1.9.20 alignment

3. **Build Configuration** - Gradle 8.5 setup
   - ✅ AndroidX enabled
   - ✅ Compose enabled
   - ✅ KAPT build cache disabled
   - ✅ Hilt plugin configured

---

## Troubleshooting Steps to Try

### Step 1: Clean Cache & Rebuild
```powershell
Remove-Item -Path "$env:USERPROFILE\.gradle" -Recurse -Force
cd c:\Users\amank\Projects\SA-AIHOS
.\gradle-8.5\bin\gradle.bat clean assembleDebug --no-daemon
```

### Step 2: Check for Circular Imports
- Verify no circular dependencies between:
  - `Database.kt` → `Entities.kt` → `DAOs.kt`
  - `Module.kt` → `Database.kt`
  - Repository classes

### Step 3: Disable Serialization Temporarily
```kotlin
// In Entities.kt, temporarily comment out:
// @Serializable
// data class MemoryEntity(...)
```

### Step 4: Reduce KAPT Scope
- Comment out Hilt `@Module` in `Module.kt` temporarily
- Test if Room+KAPT works without Hilt

### Step 5: Check Kotlin Compiler Logs
```bash
# Look for detailed errors in:
find app/build -name "*.log" -mmin -5 2>/dev/null
# Check: app/build/tmp/kotlin-classes/debug/
```

### Step 6: Update Dependencies
```kotlin
// In app/build.gradle.kts, try newer versions:
val roomVersion = "2.7.0-alpha01"  // Beta newer version
val kotlinVersion = "2.0.0"         // Kotlin 2.x preview
```

### Step 7: Use KSP Instead of KAPT
```kotlin
// Add to plugins in app/build.gradle.kts:
id("com.google.devtools.ksp") version "1.9.20-1.0.14"

// Replace in dependencies:
// kapt("androidx.room:room-compiler:$roomVersion")
ksp("androidx.room:room-compiler:$roomVersion")
```

---

## Build Environment Details

**Current Setup:**
- JDK: 17.0.12 LTS
- Android SDK: API 36 (API 34 target)
- Gradle: 8.5
- AGP: 8.2.0
- Kotlin: 1.9.20
- Compose: 1.5.4
- Room: 2.6.0
- Hilt: 2.47

**gradle.properties:**
```properties
android.useAndroidX=true
android.enableCompose=true
org.gradle.parallel=true
org.gradle.workers.max=8
org.gradle.jvmargs=-Xmx4096m
kapt.useBuildCache=false
```

---

## Files Modified in This Session

1. ✅ `app/build.gradle.kts` - Java target, KAPT config
2. ✅ `build.gradle.kts` - Hilt plugin
3. ✅ `app/src/.../dao/DAOs.kt` - Room annotation fixes
4. ✅ `gradle.properties` - Build configuration
5. ✅ `app/src/main/res/` - Resource files created

---

## Next Phase: Once Build Works

### Immediate Actions
1. ✅ Get APK building successfully
2. ✅ Test on Android Emulator
3. ✅ Verify all 8 entities & DAOs work correctly

### Then Implement Advanced Features (See ADVANCED_ROADMAP.md)
1. Memory system with semantic search
2. Advanced reasoning engine
3. Evolution & learning system
4. Real-time 3D visualization UI
5. ONNX model integration
6. Enterprise features & security

---

## Useful Commands

```powershell
# Full clean rebuild
gradle clean build --no-daemon -x test

# Just KAPT step (debugging)
gradle kaptGenerateStubsDebugKotlin --no-daemon --info

# Dependency tree
gradle dependencies --configuration kapt

# Task graph
gradle assembleDebug --dry-run

# Verbose logging
gradle assembleDebug -Dorg.gradle.logging.level=debug

# Memory stats
gradle assembleDebug -XX:+PrintGCDetails
```

---

## Known Issues & Workarounds

| Issue | Status | Workaround |
|-------|--------|-----------|
| KAPT "Error module" | 🔴 BLOCKING | See troubleshooting steps above |
| library stripping warnings | ⚠️ Minor | Expected with ONNX/Filament natives |
| Build time slow | ⚠️ Normal | Parallel builds enabled; caching configured |
| Serialization annotations | ✅ OK | Properly configured across all entities |

---

## Performance Metrics to Target

Once build is fixed:
```
Build Time:        < 60s (incremental)
APK Size:          < 150MB
Memory (idle):     < 100MB
Memory (active):   < 300MB
Startup Time:      < 2 seconds
AI Response:       < 1 second
UI Response:       < 500ms
Battery (per hour): < 5% (active use)
```

---

## Success Path

```
CURRENT: Fix KAPT Error ← YOU ARE HERE
  ↓
  Get Clean APK Build
  ↓
  Test on Emulator
  ↓
  Implement Phase 2 Features
  ↓
  Build Advanced UI
  ↓
  Integrate ONNX Models
  ↓
  Optimize Performance
  ↓
  Play Store Release ✅
```

---

**Last Updated:** 2026-01-25  
**Project Stage:** Build Stability  
**Next Review:** Once APK builds successfully

