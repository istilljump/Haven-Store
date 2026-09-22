# legacy —— 已废弃文件（请勿使用）

本目录存放项目早期版本遗留、**当前代码已不再使用**的文件。
保留它们只是为了留档，避免有人从旧文档或旧习惯里翻出来误用。

**当前唯一有效的建表脚本是 `backend/scripts/01-schema.sql`。**

---

## 1. `init.sql` —— 危险，schema 与代码不一致

早期版本的建表脚本，建库名为 `second_hand_market`（带下划线），共 6 张表。

**问题**：它建的是 `message` 表：

```sql
CREATE TABLE IF NOT EXISTS message (
    from_user_id BIGINT NOT NULL,
    to_user_id   BIGINT NOT NULL,
    ...
);
```

而代码里 `com.example.message.entity.Message` 的映射是：

```java
@TableName("system_message")
public class Message { private Long receiverId; ... }
```

两者**表名不同、字段完全不同，没有任何实体映射 `message` 表**。
用这份脚本初始化数据库后，系统消息功能一执行就会因「表不存在」失败。

此外库名 `second_hand_market` 与 `application.yml` 里的
`jdbc:mysql://localhost:3306/secondhand_market` 也不一致，服务连不上。

**替代方案**：`backend/scripts/01-schema.sql`。

---

## 2. `init_database.sql` —— 缺表

另一份早期的建表脚本，只建 4 张表
（`user`、`category`、`product`、`system_message`），
缺少 `comment` 与 `user_product_relation`，与 `01-schema.sql` 不一致。

**替代方案**：`backend/scripts/01-schema.sql`。

---

## 3. `application-dev.properties` —— 从未生效的死配置

一个"H2 内存库开发环境"配置，存在三个问题使其完全无效：

1. **位置错误**：放在 `backend/` 根目录，而不是 `src/main/resources/`。
   Spring Boot 只加载 classpath 下的 `application*.properties`，这份文件从未被读取。
2. **引用了不存在的脚本**：
   `INIT=RUNSCRIPT FROM 'classpath:sql/init.sql'`，
   但 `src/main/resources/` 下并没有 `sql/` 目录。
3. **缺少依赖**：`pom.xml` 中根本没有 H2 驱动，即使位置正确也无法启动。

**当前做法**：开发环境直接连本地 MySQL 与 Redis，
配置统一写在 `src/main/resources/application.yml`。
如需 H2 方案，需要补 H2 依赖并把配置挪到 `src/main/resources/`。
