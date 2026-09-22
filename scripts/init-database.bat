@echo off
title 初始化数据库
setlocal
pushd "%~dp0.."
set "ROOT=%CD%"
popd

echo ==================================================
echo  初始化数据库（只在第一次、或想重置数据时执行）
echo ==================================================
echo.
echo [注意] 这一步会清空并重建所有表，原有数据会丢失！
echo.
pause

set "MYSQL="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL (
    echo [错误] 没找到 mysql.exe，请确认已安装 MySQL 8.0。
    echo        默认路径：C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
    pause
    exit /b 1
)

echo 请输入 MySQL 的 root 密码（本机为 1234，输入时屏幕不显示任何字符）：
"%MYSQL%" -uroot -p --default-character-set=utf8mb4 < "%ROOT%\backend\scripts\01-schema.sql"

if errorlevel 1 (
    echo.
    echo [失败] 初始化未成功，请检查密码是否正确、MySQL 服务是否已启动。
) else (
    echo.
    echo [成功] 数据库 secondhand_market 已就绪（6 张表 + 默认账号）。
)
echo.
pause
