@echo off
chcp 65001 > nul
echo ========================================================================
echo   KIEM THU T11: TINH BAT BIEN CUA HISTOGRAM VA ENTROPY H0 (RQ2)
echo ========================================================================

if not exist bin mkdir bin

set CP="bin;%USERPROFILE%\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.11.4\junit-jupiter-api-5.11.4.jar;%USERPROFILE%\.m2\repository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar"

javac -nowarn -encoding UTF-8 -cp %CP% -d bin src/main/java/vn/csd/compression/dataset/ByteOrdering.java src/main/java/vn/csd/compression/metrics/Entropy.java src/test/java/vn/csd/compression/ByteOrderingTest.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [LOI] Bien dich that bai! Vui long kiem tra lai.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Bien dich thanh cong! Bat dau kiem thu Test T11...
echo.
java "-Dfile.encoding=UTF-8" -cp %CP% vn.csd.compression.ByteOrderingTest

echo.
pause
