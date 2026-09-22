# 提交记录#001

Z快照ID：zcode-logs-20260921-164246-mGjuAt

## 本轮目标

搭建二手商品交易平台后端工程基础架构，并完成用户模块（注册登录与 JWT 鉴权）的完整开发。

## 核心Prompt摘要

- 技术栈约束：SpringBoot 2.7.12 + MyBatis-Plus 3.5.3.1 + MySQL 8.0 + Redis + Vue3 + Element Plus，Maven 构建，严格遵循阿里巴巴 Java 开发规范
- 架构分层：controller / service / serviceImpl / mapper / entity / vo / dto / common / utils；统一返回 Result<T>（code/msg/data）；全局异常处理器
- 工程基础：application.yml（端口 8080、上下文 /api、utf8mb4、Asia/Shanghai）、分页插件、字段自动填充、RedisTemplate 序列化配置、Knife4j 文档（标题「二手商品交易平台 API」）
- 工具类：JwtUtil（生成/解析/校验 Token）、PasswordUtil（BCrypt）、RedisUtil（set/get/delete/expire/hasKey）、UserHolder（ThreadLocal 用户上下文，请求结束清理）
- 用户模块：注册（用户名/手机号唯一、BCrypt 加密、默认头像）、登录（状态校验、密码比对、生成 Token、Redis 缓存用户信息）、获取当前用户（脱敏，优先读缓存）
- 强制规范：全中文注释、禁魔法值（枚举/常量类）、@Validated 参数校验、MyBatis-Plus API 杜绝 SQL 注入、密码不出现在日志与返回结果

## 变更文件

- backend/pom.xml（Boot 2.7.12 父工程、MP 3.5.3.1、mysql-connector-j、jjwt 0.11.5、knife4j 4.1.0、lombok 1.18.36）
- backend/sql/init.sql（second_hand_market 库 + user 表，唯一索引）
- backend/src/main/resources/application.yml
- backend/src/main/java/com/example/SecondHandMarketApplication.java
- com.example.common：Result、ResultCodeEnum、BusinessException、GlobalExceptionHandler、Constants、RedisKeyConst（6 个）
- com.example.utils：JwtUtil、PasswordUtil、RedisUtil、UserHolder（4 个）
- com.example.config：MybatisPlusConfig、MyMetaObjectHandler、RedisConfig、JacksonConfig、Knife4jConfig、JwtInterceptor、WebMvcConfig（7 个）
- com.example.user：entity/User、mapper/UserMapper、dto/RegisterDTO、dto/LoginDTO、vo/LoginUserVO、enums/UserStatusEnum、constant/UserConstant、service/UserService、serviceImpl/UserServiceImpl、controller/UserController（10 个）
- backend/README.md、backend/.gitignore
- 根 .gitignore、ai_commit_notes/001（本文件）

合计：后端 28 个 Java 源文件 + 2 个配置/文档 + SQL 脚本 + 提交笔记。

## 测试情况

1. Maven 编译验证：BUILD SUCCESS（28 个源文件，便携 Maven 3.9.16 + 阿里云镜像）
2. 数据库：init.sql 执行成功，user 表结构与实体一一对应
3. 冒烟测试 9 用例全部通过：
   - 未登录访问 → code 401；非法 Token → code 401
   - 正常注册 → 200（DB 密码为 BCrypt 密文 $2a$10$...）
   - 重复用户名注册 → 业务拦截；用户名过短 → 400 参数校验；两次密码不一致 → 业务拦截
   - 正确密码登录 → 200 + JWT Token；错误密码 → 统一提示"用户名或密码错误"（防账号枚举）
   - Token 获取当前用户 → 200，password 为 null（脱敏），时间格式 yyyy-MM-dd HH:mm:ss
4. Redis 验证：user:info:1 缓存写入成功，TTL 604778s（与 Token 7 天有效期同步）
5. 过程中修复：MySQL 驱动坐标迁移（mysql-connector-java → mysql-connector-j）、Lombok 升级 1.18.36（JDK 22 兼容）、响应头强制 charset=UTF-8（application/json;charset=UTF-8）
