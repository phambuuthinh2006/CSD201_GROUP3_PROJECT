@echo off
chcp 65001 > nul
echo ========================================================================
echo   BIEN DICH VA CHAY BENCHMARK TOAN DIEN RQ2 (BYTE ORDERING & ENTROPY)
echo ========================================================================

if not exist bin mkdir bin

javac -encoding UTF-8 -d bin src/main/java/vn/csd/compression/codec/*.java src/main/java/vn/csd/compression/io/*.java src/main/java/vn/csd/compression/dataset/*.java src/main/java/vn/csd/compression/metrics/*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LOI] Bien dich that bai! Vui long kiem tra lai.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Bien dich thanh cong! Bat dau chay RQ2Runner voi heap 4GB...
echo.
java -Xms1g -Xmx4g "-Dfile.encoding=UTF-8" -cp bin vn.csd.compression.metrics.RQ2Runner

echo.
pause
