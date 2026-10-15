@echo off
chcp 936 >nul 2>&1
REM ============================================================
REM 魔族修行 一键暂停脚本 (GBK 编码)
REM 作用: 停止后端(8081)与前端(5173), 并关闭启动脚本弹出的窗口
REM ============================================================

echo 正在停止魔族修行服务 ...

REM 按监听端口结束进程: 8081=后端(java), 5173=前端(node)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1

REM 关闭启动脚本弹出的命令行窗口
taskkill /F /FI "WINDOWTITLE eq 魔族后端*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq 魔族前端*" >nul 2>&1

echo 已停止. 需要重新启动请运行 启动.bat
