@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM   https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.

@echo off
set MAVEN_PROJECTBASEDIR=%~dp0
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties
set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar

for /F "usebackq tokens=2 delims==" %%i in (`findstr /i "wrapperUrl" "%WRAPPER_PROPERTIES%"`) do set DOWNLOAD_URL=%%i
for /F "usebackq tokens=2 delims==" %%i in (`findstr /i "distributionUrl" "%WRAPPER_PROPERTIES%"`) do set DISTRIBUTION_URL=%%i

if not exist "%WRAPPER_JAR%" (
  if not "%MVNW_VERBOSE%"=="false" echo Downloading Maven wrapper...
  powershell -Command "(New-Object Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%')"
)

if "%JAVA_HOME%"=="" (
  set JAVACMD=java.exe
) else (
  set JAVACMD="%JAVA_HOME%\bin\java.exe"
)

%JAVACMD% %MAVEN_OPTS% -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
