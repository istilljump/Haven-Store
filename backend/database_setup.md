# 数据库初始化指南

## 问题描述
目前后端服务运行正常，但数据库表未初始化，导致API调用返回501系统内部错误。

## 解决步骤

### 1. 创建数据库
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS second_hand_market CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

### 2. 导入初始化脚本
```bash
mysql -u root -p second_hand_market < sql/init.sql
```

### 3. 验证表结构
```sql
USE second_hand_market;
SHOW TABLES;
DESCRIBE user;
DESCRIBE category;
DESCRIBE product;
```

### 4. 检查数据
```sql
SELECT * FROM category;
SELECT * FROM user;
```

## 如果没有MySQL客户端

### 方式一：使用Navicat等GUI工具
1. 连接到localhost:3306
2. 执行`sql/init.sql`脚本

### 方式二：使用Docker容器
```bash
# 拉取MySQL镜像
docker pull mysql:8.0

# 运行MySQL容器
docker run --name mysql-container -e MYSQL_ROOT_PASSWORD=1234 -p 3306:3306 -d mysql:8.0

# 复制SQL文件到容器
docker cp sql/init.sql mysql-container:/init.sql

# 执行初始化
docker exec mysql-container mysql -u root -p1234 -e "CREATE DATABASE second_hand_market CHARACTER SET utf8mb4;"
docker exec mysql-container mysql -u root -p1234 second_hand_market < init.sql
```

### 方式三：使用在线SQL工具
1. 访问 https://www.db-fiddle.com/
2. 粘贴`sql/init.sql`内容
3. 执行脚本

## 验证初始化成功
执行以下API测试，应该能够正常工作：

```bash
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "confirmPassword": "123456",
    "phone": "13800138000",
    "nickname": "测试用户"
  }'
```

期望返回：
```json
{"code":200,"msg":"注册成功","data":null}
```