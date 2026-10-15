@echo off
chcp 936 >nul
cd /d "%~dp0魔族修行登录-Vue项目"

if not exist "dist\index.html" (
  echo 未找到 dist，正在构建，请稍候……
  call npm run build
)

echo 正在开启魔域，浏览器将自动打开 http://localhost:8080
start "" http://localhost:8080
node preview.cjs
pause
