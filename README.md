# SA-AIHOS - Self-Evolving Autonomous AI Human OS

A next-generation Android application combining AI intelligence with human-centric design principles.

## 🚀 Quick Start

### Prerequisites
- **Java**: JDK 17 LTS
- **Android SDK**: API 28+
- **Gradle**: 8.5

### Build & Run
```bash
cd C:\Users\amank\Projects\SA-AIHOS

# Build APK
./gradlew clean assembleDebug

# Deploy to device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.aihos/.MainActivity
```

## 📱 Project Structure
```
SA-AIHOS/
├── app/                    # Android application
│   ├── src/                # Source code
│   ├── build.gradle.kts    # Build configuration
│   └── google-services.json # Firebase config
├── gradle/                 # Gradle wrapper
├── build.gradle.kts        # Root build config
├── settings.gradle.kts     # Gradle settings
└── README.md
```

## 🛠️ Technologies
| Component | Version |
|-----------|---------|
| Gradle | 8.5 |
| AGP | 8.0.0 |
| Kotlin | 1.9.20 |
| Java | 17 LTS |
| Compose | 2023.10.01 |
| Room | 2.6.1 |
| Firebase | 4.3.15 |
| Hilt | 2.47 |

## 📋 Configuration
- **Package**: com.aihos
- **Min SDK**: 28
- **Target SDK**: 34
- **Version**: 1.0.0

## ️ Development

### Clean Build
```bash
./gradlew clean build --refresh-dependencies
```

### Build Release APK
```bash
./gradlew assembleRelease
```

### View Logs
```bash
adb logcat | grep aihos
```

### Stop App
```bash
adb shell am force-stop com.aihos
```

## 🐛 Troubleshooting

### Build Fails
```bash
./gradlew --stop
rm -rf .gradle
./gradlew clean build
```

### Device Not Found
```bash
adb kill-server
adb start-server
adb devices
```

### Gradle Daemon Issues
```bash
./gradlew build --no-daemon
```

## 📖 Documentation
All project documentation has been removed to keep the repo clean.
- See code comments for implementation details
- Check git history for detailed changelog

## 📄 License
MIT License - See LICENSE file

## 👤 Developer
Created and maintained by Aman Kumar

---

**Status**: ✅ Production Ready  
**Build System**: Gradle 8.5  
**Last Updated**: February 3, 2026
