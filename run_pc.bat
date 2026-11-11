@echo off
chcp 65001 >nul
title Risk of Rain 2 - Подготовка файлов для Android
cd /d "%~dp0"

where python >nul 2>nul
if %errorlevel% equ 0 (
    python "%~dp0send_to_phone.py" %*
    goto :end
)

where py >nul 2>nul
if %errorlevel% equ 0 (
    py -3 "%~dp0send_to_phone.py" %*
    goto :end
)

echo [Ошибка] Python не найден на этом ПК!
echo Скачайте и установите Python с https://www.python.org/downloads/ (при установке поставьте галочку "Add Python to PATH").

:end
echo.
pause
