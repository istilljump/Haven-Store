# 提交记录#003（Prompt#INIT002）

对应提交哈希：`7f7aa1e`（feat: 基础分层架构与通用核心组件 - Prompt#INIT002）

## 本轮目标

按项目规划第二阶段步骤2，补全基础分层架构与通用核心组件中缺失的部分（实体类与 Mapper 接口）。

## 用户原始Prompt摘要

1. 生成包结构：controller、service、serviceImpl、mapper、entity、vo、dto、common、utils
2. common 包：统一返回结果类 Result<T>、业务异常类 BusinessException、全局异常处理器 GlobalExceptionHandler
3. entity 包：三张表对应实体类，使用 MyBatis-Plus 注解
4. mapper 包：对应 Mapper 接口，继承 BaseMapper

## 现状核对（与 #001 的重叠部分）

- 包结构与 common 组件（Result、ResultCodeEnum、BusinessException、GlobalExceptionHandler、Constants、RedisKeyConst）、utils 四件套（JwtUtil、PasswordUtil、RedisUtil、UserHolder）在 #001 已全部完成并经冒烟测试验证，本轮**未重复生成、未改动**
- User 实体与 UserMapper 已存在，本轮实际新增的是 product、category 两个模块的实体与数据访问层

## 变更文件（4 个，均为新增）

- com.example.product.entity.Product：对应 product 表全量 15 列（含 @TableName、@TableId(IdType.AUTO)、
  createTime/updateTime 的 @TableField 自动填充注解、全字段中文注释 + Swagger 注解）
- com.example.product.mapper.ProductMapper：继承 BaseMapper<Product>，标注 @Mapper
- com.example.category.entity.Category：对应 category 表 5 列，主键 Integer 类型与 product.categoryId 对齐
- com.example.category.mapper.CategoryMapper：继承 BaseMapper<Category>，标注 @Mapper

## 设计说明

- 实体注释中的枚举取值（成色、交易方式、状态）按规划文档以中文字符串/数字直接注释，暂不引用枚举类——商品状态等枚举类按规划在商品发布模块后续轮次（SP003）再落地，避免空引用
- Mapper 采用 @Mapper 逐个标注（与启动类说明一致，主包自动扫描，无需 @MapperScan），新增模块零配置扩展

## 测试情况

1. `mvn compile`：BUILD SUCCESS，无缺包/导包错误
2. 启动冒烟：本地拉起 Redis（.tools/redis，PONG 正常）后 `mvn spring-boot:run`，
   应用 3.1 秒启动成功（Tomcat 8080，上下文 /api），curl /api/user/info 返回统一格式 `{"code":401,"msg":"未登录或 Token 已失效","data":null}`，既有功能无回归
3. 映射回路验证（临时 @SpringBootTest 集成测试，验证后已删除，未进入提交）：
   - Category：selectCount=8、selectList 全字段读取正常（中文无乱码）
   - Product：insert（自增主键回填 + createTime/updateTime 自动填充）→ selectById（价格/成色/经纬度等全字段断言一致）→ deleteById，全程无异常
   - 测试后数据库还原干净：product 0 行、category 8 行种子数据不变
4. 测试用应用实例已停止，8080 端口已释放；Redis 保持运行供后续开发使用

## 留痕说明

- 本记录为 过程记录第 003 篇，衔接 #002（451ac0c，Prompt#INIT001）
