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

$candidates = @(
    "C:\Program Files (x86)\Steam\steamapps\common\Risk of Rain 2",
    "C:\Program Files\Steam\steamapps\common\Risk of Rain 2",
    "D:\SteamLibrary\steamapps\common\Risk of Rain 2",
    "D:\Steam\steamapps\common\Risk of Rain 2",
    "E:\SteamLibrary\steamapps\common\Risk of Rain 2",
    "E:\Steam\steamapps\common\Risk of Rain 2",
    "F:\SteamLibrary\steamapps\common\Risk of Rain 2"
)

foreach ($rk in @("HKCU:\Software\Valve\Steam", "HKLM:\SOFTWARE\WOW6432Node\Valve\Steam")) {
    if (Test-Path $rk) {
        $sp = (Get-ItemProperty $rk -ErrorAction SilentlyContinue).SteamPath
        if ($sp) {
            $candidates = @((Join-Path $sp "steamapps\common\Risk of Rain 2")) + $candidates
            $vdf = Join-Path $sp "steamapps\libraryfolders.vdf"
            if (Test-Path $vdf) {
                Select-String -Path $vdf -Pattern '"path"\s+"([^"]+)"' | ForEach-Object {
                    $lp = $_.Matches[0].Groups[1].Value -replace "\\\\", "\"
                    $candidates += (Join-Path $lp "steamapps\common\Risk of Rain 2")
                }
            }
        }
    }
}

$gameDir = $null
foreach ($c in $candidates) {
    if ($c -and (Test-Path $c)) {
        $gameDir = (Resolve-Path $c).Path
        break
    }
}

if (-not $gameDir) {
    Write-Host ""
    $inputPath = Read-Host "Drag and drop your [Risk of Rain 2] folder here and press Enter"
    $inputPath = $inputPath.Trim('"', "'", " ")
    if (-not (Test-Path $inputPath)) {
        Write-Host "[Error] Folder not found: $inputPath" -ForegroundColor Red
        exit 1
    }
    $gameDir = (Resolve-Path $inputPath).Path
}

$outZip = Join-Path (Get-Location).Path "ror2-game-files.zip"
if (Test-Path $outZip) { Remove-Item $outZip -Force }

Write-Host ""
Write-Host "[1/2] Found game folder: $gameDir" -ForegroundColor Green
Write-Host "      Creating archive: $outZip (please wait 1-3 minutes)..." -ForegroundColor Yellow

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($gameDir, $outZip, [System.IO.Compression.CompressionLevel]::Fastest, $false)

$sizeMb = [Math]::Round((Get-Item $outZip).Length / 1MB, 1)
Write-Host "  [OK] Archive created: $outZip ($sizeMb MB)" -ForegroundColor Green

$adb = Get-Command adb -ErrorAction SilentlyContinue
if ($adb) {
    Write-Host ""
    Write-Host "[2/2] ADB found. Pushing ror2-game-files.zip to /sdcard/Download/..." -ForegroundColor Cyan
    & adb push $outZip "/sdcard/Download/ror2-game-files.zip"
} else {
    Write-Host ""
    Write-Host "[2/2] Copy ror2-game-files.zip to your phone Download folder via USB cable." -ForegroundColor Cyan
}

Write-Host ""
Write-Host "Done! Open the RoR2 APK on your phone -> Set up -> Select game files -> pick ror2-game-files.zip" -ForegroundColor Green
