#!/usr/bin/env python3
"""
Автономный скрипт подготовки и отправки файлов Risk of Rain 2 на телефон.
Не требует никаких других файлов репозитория — только этот .py и run_pc.bat!
"""
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

STEAM_CANDIDATES = [
    Path("C:/Program Files (x86)/Steam/steamapps/common/Risk of Rain 2"),
    Path("C:/Program Files/Steam/steamapps/common/Risk of Rain 2"),
    Path("D:/SteamLibrary/steamapps/common/Risk of Rain 2"),
    Path("D:/Steam/steamapps/common/Risk of Rain 2"),
    Path("E:/SteamLibrary/steamapps/common/Risk of Rain 2"),
    Path("E:/Steam/steamapps/common/Risk of Rain 2"),
    Path("F:/SteamLibrary/steamapps/common/Risk of Rain 2"),
    Path.home() / ".local/share/Steam/steamapps/common/Risk of Rain 2",
    Path.home() / "Library/Application Support/Steam/steamapps/common/Risk of Rain 2",
]


def find_steam_via_registry() -> list[Path]:
    found = []
    if os.name != "nt":
        return found
    try:
        import winreg
        for root_key in (winreg.HKEY_CURRENT_USER, winreg.HKEY_LOCAL_MACHINE):
            for subkey in (r"Software\Valve\Steam", r"SOFTWARE\WOW6432Node\Valve\Steam"):
                try:
                    with winreg.OpenKey(root_key, subkey) as key:
                        val, _ = winreg.QueryValueEx(key, "SteamPath")
                        if val:
                            steam_root = Path(val)
                            found.append(steam_root / "steamapps/common/Risk of Rain 2")
                            vdf = steam_root / "steamapps/libraryfolders.vdf"
                            if vdf.is_file():
                                for line in vdf.read_text(encoding="utf-8", errors="ignore").splitlines():
                                    if '"path"' in line:
                                        parts = line.split('"')
                                        if len(parts) >= 4:
                                            lib_path = Path(parts[3].replace("\\\\", "\\"))
                                            found.append(lib_path / "steamapps/common/Risk of Rain 2")
                except OSError:
                    pass
    except Exception:
        pass
    return found


def find_game_dir() -> Path:
    if len(sys.argv) > 1:
        p = Path(sys.argv[1]).expanduser().resolve()
        if p.is_dir():
            return p
        print(f"[!] Указанная папка не найдена: {p}")

    for candidate in [*find_steam_via_registry(), *STEAM_CANDIDATES]:
        if candidate.is_dir() and (candidate / "Risk of Rain 2_Data").is_dir():
            return candidate
        if candidate.is_dir():
            return candidate

    print("\n[?] Автоматически папка Steam с Risk of Rain 2 не найдена.")
    user_input = input("Перетащите сюда папку 'Risk of Rain 2' или вставьте путь и нажмите Enter: ").strip().strip('"').strip("'")
    p = Path(user_input).expanduser().resolve()
    if not p.is_dir():
        input(f"\n[Ошибка] Папка не найдена: {p}\nНажмите Enter для выхода...")
        sys.exit(1)
    return p


def main():
    print("=" * 64)
    print("  Risk of Rain 2 -> Android (x64 / ARM64) | Быстрая упаковка")
    print("=" * 64)

    game_dir = find_game_dir()
    out_zip = Path(__file__).resolve().parent / "ror2-game-files.zip"

    all_files = [f for f in game_dir.rglob("*") if f.is_file()]
    total = len(all_files)
    print(f"\n[1/2] Найдена игра: {game_dir}")
    print(f"      Всего файлов: {total}. Упаковка в {out_zip.name}...")

    with zipfile.ZipFile(out_zip, "w", zipfile.ZIP_DEFLATED, allowZip64=True) as zf:
        for idx, file_path in enumerate(all_files, 1):
            rel = file_path.relative_to(game_dir).as_posix()
            zf.write(file_path, rel)
            if idx % 50 == 0 or idx == total:
                pct = (idx * 100) // max(total, 1)
                print(f"\r      Прогресс: {pct}% ({idx}/{total}) - {file_path.name[:35]:<35}", end="", flush=True)

    size_mb = out_zip.stat().st_size / (1024 * 1024)
    print(f"\n\n  [OK] Архив готов: {out_zip} ({size_mb:.1f} МБ)")

    adb_bin = shutil.which("adb")
    if adb_bin:
        print("\n[2/2] Найден ADB. Проверяем подключенный телефон...")
        res = subprocess.run([adb_bin, "devices"], capture_output=True, text=True)
        lines = [line.strip() for line in res.stdout.splitlines()[1:] if line.strip() and "\tdevice" in line]
        if lines:
            print(f"      Устройство подключено ({lines[0].split()[0]}). Копируем в /sdcard/Download/ror2-game-files.zip...")
            subprocess.run([adb_bin, "push", str(out_zip), "/sdcard/Download/ror2-game-files.zip"], check=False)
            print("\n  [ГОТОВО] Архив отправлен в папку Download на телефоне!")
        else:
            print("      Телефон по ADB не обнаружен. Просто скиньте файл ror2-game-files.zip на телефон по кабелю (MTP).")
    else:
        print("\n[2/2] Теперь просто скиньте созданный файл ror2-game-files.zip на телефон (по кабелю в папку Download).")

    print("\nЧто делать на телефоне:")
    print("  1. Откройте установленный APK Risk of Rain 2.")
    print("  2. Нажмите 'Set up' -> 'Select game files'.")
    print("  3. Выберите файл ror2-game-files.zip — игра сама распакует кэш и включит кнопку Play!")
    print("=" * 64)


if __name__ == "__main__":
    main()
