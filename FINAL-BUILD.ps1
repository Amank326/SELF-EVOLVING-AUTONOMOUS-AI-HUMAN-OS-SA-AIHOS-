#!/usr/bin/env powershell
# Final Build Script - Run this manually in PowerShell

cd "C:\Users\amank\Projects\SA-AIHOS"

# Environment setup
$env:GRADLE_HOME = "C:\Users\amank\Downloads\gradle-8.5-all\gradle-8.5"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"  
$env:ANDROID_HOME = "C:\Users\amank\AppData\Local\Android\Sdk"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "SA-AIHOS FINAL BUILD" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

# Stop existing daemons
Write-Host "`nStopping gradle daemons..."
& "$env:GRADLE_HOME\bin\gradle.bat" --stop 2>&1 | Out-Null

# Final build
Write-Host "`nBuilding (2-3 minutes)...`n"
& "$env:GRADLE_HOME\bin\gradle.bat" clean assembleDebug --no-daemon

# Check result
Write-Host "`n======================================" -ForegroundColor Cyan
$apkPath = "C:\Users\amank\Projects\SA-AIHOS\app\build\outputs\apk\debug\app-debug.apk"

if (Test-Path $apkPath) {
    $size = (Get-Item $apkPath).Length / 1MB
    Write-Host "✅ BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "APK: $apkPath"
    Write-Host "Size: $([math]::Round($size, 2)) MB"
} else {
    Write-Host "❌ BUILD FAILED" -ForegroundColor Red
    Write-Host "APK not found at $apkPath"
    Write-Host "`nCheck the build output above for errors." -ForegroundColor Yellow
}

Write-Host "======================================" -ForegroundColor Cyan
