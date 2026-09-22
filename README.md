# 电商管理系统

这是一个基于Spring Boot + Vue.js的电商管理系统，包含商品管理、用户管理、订单管理等功能。

## 项目结构

```
organized-project/
├── backend/                 # 后端项目 (Spring Boot)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/       # Java源代码
│   │   │   ├── resources/ # 配置文件
│   │   │   └── resources/mapper/ # MyBatis映射文件
│   │   └── test/           # 测试代码
│   ├── scripts/            # 数据库脚本
│   └── sql/               # SQL脚本
├── frontend/               # 前端项目 (Vue.js)
│   ├── src/
│   │   ├── components/    # 组件
│   │   ├── views/         # 页面
│   │   ├── router/        # 路由配置
│   │   ├── store/         # 状态管理
│   │   ├── utils/         # 工具函数
│   │   └── api/           # API接口
│   └── dist/              # 构建输出
├── docs/                   # 项目文档
├── reports/                # 报告和说明文档
├── scripts/               # 脚本文件
│   ├── *.sh               # Shell脚本
│   ├── *.bat              # 批处理脚本
│   ├── *.ps1              # PowerShell脚本
│   └── *.py               # Python脚本
├── tests/                 # 测试相关文件
├── docker-compose.yml      # Docker Compose配置
└── nginx.conf             # Nginx配置
```

## 功能特性

### 后端功能
- 用户管理：注册、登录、权限管理
- 商品管理：商品CRUD、分类管理
- 订单管理：订单处理、状态更新
- 支付集成：微信支付、支付宝
- 权限管理：管理员权限、用户权限

### 前端功能
- 用户界面：响应式设计、多主题
- 商品展示：商品列表、详情、搜索
- 购物车：添加、删除、修改数量
- 订单管理：下单、支付、查看订单
- 管理后台：商品管理、用户管理、数据统计

## 技术栈

### 后端技术
- Spring Boot 2.7.x
- MyBatis-Plus 3.5.x
- MySQL 8.0
- Redis 6.0
- JWT 0.11.x
- Maven 3.6+

### 前端技术
- Vue.js 3.x
- Element Plus 2.x
- Vue Router 4.x
- Pinia 2.x
- Axios 1.x
- Vite 4.x

### 部署技术
- Docker 20.10+
- Docker Compose 2.x
- Nginx 1.21+

## 快速开始

### 环境要求
- Java 8+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+
- Docker 20.10+

### 使用Docker启动

1. 克隆项目
```bash
git clone <repository-url>
cd organized-project
```

2. 启动所有服务
```bash
docker-compose up -d
```

3. 访问系统
- 前端：http://localhost:8080
- 后端API：http://localhost:8081
- 管理后台：http://localhost:8080/admin

### 手动启动

#### 后端启动
```bash
cd backend
mvn clean install
java -jar target/your-app.jar --spring.profiles.active=dev
```

#### 前端启动
```bash
cd frontend
npm install
npm run dev
```

## 数据库初始化

1. 创建数据库
```sql
CREATE DATABASE ecommerce_db;
```

2. 执行SQL脚本
```bash
mysql -u username -p ecommerce_db < backend/scripts/01-schema.sql
```

## 开发指南

### 代码规范
- 后端：遵循Spring Boot最佳实践
- 前端：遵循Vue.js官方规范
- 注释：中文注释，关键业务逻辑

### 提交规范
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式化
- refactor: 重构
- test: 测试相关
- chore: 构建或辅助工具变动

## 部署说明

### 生产环境部署
1. 修改数据库配置
2. 构建前端项目
3. 配置Nginx
4. 使用Docker部署

### 监控和日志
- 使用Spring Boot Actuator进行健康检查
- 日志使用Logback配置
- 前端错误监控使用Sentry

## 联系方式

如有问题，请联系开发团队或查看相关文档。

## 许可证

本项目采用MIT许可证。