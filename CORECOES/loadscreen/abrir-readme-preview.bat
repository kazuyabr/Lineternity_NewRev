@echo off
REM Abre o README no navegador com preview estilo GitHub (sem GitHub).
REM Desenvolvido por Dev ⩽ A.L.N/⩾ para o BrProject.
setlocal
cd /d "%~dp0"

where py >nul 2>&1
if %ERRORLEVEL%==0 (
  start "" cmd /c "py -m http.server 8765 --bind 127.0.0.1"
  timeout /t 1 /nobreak >nul
  start "" "http://127.0.0.1:8765/README_PREVIEW.html"
  echo Preview: http://127.0.0.1:8765/README_PREVIEW.html
  echo Feche a janela do servidor quando terminar.
  goto :eof
)

where python >nul 2>&1
if %ERRORLEVEL%==0 (
  start "" cmd /c "python -m http.server 8765 --bind 127.0.0.1"
  timeout /t 1 /nobreak >nul
  start "" "http://127.0.0.1:8765/README_PREVIEW.html"
  echo Preview: http://127.0.0.1:8765/README_PREVIEW.html
  goto :eof
)

REM Sem Python: abre HTML direto + dica do Cursor
start "" "%~dp0README_PREVIEW.html"
echo.
echo Dica Cursor: abra README.md e pressione Ctrl+Shift+V
echo.
pause
