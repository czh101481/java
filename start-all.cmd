@echo off
chcp 936 >nul
setlocal
cd /d "%~dp0"

echo ============================================================
echo  一键启动前后端（各开一个窗口）
echo    后端 springBootTest      -^> http://localhost:8081
echo    前端 dragon-register-vue -^> http://localhost:5173
echo ============================================================
echo.

if not exist "springBootTest\start-backend.cmd" (
  echo [ERROR] 未找到 springBootTest\start-backend.cmd
  pause
  exit /b 1
)
if not exist "dragon-register-vue\start-frontend.cmd" (
  echo [ERROR] 未找到 dragon-register-vue\start-frontend.cmd
  pause
  exit /b 1
)

echo 正在启动后端窗口 ...
start "SpringBoot-Backend 8081" cmd /k "%~dp0springBootTest\start-backend.cmd"

echo 等待后端端口 8081 就绪（首次构建可能需下载依赖，请稍候）...
set /a TRIES=0
:waitloop
set /a TRIES+=1
>nul 2>&1 powershell -NoProfile -Command "try{ $c=New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1',8081); $c.Close(); exit 0 } catch { exit 1 }"
if not errorlevel 1 goto ready
if %TRIES% GEQ 150 goto timeout
ping -n 3 127.0.0.1 >nul 2>&1
goto waitloop

:ready
echo [OK] 后端 8081 已就绪，正在启动前端窗口 ...
start "Vue-Frontend 5173" cmd /k "%~dp0dragon-register-vue\start-frontend.cmd"
goto end

:timeout
echo [WARN] 等待后端超时（约 5 分钟），仍继续启动前端，请检查后端窗口的日志。
start "Vue-Frontend 5173" cmd /k "%~dp0dragon-register-vue\start-frontend.cmd"
goto end

:end
echo.
echo 完成。关闭对应窗口即可停止服务。
pause
endlocal
