<# :
@echo off
title Risk of Rain 2 - PC to Android Packager
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-Expression ([System.IO.File]::ReadAllText('%~f0'))"
pause
exit /b
#>
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  Risk of Rain 2 -> Android (ARM64 / x64) | Standalone Packager" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

 = @(
    "C:\Program Files (x86)\Steam\steamapps\common\Risk of Rain 2",
    "C:\Program Files\Steam\steamapps\common\Risk of Rain 2",
    "D:\SteamLibrary\steamapps\common\Risk of Rain 2",
    "D:\Steam\steamapps\common\Risk of Rain 2",
    "E:\SteamLibrary\steamapps\common\Risk of Rain 2",
    "E:\Steam\steamapps\common\Risk of Rain 2",
    "F:\SteamLibrary\steamapps\common\Risk of Rain 2"
)

foreach ( in @("HKCU:\Software\Valve\Steam", "HKLM:\SOFTWARE\WOW6432Node\Valve\Steam")) {
    if (Test-Path ) {
         = (Get-ItemProperty  -ErrorAction SilentlyContinue).SteamPath
        if () {
             = @((Join-Path  "steamapps\common\Risk of Rain 2")) + 
             = Join-Path  "steamapps\libraryfolders.vdf"
            if (Test-Path ) {
                Select-String -Path  -Pattern '"path"\s+"([^"]+)"' | ForEach-Object {
                     = .Matches[0].Groups[1].Value -replace "\\\\", "\"
                     += (Join-Path  "steamapps\common\Risk of Rain 2")
                }
            }
        }
    }
}

 = 
foreach ( in ) {
    if ( -and (Test-Path )) {
         = (Resolve-Path ).Path
        break
    }
}

if (-not ) {
    Write-Host ""
     = Read-Host "Drag and drop your [Risk of Rain 2] folder here and press Enter"
     = .Trim('"', "'", " ")
    if (-not (Test-Path )) {
        Write-Host "[Error] Folder not found: " -ForegroundColor Red
        exit 1
    }
     = (Resolve-Path ).Path
}

 = Join-Path (Get-Location).Path "ror2-game-files.zip"
if (Test-Path ) { Remove-Item  -Force }

Write-Host ""
Write-Host "[1/2] Found game folder: " -ForegroundColor Green
Write-Host "      Creating archive:  (please wait 1-3 minutes)..." -ForegroundColor Yellow

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory(, , [System.IO.Compression.CompressionLevel]::Fastest, False)

 = [Math]::Round((Get-Item ).Length / 1MB, 1)
Write-Host "  [OK] Archive created:  ( MB)" -ForegroundColor Green

 = Get-Command adb -ErrorAction SilentlyContinue
if () {
    Write-Host ""
    Write-Host "[2/2] ADB found. Pushing ror2-game-files.zip to /sdcard/Download/..." -ForegroundColor Cyan
    & adb push  "/sdcard/Download/ror2-game-files.zip"
} else {
    Write-Host ""
    Write-Host "[2/2] Copy ror2-game-files.zip to your phone Download folder via USB cable." -ForegroundColor Cyan
}

Write-Host ""
Write-Host "Done! Open the RoR2 APK on your phone -> Set up -> Select game files -> pick ror2-game-files.zip" -ForegroundColor Green
