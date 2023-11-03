@echo off
SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

rem The Java version to install when it's not installed on the system yet
if "%JBANG_DEFAULT_JAVA_VERSION%"=="" (set javaVersion=11) else (set javaVersion=%JBANG_DEFAULT_JAVA_VERSION%)

if "%JBANG_DIR%"=="" (set JBDIR=%userprofile%\.jbang) else (set JBDIR=%JBANG_DIR%)
if "%JBANG_CACHE_DIR%"=="" (set TDIR=%JBDIR%\cache) else (set TDIR=%JBANG_CACHE_DIR%)

rem resolve application jar path from script location and convert to windows path when using cygwin
if exist "%~dp0jbang.jar" (
  set jarPath=%~dp0jbang.jar
) else if exist "%~dp0.jbang\jbang.jar" (
  set jarPath=%~dp0.jbang\jbang.jar
) else (
  if not exist "%JBDIR%\bin\jbang.jar" (
    powershell -NoProfile -ExecutionPolicy Bypass -NonInteractive -Command "%~dp0jbang.ps1 version" > nul
    if !ERRORLEVEL! NEQ 0 ( exit /b %ERRORLEVEL% )
  )
  call "%JBDIR%\bin\jbang.cmd" %*
  exit /b %ERRORLEVEL%
)
if exist "%jarPath%.new" (
  rem a new jbang version was found, we replace the old one with it
  copy /y "%jarPath%.new" "%jarPath%" > nul 2>&1
  del /f /q "%jarPath%.new"
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
    set JAVA_HOME=
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
      echo powershell -NoProfile -ExecutionPolicy Bypass -NonInteractive -Command "%~dp0jbang.ps1 jdk install %JBANG_DEFAULT_JAVA_VERSION%"
      if !ERRORLEVEL! NEQ 0 ( exit /b %ERRORLEVEL% )
      rem Set the current JDK
      !JAVA_EXEC! -classpath "%jarPath%" dev.jbang.Main jdk default "%javaVersion%"
    )
  )
)

if not exist "%TDIR%" ( mkdir "%TDIR%" )
set tmpfile=%TDIR%\%RANDOM%.jbang.tmp
rem execute jbang and pipe to temporary random file
set JBANG_RUNTIME_SHELL=cmd
2>nul >nul timeout /t 0 && (set JBANG_STDIN_NOTTY=false) || (set JBANG_STDIN_NOTTY=true)
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
