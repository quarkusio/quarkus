@REM ----------------------------------------------------------------------------
@REM  Copyright 2001-2006 The Apache Software Foundation.
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM       http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM ----------------------------------------------------------------------------
@REM
@REM   Copyright (c) 2001-2006 The Apache Software Foundation.  All rights
@REM   reserved.

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\.. 
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
for %%i in ("%~dp0..") do set "BASEDIR=%%~fi"

:repoSetup
set REPO=


if "%JAVACMD%"=="" set JAVACMD=java

@REM The Quarkus CLI requires a recent Java version: check it to provide a clear error message.
@REM If we can't determine the version, we just start the CLI.
@REM The minimum Java version is injected by release-cli.sh.
set MINIMUM_JAVA_VERSION=__MINIMUM_JAVA_VERSION__
set JAVA_VERSION_STRING=
set JAVA_MAJOR_VERSION=
@REM the version is the third token of the line containing "version", e.g. openjdk version "21.0.1" 2023-10-17
for /f "tokens=2,3" %%a in ('%JAVACMD% -version 2^>^&1') do if /i "%%a"=="version" if not defined JAVA_VERSION_STRING set "JAVA_VERSION_STRING=%%~b"
if not defined JAVA_VERSION_STRING goto javaVersionChecked
@REM 1.8.0_292 -> 8, 17.0.2 -> 17, 21-ea -> 21
for /f "tokens=1,2 delims=.-_+" %%a in ("%JAVA_VERSION_STRING%") do if "%%a"=="1" (set "JAVA_MAJOR_VERSION=%%b") else (set "JAVA_MAJOR_VERSION=%%a")
if not defined JAVA_MAJOR_VERSION goto javaVersionChecked
echo %JAVA_MAJOR_VERSION%| findstr /r /x "[0-9][0-9]*" >nul || goto javaVersionChecked
if %JAVA_MAJOR_VERSION% GEQ %MINIMUM_JAVA_VERSION% goto javaVersionChecked
1>&2 echo Error: the Quarkus CLI requires Java %MINIMUM_JAVA_VERSION% or later but %JAVACMD% is Java %JAVA_VERSION_STRING%.
1>&2 echo   Please make sure a Java %MINIMUM_JAVA_VERSION%+ java is in your PATH or set JAVACMD.
@REM sets ERRORLEVEL to 1
verify other 2>nul
goto error
:javaVersionChecked

if "%REPO%"=="" set REPO=%BASEDIR%\lib

set CLASSPATH="%BASEDIR%"\etc;"%REPO%"\__RUNNER_JAR__

set ENDORSED_DIR=
if NOT "%ENDORSED_DIR%" == "" set CLASSPATH="%BASEDIR%"\%ENDORSED_DIR%\*;%CLASSPATH%

if NOT "%CLASSPATH_PREFIX%" == "" set CLASSPATH=%CLASSPATH_PREFIX%;%CLASSPATH%

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS%  -classpath %CLASSPATH% -Dapp.name="quarkus" -Dapp.repo="%REPO%" -Dapp.home="%BASEDIR%" -Dbasedir="%BASEDIR%" io.quarkus.cli.Main %CMD_LINE_ARGS%
if %ERRORLEVEL% NEQ 0 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=%ERRORLEVEL%

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@REM If error code is set to 1 then the endlocal was done already in :error.
if %ERROR_CODE% EQU 0 @endlocal


:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
