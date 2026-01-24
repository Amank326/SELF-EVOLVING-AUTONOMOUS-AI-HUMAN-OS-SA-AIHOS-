# SA-AIHOS Build and Release Guide

**Status**: Ready for Release (v1.0.0)
**Last Updated**: January 24, 2026

---

## Quick Start

### For Development Builds

```bash
# Clean build
./gradlew.bat clean

# Build debug APK
./gradlew.bat assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### For Release Builds

```bash
# Generate signed release bundle
./gradlew.bat bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

---

## Prerequisites

### System Requirements
- **Java**: JDK 17 or later (OpenJDK or Oracle JDK)
- **Android SDK**: API 34 (Android 14)
  - Minimum SDK: API 28 (Android 9.0)
  - Target SDK: API 34 (Android 14)
- **Gradle**: 8.5+ (handled by gradle wrapper)
- **OS**: Windows, macOS, or Linux

### Environment Setup

#### Windows

```batch
# Set JAVA_HOME environment variable
setx JAVA_HOME "C:\Program Files\Java\jdk-17"

# Optional: Verify Java installation
java -version

# On macOS/Linux, add to ~/.bashrc or ~/.zshrc:
# export JAVA_HOME=/usr/libexec/java_home -v 17
# export PATH=$JAVA_HOME/bin:$PATH
```

