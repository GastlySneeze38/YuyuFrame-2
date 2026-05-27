@echo off
setlocal
set AGENT_DIR=%~dp0
set SRC_MAIN=%AGENT_DIR%src\main\java
set SRC_STUBS=%AGENT_DIR%src\stubs
set RES=%AGENT_DIR%src\main\resources
set LIB=%AGENT_DIR%lib
set OUT_MAIN=%AGENT_DIR%build\main
set OUT_STUBS=%AGENT_DIR%build\stubs
set OUT_ASM=%AGENT_DIR%build\_asm_tmp
set JAR=%AGENT_DIR%build\p2p-agent.jar
set JAR_CMD="C:\Program Files\Java\jdk-24\bin\jar.exe"
set JAVAC_CMD="C:\Program Files\Java\jdk-24\bin\javac.exe"

echo === P2P Agent Build (Mixin) ===

:: Télécharger Mixin si absent
if not exist "%LIB%\mixin.jar" (
    echo Téléchargement Mixin 0.8.7...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.spongepowered.org/maven/org/spongepowered/mixin/0.8.7/mixin-0.8.7.jar' -OutFile '%LIB%\mixin.jar' -UseBasicParsing"
    if errorlevel 1 (echo ERREUR: téléchargement Mixin & exit /b 1)
)

:: Télécharger ASM si absent (base + tree)
if not exist "%LIB%\asm-9.5.jar" (
    echo Téléchargement ASM 9.5...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/ow2/asm/asm/9.5/asm-9.5.jar' -OutFile '%LIB%\asm-9.5.jar' -UseBasicParsing"
    if errorlevel 1 (echo ERREUR: téléchargement ASM & exit /b 1)
)
if not exist "%LIB%\asm-tree-9.5.jar" (
    echo Téléchargement ASM-Tree 9.5...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.5/asm-tree-9.5.jar' -OutFile '%LIB%\asm-tree-9.5.jar' -UseBasicParsing"
    if errorlevel 1 (echo ERREUR: téléchargement ASM-Tree & exit /b 1)
)

:: Compiler les stubs MC (compile-only, non inclus dans le JAR final)
if not exist "%OUT_STUBS%" mkdir "%OUT_STUBS%"
echo Compilation stubs Minecraft...
%JAVAC_CMD% --release 17 ^
  -d "%OUT_STUBS%" ^
  "%SRC_STUBS%\net\minecraft\world\level\Level.java" ^
  "%SRC_STUBS%\net\minecraft\world\level\ChunkPos.java" ^
  "%SRC_STUBS%\net\minecraft\core\BlockPos.java" ^
  "%SRC_STUBS%\net\minecraft\world\level\block\state\BlockState.java" ^
  "%SRC_STUBS%\net\minecraft\world\level\chunk\LevelChunk.java"
if errorlevel 1 (echo ERREUR: compilation stubs & exit /b 1)

:: Compiler le code principal
if not exist "%OUT_MAIN%" mkdir "%OUT_MAIN%"
echo Compilation principale...
%JAVAC_CMD% --release 17 ^
  -cp "%LIB%\mixin.jar;%LIB%\asm-9.5.jar;%LIB%\asm-tree-9.5.jar;%OUT_STUBS%" ^
  -d "%OUT_MAIN%" ^
  "%SRC_MAIN%\com\p2pminecraft\agent\*.java" ^
  "%SRC_MAIN%\com\p2pminecraft\mixin\*.java" ^
  "%SRC_MAIN%\com\p2pminecraft\mixin\service\*.java" ^
  "%SRC_MAIN%\com\p2pminecraft\runtime\*.java"
if errorlevel 1 (echo ERREUR: compilation & exit /b 1)

:: Copier les ressources
echo Copie des ressources...
copy /Y "%RES%\mixins.p2p.json" "%OUT_MAIN%\" >nul
if not exist "%OUT_MAIN%\META-INF\services" mkdir "%OUT_MAIN%\META-INF\services"
copy /Y "%RES%\META-INF\services\*" "%OUT_MAIN%\META-INF\services\" >nul

:: Extraire ASM (base + tree) sans module-info
if exist "%OUT_ASM%" rmdir /S /Q "%OUT_ASM%"
mkdir "%OUT_ASM%"
pushd "%OUT_ASM%"
%JAR_CMD% xf "%LIB%\asm-9.5.jar"
%JAR_CMD% xf "%LIB%\asm-tree-9.5.jar"
if exist module-info.class del /F module-info.class
popd
:: Copier les classes org/objectweb/ dans build/main
xcopy /E /Y /Q "%OUT_ASM%\org\" "%OUT_MAIN%\org\" >nul 2>nul
rmdir /S /Q "%OUT_ASM%"

:: Créer le JAR final
echo Packaging...
%JAR_CMD% --create --file="%JAR%" ^
    --manifest="%RES%\META-INF\MANIFEST.MF" ^
    -C "%OUT_MAIN%" .
if errorlevel 1 (echo ERREUR: packaging & exit /b 1)

:: Déployer automatiquement dans AppData
set P2P_DIR=%APPDATA%\YuyuFrame\p2p
if not exist "%P2P_DIR%" mkdir "%P2P_DIR%"
copy /Y "%JAR%" "%P2P_DIR%\p2p-agent.jar" >nul
copy /Y "%LIB%\mixin.jar" "%P2P_DIR%\mixin.jar" >nul
echo Déployé dans %P2P_DIR%

echo.
echo === Build réussi : %JAR% ===
echo.
