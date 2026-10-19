@echo off
setlocal
cd /d "%~dp0"
where dotnet >nul 2>nul
if errorlevel 1 (
  echo .NET 10 SDK not found. Install the .NET 10 SDK / Visual Studio .NET desktop workload first.
  pause
  exit /b 1
)
dotnet publish "launcher\T1Launcher\T1Launcher.csproj" -c Release -r win-x64 --self-contained true /p:PublishSingleFile=true /p:IncludeNativeLibrariesForSelfExtract=true
if errorlevel 1 (
  echo Launcher build failed. Check the error output above.
  pause
  exit /b 1
)
echo.
echo Launcher EXE created in:
echo launcher\T1Launcher\bin\Release\net10.0-windows\win-x64\publish\T1Launcher.exe
pause
