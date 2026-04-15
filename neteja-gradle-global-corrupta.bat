@echo off
setlocal

echo Comprovant si Android Studio esta obert...
tasklist | findstr /i "studio64.exe" >nul
if not errorlevel 1 (
    echo.
    echo Tanca Android Studio abans d'executar aquesta neteja.
    echo Despres torna a obrir el projecte amb:
    echo obrir-android-studio-viabacar.bat
    pause
    exit /b 1
)

echo Aturant daemons de Gradle...
call "%~dp0gradlew.bat" --stop >nul 2>&1

set "TRANSFORMS_DIR=%USERPROFILE%\.gradle\caches\9.3.1\transforms"

if exist "%TRANSFORMS_DIR%" (
    echo Esborrant la caché corrupta:
    echo %TRANSFORMS_DIR%
    rmdir /s /q "%TRANSFORMS_DIR%"
) else (
    echo No s'ha trobat la carpeta de transforms.
)

if exist "%~dp0.idea\workspace.xml" (
    echo Esborrant l'estat local de workspace d'Android Studio...
    del /f /q "%~dp0.idea\workspace.xml" >nul 2>&1
)

if exist "%~dp0.idea\caches" (
    echo Esborrant la caché local de la carpeta .idea...
    rmdir /s /q "%~dp0.idea\caches"
)

echo.
echo Neteja completada.
echo Ara obre el projecte amb:
echo obrir-android-studio-viabacar.bat
pause
endlocal
