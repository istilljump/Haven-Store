# 二手商品交易市场

基于Spring Boot + Vue.js的全栈二手交易平台，支持商品发布、附近商品搜索、AI智能估价、系统消息等功能。

## 🚀 快速开始

### 环境要求

- JDK 8+（推荐 17；Docker 镜像基于 JDK 17）
- Node.js 18+
- MySQL 8.0+
- Redis 7+
- Docker & Docker Compose (可选)

### 方式一：Docker部署（推荐）

1. **克隆项目**
```bash
git clone https://github.com/istilljump/Haven-Store.git
cd Haven-Store
```

2. **构建并启动服务**
```bash
# Linux/Mac
chmod +x scripts/deploy.sh
./scripts/deploy.sh start

# Windows
.\scripts\deploy.ps1 start
```

3. **访问应用**
- 前端：http://localhost
- 后端API：http://localhost:8080
- 管理后台：http://localhost/admin

4. **默认账号**（由 `backend/scripts/01-schema.sql` 初始化）

| 用户名 | 密码 | 说明 |
|---|---|---|
| `admin` | `admin123` | 管理员 |
| `testuser1` | `123456` | 普通用户（含 2 条示例商品） |
| `testuser2` | `123456` | 普通用户 |

### 方式二：本地部署

#### 前置：初始化数据库

先启动 MySQL，然后执行建表脚本：

```bash
mysql -uroot -p < backend/scripts/01-schema.sql
```

> 脚本会创建数据库 `secondhand_market`（6 张表 + 种子数据），并写入默认账号：
> `admin/admin123`（管理员）、`testuser1/123456`、`testuser2/123456`。
> 库名必须与 `application.yml` 中的 JDBC URL 一致。

#### 1. 后端部署

```bash
cd backend
mvn clean package -DskipTests
java -jar target/second-hand-market-*.jar
```
> 产物名以 `pom.xml` 的 `artifactId` 为准（当前为 `second-hand-market-1.0.0.jar`，注意带连字符）。
> 单元测试需要本机已启动 MySQL 与 Redis 并完成建表，否则请加 `-DskipTests` 跳过测试。
> 后端默认连接 `localhost:3306/secondhand_market` 与 `localhost:6379`，
> 账号密码在 `backend/src/main/resources/application.yml` 中按本地环境修改。
> 启动后接口地址带 `/api` 前缀，例如 `http://localhost:8080/api/user/login`。

#### 2. 前端部署

```bash
cd frontend
npm install
npm run dev      # 开发模式，访问 http://localhost:3000
npm run build    # 生产构建，产物在 dist/
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
# 查看API文档（Knife4j，基于 Swagger2）
http://localhost:8080/api/doc.html

# 健康检查
curl http://localhost:8080/api/health
```

## 📊 压力测试

使用内置的压力测试脚本（位于 `tests/`）：

```bash
# 基本测试（默认10线程，100请求）
node tests/stress_test.js

# 自定义参数测试
node tests/stress_test.js -t 20 -r 100 -u http://localhost:8080/api/health

# 测试登录接口
node tests/stress_test.js -t 5 -r 50 -u http://localhost:8080/api/user/login -m POST -p '{"username":"testuser1","password":"123456"}'
```

## 🔧 部署脚本使用

### Linux/Mac

```bash
./scripts/deploy.sh [command]
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
.\scripts\deploy.ps1 [command]
```

命令说明同上。

## 📁 项目结构

```
Haven-Store/
├── backend/                 # Spring Boot后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   └── resources/
│   │   └── test/
│   ├── scripts/01-schema.sql # MySQL 建表 + 种子数据
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                # Vue.js前端
│   ├── src/
│   ├── package.json
│   ├── nginx.conf
│   └── Dockerfile
├── docs/                    # 开发过程记录
├── reports/                 # 测试报告与项目文档
├── scripts/                 # 部署与运维脚本
│   ├── deploy.sh            # Linux/Mac 部署脚本
│   ├── deploy.ps1           # Windows 部署脚本
│   ├── demo.sh
│   ├── check_git_status.bat
│   └── verify_github_upload.bat
├── tests/                   # 接口测试与压测脚本
│   ├── api_test.py
│   ├── frontend_api_test.js
│   ├── frontend_tester.js
│   ├── stress_test.js
│   ├── test_db_connection.py
│   └── mock_server.py
├── docker-compose.yml       # Docker Compose配置
├── nginx.conf               # 反向代理配置（可选）
├── README.md
├── CONTRIBUTING.md
└── LICENSE
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

- 邮箱：1593504559@qq.com
- 文档：[项目文档](docs/)

---

**祝你使用愉快！** 🎉