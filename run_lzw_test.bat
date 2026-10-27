@echo off
chcp 65001 > nul
echo =======================================================
echo   DANG BIEN DICH VA CHAY KIEM THU LZW (TV2 - PHAT)...
echo =======================================================

if not exist bin mkdir bin

set CP="bin;%USERPROFILE%\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.11.4\junit-jupiter-api-5.11.4.jar;%USERPROFILE%\.m2\repository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar"

javac -nowarn -encoding UTF-8 -cp %CP% -d bin src/main/java/vn/csd/compression/codec/*.java src/test/java/vn/csd/compression/LZWCodecTest.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LOI] Bien dich that bai! Vui long kiem tra lai ma nguon.
    pause
    exit /b %ERRORLEVEL%
)

echo.
java "-Dfile.encoding=UTF-8" -cp %CP% vn.csd.compression.LZWCodecTest

echo.
pause
