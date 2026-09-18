# ==============================================================================
# Windows Media Foundation Virtual Camera Build Script (PowerShell)
# Prerequisites: Visual Studio 2022 with C++ Desktop & Windows 10/11 SDK, CMake
# ==============================================================================

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "   Building Windows Media Foundation Virtual Camera Solution" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

$BuildDir = Join-Path $PSScriptRoot "build"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Path $BuildDir | Out-Null
}

Set-Location $BuildDir

Write-Host "`n[*] Configuring build with CMake..." -ForegroundColor Yellow
try {
    cmake -G "Visual Studio 17 2022" -A x64 ..
} catch {
    cmake -A x64 ..
}

Write-Host "`n[*] Compiling Release binaries with MSBuild..." -ForegroundColor Yellow
cmake --build . --config Release --parallel

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n================================================================" -ForegroundColor Green
    Write-Host "[SUCCESS] Windows Virtual Camera built successfully!" -ForegroundColor Green
    Write-Host "Binaries located in: build\Release\" -ForegroundColor Green
    Write-Host "  - VirtualCameraController.exe   (Desktop Studio Controller)" -ForegroundColor White
    Write-Host "  - VirtualCameraRegister.exe     (Driver Registration Tool)" -ForegroundColor White
    Write-Host "  - VirtualCameraDirectShowFilter.dll (DirectShow Filter)" -ForegroundColor White
    Write-Host "================================================================" -ForegroundColor Green
} else {
    Write-Host "`n[ERROR] Build failed with exit code $LASTEXITCODE" -ForegroundColor Red
}

Set-Location $PSScriptRoot
