@echo off
chcp 936 >nul 2>&1
REM ============================================================
REM 魔族修行 一键启动脚本 (GBK 编码 / 相对路径)
REM 场景: 每次开机 node_modules 与 target 会被清空, 本脚本自行重建
REM 前置: MySQL 服务可启动; 系统 mvn / java / node / npm 在 PATH 中
REM ============================================================

REM 若非管理员, 自动请求提权后重新运行本脚本(以便启动 MySQL 服务)
NET SESSION >nul 2>&1 || (
  echo 检测到非管理员权限, 正在请求管理员权限以启动 MySQL 服务...
  powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)

setlocal enabledelayedexpansion

REM ---------- 可按需修改的配置 ----------
set "MYSQL_SERVICE=MySQL84"
set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
set "BACKEND=springBootTest"
set "FRONTEND=魔族修行登录-Vue项目"

cd /d "%~dp0"

echo [1/5] 启动 MySQL 服务 (%MYSQL_SERVICE%) ...
net start %MYSQL_SERVICE% >nul 2>&1 || echo       (MySQL 可能已在运行, 若未启动请手动: net start %MYSQL_SERVICE%)

echo [2/5] 导入数据库 (务必用 utf8mb4 客户端字符集, 防止中文乱码) ...
if not exist "%MYSQL_BIN%" set "MYSQL_BIN=mysql"
"%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p123456 < "%BACKEND%\db\demon_init.sql"
"%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p123456 < "%BACKEND%\db\seed_demo.sql"
"%MYSQL_BIN%" --default-character-set=utf8mb4 -uroot -p123456 < "%BACKEND%\db\demon_content.sql"

echo [3/5] Maven 打包后端 (重建 target, 首次需联网拉取依赖) ...
cd "%BACKEND%"
where mvn >nul 2>&1 && mvn -q -DskipTests package || mvnw.cmd -q -DskipTests package
cd ..

echo [4/5] 启动后端 (端口 8081) ...
start "魔族后端" cmd /k "cd /d %~dp0%BACKEND% && java -jar target\springBootTest-0.0.1-SNAPSHOT.jar"

echo [5/5] 安装前端依赖并启动 (端口 5173) ...
start "魔族前端" cmd /k "cd /d %~dp0%FRONTEND% && call npm install && npm run dev"

echo.
echo ===== 启动完成 =====
echo   前端页面:  http://localhost:5173
echo   后端接口:  http://localhost:8081
echo   演示账号:  魔祖 / 123456
echo   日志请查看弹出的两个命令行窗口
echo   停止请运行 暂停.bat
