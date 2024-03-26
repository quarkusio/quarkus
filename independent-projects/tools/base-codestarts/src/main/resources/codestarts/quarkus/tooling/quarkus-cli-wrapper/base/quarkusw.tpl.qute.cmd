@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version {quarkus.version}
@REM
@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM MAVEN_BATCH_PAUSE - set to 'on' to wait for a keystroke before ending
@REM MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@REM Begin all REM lines with '@' in case MAVEN_BATCH_ECHO is 'on'
@echo off
@REM set title of command window
title %0
@REM enable echoing by setting MAVEN_BATCH_ECHO to 'on'
@if "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%

@REM set %HOME% to equivalent of $HOME
if "%HOME%" == "" (set "HOME=%HOMEDRIVE%%HOMEPATH%")

@setlocal

set ERROR_CODE=0

@REM To isolate internal variables from possible post scripts, we use another setlocal
@setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%" == "" goto OkJHome

echo.
echo Error: JAVA_HOME not found in your environment. >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo.
goto error

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto init

echo.
echo Error: JAVA_HOME is set to an invalid directory. >&2
echo JAVA_HOME = "%JAVA_HOME%" >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo.
goto error

@REM ==== END VALIDATION ====

:init

@REM Find the project base dir, i.e. the directory that contains the folder ".quarkus".
@REM Fallback to current working directory if not found.

set MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
IF NOT "%MAVEN_PROJECTBASEDIR%"=="" goto endDetectBaseDir

set EXEC_DIR=%CD%
set WDIR=%EXEC_DIR%
:findBaseDir
IF EXIST "%WDIR%"\.quarkus goto baseDirFound
cd ..
IF "%WDIR%"=="%CD%" goto baseDirNotFound
set WDIR=%CD%
goto findBaseDir

:baseDirFound
set MAVEN_PROJECTBASEDIR=%WDIR%
cd "%EXEC_DIR%"
goto endDetectBaseDir

:baseDirNotFound
set MAVEN_PROJECTBASEDIR=%EXEC_DIR%
cd "%EXEC_DIR%"

:endDetectBaseDir

SET QUARKUS_JAVA_EXE="%JAVA_HOME%\bin\java.exe"
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.quarkus\cli\wrapper\quarkus-cli.jar"

set WRAPPER_URL="https://repo.maven.apache.org/maven2/io/quarkus/quarkus-cli/{quarkus.version}/quarkus-cli-{quarkus.version}-runner.jar"

FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.quarkus\cli\wrapper\quarkus-cli.properties") DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
)

@REM Extension to allow automatically downloading the quarkus-cli.jar from Maven-central
@REM This allows using the maven wrapper in projects that prohibit checking in binary data.
if exist %WRAPPER_JAR% (
    if "%QUARKUSW_VERBOSE%" == "true" (
        echo Found %WRAPPER_JAR%
    )
) else if exist "%USERPROFILE%\.m2\repository\io\quarkus\quarkus-cli\{quarkus.version}\quarkus-cli-{quarkus.version}-runner.jar" (
  copy "%USERPROFILE%\.m2\repository\io\quarkus\quarkus-cli\{quarkus.version}\quarkus-cli-{quarkus.version}-runner.jar" %WRAPPER_JAR%
) else (
    if not "%QUARKUSW_REPOURL%" == "" (
        SET WRAPPER_URL="%QUARKUSW_REPOURL%/io/quarkus/quarkus-cli/{quarkus.version}/quarkus-cli-{quarkus.version}-runner.jar"
    )
    if "%QUARKUSW_VERBOSE%" == "true" (
        echo Couldn't find %WRAPPER_JAR%, downloading it ...
        echo Downloading from: %WRAPPER_URL%
    )

    powershell -Command "&{"^
		"$webclient = new-object System.Net.WebClient;"^
		"if (-not ([string]::IsNullOrEmpty('%QUARKUSW_USERNAME%') -and [string]::IsNullOrEmpty('%QUARKUSW_PASSWORD%'))) {"^
		"$webclient.Credentials = new-object System.Net.NetworkCredential('%QUARKUSW_USERNAME%', '%QUARKUSW_PASSWORD%');"^
		"}"^
		"[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"^
		"}"
    if "%QUARKUSW_VERBOSE%" == "true" (
        echo Finished downloading %WRAPPER_JAR%
    )
)
@REM End of extension

@REM If specified, validate the SHA-256 sum of the Maven wrapper jar file
SET WRAPPER_SHA_256_SUM=""
FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.quarkus\cli\wrapper\quarkus-cli.properties") DO (
    IF "%%A"=="wrapperSha256Sum" SET WRAPPER_SHA_256_SUM=%%B
)
IF NOT %WRAPPER_SHA_256_SUM%=="" (
    powershell -Command "&{"^
       "$hash = (Get-FileHash \"%WRAPPER_JAR%\" -Algorithm SHA256).Hash.ToLower();"^
       "If('%WRAPPER_SHA_256_SUM%' -ne $hash){"^
       "  Write-Output 'Error: Failed to validate Maven wrapper SHA-256, your Maven wrapper might be compromised.';"^
       "  Write-Output 'Investigate or delete %WRAPPER_JAR% to attempt a clean download.';"^
       "  Write-Output 'If you updated your Maven version, you need to update the specified wrapperSha256Sum property.';"^
       "  exit 1;"^
       "}"^
       "}"
    if ERRORLEVEL 1 goto error
)

%QUARKUS_JAVA_EXE% ^
  -jar %WRAPPER_JAR% ^
  %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

if "%QUARKUS_TERMINATE_CMD%"=="on" exit %ERROR_CODE%

cmd /C exit /B %ERROR_CODE%
