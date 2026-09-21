# 线下融合型二手商品交易平台（后端）

## 技术栈

| 分类 | 技术 | 版本 |
| ---- | ---- | ---- |
| 基础框架 | Spring Boot | 2.7.12 |
| 持久层 | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 单机版 |
| 鉴权 | JJWT | 0.11.5 |
| 接口文档 | Knife4j（Swagger2） | 4.1.0 |
| 构建工具 | Maven | 3.6+ |

## 快速启动

1. 初始化数据库：执行 `sql/init.sql` 创建 `second_hand_market` 数据库与 `user`、`category`、`product` 三张表（含基础分类种子数据，脚本可重复执行，但会清空表内数据）；
2. 修改 `src/main/resources/application.yml` 中的 MySQL 账号密码、Redis 地址为本机环境；
3. 启动应用：运行 `SecondHandMarketApplication` 主类，或执行 `mvn spring-boot:run`；
4. 打开接口文档：<http://localhost:8080/api/doc.html>。

## 工程结构

```text
backend
├── pom.xml                                    # Maven 依赖与构建配置
├── sql/init.sql                               # 数据库初始化脚本
└── src/main
    ├── resources/application.yml              # 核心配置文件
    └── java/com/example
        ├── SecondHandMarketApplication.java   # 启动类
        ├── common                             # 通用层：统一返回、异常体系、常量
        │   ├── Result.java                    # 统一返回结果 {code, msg, data}
        │   ├── ResultCodeEnum.java            # 响应状态码枚举
        │   ├── BusinessException.java         # 自定义业务异常
        │   ├── GlobalExceptionHandler.java    # 全局异常处理器
        │   ├── Constants.java                 # 系统通用常量
        │   └── RedisKeyConst.java             # Redis Key 常量
        ├── utils                              # 工具层
        │   ├── JwtUtil.java                   # JWT 生成/解析/校验
        │   ├── Md5Util.java                   # MD5 摘要（缓存Key/签名，禁用于密码）
        │   ├── PasswordUtil.java              # BCrypt 密码加密与比对
        │   ├── RedisUtil.java                 # Redis 常用操作封装
        │   └── UserHolder.java                # ThreadLocal 用户上下文
        ├── config                             # 配置层
        │   ├── MybatisPlusConfig.java         # 分页插件配置
        │   ├── MyMetaObjectHandler.java       # 时间字段自动填充
        │   ├── RedisConfig.java               # RedisTemplate 序列化配置
        │   ├── JacksonConfig.java             # 全局时间序列化格式
        │   ├── Knife4jConfig.java             # 接口文档配置
        │   ├── JwtInterceptor.java            # JWT 登录鉴权拦截器
        │   └── WebMvcConfig.java              # 全局跨域、拦截器注册与白名单
        └── user                               # 用户模块
            ├── controller/UserController.java # 控制器：注册/登录/当前用户
            ├── service/UserService.java       # 业务接口
            ├── serviceImpl/UserServiceImpl.java # 业务实现
            ├── mapper/UserMapper.java         # 数据访问层
            ├── entity/User.java               # 用户实体
            ├── dto                            # 入参：RegisterDTO / LoginDTO
            ├── vo/LoginUserVO.java            # 出参：登录返回信息
            ├── enums/UserStatusEnum.java      # 账号状态枚举
            └── constant/UserConstant.java     # 模块常量
```

## 鉴权说明

- 登录成功后服务端返回 JWT Token（有效期 7 天）；
- 后续请求在请求头携带 `Authorization: Bearer {token}`；
- 拦截器校验通过后将用户 ID 写入 `UserHolder`（ThreadLocal），请求结束自动清理；
- 白名单：`/user/login`、`/user/register`、Knife4j 文档相关资源。

## 开发约定

- 所有类、方法、字段必须带中文注释；禁止魔法值，常量统一收敛到常量类/枚举；
- 数据库操作全部使用 MyBatis-Plus API（预编译参数化），禁止手写拼接 SQL；
- 密码 BCrypt 加密存储，明文不出现在日志与返回结果中；
- 统一返回 `Result<T>` 结构，异常由全局异常处理器统一转换。
