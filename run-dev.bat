@echo off
echo Demarrage de YuyuFrame (Tauri dev)...
echo.

:: Verifier si l'API tourne deja sur le port 3000
netstat -ano | findstr ":3000 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% neq 0 (
    echo Lancement de la Launcher API sur le port 3000...
    start "LauncherAPI" cmd /c "cd /d "%~dp0LauncherAPI" && cargo run"
    echo.
) else (
    echo Launcher API deja active sur le port 3000.
    echo.
)

cd /d "%~dp0Backend"
call "..\Frontend\node_modules\.bin\tauri.cmd" dev
