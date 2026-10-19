@echo off
setlocal
cd /d "%~dp0"
echo [1/2] Building T1 Client mod for Minecraft 1.21.11...
call gradlew.bat clean build
if errorlevel 1 (
  echo Gradle build failed. Check the log above.
  pause
  exit /b 1
)
echo.
echo [2/2] Publishing Windows launcher...
call publish-launcher.bat
exit /b %errorlevel%
