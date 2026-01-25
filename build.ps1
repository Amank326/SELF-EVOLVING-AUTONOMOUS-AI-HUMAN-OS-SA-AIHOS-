#!/usr/bin/env powershell
# SA-AIHOS Automated Build Setup Script
# This script sets up the complete build environment and runs builds

param(
    [ValidateSet('debug', 'release', 'full')]
    [string]$BuildType = 'debug',
    
    [switch]$Clean = $false,
    [switch]$Verbose = $false
)

# Colors for output
$colors = @{
    Success = "Green"
    Error = "Red"
    Warning = "Yellow"
    Info = "Cyan"
}

function Write-Status {
    param([string]$Message, [string]$Type = "Info")
    $color = $colors[$Type]
    Write-Host "[$Type] $Message" -ForegroundColor $color
}

function Test-Environment {
    Write-Status "Verifying build environment..." "Info"
    
    # Check Java
    $javaHome = "C:\Program Files\Java\jdk-17"
    if (!(Test-Path $javaHome)) {
        Write-Status "ERROR: Java JDK 17 not found at $javaHome" "Error"
        return $false
    }
    
    # Check Android SDK
    $androidHome = "C:\Users\amank\AppData\Local\Android\Sdk"
    if (!(Test-Path $androidHome)) {
        Write-Status "ERROR: Android SDK not found at $androidHome" "Error"
        return $false
    }
    
    # Check Gradle wrapper
    if (!(Test-Path ".\gradlew.bat")) {
        Write-Status "ERROR: gradlew.bat not found" "Error"
        return $false
    }
    
    Write-Status "✅ Java (JDK 17): OK" "Success"
    Write-Status "✅ Android SDK: OK" "Success"
    Write-Status "✅ Gradle wrapper: OK" "Success"
    
    return $true
}

function Setup-Environment {
    Write-Status "Setting up environment variables..." "Info"
    
    # Set environment variables for this session
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
    $env:ANDROID_HOME = "C:\Users\amank\AppData\Local\Android\Sdk"
    $env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\tools\bin;$env:PATH"
    $env:GRADLE_USER_HOME = "$PSScriptRoot\.gradle"
    
    Write-Status "✅ Environment variables set" "Success"
}

function Invoke-Build {
    param([string]$Task)
    
    Write-Status "Running Gradle task: $Task" "Info"
    Write-Status "=" * 60 "Info"
    
    if ($Verbose) {
        & .\gradlew.bat $Task --info 2>&1
    } else {
        & .\gradlew.bat $Task 2>&1
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Status "✅ Build task '$Task' completed successfully" "Success"
        return $true
    } else {
        Write-Status "❌ Build task '$Task' failed" "Error"
        return $false
    }
}

function Run-Build {
    Write-Status "=" * 60 "Info"
    Write-Status "Starting SA-AIHOS Build Process" "Info"
    Write-Status "Build Type: $BuildType" "Info"
    Write-Status "=" * 60 "Info"
    
    if ($Clean) {
        Write-Status "Cleaning previous build artifacts..." "Warning"
        Invoke-Build "clean" | Out-Null
    }
    
    # Run build based on type
    switch ($BuildType) {
        'debug' {
            $success = Invoke-Build "assembleDebug"
            if ($success) {
                Write-Status "📦 Debug APK: app/build/outputs/apk/debug/app-debug.apk" "Success"
            }
            return $success
        }
        
        'release' {
            $success = Invoke-Build "bundleRelease"
            if ($success) {
                Write-Status "📦 Release AAB: app/build/outputs/bundle/release/app-release.aab" "Success"
            }
            return $success
        }
        
        'full' {
            $debugOk = Invoke-Build "assembleDebug"
            if ($debugOk) {
                Write-Status "📦 Debug APK: app/build/outputs/apk/debug/app-debug.apk" "Success"
            }
            
            $releaseOk = Invoke-Build "bundleRelease"
            if ($releaseOk) {
                Write-Status "📦 Release AAB: app/build/outputs/bundle/release/app-release.aab" "Success"
            }
            
            return ($debugOk -and $releaseOk)
        }
    }
}

# Main execution
try {
    # Verify environment
    if (!(Test-Environment)) {
        exit 1
    }
    
    # Setup environment
    Setup-Environment
    
    # Run build
    $buildSuccess = Run-Build
    
    # Summary
    Write-Status "=" * 60 "Info"
    if ($buildSuccess) {
        Write-Status "✅ Build completed successfully!" "Success"
        Write-Status "=" * 60 "Info"
        exit 0
    } else {
        Write-Status "❌ Build failed. Check errors above." "Error"
        Write-Status "=" * 60 "Info"
        exit 1
    }
}
catch {
    Write-Status "❌ Unexpected error: $_" "Error"
    exit 1
}
