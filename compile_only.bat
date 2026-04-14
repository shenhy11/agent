@echo off
set "JAVA_HOME=C:\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;C:\Maven\apache-maven-3.9.9\bin;%PATH%"
cd agent-server
call ..\mvnw.cmd clean compile -DskipTests
