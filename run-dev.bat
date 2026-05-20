@echo off
echo Demarrage de YuyuFrame (Tauri dev)...
echo.
cd /d "%~dp0Backend"
call "..\Frontend\node_modules\.bin\tauri.cmd" dev
