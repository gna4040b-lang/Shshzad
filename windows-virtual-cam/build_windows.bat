@echo off
REM ==============================================================================
REM Windows Media Foundation Virtual Camera Build Script (Batch)
REM Prerequisites: Visual Studio 2022 / 2019 with C++ Desktop Workload & Windows SDK
REM ==============================================================================

setlocal enabledelayedexpansion

echo ================================================================
echo   Building Windows Media Foundation Virtual Camera Solution
echo ================================================================

if not exist build (
    mkdir build
)

cd build

echo.
echo [*] Generating Visual Studio Project files via CMake...
cmake -G "Visual Studio 17 2022" -A x64 ..
if %ERRORLEVEL% neq 0 (
    echo [!] CMake configuration failed with Visual Studio 2022, trying default generator...
    cmake -A x64 ..
)

echo.
echo [*] Compiling Release binaries with MSBuild...
cmake --build . --config Release --parallel

if %ERRORLEVEL% equ 0 (
    echo.
    echo ================================================================
    echo [SUCCESS] Build completed successfully!
    echo Binaries generated in: build\Release\
    echo   - VirtualCameraController.exe   (Desktop Studio Controller)
    echo   - VirtualCameraRegister.exe     (Driver Registration Tool)
    echo   - VirtualCameraDirectShowFilter.dll (DirectShow Filter)
    echo ================================================================
) else (
    echo.
    echo [ERROR] Build encountered errors. Please check compiler output above.
)

cd ..
pause
