@echo off
chcp 65001 > nul
echo =======================================================
echo   DANG BIEN DICH VA CHAY KIEM THU HUFFMAN (TV3)...
echo =======================================================

if not exist bin mkdir bin

set "JUNIT_CP=C:\Program Files\NetBeans-17\netbeans\platform\modules\ext\junit-jupiter-api-5.6.0.jar;C:\Users\QUOC VUONG\.m2\repository\org\opentest4j\opentest4j\1.2.0\opentest4j-1.2.0.jar;C:\Users\QUOC VUONG\.m2\repository\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar"

javac -encoding UTF-8 -cp "%JUNIT_CP%" -d bin src/main/java/vn/csd/compression/codec/*.java src/main/java/vn/csd/compression/io/*.java src/test/java/vn/csd/compression/HuffmanCodecTest.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LOI] Bien dich that bai! Vui long kiem tra lai ma nguon.
    pause
    exit /b %ERRORLEVEL%
)

echo.
java "-Dfile.encoding=UTF-8" -cp "bin;%JUNIT_CP%" vn.csd.compression.HuffmanCodecTest

echo.
pause
