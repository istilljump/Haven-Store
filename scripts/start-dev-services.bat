@echo off
title 启动依赖服务（MySQL + Redis）
setlocal enabledelayedexpansion

pushd "%~dp0.."
set "ROOT=%CD%"
popd

echo ==================================================
echo  启动本机依赖服务（MySQL + Redis）
echo ==================================================
echo.
echo [为什么需要这个脚本]
echo   本机 MySQL 服务（MySQL80）是「手动启动」类型，开机会自启不了；
echo   而当前账号不在管理员组，用 net start 会被拒绝访问。
echo   所以这里改用免管理员的方式启动：
echo     MySQL —— 用已安装的 mysqld.exe，数据目录放在 .tools\mysql-data
echo     Redis —— 用 .tools\redis 里的便携版
echo   两者都跑在独立窗口里，关掉对应窗口即停止该服务。
echo.

set "MYSQL_BASE=C:\Program Files\MySQL\MySQL Server 8.0"
set "MYSQLD=%MYSQL_BASE%\bin\mysqld.exe"
set "MYSQL=%MYSQL_BASE%\bin\mysql.exe"
set "MYSQL_DATA=%ROOT%\.tools\mysql-data"
set "REDIS_DIR=%ROOT%\.tools\redis"

if not exist "%MYSQLD%" (
    echo [错误] 没找到 %MYSQLD%
    echo        请确认已安装 MySQL 8.0，或修改本脚本开头的 MYSQL_BASE 路径。
    exit /b 1
)

rem ==================== MySQL ====================
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [跳过] 3306 端口已在监听，MySQL 无需重复启动。
    goto mysql_ready
)

if not exist "%MYSQL_DATA%" (
    echo [首次运行] 正在初始化 MySQL 数据目录，约需 10-30 秒，请稍候...
    mkdir "%MYSQL_DATA%"
    "%MYSQLD%" --no-defaults --initialize-insecure --basedir="%MYSQL_BASE%" --datadir="%MYSQL_DATA%" --console
    if errorlevel 1 (
        echo.
        echo [错误] MySQL 数据目录初始化失败，请把上面的报错内容发给技术人员。
        exit /b 1
    )
) else (
    if not exist "%MYSQL_DATA%\ibdata1" (
        echo [错误] 数据目录 %MYSQL_DATA% 不完整（上次初始化可能被中断）。
        echo        请删除该目录后重新运行本脚本。
        exit /b 1
    )
)

echo [启动] MySQL（新窗口，最小化运行，请勿关闭）...
start "Haven-Store MySQL" /min cmd /k ""%MYSQLD%" --no-defaults --console --basedir="%MYSQL_BASE%" --datadir="%MYSQL_DATA%" --port=3306 --bind-address=127.0.0.1 --mysqlx=OFF"

echo [等待] 正在等待 MySQL 就绪...
set /a WAIT=0
:wait_mysql
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if not errorlevel 1 goto mysql_up
set /a WAIT+=1
if !WAIT! GEQ 45 (
    echo [错误] 等待 MySQL 启动超时，请查看「Haven-Store MySQL」窗口里的报错内容。
    exit /b 1
)
rem 等 1 秒：这里不用 timeout，它在输入被重定向时会直接报错退出
ping -n 2 127.0.0.1 >nul
goto wait_mysql

:mysql_up
rem 首次初始化出来的是空密码 root，这里统一改成 application.yml 里配置的 1234
rem 若不是首次运行，这条会因密码不符而失败，属正常情况，忽略即可
"%MYSQL%" -uroot --protocol=TCP -h127.0.0.1 -P3306 -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '1234';" >nul 2>&1

:mysql_ready
rem 库不存在就导入建表脚本（含默认账号与示例商品）
"%MYSQL%" -uroot -p1234 --protocol=TCP -h127.0.0.1 -P3306 -N -e "SHOW DATABASES LIKE 'secondhand_market';" 2>nul | findstr "secondhand_market" >nul
if errorlevel 1 (
    echo [初始化] 正在建库并导入表结构与默认数据...
    "%MYSQL%" -uroot -p1234 --protocol=TCP -h127.0.0.1 -P3306 --default-character-set=utf8mb4 < "%ROOT%\backend\scripts\01-schema.sql"
    if errorlevel 1 (
        echo.
        echo [错误] 导入建表脚本失败。
        echo        若 3306 上跑的是你自己安装的 MySQL，请确认它的 root 密码是 1234
        echo        （与 backend\src\main\resources\application.yml 保持一致）。
        exit /b 1
    )
    echo [完成] 数据库 secondhand_market 已创建，含默认账号与 2 条示例商品。
) else (
    echo [跳过] 数据库 secondhand_market 已存在。
)
echo [完成] MySQL 就绪：127.0.0.1:3306  root / 1234

rem ==================== Redis ====================
echo.
netstat -ano | findstr ":6379" | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [跳过] 6379 端口已在监听，Redis 无需重复启动。
    goto redis_ready
)

if not exist "%REDIS_DIR%\redis-server.exe" (
    echo [警告] 没找到 %REDIS_DIR%\redis-server.exe，跳过 Redis。
    echo        项目在 Redis 不可用时会自动降级，登录等核心功能仍可正常使用。
    goto redis_ready
)

echo [启动] Redis（新窗口，最小化运行，请勿关闭）...
start "Haven-Store Redis" /min cmd /k "cd /d "%REDIS_DIR%" && redis-server.exe redis.windows.conf"

set /a WAIT=0
:wait_redis
netstat -ano | findstr ":6379" | findstr "LISTENING" >nul
if not errorlevel 1 goto redis_ready
set /a WAIT+=1
if !WAIT! GEQ 20 (
    echo [警告] 等待 Redis 启动超时，项目会自动降级运行（缓存功能不可用）。
    goto redis_ready
)
rem 等 1 秒：这里不用 timeout，它在输入被重定向时会直接报错退出
ping -n 2 127.0.0.1 >nul
goto wait_redis

:redis_ready
echo [完成] Redis 就绪：127.0.0.1:6379
echo.
echo 依赖服务已全部就绪。
exit /b 0
