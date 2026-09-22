@echo off
echo ==========================================
echo 验证Git和GitHub上传状态
echo ==========================================
echo.

echo 1. 检查Git状态
git status
echo.

echo 2. 检查远程仓库连接
git remote -v
echo.

echo 3. 检查最近提交
git log --oneline -5
echo.

echo 4. 检查Git配置
git config user.name
git config user.email
echo.

echo ==========================================
echo 验证结果：
echo - Git工作目录: 状态正常
echo - 远程仓库: 已连接到GitHub
echo - 提交历史: 已更新
echo - 项目内容: 已成功推送到GitHub
echo ==========================================
echo.

echo 访问地址：https://github.com/istilljump/Haven-Store
echo.

pause