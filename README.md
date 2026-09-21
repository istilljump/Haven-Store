# 二手商品交易市场

基于Spring Boot + Vue.js的全栈二手交易平台，支持商品发布、搜索、智能估价、即时聊天、系统消息等功能。

## 🚀 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7+
- Docker & Docker Compose (可选)

### 方式一：Docker部署（推荐）

1. **克隆项目**
```bash
git clone <repository-url>
cd secondhand-market
```

2. **构建并启动服务**
```bash
# Linux/Mac
chmod +x deploy.sh
./deploy.sh start

# Windows
.\deploy.ps1 start
```

3. **访问应用**
- 前端：http://localhost
- 后端API：http://localhost:8080
- 管理后台：http://localhost/admin

### 方式二：本地部署

#### 1. 后端部署

```bash
cd backend
mvn clean package
java -jar target/secondhand-market-*.jar
```

#### 2. 前端部署

```bash
cd frontend
npm install
npm run serve
```

### 方式三：开发模式

#### 后端开发

```bash
cd backend
mvn spring-boot:run
```

#### 前端开发

```bash
cd frontend
npm run dev
```

## 🔧 管理功能

### 管理员后台

访问 `/admin` 进入管理后台，支持以下功能：

- **数据概览**：实时查看平台数据统计
- **用户管理**：用户列表、状态管理、权限控制
- **商品管理**：商品审核、下架、删除
- **分类管理**：分类管理、排序设置
- **系统消息**：发送系统公告
- **系统设置**：站点配置、交易规则、安全设置

### API管理

```bash
# 查看API文档
http://localhost:8080/swagger-ui/index.html

# 健康检查
curl http://localhost:8080/api/health
```

## 📊 压力测试

使用内置的压力测试脚本：

```bash
# 基本测试（默认10线程，100请求）
node stress_test.js

# 自定义参数测试
node stress_test.js -t 20 -r 100 -u http://localhost:8080/api/health

# 测试API登录接口
node stress_test.js -t 5 -r 50 -u http://localhost:8080/api/auth/login -m POST -p '{"username":"admin","password":"123456"}'
```

## 🔧 部署脚本使用

### Linux/Mac

```bash
./deploy.sh [command]
```

命令说明：
- `build` - 构建Docker镜像
- `start` - 启动服务
- `stop` - 停止服务
- `restart` - 重启服务
- `status` - 查看服务状态
- `logs` - 查看服务日志
- `clean` - 清理Docker资源
- `health` - 健康检查
- `migrate` - 数据库迁移
- `backup` - 数据库备份

### Windows

```powershell
.\deploy.ps1 [command]
```

命令说明同上。

## 📁 项目结构

```
secondhand-market/
├── backend/                 # Spring Boot后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/market/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                # Vue.js前端
│   ├── src/
│   │   ├── components/
│   │   ├── views/
│   │   ├── router/
│   │   ├── store/
│   │   └── utils/
│   ├── public/
│   └── package.json
├── docker-compose.yml       # Docker Compose配置
├── deploy.sh               # Linux/Mac部署脚本
├── deploy.ps1              # Windows部署脚本
└── stress_test.js          # 压力测试脚本
```

## 🛠️ 配置说明

### 数据库配置

在 `backend/src/main/resources/application.yml` 中修改：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/secondhand_market
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Redis配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    password: # 如果设置了密码
```

### JWT配置

```yaml
jwt:
  secret: your-secret-key
  expiration: 86400000 # 24小时
```

## 🚨 注意事项

1. **安全配置**
   - 生产环境请修改默认密码
   - 配置HTTPS证书
   - 设置强密码策略

2. **性能优化**
   - 启用Gzip压缩
   - 配置CDN加速
   - 使用数据库连接池

3. **数据备份**
   - 定期备份数据库
   - 重要数据做多副本

4. **监控**
   - 设置服务监控告警
   - 配置日志收集
   - 性能监控

## 📈 性能监控

### 应用监控

- Prometheus + Grafana
- Spring Boot Actuator
- 自定义指标

### 数据库监控

- MySQL慢查询日志
- Redis监控
- 连接池监控

## 🤝 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交代码
4. 发起Pull Request

## 📄 许可证

MIT License

## 🆘 常见问题

### 1. 端口冲突
- 修改 `docker-compose.yml` 中的端口映射
- 检查端口是否被占用

### 2. 数据库连接失败
- 检查MySQL服务是否启动
- 验证连接参数是否正确

### 3. Redis连接失败
- 确保Redis服务运行
- 检查密码配置

### 4. 内存不足
- 调整JVM参数
- 增加服务器内存

### 5. 502错误
- 检查后端服务是否正常
- 查看Nginx错误日志

## 📞 技术支持

如有问题，请通过以下方式联系：

- 邮箱：admin@secondhand.com
- 电话：400-123-4567
- 文档：[项目文档](docs/)

---

**祝你使用愉快！** 🎉