@echo off
title 停止 Haven-Store 开发服务
setlocal enabledelayedexpansion

pushd "%~dp0.."
set "ROOT=%CD%"
popd

set "MYSQL_BASE=C:\Program Files\MySQL\MySQL Server 8.0"
set "MYSQLADMIN=%MYSQL_BASE%\bin\mysqladmin.exe"
set "REDIS_CLI=%ROOT%\.tools\redis\redis-cli.exe"

echo ==================================================
echo  停止 Haven-Store 开发服务（后端 / 前端 / Redis / MySQL）
echo ==================================================
echo.

rem ==================== 1. 后端 jar ====================
echo [1/4] 查找并停止后端进程（second-hand-market jar）...
set "BACKEND_KILLED=0"
for /f "tokens=1" %%P in ('jps -l 2^>nul ^| findstr /i "second-hand-market"') do (
    taskkill /PID %%P /F >nul 2>&1
    set "BACKEND_KILLED=1"
    echo   已停止后端进程 PID=%%P
)
if "!BACKEND_KILLED!"=="0" (
    rem 兜底：jps 不可用时按命令行匹配 java 进程
    for /f "tokens=2 delims=," %%P in ('wmic process where "name like 'java%%' and commandline like '%%second-hand-market-1.0.0.jar%%'" get processid /format:csv 2^>nul ^| findstr /r "[0-9]"') do (
        taskkill /PID %%P /F >nul 2>&1
        set "BACKEND_KILLED=1"
        echo   已停止后端进程 PID=%%P
    )
)
if "!BACKEND_KILLED!"=="0" echo   [跳过] 后端未在运行。

rem ==================== 2. 前端 vite ====================
echo [2/4] 停止前端开发服务（3000 端口）...
set "FRONT_KILLED=0"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":3000" ^| findstr "LISTENING"') do (
    taskkill /PID %%P /F >nul 2>&1
    set "FRONT_KILLED=1"
    echo   已停止前端进程 PID=%%P
)
if "!FRONT_KILLED!"=="0" echo   [跳过] 前端未在运行。

rem ==================== 3. Redis ====================
echo [3/4] 安全关闭 Redis（数据落盘）...
if exist "%REDIS_CLI%" (
    "%REDIS_CLI%" -h 127.0.0.1 -p 6379 shutdown 2>nul
    if not errorlevel 1 (
        echo   Redis 已关闭。
    ) else (
        echo   [跳过] Redis 未在运行或已关闭。
    )
) else (
    echo   [跳过] 未找到 redis-cli.exe，请手动关闭 Redis 窗口。
)

rem ==================== 4. MySQL ====================
echo [4/4] 安全关闭 MySQL（数据落盘，避免强杀损坏表）...
if exist "%MYSQLADMIN%" (
    "%MYSQLADMIN%" -uroot -p1234 shutdown 2>nul
    if not errorlevel 1 (
        echo   MySQL 已关闭。
    ) else (
        echo   [跳过] MySQL 未在运行，或口令不是默认值（请手动 net stop MySQL80 / 关闭窗口）。
    )
) else (
    echo   [跳过] 未找到 mysqladmin.exe，请手动关闭 MySQL 窗口。
)

echo.
echo 全部完成。重新启动请双击 scripts\start-dev-services.bat
endlocal
