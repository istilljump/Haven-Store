@echo off
echo ==========================================
echo 检查Git状态和仓库信息
echo ==========================================
echo.

echo 1. 检查当前工作目录状态
git status
echo.

echo 2. 检查Git配置信息
git config --list
echo.

echo 3. 检查远程仓库信息
git remote -v
echo.

echo 4. 检查提交历史
git log --oneline -10
echo.

echo 5. 检查分支状态
git branch -a
echo.

echo ==========================================
echo 检查完成！
echo ==========================================
pause