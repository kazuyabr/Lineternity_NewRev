@echo off
REM Resolve o Java a partir do JAVA_HOME configurado na maquina.
if not defined JAVA_HOME (
  echo.
  echo ERRO: JAVA_HOME nao definido no ambiente.
  echo Configure JAVA_HOME para a raiz do JDK e tente novamente.
  pause
  exit /b 1
)

set "JAVA_HOME=%JAVA_HOME:\"=%"
set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

if not exist "%JAVA_CMD%" (
  echo.
  echo ERRO: JAVA_HOME aponta para um JDK invalido: %JAVA_HOME%
  echo Esperado: %JAVA_HOME%\bin\java.exe
  echo Ajuste JAVA_HOME para a raiz correta do JDK e tente novamente.
  pause
  exit /b 1
)
