@echo off
chcp 65001 >nul
cd /d %~dp0
echo [dragon-auth] starting on http://localhost:8081 ...
java RegisterServer.java
pause
