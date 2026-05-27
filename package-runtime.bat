@echo off
setlocal

set APP_NAME=Dispelltacle
set APP_VERSION=1.0
set MAIN_JAR=billetterie-main-1.0-SNAPSHOT-all.jar
set PACKAGE_DIR=target\package

call mvn clean package
if errorlevel 1 exit /b 1

if exist "%PACKAGE_DIR%\%APP_NAME%" rmdir /s /q "%PACKAGE_DIR%\%APP_NAME%"

jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --input target ^
  --main-jar "%MAIN_JAR%" ^
  --main-class fr.billetterie.Main ^
  --dest "%PACKAGE_DIR%" ^
  --java-options "--enable-native-access=javafx.graphics"

if errorlevel 1 exit /b 1

echo.
echo Package genere : %PACKAGE_DIR%\%APP_NAME%\%APP_NAME%.exe
