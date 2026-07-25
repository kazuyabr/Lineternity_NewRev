@echo off
REM Resolve o Java — detecta automaticamente ou usa JAVA_HOME do ambiente.

REM Se JAVA_HOME nao esta definido, tenta detectar
if not defined JAVA_HOME (
  if exist "C:\Program Files\jdk\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\jdk"
  ) else if exist "C:\Program Files\Java\jdk-25\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25"
  ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
  )
)

if not defined JAVA_HOME (
  echo.
  echo ERRO: JAVA_HOME nao definido e JDK nao encontrado nos caminhos padrao.
  echo Configure JAVA_HOME para a raiz do JDK e tente novamente.
  pause
  exit /b 1
)

set "JAVA_HOME=%JAVA_HOME:"=%"
set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

if not exist "%JAVA_CMD%" (
  echo.
  echo ERRO: JAVA_HOME aponta para um JDK invalido: %JAVA_HOME%
  echo Esperado: %JAVA_HOME%\bin\java.exe
  echo Ajuste JAVA_HOME para a raiz correta do JDK e tente novamente.
  pause
  exit /b 1
)
