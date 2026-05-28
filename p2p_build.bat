@echo off
setlocal
set AGENT_DIR=d:\projets\YuyuFrame 2\P2P-Server\p2p-agent
set SRC_MAIN=%AGENT_DIR%\src\main\java
set SRC_STUBS=%AGENT_DIR%\src\stubs
set RES=%AGENT_DIR%\src\main\resources
set LIB=%AGENT_DIR%\lib
set OUT_MAIN=%AGENT_DIR%\build\main
set OUT_STUBS=%AGENT_DIR%\build\stubs
set OUT_ASM=%AGENT_DIR%\build\_asm_tmp
set JAR=%AGENT_DIR%\build\p2p-agent.jar
set ARGS_FILE=%AGENT_DIR%\build\sources.txt
set JAR_CMD="C:\Program Files\Java\jdk-24\bin\jar.exe"
set JAVAC_CMD="C:\Program Files\Java\jdk-24\bin\javac.exe"

echo === Compilation stubs ===
if not exist "%OUT_STUBS%" mkdir "%OUT_STUBS%"
%JAVAC_CMD% --release 17 -d "%OUT_STUBS%" "%SRC_STUBS%\net\minecraft\world\level\Level.java" "%SRC_STUBS%\net\minecraft\world\level\ChunkPos.java" "%SRC_STUBS%\net\minecraft\core\BlockPos.java" "%SRC_STUBS%\net\minecraft\world\level\block\state\BlockState.java" "%SRC_STUBS%\net\minecraft\world\level\chunk\LevelChunk.java"
if errorlevel 1 (echo ERREUR stubs & exit /b 1)

echo === Generation liste sources ===
if not exist "%OUT_MAIN%" mkdir "%OUT_MAIN%"
if exist "%ARGS_FILE%" del /F "%ARGS_FILE%"
for /r "%SRC_MAIN%" %%f in (*.java) do echo "%%f">> "%ARGS_FILE%"

echo === Compilation principale ===
%JAVAC_CMD% --release 17 -cp "%LIB%\mixin.jar;%LIB%\asm-9.5.jar;%LIB%\asm-tree-9.5.jar;%OUT_STUBS%" -d "%OUT_MAIN%" @"%ARGS_FILE%"
if errorlevel 1 (echo ERREUR compilation & exit /b 1)

echo === Copie ressources ===
copy /Y "%RES%\mixins.p2p.json" "%OUT_MAIN%\" >nul
if not exist "%OUT_MAIN%\META-INF\services" mkdir "%OUT_MAIN%\META-INF\services"
copy /Y "%RES%\META-INF\services\*" "%OUT_MAIN%\META-INF\services\" >nul

echo === Extraction ASM ===
if exist "%OUT_ASM%" rd /S /Q "%OUT_ASM%"
mkdir "%OUT_ASM%"
pushd "%OUT_ASM%"
%JAR_CMD% xf "%LIB%\asm-9.5.jar"
%JAR_CMD% xf "%LIB%\asm-tree-9.5.jar"
%JAR_CMD% xf "%LIB%\asm-util-9.5.jar"
%JAR_CMD% xf "%LIB%\asm-analysis-9.5.jar"
%JAR_CMD% xf "%LIB%\asm-commons-9.5.jar"
if exist module-info.class del /F module-info.class
popd
xcopy /E /Y /Q "%OUT_ASM%\org\" "%OUT_MAIN%\org\" >nul 2>nul
rd /S /Q "%OUT_ASM%"

echo === Packaging ===
%JAR_CMD% --create --file="%JAR%" --manifest="%RES%\META-INF\MANIFEST.MF" -C "%OUT_MAIN%" .
if errorlevel 1 (echo ERREUR packaging & exit /b 1)

echo === Deploiement ===
set P2P_DIR=%APPDATA%\YuyuFrame\p2p
if not exist "%P2P_DIR%" mkdir "%P2P_DIR%"
copy /Y "%JAR%" "%P2P_DIR%\p2p-agent.jar" >nul
copy /Y "%LIB%\mixin.jar" "%P2P_DIR%\mixin.jar" >nul
copy /Y "%LIB%\asm-9.5.jar" "%P2P_DIR%\asm-9.5.jar" >nul
copy /Y "%LIB%\asm-tree-9.5.jar" "%P2P_DIR%\asm-tree-9.5.jar" >nul
copy /Y "%LIB%\asm-util-9.5.jar" "%P2P_DIR%\asm-util-9.5.jar" >nul
copy /Y "%LIB%\asm-analysis-9.5.jar" "%P2P_DIR%\asm-analysis-9.5.jar" >nul
copy /Y "%LIB%\asm-commons-9.5.jar" "%P2P_DIR%\asm-commons-9.5.jar" >nul
echo Deploye dans %P2P_DIR%
echo.
echo === BUILD REUSSI ===
