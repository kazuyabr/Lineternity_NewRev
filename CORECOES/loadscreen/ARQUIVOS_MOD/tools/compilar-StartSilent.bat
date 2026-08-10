@echo off
REM Gera Start.exe silencioso (sem janela CMD) usando StartSilent.cs
REM Desenvolvido por Dev ⩽ A.L.N/⩾ para o BrProject.
setlocal
cd /d "%~dp0\..\..\..\.."

set CSC=C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe
set SRC=%~dp0StartSilent.cs
set OUT=Brproject_Distribution\Start.exe

if not exist "%CSC%" (
  echo csc.exe nao encontrado: %CSC%
  exit /b 1
)
if not exist "%SRC%" (
  echo StartSilent.cs nao encontrado: %SRC%
  exit /b 1
)

if exist "%OUT%" copy /Y "%OUT%" "Brproject_Distribution\Start.native.bak.exe" >nul

"%CSC%" /nologo /target:winexe /optimize+ /r:System.Windows.Forms.dll /out:"%OUT%" "%SRC%"
if errorlevel 1 exit /b 1

echo OK: %OUT%
dir "%OUT%"
endlocal
