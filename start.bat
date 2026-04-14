@echo off
set "JAVA_HOME=C:\Java\jdk-21"
set "DASHSCOPE_API_KEY=sk-45860ea7946e4162a8ed67610dcb774b"
set "PATH=%JAVA_HOME%\bin;C:\Maven\apache-maven-3.9.9\bin;%PATH%"
echo Starting agent-server...
java -version
echo ---
cd /d "d:\code\agent\agent-server"
call mvn spring-boot:run -Dspring-boot.run.profiles=dev
