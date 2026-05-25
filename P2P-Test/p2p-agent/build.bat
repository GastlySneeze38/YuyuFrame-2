@echo off
setlocal
set AGENT_DIR=%~dp0
set SRC=%AGENT_DIR%src\main\java
set LIB=%AGENT_DIR%lib
set OUT=%AGENT_DIR%build\classes
set JAR=%AGENT_DIR%build\p2p-agent.jar

echo === P2P Agent Build ===

:: Télécharger ASM si absent
if not exist "%LIB%\asm-9.5.jar" (
    echo Téléchargement ASM 9.5...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/ow2/asm/asm/9.5/asm-9.5.jar' -OutFile '%LIB%\asm-9.5.jar' -UseBasicParsing"
    if errorlevel 1 (echo ERREUR: téléchargement ASM & exit /b 1)
)
if not exist "%LIB%\asm-commons-9.5.jar" (
    echo Téléchargement ASM-Commons 9.5...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.5/asm-commons-9.5.jar' -OutFile '%LIB%\asm-commons-9.5.jar' -UseBasicParsing"
    if errorlevel 1 (echo ERREUR: téléchargement ASM-Commons & exit /b 1)
)

:: Compiler
if not exist "%OUT%" mkdir "%OUT%"
echo Compilation...
javac -source 17 -target 17 ^
  -cp "%LIB%\asm-9.5.jar;%LIB%\asm-commons-9.5.jar" ^
  -d "%OUT%" ^
  "%SRC%\com\p2pminecraft\agent\*.java" ^
  "%SRC%\com\p2pminecraft\transformer\*.java" ^
  "%SRC%\com\p2pminecraft\runtime\*.java"
if errorlevel 1 (echo ERREUR: compilation & exit /b 1)

:: Packager le JAR avec manifest + dépendances ASM relocalisées
echo Packaging...
if not exist "%AGENT_DIR%build" mkdir "%AGENT_DIR%build"

:: Extraire ASM dans le répertoire de build
pushd "%OUT%"
jar xf "%LIB%\asm-9.5.jar"
jar xf "%LIB%\asm-commons-9.5.jar"
popd

:: Créer le JAR final
jar --create --file="%JAR%" ^
    --manifest="%AGENT_DIR%src\main\resources\META-INF\MANIFEST.MF" ^
    -C "%OUT%" .

if errorlevel 1 (echo ERREUR: packaging & exit /b 1)

echo.
echo === Build réussi : %JAR% ===
echo.
echo Utilisation dans YuyuFrame :
echo   -javaagent:%JAR%=peerId=xxx,name=Alice,server=ws://localhost:8765
echo.
