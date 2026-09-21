# AI提交记录#002（Prompt#INIT001）

对应提交哈希：`451ac0c`（init(AI生成): 数据库建表脚本与Maven依赖配置 - Prompt#INIT001）

## 本轮目标

按项目规划第二阶段步骤1，生成/补全数据库建表脚本（user、product、category 三张表）与 Maven 依赖配置。

## 用户原始Prompt摘要

1. MySQL 建表脚本：
   - 用户表 user：id、用户名、密码加密存储、手机号、昵称、头像、创建时间、更新时间、状态（AI#001 已建）
   - 商品表 product：二手商品字段（含经纬度、交易方式、状态）
   - 商品分类表 category：id、分类名称、排序
   - 所有表加主键与合理的普通索引，引擎 InnoDB，字符集 utf8mb4
2. pom.xml 依赖：SpringBoot 2.7.12、MyBatis-Plus、MySQL 驱动、Redis、JWT、Lombok、Knife4j、校验 starter、commons-lang3 等常用工具

## 设计依据（字段规格来源）

- product 字段清单严格按《项目初步规划》模块1（商品发布）定义：
  id、userId、title VARCHAR(100)、description VARCHAR(1000)、categoryId INT、
  price DECIMAL(10,2)、productCondition VARCHAR(10)（全新/九成新/八成新/七成新及以下）、
  tradeType VARCHAR(10)（线上/线下）、address VARCHAR(200)（线上交易可空）、
  longitude/latitude DECIMAL(10,6)、status（1上架/0下架）、createTime、updateTime
- 新增 cover_image 封面图字段：规划文档 LBS 模块返回字段包含「缩略图」，提前建列避免后续 ALTER
- Java 字段名 productCondition 经驼峰映射对应列 product_condition，规避 MySQL 保留字 CONDITION
- 不建物理外键（阿里巴巴规范：外键概念在应用层解决），product.category_id 由后端校验
- product 业务规则：线下交易时 address/longitude/latitude 必填，线上可空（由参数校验保证，见规划文档模块1第2轮）
- LBS 性能预留：idx_longitude 支撑矩形范围预过滤；idx_create_time 支撑发布时间倒序；idx_status 支撑上架筛选
- category 附 8 条种子数据，便于商品发布、分类筛选联调

## 变更文件

- backend/sql/init.sql：新增 category 表（uk_name 唯一索引）、product 表（5 个普通索引）与分类种子数据；user 表保持 AI#001 原样；脚本可重复执行
- backend/pom.xml：新增 commons-lang3（3.12.0，版本由 Boot 2.7.12 父工程 BOM 统一管理）；其余全部依赖 AI#001 已就绪，未重复改动
- backend/README.md：数据库初始化说明更新为三张表

## 测试情况

1. 临时库 second_hand_market_ddl_check 完整执行 init.sql 改名版：3 表建表、8 条中文分类数据、5 个索引全部正确，验证后已删除临时库
2. 正式库 second_hand_market 已执行 init.sql：category/product 建表成功；user 表按脚本 DROP 重建（原表仅剩 AI#001 冒烟测试账号 zhangsan，属测试残留）
3. Maven 编译验证：BUILD SUCCESS；dependency:tree 确认 commons-lang3:3.12.0 由父工程管理解析

## 留痕说明

- 提交信息格式遵循《项目初步规划》推荐规范：`类型(AI参与度)：功能描述 - Prompt编号`
- 本记录与 AI#001（f801b82）衔接，为 AI 过程记录第 002 篇；Prompt#INIT001 为项目规划第二阶段步骤1的指令编号
