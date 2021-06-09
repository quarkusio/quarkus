@echo off
SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION


rem The Java version to install when it's not installed on the system yet
if "%JBANG_DEFAULT_JAVA_VERSION%"=="" (set javaVersion=11) else (set javaVersion=%JBANG_DEFAULT_JAVA_VERSION%)

set os=windows
set arch=x64

set jburl="https://github.com/jbangdev/jbang/releases/latest/download/jbang.zip"
set jdkurl="https://api.adoptopenjdk.net/v3/binary/latest/%javaVersion%/ga/%os%/%arch%/jdk/hotspot/normal/adoptopenjdk"

if "%JBANG_DIR%"=="" (set JBDIR=%userprofile%\.jbang) else (set JBDIR=%JBANG_DIR%)
if "%JBANG_CACHE_DIR%"=="" (set TDIR=%JBDIR%\cache) else (set TDIR=%JBANG_CACHE_DIR%)

rem resolve application jar path from script location and convert to windows path when using cygwin
if exist "%~dp0jbang.jar" (
  set jarPath=%~dp0jbang.jar
) else if exist "%~dp0.jbang\jbang.jar" (
  set jarPath=%~dp0.jbang\jbang.jar
) else (
  if not exist "%JBDIR%\bin\jbang.jar" (
    echo Downloading JBang... 1>&2
    if not exist "%TDIR%\urls" ( mkdir "%TDIR%\urls" )
    powershell -NoProfile -ExecutionPolicy Bypass -NonInteractive -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest %jburl% -OutFile %TDIR%\urls\jbang.zip"
    if !ERRORLEVEL! NEQ 0 ( echo Error downloading JBang 1>&2 & exit /b %ERRORLEVEL% )
    echo Installing JBang... 1>&2
    if exist "%TDIR%\urls\jbang" ( rd /s /q "%TDIR%\urls\jbang" > nul 2>&1 )
    powershell -NoProfile -ExecutionPolicy Bypass -NonInteractive -Command "$ProgressPreference = 'SilentlyContinue'; Expand-Archive -Path %TDIR%\urls\jbang.zip -DestinationPath %TDIR%\urls"
    if !ERRORLEVEL! NEQ 0 ( echo Error installing JBang 1>&2 & exit /b %ERRORLEVEL% )
    if not exist "%JBDIR%\bin" ( mkdir "%JBDIR%\bin" )
    del /f /q "%JBDIR%\bin\jbang" "%JBDIR%\bin\jbang.*"
    copy /y "%TDIR%\urls\jbang\bin\*" "%JBDIR%\bin" > nul 2>&1
  )
  call "%JBDIR%\bin\jbang.cmd" %*
  exit /b %ERRORLEVEL%
)

rem Find/get a JDK
set JAVA_EXEC=
if not "%JAVA_HOME%"=="" (
  rem Determine if a (working) JDK is available in JAVA_HOME
  if exist "%JAVA_HOME%\bin\javac.exe" (
    set JAVA_EXEC="%JAVA_HOME%\bin\java.exe"
  ) else (
    echo JAVA_HOME is set but does not seem to point to a valid Java JDK 1>&2
  )
)
if "!JAVA_EXEC!"=="" (
  rem Determine if a (working) JDK is available on the PATH
  where javac > nul 2>&1
  if !errorlevel! equ 0 (
    set JAVA_EXEC=java.exe
  ) else if exist "%JBDIR%\currentjdk\bin\javac" (
    set JAVA_HOME=%JBDIR%\currentjdk
    set JAVA_EXEC=%JBDIR%\currentjdk\bin\java
  ) else (
    set JAVA_HOME=%TDIR%\jdks\%javaVersion%
    set JAVA_EXEC=!JAVA_HOME!\bin\java.exe
    rem Check if we installed a JDK before
    if not exist "%TDIR%\jdks\%javaVersion%" (
      rem If not, download and install it
      if not exist "%TDIR%\jdks" ( mkdir "%TDIR%\jdks" )
      echo Downloading JDK %javaVersion%. Be patient, this can take several minutes... 1>&2
      powershell -NoProfile -ExecutionPolicy Bypass -NonInteractive -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest %jdkurl% -OutFile %TDIR%\bootstrap-jdk.zip"
      if !ERRORLEVEL! NEQ 0 ( echo Error downloading JDK 1>&2 & exit /b %ERRORLEVEL% )
      echo Installing JDK %javaVersion%... 1>&2
      if exist "%TDIR%\jdks\%javaVersion%.tmp" ( rd /s /q "%TDIR%\jdks\%javaVersion%.tmp" > nul 2>&1 )
      powershell -NoProfile -ExecutionPolicy Bypass -NonInteractive -Command "$ProgressPreference = 'SilentlyContinue'; Expand-Archive -Path %TDIR%\bootstrap-jdk.zip -DestinationPath %TDIR%\jdks\%javaVersion%.tmp"
      if !ERRORLEVEL! NEQ 0 ( echo Error installing JDK 1>&2 & exit /b %ERRORLEVEL% )
      for /d %%d in (%TDIR%\jdks\%javaVersion%.tmp\*) do (
        powershell -NoProfile -ExecutionPolicy Bypass -NonInteractive -Command "Move-Item %%d\* !TDIR!\jdks\%javaVersion%.tmp"
        if !ERRORLEVEL! NEQ 0 ( echo Error installing JDK 1>&2 & exit /b %ERRORLEVEL% )
      )
      rem Check if the JDK was installed properly
      %TDIR%\jdks\%javaVersion%.tmp\bin\javac -version > nul 2>&1
      if !ERRORLEVEL! NEQ 0 ( echo "Error installing JDK" 1>&2; exit /b %ERRORLEVEL% )
      rem Activate the downloaded JDK giving it its proper name
      ren "%TDIR%\jdks\%javaVersion%.tmp" "%javaVersion%"
    )
    # Set the current JDK
    !JAVA_EXEC! -classpath "%jarPath%" dev.jbang.Main jdk default "%javaVersion%"
  )
)

if not exist "%TDIR%" ( mkdir "%TDIR%" )
set tmpfile=%TDIR%\%RANDOM%.jbang.tmp
rem execute jbang and pipe to temporary random file
set JBANG_USES_POWERSHELL=
set "CMD=!JAVA_EXEC!"
SETLOCAL DISABLEDELAYEDEXPANSION
%CMD% > "%tmpfile%" %JBANG_JAVA_OPTIONS% -classpath "%jarPath%" dev.jbang.Main %*
set ERROR=%ERRORLEVEL%
rem catch errorlevel straight after; rem or FOR /F swallow would have swallowed the errorlevel

if %ERROR% EQU 255 (
  rem read generated java command by jang, delete temporary file and execute.
  for %%A in ("%tmpfile%") do for /f "usebackq delims=" %%B in (%%A) do (
    set "OUTPUT=%%B"
    goto :break
  )
:break
  del /f /q "%tmpfile%"
  %OUTPUT%
  exit /b %ERRORLEVEL%
) else (
  type "%tmpfile%"
  del /f /q "%tmpfile%"
  exit /b %ERROR%
)
