#!/bin/bash

# Haven-Store 项目演示脚本
# 使用说明：bash demo.sh 或 ./demo.sh

echo "🚀 Haven-Store 项目演示"
echo "================================"

# 检查后端服务是否运行
echo "📡 检查后端服务状态..."
if curl -s http://localhost:8080/api/health > /dev/null; then
    echo "✅ 后端服务运行正常 (http://localhost:8080)"
else
    echo "❌ 后端服务未启动"
    echo "请先启动后端: cd backend && mvn spring-boot:run"
    echo "（需要 MySQL 与 Redis 已启动）"
    exit 1
fi

# 检查前端服务是否运行
echo "🌐 检查前端服务状态..."
if curl -s http://localhost:3000 > /dev/null; then
    echo "✅ 前端服务运行正常 (http://localhost:3000)"
else
    echo "❌ 前端服务未启动"
    echo "请先启动前端: cd frontend && npm run dev"
    exit 1
fi

echo ""
echo "📱 功能演示流程："
echo "1. 🏠 访问首页: http://localhost:3000"
echo "2. 👤 注册用户: http://localhost:3000/register"
echo "3. 🔐 登录系统: http://localhost:3000/login"
echo "4. 📦 发布商品: http://localhost:3000/publish"
echo "5. 🔍 查询商品: http://localhost:3000/products"
echo "6. 📍 附近搜索: http://localhost:3000/map"
echo "7. 💰 AI估价: 发布商品时自动估算"

echo ""
echo "📚 API测试："
echo "健康检查: curl http://localhost:8080/api/health"
echo "用户注册: curl -X POST http://localhost:8080/api/user/register"
echo "用户登录: curl -X POST http://localhost:8080/api/user/login"
echo "商品发布: curl -X POST http://localhost:8080/api/product/add"

echo ""
echo "📖 项目文档："
echo "- 完整文档: 项目完成报告.md"
echo "- API文档: http://localhost:8080/api/swagger"
echo "- 测试报告: frontend_api_test_report.html"

echo ""
echo "⚡ 快速开始："
echo "1. 打开浏览器访问: http://localhost:3000"
echo "2. 按照演示流程体验各项功能"
echo "3. 查看项目文档了解详细配置"

echo ""
echo "🎉 项目演示准备完成！"