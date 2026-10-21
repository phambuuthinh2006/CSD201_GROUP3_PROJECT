@echo off
chcp 65001 > nul
echo =======================================================
echo   DANG BIEN DICH VA CHAY KIEM THU HUFFMAN (TV3)...
echo =======================================================

if not exist bin mkdir bin

javac -encoding UTF-8 -d bin src/main/java/vn/csd/compression/codec/*.java src/main/java/vn/csd/compression/io/*.java src/test/java/vn/csd/compression/HuffmanCodecTest.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LOI] Bien dich that bai! Vui long kiem tra lai ma nguon.
    pause
    exit /b %ERRORLEVEL%
)

echo.
java "-Dfile.encoding=UTF-8" -cp bin vn.csd.compression.HuffmanCodecTest

echo.
pause
