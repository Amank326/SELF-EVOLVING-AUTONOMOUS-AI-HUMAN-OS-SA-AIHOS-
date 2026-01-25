#!/usr/bin/env powershell
# SA-AIHOS Complete Build Setup & Automation
# This script handles everything for building the Android project on Windows

param(
    [ValidateSet('setup', 'debug', 'release', 'full', 'clean')]
    [string]$Task = 'setup'
)

$ErrorActionPreference = "Continue"

function Write-Title {
    Write-Host "`n$('='*70)" -ForegroundColor Cyan
    Write-Host $args[0] -ForegroundColor Cyan
    Write-Host "$('='*70)" -ForegroundColor Cyan
}

function Write-Success {
    Write-Host "✅ $($args[0])" -ForegroundColor Green
}

function Write-Error {
    Write-Host "❌ ERROR: $($args[0])" -ForegroundColor Red
}

function Write-Info {
    Write-Host "ℹ️  $($args[0])" -ForegroundColor Yellow
}

Write-Title "SA-AIHOS Android Build Automation"

# Step 1: Verify environment
Write-Title "Step 1: Verifying Build Environment"

if (!(Test-Path "C:\Program Files\Java\jdk-17")) {
    Write-Error "Java JDK 17 not found at C:\Program Files\Java\jdk-17"
    Write-Info "Please install Java JDK 17 from https://www.oracle.com/java/technologies/downloads/"
    exit 1
}
Write-Success "Java JDK 17 found"

if (!(Test-Path "C:\Users\amank\AppData\Local\Android\Sdk")) {
    Write-Error "Android SDK not found"
    Write-Info "Please install Android SDK via Android Studio"
    exit 1
}
Write-Success "Android SDK found"

if (!(Test-Path "local.properties")) {
    Write-Info "local.properties not found, creating..."
    @"
sdk.dir=C:\\Users\\amank\\AppData\\Local\\Android\\Sdk
java.home=C:\\Program Files\\Java\\jdk-17
org.gradle.java.home=C:\\Program Files\\Java\\jdk-17
"@ | Out-File "local.properties" -Encoding UTF8
    Write-Success "local.properties created"
}

# Step 2: Setup environment
Write-Title "Step 2: Configuring Environment"

$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:ANDROID_HOME = "C:\Users\amank\AppData\Local\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\tools\bin;$env:PATH"
$env:GRADLE_USER_HOME = "$PSScriptRoot\.gradle"

Write-Success "JAVA_HOME set to: $env:JAVA_HOME"
Write-Success "ANDROID_HOME set to: $env:ANDROID_HOME"

# Step 3: Verify Java is accessible
$javaVersion = & java -version 2>&1 | Select-Object -First 1
Write-Success "Java version: $javaVersion"

# Step 4: Handle gradle wrapper
Write-Title "Step 3: Verifying Gradle Wrapper"

if (Test-Path "gradlew.bat") {
    Write-Success "gradlew.bat found"
} else {
    Write-Error "gradlew.bat not found"
    exit 1
}

# Ensure wrapper jar exists
$wrapperJar = "gradle\wrapper\gradle-wrapper.jar"
if (!(Test-Path $wrapperJar)) {
    Write-Info "gradle-wrapper.jar missing, attempting to restore from backup..."
    if (Test-Path "gradle-backup\gradle-wrapper.jar") {
        Copy-Item "gradle-backup\gradle-wrapper.jar" $wrapperJar -Force
        Write-Success "gradle-wrapper.jar restored from backup"
    } else {
        Write-Error "No backup available. gradle-wrapper.jar cannot be restored."
        Write-Info "Please ensure gradle/wrapper/gradle-wrapper.jar is present and valid"
        exit 1
    }
} else {
    Write-Success "gradle-wrapper.jar verified"
}

# Step 5: Execute build task
Write-Title "Step 4: Running Build Task [$Task]"

switch ($Task) {
    'setup' {
        Write-Success "Environment setup complete! Ready to build."
        Write-Info "Next, run: .\Build-Android.ps1 debug"
    }
    'clean' {
        Write-Info "Cleaning build artifacts..."
        & .\gradlew.bat clean 2>&1 | Select-Object -Last 10
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Clean completed successfully"
        } else {
            Write-Error "Clean build failed (exit code: $LASTEXITCODE)"
            exit 1
        }
    }
    'debug' {
        Write-Info "Building debug APK..."
        & .\gradlew.bat assembleDebug 2>&1 | Tee-Object -Variable buildOutput | Select-Object -Last 30
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Debug APK build completed successfully!"
            Write-Info "APK location: app\build\outputs\apk\debug\app-debug.apk"
            $apkSize = (Get-Item "app\build\outputs\apk\debug\app-debug.apk" -ErrorAction SilentlyContinue).Length
            if ($apkSize) {
                Write-Info "APK size: $('{0:N0}' -f ($apkSize/1MB)) MB"
            }
        } else {
            Write-Error "Debug build failed (exit code: $LASTEXITCODE)"
            exit 1
        }
    }
    'release' {
        Write-Info "Building release AAB..."
        & .\gradlew.bat bundleRelease 2>&1 | Tee-Object -Variable buildOutput | Select-Object -Last 30
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Release AAB build completed successfully!"
            Write-Info "AAB location: app\build\outputs\bundle\release\app-release.aab"
            $aabSize = (Get-Item "app\build\outputs\bundle\release\app-release.aab" -ErrorAction SilentlyContinue).Length
            if ($aabSize) {
                Write-Info "AAB size: $('{0:N0}' -f ($aabSize/1MB)) MB"
            }
        } else {
            Write-Error "Release build failed (exit code: $LASTEXITCODE)"
            exit 1
        }
    }
    'full' {
        Write-Info "Running full build (debug + release)..."
        
        Write-Info "[1/2] Building debug APK..."
        & .\gradlew.bat assembleDebug 2>&1 | Select-Object -Last 5
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Debug build failed"
            exit 1
        }
        Write-Success "Debug APK complete"
        
        Write-Info "[2/2] Building release AAB..."
        & .\gradlew.bat bundleRelease 2>&1 | Select-Object -Last 5
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Release build failed"
            exit 1
        }
        Write-Success "Release AAB complete"
        
        Write-Success "Full build completed successfully!"
    }
}

Write-Title "Build Process Complete"
