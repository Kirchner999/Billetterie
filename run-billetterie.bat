@echo off
setlocal
cd /d "%~dp0"

set "APP_JAR=target\billetterie-main-1.0-SNAPSHOT-all.jar"

if not exist "%APP_JAR%" (
    echo Jar introuvable: %APP_JAR%
    echo Lance d'abord: mvn clean package
    pause
    exit /b 1
)

java --enable-native-access=ALL-UNNAMED -jar "%APP_JAR%"

if errorlevel 1 (
    echo.
    echo L'application s'est arretee avec une erreur.
    pause
)
