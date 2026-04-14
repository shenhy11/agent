@echo off
echo =======================================
echo Building Release Package - ChronoTech AI Lab
echo =======================================

echo 1. Creating target directories...
mkdir release 2>nul
mkdir release\server 2>nul
mkdir release\web 2>nul
mkdir release\model 2>nul

echo 2. Packaging Spring Boot backend...
cd agent-server
set "JAVA_HOME=C:\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;C:\Maven\apache-maven-3.9.9\bin;%PATH%"
call ..\mvnw.cmd clean package -DskipTests
copy target\agent-server-1.0.0-SNAPSHOT.jar ..\release\server\agent-server.jar
cd ..

echo 3. Copying Frontend Web App...
xcopy /E /I /Y agent-web\* release\web\

echo 4. Copying Finetune LoRA scripts and data...
xcopy /E /I /Y docs\finetune\* release\model\

echo 5. Generating Server Start Script...
echo @echo off > release\start_all.bat
echo set "JAVA_HOME=C:\Java\jdk-21">> release\start_all.bat
echo set "PATH=%%JAVA_HOME%%\bin;%%PATH%%">> release\start_all.bat
echo echo Starting Frontend Web Server...>> release\start_all.bat
echo start /B python -m http.server 3000 -d web >> release\start_all.bat
echo echo Starting Backend Agent Server... >> release\start_all.bat
echo cd server >> release\start_all.bat
echo java -jar agent-server.jar >> release\start_all.bat
echo pause >> release\start_all.bat

echo =======================================
echo Build complete. The platform (Web + Backend + Model) 
echo is packaged into the 'release/' folder.
echo =======================================
