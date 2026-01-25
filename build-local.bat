@echo off
REM SA-AIHOS Build Script - Uses installed Gradle 8.5
REM This bypasses the wrapper jar issue

setlocal enabledelayedexpansion

echo.
echo ================================================
echo   SA-AIHOS Android Build (Local Gradle 8.5)
echo ================================================
echo.

REM Set environment variables
set JAVA_HOME=C:\Program Files\Java\jdk-17
set GRADLE_HOME=C:\gradle-8.5
set ANDROID_HOME=C:\Users\amank\AppData\Local\Android\Sdk
set PATH=%JAVA_HOME%\bin;%GRADLE_HOME%\bin;%PATH%

REM Verify Java
echo 1. Verifying Java...
"%JAVA_HOME%\bin\java.exe" -version
if errorlevel 1 (
    echo ERROR: Java not found!
    exit /b 1
)

REM Verify Android SDK
echo.
echo 2. Verifying Android SDK...
if exist "%ANDROID_HOME%" (
    echo OK: Android SDK found at %ANDROID_HOME%
) else (
    echo ERROR: Android SDK not found!
    exit /b 1
)

REM Create/verify local.properties
echo.
echo 3. Creating local.properties...
(
    echo sdk.dir=%ANDROID_HOME:\=\\%
    echo java.home=%JAVA_HOME:\=\\%
    echo org.gradle.java.home=%JAVA_HOME:\=\\%
) > local.properties
echo OK: local.properties created

REM Run build
echo.
echo 4. Running Gradle build...
echo ================================================
"%GRADLE_HOME%\bin\gradle.bat" clean assembleDebug

if errorlevel 1 (
    echo.
    echo BUILD FAILED
    exit /b 1
)

echo.
echo ================================================
echo BUILD SUCCESSFUL!
echo ================================================

REM Check for APK
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo APK generated successfully:
    echo app\build\outputs\apk\debug\app-debug.apk
) else (
    echo WARNING: APK not found at expected location
)

endlocal
