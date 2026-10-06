#!/usr/bin/env bash
# 目录重构回滚脚本（2026-10-06 重构迁移配套生成）
#
# 用法：在仓库根目录执行  bash scripts/undo-restructure.sh
# 前提：重构改动尚未提交（提交前回滚）。若已提交，请直接对相应提交执行 git revert。
# 效果：还原全部 8 项文件移动与 8 个文件的引用修改；幂等，可重复执行。
#
# ⚠️ 步骤 2 会对下列文件执行 git checkout --（丢弃工作区改动）：
#    README.md scripts/deploy.sh scripts/deploy.ps1 tests/stress_test.js
#    backend/README.md backend/legacy/README.md
#    请确认这些文件中没有你想保留的未提交修改（重构前它们均为干净状态）。
set -e
cd "$(dirname "$0")/.."   # 仓库根目录

echo "== 1/4 反向移动已跟踪文件（git mv 回原位）=="
if [ -f docs/database_setup.md ] && [ ! -f backend/database_setup.md ]; then
    git mv docs/database_setup.md backend/database_setup.md && echo "  docs/database_setup.md -> backend/"
fi
if [ -f config/nginx.conf ] && [ ! -f nginx.conf ]; then
    git mv config/nginx.conf nginx.conf && echo "  config/nginx.conf -> ./"
fi
if [ -f config/docker-compose.yml ] && [ ! -f docker-compose.yml ]; then
    git mv config/docker-compose.yml docker-compose.yml && echo "  config/docker-compose.yml -> ./"
fi
if [ -f backend/sql/01-schema.sql ] && [ ! -f backend/scripts/01-schema.sql ]; then
    mkdir -p backend/scripts
    git mv backend/sql/01-schema.sql backend/scripts/01-schema.sql && echo "  backend/sql/01-schema.sql -> backend/scripts/"
fi
if [ -f backend/sql/02-upgrade.sql ] && [ ! -f backend/scripts/02-upgrade.sql ]; then
    mkdir -p backend/scripts
    git mv backend/sql/02-upgrade.sql backend/scripts/02-upgrade.sql && echo "  backend/sql/02-upgrade.sql -> backend/scripts/"
fi
rmdir backend/sql 2>/dev/null || true
rmdir config    2>/dev/null || true   # 目录非空时保留，避免误删

echo "== 2/4 还原被编辑的已跟踪文件内容 =="
git checkout -- README.md scripts/deploy.sh scripts/deploy.ps1 tests/stress_test.js \
                backend/README.md backend/legacy/README.md \
                backend/database_setup.md docker-compose.yml nginx.conf 2>/dev/null || true
echo "  已还原（若某文件已不在原位则跳过）"

echo "== 3/4 移回未跟踪的压测报告 =="
for f in reports/stress/stress_test_report_*.json; do
    [ -e "$f" ] || continue
    base="$(basename "$f")"
    if [ ! -f "$base" ]; then
        mv "$f" "$base" && echo "  reports/stress/$base -> ./"
    fi
done
rmdir reports/stress 2>/dev/null || true

echo "== 4/4 清理迁移辅助文件 =="
rm -f scripts/undo-restructure.sh
rm -f "docs/目录重构迁移清单.md"
echo "回滚完成。"
