@echo off
chcp 65001 > nul
echo ===================================================================
echo   DANG BIEN DICH VA KHOI CHAY THI NGHIEM RQ1 (BENCHMARK 4 CODECS)...
echo ===================================================================

if not exist bin mkdir bin
if not exist results mkdir results

set TIMESTAMP=%DATE:~10,4%%DATE:~4,2%%DATE:~7,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set OUTDIR=results\rq1_run_%TIMESTAMP%
mkdir "%OUTDIR%"

echo.
echo [1/2] Dang bien dich cac Codecs va BenchmarkRunner...
javac -nowarn -encoding UTF-8 -d bin src/main/java/vn/csd/compression/io/*.java src/main/java/vn/csd/compression/codec/*.java scripts/benchmark/CodecComparison.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LOI] Bien dich that bai! Vui long kiem tra lai ma nguon.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Dang chay Benchmark so sanh cac thuat toan (RQ1)...
echo       (Kich thuoc: 1 MiB, 4 MiB tren cac loai du lieu lặp, text, random)
echo       Ket qua se duoc luu tai: %OUTDIR%
echo.

java "-Dfile.encoding=UTF-8" -Xms512m -Xmx1g -XX:+UseG1GC -cp bin CodecComparison "%OUTDIR%" 1

echo.
echo ===================================================================
echo [HOAN TAT] Thi nghiem RQ1 da chay xong thanh cong!
echo Du lieu CSV da duoc ghi vao thu muc: %OUTDIR%
echo ===================================================================
echo.
pause
