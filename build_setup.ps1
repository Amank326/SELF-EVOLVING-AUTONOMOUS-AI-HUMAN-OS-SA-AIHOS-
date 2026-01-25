#!/usr/bin/env pwsh
# SA-AIHOS Project Setup Script
# Purpose: Configure build environment and compile project

# Color output
function Write-Header {
    param([string]$message)
    Write-Host "`n╔════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║  $message" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════╝" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$message)
    Write-Host "✅ $message" -ForegroundColor Green
}

function Write-Info {
    param([string]$message)
    Write-Host "ℹ️  $message" -ForegroundColor Blue
}

function Write-Error {
    param([string]$message)
    Write-Host "❌ $message" -ForegroundColor Red
}

# Start setup
Write-Header "SA-AIHOS BUILD ENVIRONMENT SETUP"

# Step 1: Check Java
Write-Info "Checking Java installation..."
$javaPath = "C:\Users\amank\.jdk\jdk-17.0.16\bin\java.exe"
if (Test-Path $javaPath) {
    Write-Success "Java 17 found at $javaPath"
    $javaVersion = & $javaPath -version 2>&1
    Write-Info $javaVersion[0]
} else {
    Write-Error "Java 17 not found at expected location"
    Write-Info "Please install JDK 17 first using install_jdk tool"
    exit 1
}

# Step 2: Set Environment Variables
Write-Info "Configuring environment variables..."
$env:JAVA_HOME = "C:\Users\amank\.jdk\jdk-17.0.16"
$env:Path = "$env:Path;$env:JAVA_HOME\bin"
Write-Success "JAVA_HOME set to $env:JAVA_HOME"

# Step 3: Navigate to Project
Write-Info "Changing to project directory..."
$projectPath = "C:\Users\amank\Projects\SA-AIHOS"
if (-Not (Test-Path $projectPath)) {
    Write-Error "Project path not found: $projectPath"
    exit 1
}
Set-Location $projectPath
Write-Success "Project directory: $(Get-Location)"

# Step 4: Verify Gradle Wrapper
Write-Info "Checking Gradle wrapper..."
if (Test-Path ".\gradlew.bat") {
    Write-Success "Gradle wrapper found"
} else {
    Write-Error "Gradle wrapper not found. Cannot proceed."
    exit 1
}

# Step 5: Clean Build
Write-Header "BUILDING PROJECT WITH KSP"
Write-Info "Running: ./gradlew clean build"
Write-Info "This may take 2-5 minutes on first run..."

$buildStartTime = Get-Date
try {
    & .\gradlew clean build --info
    $buildStatus = $LASTEXITCODE
    $buildDuration = ((Get-Date) - $buildStartTime).TotalSeconds
    
    if ($buildStatus -eq 0) {
        Write-Success "Build completed successfully in $buildDuration seconds"
        Write-Header "BUILD SUCCESSFUL"
        Write-Success "APK generated at: app/build/outputs/apk/debug/app-debug.apk"
        
        # Show APK info
        $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
        if (Test-Path $apkPath) {
            $apkSize = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
            Write-Info "APK Size: $apkSize MB"
        }
        
        Write-Info "`nNext steps:"
        Write-Info "1. Deploy to emulator: adb install app/build/outputs/apk/debug/app-debug.apk"
        Write-Info "2. Launch app: adb shell am start -n com.aihos/.ui.MainActivity"
        Write-Info "3. Check logcat: adb logcat | grep aihos"
        Write-Info ""
        Write-Info "For Phase 2 enhancements, see: ADVANCED_UPGRADE_PLAN.md"
    } else {
        Write-Error "Build failed with exit code: $buildStatus"
        Write-Error "Check error output above for details"
        exit $buildStatus
    }
} catch {
    Write-Error "Build process failed: $_"
    exit 1
}

Write-Header "SETUP COMPLETE"
Write-Info "Project is ready for development!"
Write-Info "Android Studio: Open ./SA-AIHOS to edit code"
