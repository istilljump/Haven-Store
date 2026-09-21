# 提交记录#004（Prompt#INIT003）

对应提交哈希：`47f648f`（feat: 工具类与基础配置类 - Prompt#INIT003）

## 本轮目标

按项目规划第二阶段步骤3，补全常用工具类与基础配置类中缺失的部分。

## 用户原始Prompt摘要

1. utils 包：JwtUtil（生成/解析/校验 Token）、RedisUtil（常用 Redis 操作）、Md5Util（密码加密）
2. config 包：MyBatis-Plus 分页插件、Redis 配置、Knife4j 文档配置、WebMvc 配置（跨域、拦截器）
3. 拦截器：Token 登录拦截器，拦截需登录接口，从请求头解析 userId

## 现状核对（与 #001 的重叠部分）

- JwtUtil、RedisUtil、MybatisPlusConfig（分页）、RedisConfig、Knife4jConfig、JwtInterceptor 均已在 #001 完成并经冒烟测试，本轮未改动其主体逻辑
- 本轮实际增量：Md5Util 新增、WebMvcConfig 跨域配置新增、JwtInterceptor 预检放行修复

## 关键设计决策：Md5Util 不用于密码

规划文档原计划「Md5Util：密码加密」，但项目在 #001 已采用 BCrypt（spring-security-crypto，
自带随机盐、抗彩虹表）。MD5 属快速哈希，用于密码存储是公认安全反模式，且比赛评审标准明确考察
「无严重安全漏洞」。处置方式：

- 密码体系维持 BCrypt（PasswordUtil）不变，不引入第二条弱加密路径
- Md5Util 按通用摘要工具落地：适用于缓存 Key 压缩（后续 估价模块缓存入参摘要）、
  文件/字符串摘要比对等非安全场景
- 类注释中写入安全红线：「禁止用于密码加密与安全凭证存储」

## 本轮 Bug 修复实录：CORS 预检请求被鉴权拦截器误杀（过程证据）

1. **现象**：跨域配置上线后 curl 复测，OPTIONS 预检请求返回统一 401 JSON（`未登录或 Token 已失效`），
   响应缺少全部 Access-Control-* 头；普通跨域请求头正常
2. **定位**：浏览器预检请求不携带 Authorization 头；Spring MVC 中预检请求虽由 CORS 处理器
   （PreFlightHandler）应答，但拦截器链同样会先执行，JwtInterceptor 因无 Token 直接返回 401，
   预检响应被覆盖 → 浏览器跨域直接失败
3. **修复**：JwtInterceptor.preHandle 首步增加 `CorsUtils.isPreFlightRequest(request)` 判断，
   预检请求直接放行
4. **复测**：预检请求返回 200 + 完整 CORS 头（Allow-Origin/Methods/Headers/Credentials/Max-Age）；
   普通（非预检）请求鉴权不受任何影响

## 变更文件（5 个）

- com.example.utils.Md5Util（新增）：md5Hex 32 位小写摘要 + 安全红线注释
- com.example.config.WebMvcConfig：新增 addCorsMappings 全局跨域（来源由 cors.origin-patterns
  配置管理，allowCredentials 配合 allowedOriginPatterns，预检缓存 1 小时）
- com.example.config.JwtInterceptor：新增跨域预检请求放行（Bug 修复，见上节）
- application.yml：新增 cors.origin-patterns 配置项（开发 *，生产收敛为具体域名）
- backend/README.md：工程结构树同步（Md5Util、WebMvcConfig 描述）

## 测试情况

1. `mvn compile`：BUILD SUCCESS
2. 跨域三连测（应用本地拉起）：
   - OPTIONS 预检（模拟前端 5173 端口 + authorization 头）：200 + Access-Control-Allow-Origin:
     http://localhost:5173 + Allow-Methods/Headers/Credentials/Max-Age 全部正确
   - 普通跨域 GET（带 Origin）：CORS 头 + 统一 401（鉴权正常）
   - 本机不带 Origin 请求：统一 401（鉴权正常）
3. 全链路回归（UTF-8 请求体）：注册 → 登录 → 带 Token 跨域访问 /user/info：
   200 + CORS 头 + 用户信息（中文昵称无乱码、password 脱敏为 null）
4. 测试插曲（已定位为测试工具问题，非应用 Bug）：Git Bash curl 以 GBK 发送中文 JSON，
   服务端报 Invalid UTF-8 start byte 0xbf → 改用 Python 按 UTF-8 写请求体文件后正常
5. 测试数据已清理（user 表 0 行、Redis 用户缓存清空），测试用应用实例已停止、8080 端口已释放

## 留痕说明

- 本记录为 过程记录第 004 篇，衔接 #003（7f7aa1e，Prompt#INIT002）
- 本轮「CORS 预检被拦截」Bug 的发现-定位-修复-复测全过程可作为演示视频中的
  「协同过程/Bug 修复引导」素材
