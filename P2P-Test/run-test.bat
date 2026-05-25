@echo off
setlocal

echo === Build du test Java ===
cd java-test\src\main\java
javac -d . test\*.java
if errorlevel 1 (echo ERREUR compilation Java & exit /b 1)
echo OK

echo.
echo === Test Java (distribution de chunks) ===
java -ea test.AgentTest
if errorlevel 1 (echo ERREUR test Java & exit /b 1)

echo.
echo === Build du peer Rust ===
cd ..\..\..\rust-core
cargo build --quiet
if errorlevel 1 (echo ERREUR build Rust & exit /b 1)
echo OK

echo.
echo === Pret ! Pour tester le reseau P2P :
echo.
echo   Terminal 1 ^(signaling^):   cd signaling ^& npm install ^& node server.js
echo   Terminal 2 ^(peer Rust^) :   cd rust-core ^& cargo run -- Alice
echo   Terminal 3 ^(peer Rust^) :   cd rust-core ^& cargo run -- Bob
echo.
