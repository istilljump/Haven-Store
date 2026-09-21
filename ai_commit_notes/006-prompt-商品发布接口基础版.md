# 提交记录#006（Prompt#SP001）

对应提交哈希：`28d40bd`（feat: 商品发布接口基础版 - Prompt#SP001）

## 本轮目标

第三阶段核心业务模块迭代开发——模块1「二手商品发布功能」第1轮：初始生成基础版本。

## 用户原始Prompt摘要

1. ProductController 新增 add 方法，路径 `/product/add`，需要登录才能访问
2. 接收前端传的商品参数：标题、描述、分类id、价格、成色、交易方式、地址、经度、纬度
3. 从 token 中获取 userId，禁止前端传入
4. 商品默认上架状态，保存到数据库
5. 返回商品 id
6. 生成 Controller、Service、ServiceImpl、对应的 VO/DTO

## 设计决策

1. **卖家用户 ID 安全隔离**：DTO 中不定义 userId 字段，ServiceImpl 从 `UserHolder.getUserId()` 取值，
   前端传入的任何用户 ID 一律不被信任，杜绝越权发布
2. **商品状态枚举**：新增 `ProductStatusEnum`（上架=1 / 下架=0），与 UserStatusEnum 风格对齐，
   系统内禁止魔法值，商品插入时取 `ON_SHELF.getCode()` 设置默认上架
3. **参数校验分层**：
   - DTO 层：`@NotBlank` / `@NotNull` / `@Size` / `@DecimalMin` / `@Digits` 等 JSR-303 注解做格式与基础校验
   - Service 层：当前版本仅校验登录态（userId 非空），分类 ID 存在性等跨表校验留待第2轮优化增强
4. **无需修改拦截器配置**：WebMvcConfig 默认拦截 `/**`，`/product/add` 天然需要登录，
   不必加入白名单
5. **createTime / updateTime**：由 MyBatis-Plus MetaObjectHandler 自动填充，业务代码不手动赋值

## 变更文件（7 个，其中新建 6 个、修改 1 个）

| 文件 | 操作 | 说明 |
| --- | --- | --- |
| `product/controller/ProductController.java` | 新增 | 商品模块控制器：POST /product/add |
| `product/service/ProductService.java` | 新增 | 商品模块业务接口 |
| `product/serviceImpl/ProductServiceImpl.java` | 新增 | 商品模块业务实现：卖家取登录态、默认上架、返回商品ID |
| `product/dto/ProductAddDTO.java` | 新增 | 发布入参：标题/描述/分类ID/价格/成色/交易方式/地址/经度/纬度 |
| `product/vo/ProductAddVO.java` | 新增 | 发布出参：productId |
| `product/enums/ProductStatusEnum.java` | 新增 | 商品状态枚举：上架(1)/下架(0) |
| `backend/README.md` | 修改 | 工程结构树新增 product 模块全部文件说明 |

## 测试情况

1. `mvn compile`：BUILD SUCCESS（便携 Maven 3.9.16 + JDK 22.0.2）
2. 启动应用冒烟测试（Python 3.13 发送 UTF-8 请求，避免 Git Bash curl GBK 编码问题）：
   - **未登录访问 /product/add**：返回 `{"code":401, "msg":"未登录或 Token 已失效"}` ✅
   - **注册 + 登录**：成功获取 Token ✅
   - **发布线下交易商品**（带地址、经度、纬度）：返回 `{"code":200, "data":{"productId":2}}` ✅
   - **发布线上交易商品**（不传地址、经纬度）：返回 `{"code":200, "data":{"productId":3}}` ✅
   - **缺少必填字段 title**：返回 `{"code":400, "msg":"商品标题不能为空"}` ✅
   - **价格为 0**：返回 `{"code":400, "msg":"商品价格必须大于0"}` ✅
3. 数据库落库核验：
   - 线下商品：title / categoryId / price / productCondition / tradeType / address / longitude / latitude 全部正确映射
   - 线上商品：address / longitude / latitude 为 NULL（符合预期）
   - 两行 status 均为 1（上架）、create_time 自动填充 ✅
4. 测试数据已清理：product 表 DELETE、user 表 DELETE、Redis 用户缓存 DEL；应用实例已停止、8080 端口已释放

## 留痕说明

- 本记录为 过程记录第 006 篇，衔接 #005（f13a894，Prompt#INIT004）
- 对应比赛第三阶段核心业务模块迭代开发——模块1第1轮：初始生成基础版本
- 第2轮（优化增强）将补充：分类 ID 存在性校验、交易方式与地址/经纬度联动校验、参数越界校验等
