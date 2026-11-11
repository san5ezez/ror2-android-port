#!/usr/bin/env python3
"""
Простой скрипт для ПК без всяких конфигов и проверок:
Автоматически находит Risk of Rain 2 в Steam (или берет указанную папку),
упаковывает в ror2-game-files.zip и закидывает на телефон в папку Download/.
"""
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

STEAM_CANDIDATES = [
    Path("C:/Program Files (x86)/Steam/steamapps/common/Risk of Rain 2"),
    Path("D:/SteamLibrary/steamapps/common/Risk of Rain 2"),
    Path("E:/SteamLibrary/steamapps/common/Risk of Rain 2"),
    Path.home() / ".local/share/Steam/steamapps/common/Risk of Rain 2",
    Path.home() / "Library/Application Support/Steam/steamapps/common/Risk of Rain 2",
]


def find_game_dir() -> Path:
    if len(sys.argv) > 1:
        p = Path(sys.argv[1]).expanduser().resolve()
        if p.is_dir():
            return p
        sys.exit(f"Папка не найдена: {p}")
    for candidate in STEAM_CANDIDATES:
        if candidate.is_dir():
            return candidate
    user_input = input("Введите путь к папке 'Risk of Rain 2': ").strip().strip('"')
    p = Path(user_input).expanduser().resolve()
    if not p.is_dir():
        sys.exit(f"Папка не найдена: {p}")
    return p


def main():
    game_dir = find_game_dir()
    out_zip = Path("ror2-game-files.zip").resolve()
    print(f"[1/2] Упаковка файлов игры из: {game_dir}")
    with zipfile.ZipFile(out_zip, "w", zipfile.ZIP_DEFLATED) as zf:
        for file_path in game_dir.rglob("*"):
            if file_path.is_file():
                rel = file_path.relative_to(game_dir).as_posix()
                zf.write(file_path, rel)
    print(f"  ✓ Создан архив: {out_zip} ({out_zip.stat().st_size // (1024 * 1024)} МБ)")

    if shutil.which("adb"):
        print("[2/2] Отправка ror2-game-files.zip на телефон в /sdcard/Download/...")
        subprocess.run(["adb", "push", str(out_zip), "/sdcard/Download/ror2-game-files.zip"], check=False)
        print("  ✓ Готово! Откройте приложение на телефоне -> Set up -> Select game files -> выберите ror2-game-files.zip.")
    else:
        print("[2/2] ADB не найден. Просто скопируйте файл ror2-game-files.zip на телефон и выберите его в лаунчере кнопкой 'Select game files'.")


if __name__ == "__main__":
    main()
