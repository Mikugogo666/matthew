@echo off
chcp 65001 >nul
setlocal

set "ROOT=%~dp0"
set "MVN_EXE=D:\apache-maven-3.9.10\apache-maven-3.9.10\bin\mvn.cmd"

cd /d "%ROOT%"
call "%MVN_EXE%" spring-boot:run
pause
