@echo off
title Haven-Store 一键启动
setlocal
set "CHECKONLY="
if /i "%~1"=="check" set "CHECKONLY=1"

pushd "%~dp0.."
set "ROOT=%CD%"
popd

echo ==================================================
echo            Haven-Store 二手交易平台
echo ==================================================
echo.

echo [检查 1/3] 数据库与缓存服务...
netstat -ano | findstr ":3306" >nul
if errorlevel 1 (echo    [警告] MySQL 没在运行，请先启动 MySQL 服务) else (echo    MySQL 正常)
netstat -ano | findstr ":6379" >nul
if errorlevel 1 (echo    [警告] Redis 没在运行，请先启动 Redis) else (echo    Redis 正常)
echo.

echo [检查 2/3] 后端程序包...
if exist "%ROOT%\backend\target\second-hand-market-1.0.0.jar" (
    echo    已找到 second-hand-market-1.0.0.jar
) else (
    echo    [错误] 找不到后端程序包，请先双击 scripts\rebuild-backend.bat
    echo.
    if not defined CHECKONLY pause
    exit /b 1
)
echo.

echo [检查 3/3] 前端依赖...
if exist "%ROOT%\frontend\node_modules" (echo    前端依赖已安装) else (echo    [提示] 前端依赖未安装，稍后会自动执行 npm install)
echo.

if defined CHECKONLY (
    echo [检查模式] 只做检查，未启动任何服务。
    exit /b 0
)

echo 正在启动后端（新窗口，请勿关闭）...
start "Haven-Store 后端" cmd /k "cd /d "%ROOT%" && java -jar backend\target\second-hand-market-1.0.0.jar --server.port=8080"

timeout /t 3 /nobreak >nul

if exist "%ROOT%\frontend\node_modules" (
    echo 正在启动前端（新窗口，请勿关闭）...
    start "Haven-Store 前端" cmd /k "cd /d "%ROOT%\frontend" && npm run dev"
) else (
    echo 首次运行：正在安装前端依赖并启动，请稍候...
    start "Haven-Store 前端" cmd /k "cd /d "%ROOT%\frontend" && npm install && npm run dev"
)

echo.
echo ==================================================
echo  启动完成。请等约 10 秒，浏览器会自动打开：
echo     前台     http://localhost:3000
echo     管理后台 http://localhost:3000/admin/login
echo.
echo  账号：admin / admin123        普通用户：testuser1 / 123456
echo.
echo  关闭项目：把弹出的两个黑窗口都关掉即可。
echo ==================================================
echo.
pause
