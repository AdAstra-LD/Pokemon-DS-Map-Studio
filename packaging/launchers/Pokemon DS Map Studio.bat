@echo off
setlocal
set "APP_DIR=%~dp0"
where javaw.exe >nul 2>&1
if errorlevel 1 (
    echo Java 11 or newer was not found.
    echo Install Java or download the Windows package, which includes Java.
    pause
    exit /b 1
)
start "Pokemon DS Map Studio" javaw.exe -jar "%APP_DIR%Pokemon DS Map Studio.jar" %*
