# 数据库初始化指南

本文档描述如何把数据库初始化到「后端能正常跑」的状态。

## 唯一正确的建表脚本

**`backend/scripts/01-schema.sql`**（10 张表 + 种子数据）

> 历史遗留的 `backend/legacy/init.sql` 与 `backend/legacy/init_database.sql` **请勿使用**：
> 前者建的是 `message` 表（字段为 `from_user_id`/`to_user_id`），
> 而代码里的消息实体映射的是 `system_message` 表（`@TableName("system_message")`），
> 导入它会导致系统消息功能直接报「表不存在」；
> 后者缺少 `system_message`、`comment`、`user_product_relation` 三张表。
> 详见 `backend/legacy/README.md`。

脚本建的表：

| 表名 | 说明 |
| --- | --- |
| `user` | 用户（`status` 1 正常 / 0 禁用；`role` 1 管理员 / 0 普通用户） |
| `category` | 商品分类（`status` 1 启用 / 0 禁用） |
| `product` | 二手商品（`status` 1 在售 / 2 已售出 / 3 已下架） |
| `system_message` | 系统消息（`receiver_type` 标记群发接收范围） |
| `user_product_relation` | 用户与商品的关联（收藏等） |
| `comment` | 商品评论 |

## 初始化步骤

### 1. 执行脚本（脚本自带建库语句，无需手动建库）

```bash
mysql -uroot -p < backend/scripts/01-schema.sql
```

脚本会创建数据库 `secondhand_market`（**注意不是 `second_hand_market`**，
名称必须与 `backend/src/main/resources/application.yml` 的 JDBC URL 一致），
建 10 张表并写入种子数据。

> 脚本开头是 `DROP TABLE IF EXISTS`，重复执行会**清空表内数据**，请勿在生产库执行。

### 2. 修改连接配置

`backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/secondhand_market
    username: root
    password: 你的密码
  redis:
    host: localhost
    port: 6379
```

### 3. 验证

```sql
USE secondhand_market;
SHOW TABLES;                       -- 应为 6 张
SELECT id, username, role, status FROM user;   -- 应为 3 条种子账号
SELECT id, name, sort, status FROM category;   -- 应为 8 条分类
```

种子账号（密码为 BCrypt 密文，可直接登录）：

| 用户名 | 密码 | 说明 |
| --- | --- | --- |
| `admin` | `admin123` | 管理员，可登录管理后台 `/admin` |
| `testuser1` | `123456` | 普通用户，含 2 条示例商品 |
| `testuser2` | `123456` | 普通用户 |

### 4. 冒烟测试

```bash
# 健康检查（无需登录）
curl http://localhost:8080/api/health

# 登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

期望登录返回：

```json
{"code":200,"msg":"成功","data":{"userId":1,"username":"admin","nickname":"管理员","token":"...","isAdmin":true}}
```

## 已有数据库如何增量升级

如果数据库里已有数据、不想重建，可按下面两条语句补齐本次新增的字段：

```sql
USE secondhand_market;

-- 分类启用/禁用状态
ALTER TABLE `category`
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用' AFTER `sort`;

-- 系统消息的群发接收范围
ALTER TABLE `system_message`
    ADD COLUMN `receiver_type` VARCHAR(10) NOT NULL DEFAULT 'user'
        COMMENT '接收群体：all所有用户 admin仅管理员 user仅普通用户' AFTER `message_type`;
```

## 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 接口返回 501「系统内部错误」 | 表未创建 | 执行 `backend/scripts/01-schema.sql` |
| 消息功能报表不存在 | 误导了 `legacy/init.sql` | 改用 `backend/scripts/01-schema.sql` |
| 登录接口报错、日志出现 Redis 连接异常 | Redis 未启动（登录会把用户信息写入 Redis 缓存） | 启动 Redis 或改 `application.yml` 中的 Redis 地址 |
| 中文乱码 | 连接串缺少字符集参数 | 保留 URL 中的 `characterEncoding=utf8` |
