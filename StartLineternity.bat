@echo off
title Login Server - Lineternity
color 0B
cd /d "%~dp0"

call "%~dp0cache\lineternity-java.inc.bat"
if errorlevel 1 exit /b 1

REM --- Flags para VPS/servidor: evita crash de driver grafico (awt.dll) ---
REM Use Java 21 LTS em datacenter se ainda tiver problemas
REM -Dlineternity.safe.graphics=true = molduras e paineis com cores solidas (sem gradiente)
"%JAVA_CMD%" -Xms256m -Xmx512m -Dsun.java2d.opengl=false -Dsun.java2d.d3d=false -Dsun.java2d.pmoffscreen=false -Dlineternity.safe.graphics=true -cp "libs/*" ext.mods.security.LicenseInit
pause
