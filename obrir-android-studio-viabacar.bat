@echo off
setlocal

rem Obrim Android Studio amb una caché de Gradle local del projecte
set "PROJECTE_DIR=%~dp0"
if "%PROJECTE_DIR:~-1%"=="\" set "PROJECTE_DIR=%PROJECTE_DIR:~0,-1%"

set "GRADLE_USER_HOME=%PROJECTE_DIR%\.gradle-local"
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"

set "STUDIO_EXE=C:\Program Files\Android\Android Studio\bin\studio64.exe"
if not exist "%STUDIO_EXE%" (
    echo No s'ha trobat Android Studio a:
    echo %STUDIO_EXE%
    echo.
    echo Si el tens instal.lat a un altre lloc, canvia la ruta dins d'aquest fitxer.
    pause
    exit /b 1
)

echo Obrint ViBaCar amb GRADLE_USER_HOME local:
echo %GRADLE_USER_HOME%
start "" "%STUDIO_EXE%" "%PROJECTE_DIR%"

endlocal
