@echo off
title Lineternity - Login
color 0A

REM --- Habilita cores ANSI no console (cmd.exe) antes do Java imprimir o banner ---
call "%~dp0cache\lineternity-ansi.inc.bat"

call "%~dp0cache\lineternity-java.inc.bat"

REM ===== Inicializador sem dashboard elaborado By Eduardo.SilvaL2J =====
call "%~dp0cache\lineternity-g1-reclaim.inc.bat"
set JVM_FLAGS=-Xms256m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=8m -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+UseCompactObjectHeaders -XX:+TieredCompilation -XX:TieredStopAtLevel=4 %G1_RECLAIM_FLAGS%

cd /d "%~dp0login"

if not exist cache mkdir cache
if exist cache\lineternity_cds.jsa del /f /q cache\lineternity_cds.jsa 2>nul
if exist cache\lineternity_cds.gc del /f /q cache\lineternity_cds.gc 2>nul

call "%~dp0cache\lineternity-classpath.inc.bat" "%~dp0libs"

"%JAVA_CMD%" %JVM_FLAGS% -cp "%LINETERNITY_CP%" ext.mods.loginserver.LoginServer

pause
