@echo off 
set "JAVA_HOME=C:\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Starting Frontend Web Server...
start /B python -m http.server 3000 -d web 
echo Starting Backend Agent Server... 
cd server 
java -jar agent-server.jar 
pause 
