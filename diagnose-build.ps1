#!/usr/bin/env powershell
<# 
SA-AIHOS Build Diagnostic Script
Run this manually and share the output if build fails
#>

cd "C:\Users\amank\Projects\SA-AIHOS"

# Setup
$env:GRADLE_HOME = "C:\Users\amank\Downloads\gradle-8.5-all\gradle-8.5"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:ANDROID_HOME = "C:\Users\amank\AppData\Local\Android\Sdk"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SA-AIHOS Build Diagnostic" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Check prerequisites
Write-Host "`n1. Checking prerequisites..." -ForegroundColor Yellow
Write-Host "   Java: $(& 'C:\Program Files\Java\jdk-17\bin\java.exe' -version 2>&1 | Select-Object -First 1)"
Write-Host "   Gradle: $(& "$env:GRADLE_HOME\bin\gradle.bat" --version 2>&1 | Select-Object -First 1)"

# 2. Clean gradle cache
Write-Host "`n2. Cleaning gradle cache..." -ForegroundColor Yellow
& "$env:GRADLE_HOME\bin\gradle.bat" clean --no-daemon

# 3. Try building with debug output
Write-Host "`n3. Running build..." -ForegroundColor Yellow
Write-Host "   This may take 3-5 minutes" -ForegroundColor Gray
& "$env:GRADLE_HOME\bin\gradle.bat" assembleDebug --no-daemon --stacktrace 2>&1 | Tee-Object -FilePath "build-debug.txt"

# 4. Check result
Write-Host "`n4. Checking result..." -ForegroundColor Yellow
if (Test-Path "C:\Users\amank\Projects\SA-AIHOS\app\build\outputs\apk\debug\app-debug.apk") {
    Write-Host "✅ SUCCESS!" -ForegroundColor Green
    $size = (Get-Item "C:\Users\amank\Projects\SA-AIHOS\app\build\outputs\apk\debug\app-debug.apk").Length
    Write-Host "   APK Size: $([math]::Round($size/1MB, 2)) MB"
} else {
    Write-Host "❌ Build Failed" -ForegroundColor Red
    Write-Host "   Check build-debug.txt for details"
}