#### Install Android SDK
- Download [Android Studio](https://developer.android.com/studio)
- Or use [Android SDK Command-line Tools](https://developer.android.com/studio/command-line)

```bash
# Install required SDK components
sdkmanager "platforms;android-34" "build-tools;34.0.0" "ndk-bundle"
```

---

## Build Configuration

### Project Structure

```
SA-AIHOS/
├── app/                          # Main application module
│   ├── build.gradle.kts         # App-level build configuration
│   ├── proguard-rules.pro       # ProGuard/R8 configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── kotlin/com/aihos/  # Source code
│   │   │   └── assets/            # 3D scene files
│   │   └── test/                  # Unit tests
│   ├── build/                     # Build outputs
│   │   └── outputs/
│   │       ├── apk/              # APK files
│   │       └── bundle/           # AAB files
├── build.gradle.kts             # Project-level build configuration
├── settings.gradle.kts          # Gradle settings
└── gradle/wrapper/              # Gradle wrapper files
```

### Key Build Configuration Details

#### build.gradle.kts (Project Level)

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}
```

- **AGP Version**: 8.2.0 (Android Gradle Plugin)
- **Kotlin Version**: 1.9.20

#### build.gradle.kts (App Level)

```kotlin
android {
    namespace = "com.aihos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aihos"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}
```

**Key Settings**:
- **Namespace**: `com.aihos` (must match in AndroidManifest.xml)
- **Target SDK**: 34 (latest stable Android API)
- **Min SDK**: 28 (Android 9.0 - supports ~97% of devices)
- **Version Code**: Increment for each release
- **Version Name**: Semantic versioning (MAJOR.MINOR.PATCH)
- **Proguard**: Enabled in release builds for code shrinking
- **Compose**: Latest stable versions

---

## Building the App

### Debug Build (Development)

```bash
./gradlew.bat assembleDebug
```

**What it does**:
- Compiles code without minification (faster builds)
- Creates an unsigned APK suitable for development and testing
- No code obfuscation (full method names visible in debugger)
- All logging statements enabled
- Suitable for: Development, testing, profiling

**Output Location**:
```
app/build/outputs/apk/debug/app-debug.apk
```

**Install on Device/Emulator**:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release Build (Production)

```bash
./gradlew.bat bundleRelease
```

**What it does**:
- Minifies code with ProGuard/R8 (reduces APK size ~30%)
- Removes debug logging
- Generates optimized machine code
- Creates an Android App Bundle (.aab format)

**Output Location**:
```
app/build/outputs/bundle/release/app-release.aab
```

**For Testing Before Release**:
```bash
./gradlew.bat assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Code Minification (ProGuard/R8)

### ProGuard Configuration (`proguard-rules.pro`)

The project includes R8 configuration that:

1. **Keeps Essential Classes**:
   - Kotlin runtime (`kotlin.**`)
   - Room database (`androidx.room.**`)
   - Hilt DI framework (`dagger.**`)
   - AI model classes (`com.aihos.ai.**`)
   - Serialization classes

2. **Removes Debug Logging**:
   ```proguard
   -assumenosideeffects class timber.log.Timber {
       public static *** v(...);
       public static *** d(...);
       public static *** i(...);
   }
   ```

3. **Preserves Method Names**:
   - Critical for reflection-based code
   - Maintains performance of rule engine

**If you add new packages**:
- Update `proguard-rules.pro` if they contain reflection/serialization
- Example: `-keep class com.aihos.newfeature.** { *; }`

---

## AndroidManifest Validation

### Current Configuration

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name="com.aihos.SAIHOSApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.SAIHOS"
        android:usesCleartextTraffic="false"
        tools:targetApi="34">
        
        <activity android:name="com.aihos.ui.MainActivity"
                  android:exported="true" />
        
        <service android:name="com.aihos.ai.AutonomousDecisionService"
                 android:exported="false" />
    </application>
</manifest>
```

### Permissions (Production)

```xml
<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Device Information -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.GET_PACKAGE_SIZE" />
<uses-permission android:name="android.permission.READ_DEVICE_CONFIG" />
```

**Note**: `PACKAGE_USAGE_STATS` (protected) requires user to enable in Settings → Apps & Notifications → Advanced → Special App Access → Usage Access.

### Security Checklist

- [x] `android:usesCleartextTraffic="false"` - HTTPS only
- [x] `android:allowBackup="true"` - Data backup configured
- [x] `android:exported="false"` for Service - Not exposed to other apps
- [x] `android:exported="true"` for MainActivity - Launcher activity
- [x] `tools:targetApi="34"` - Latest API compatibility

---

## Dependencies Management

### Dependency Verification

All dependencies are sourced from:
- `google()` - Official Android repository
- `mavenCentral()` - Maven Central Repository
- `jitpack.io()` - GitHub package repository

### Key Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose | 2023.10.01 | UI Framework |
| Room | 2.6.0 | Database |
| Hilt | 2.47 | Dependency Injection |
| Filament | 1.51.6 | Native 3D Rendering |
| ONNX Runtime | 1.16.3 | Local ML Models |
| Coroutines | 1.7.3 | Async/Concurrency |
| WorkManager | 2.8.1 | Background Tasks |

### Adding New Dependencies

1. Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("group:artifact:version")
}
```

2. Sync Gradle
3. If using ProGuard/R8, update `proguard-rules.pro` if needed

---

## Release Preparation Checklist

### Code Review
- [ ] All TODOs resolved
- [ ] Debug logging removed or wrapped with `BuildConfig.DEBUG`
- [ ] No hardcoded API keys or secrets
- [ ] ProGuard rules tested (remove, rename verification)

### Testing
- [ ] Debug APK tested on multiple devices (API 28-34)
- [ ] Release APK tested on emulator/device
- [ ] App startup verified (no crashes)
- [ ] Main features working
- [ ] Performance acceptable
- [ ] Battery/thermal impact verified

### Version Management
- [ ] Update `versionCode` in `build.gradle.kts`
- [ ] Update `versionName` (semantic versioning)
- [ ] Update RELEASE_NOTES.md
- [ ] Create git tag: `git tag -a v1.0.0 -m "Release v1.0.0"`

### Release Notes Template

```markdown
# Release v1.0.0

**Release Date**: YYYY-MM-DD
**Minimum Android**: 9.0 (API 28)
**Target Android**: 14 (API 34)

## Features
- Autonomous AI reasoning loop
- Real-time 3D visualization
- Energy-aware cognition
- Device signal integration

## Improvements
- Performance optimizations
- Battery life improvements
- UI/UX refinements

## Bug Fixes
- [List any fixes]

## Known Issues
- [List any known issues]

## Installation
1. Download app-release.aab
2. Use Google Play Console or bundletool to generate APK
3. Install on device

## Upgrade Notes
- No data migration needed for first release
- All settings reset to defaults
```

---

## Release Build Signing

### Generate Signing Key

```bash
# Create keystore (run once)
keytool -genkey -v -keystore release.keystore \
  -alias release_key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# This creates release.keystore file
```

### Configure Signing in Gradle

Create `app/signing.properties` (Git-ignored):

```properties
storeFile=../release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=release_key
keyPassword=YOUR_KEY_PASSWORD
```

Update `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            val signingFile = rootProject.file("app/signing.properties")
            val signingProperties = Properties()
            signingProperties.load(signingFile.inputStream())
            
            storeFile = file(signingProperties.getProperty("storeFile"))
            storePassword = signingProperties.getProperty("storePassword")
            keyAlias = signingProperties.getProperty("keyAlias")
            keyPassword = signingProperties.getProperty("keyPassword")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Build Signed Release APK

```bash
./gradlew.bat assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## Troubleshooting Build Issues

### Issue: "Could not find AGP"
```
Error: Could not find com.android.tools.build:gradle:8.2.0
```
**Solution**: Run `./gradlew.bat --refresh-dependencies`

### Issue: "Kotlin Compiler Version Mismatch"
```
Kotlin Compiler Version (1.9.20) != Plugin Version (1.9.10)
```
**Solution**: Update all Kotlin versions in build.gradle.kts to match

### Issue: "Cannot Resolve Symbol"
```
Cannot resolve symbol 'String' or similar stdlib symbols
```
**Solution**: 
- Run `./gradlew.bat clean` 
- Invalidate Android Studio cache
- Restart IDE

### Issue: "Minification Error"
```
R8: java.lang.RuntimeException: Method ... is abstract
```
**Solution**: Add to `proguard-rules.pro`:
```proguard
-keep class com.problematic.package.** { *; }
```

### Issue: "Out of Memory"
```
java.lang.OutOfMemoryError: Java heap space
```
**Solution**: Increase gradle memory in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m
```

---

## Performance Targets

### Build Times
- **Clean Debug Build**: ~45 seconds
- **Incremental Debug Build**: ~15 seconds
- **Release Build**: ~60 seconds

### APK Size
- **Debug APK**: ~180 MB (includes debug symbols)
- **Release APK**: ~120 MB (after minification)

### App Performance
- **Cold Start**: <2 seconds
- **First Frame**: <1.5 seconds
- **Memory Usage**: <60 MB (typical)
- **Battery Impact**: <1% per hour (idle)

---

## Distribution

### Google Play Console

1. Create project in [Google Play Console](https://play.google.com/console)
2. Upload app-release.aab
3. Create app listing with:
   - Screenshots (4-5 preferred)
   - Description (80-4000 characters)
   - Permissions justification
   - Target audience
4. Set pricing (free recommended)
5. Submit for review (48-72 hours typical)

### Direct Distribution (sideload)

```bash
# Install APK directly
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Bundletool (Convert AAB to APKs)

```bash
# Download bundletool
wget https://github.com/google/bundletool/releases/download/1.15.6/bundletool-all-1.15.6.jar

# Generate APKs from AAB
java -jar bundletool-all-1.15.6.jar build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app.apks \
  --mode=universal

# Install on device
adb install-multiple app.apks
```

---

## Version Management & Tagging

### Semantic Versioning

```
versionName = "MAJOR.MINOR.PATCH[-PRERELEASE]"

Examples:
- 1.0.0        (First release)
- 1.0.1        (Patch: bug fix)
- 1.1.0        (Minor: feature addition)
- 2.0.0        (Major: breaking changes)
- 1.0.0-alpha1 (Pre-release: alpha)
- 1.0.0-beta1  (Pre-release: beta)
```

### Version Code

```
versionCode = MAJOR * 10000 + MINOR * 100 + PATCH

Examples:
- v1.0.0 → versionCode = 10000
- v1.0.1 → versionCode = 10001
- v1.1.0 → versionCode = 10100
- v2.0.0 → versionCode = 20000
```

### Creating Release Tags

```bash
# Update version in build.gradle.kts
# Commit changes
git add app/build.gradle.kts
git commit -m "Bump version to 1.0.0"

# Create annotated tag
git tag -a v1.0.0 -m "Release v1.0.0: Initial public release"

# Push tag
git push origin v1.0.0
```

### Current Release Status

```
Release Version: v1.0.0
Release Date: January 24, 2026
Status: Ready for Release
```

---

## CI/CD Integration

### GitHub Actions Example

Create `.github/workflows/build.yml`:

```yaml
name: Build Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Build Release APK
        run: ./gradlew assembleRelease
      - name: Upload to Release
        uses: actions/upload-artifact@v3
        with:
          name: release-apk
          path: app/build/outputs/apk/release/**/*.apk
```

---

## Documentation Standards

### Adding Documentation
- Keep README.md up-to-date with version info
- Document breaking changes prominently
- Maintain CHANGELOG.md for all releases
- Comment non-obvious code sections

### Build Configuration Documentation
- Comment any non-standard gradle settings
- Document reasons for ProGuard rules
- Update when adding new build types

---

## Support & Issues

### Common Questions

**Q: How do I build for a specific API level?**
A: Update `minSdk` and `targetSdk` in build.gradle.kts, then rebuild.

**Q: Can I use older Kotlin/AGP versions?**
A: Minimum supported: Kotlin 1.8.0, AGP 8.0.0. Older versions may have compatibility issues.

**Q: How do I optimize APK size?**
A: Enable minification, use ProGuard rules, split APKs per ABI, remove unused resources.

**Q: Where are native libraries (Filament)?**
A: Packaged automatically in AAB. For APK, they're in app/build/intermediates/cmake/

---

## Release Checklist

- [ ] Code compiles without errors
- [ ] All tests passing
- [ ] Version number updated
- [ ] RELEASE_NOTES.md updated
- [ ] Permissions reviewed and justified
- [ ] SDK compatibility verified (minSdk 28, targetSdk 34)
- [ ] Debug APK tested
- [ ] Release APK built and tested
- [ ] Signing configuration in place
- [ ] Git tag created (git tag -a v1.0.0)
- [ ] Ready for Play Store / distribution

---

**Last Updated**: January 24, 2026
**Next Review**: After v1.0.1 release
