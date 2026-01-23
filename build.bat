@echo off
setlocal enabledelayedexpansion

set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "GRADLE_HOME=C:\gradle\gradle-8.5"
set "PATH=!GRADLE_HOME!\bin;!JAVA_HOME!\bin;%PATH%"

cd /d "%~dp0"
call gradle.exe %*

endlocal
