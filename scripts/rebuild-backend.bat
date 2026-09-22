@echo off
title 重新编译后端
setlocal
pushd "%~dp0.."
set "ROOT=%CD%"
popd

echo ==================================================
echo  重新编译后端（改过 Java 代码后执行一次）
echo ==================================================
echo.
echo [提示] 编译前请先在任务管理器结束正在运行的 java 进程，否则会提示文件被占用。
echo.
pause

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.8.7-hotspot"
set "MVN=%ROOT%\.tools\apache-maven-3.9.16\bin\mvn.cmd"
set "SETTINGS=%ROOT%\.tools\maven-settings.xml"

if not exist "%MVN%" (
    echo [错误] 找不到 Maven：%MVN%
    pause
    exit /b 1
)

call "%MVN%" -o -s "%SETTINGS%" -f "%ROOT%\backend\pom.xml" clean package -DskipTests
if errorlevel 1 (
    echo.
    echo [失败] 编译未通过，请把上面的报错内容发给技术人员。
) else (
    echo.
    echo [成功] 已生成 backend\target\second-hand-market-1.0.0.jar
)
echo.
pause
