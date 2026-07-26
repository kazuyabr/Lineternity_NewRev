@echo off
setlocal
cd /d "%~dp0"
title Lineternity - Build

:menu
cls
echo.
echo   ========================================================================================================
echo                                          Lineternity - Build
echo.
echo   [1] Compilar incrementalmente os arquivos de codigo Java e Kotlin de forma rapida.
echo   [2] Clean + compilacao completa (com distribution)
echo   [3] Clean + compilacao completa (com distribution)
echo.
echo   ========================================================================================================
echo.
echo.
echo.
echo   [0] Sair
echo.
rem choice: teclas 1, 2, 3, 0  -  ERRORLEVEL 1..4 (testar do maior para o menor)
choice /C 1230 /N /M "Digite a opcao: "

if errorlevel 4 goto :sair
if errorlevel 3 goto :ant_dist
if errorlevel 2 goto :clean_build
if errorlevel 1 goto :compile

:compile
echo.
echo --- Compilando... ---
call "%~dp0gradlew.bat" li-compile
goto :apos

:clean_build
echo.
echo --- Clean + compilar... ---
call "%~dp0gradlew.bat" li-compile-clean
goto :apos

:ant_dist
echo.
echo --- Clean + Compilar completo... ---
call "%~dp0gradlew.bat" li-compile-clean
goto :apos

:apos
echo.
pause
goto :menu

:sair
echo.
endlocal
exit /b 0
