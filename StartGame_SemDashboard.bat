@echo off
title Lineternity - Game
color 0B

call "%~dp0cache\lineternity-java.inc.bat"

REM ===== Inicializador sem dashboard elaborado By Eduardo.SilvaL2J =====
set L2_EMAIL=brprojeto@l2jbrasil.com

REM Gera uma key simples baseada no horario
set KEY=%RANDOM%%RANDOM%%RANDOM%

REM ===== JVM FLAGS: G1GC + AppCDS + reclaim periodico (similar ao ZGC) =====
call "%~dp0cache\lineternity-g1-reclaim.inc.bat"
set JVM_FLAGS=-Xms3g -Xmx3g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+UseCompactObjectHeaders -XX:+TieredCompilation -XX:TieredStopAtLevel=4 %G1_RECLAIM_FLAGS% -XX:+AutoCreateSharedArchive -XX:SharedArchiveFile=cache/lineternity_cds.jsa -Xlog:cds=error

cd /d "%~dp0game"

if not exist cache mkdir cache

call "%~dp0cache\lineternity-cds-check.inc.bat" "cache\lineternity_cds.jsa" "%~dp0libs\server.jar" "G1"
call "%~dp0cache\lineternity-classpath.inc.bat" "%~dp0libs"

"%JAVA_CMD%" %JVM_FLAGS% -cp "%LINETERNITY_CP%" ext.mods.gameserver.GameServer %KEY% %L2_EMAIL%

pause
