-- ============================================================
-- 二手商品交易平台 —— MySQL 8.0 初始化脚本
-- 数据库：secondhand_market（与 application.yml / docker-compose.yml 保持一致）
-- 用途：docker-compose 启动 MySQL 时自动执行（挂载到 /docker-entrypoint-initdb.d）
--       也可手动执行：mysql -uroot -p < 01-schema.sql
-- 说明：Docker 官方镜像会先根据 MYSQL_DATABASE 建库，这里再显式建一次以兼容手动执行
-- ============================================================

CREATE DATABASE IF NOT EXISTS `secondhand_market`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `secondhand_market`;

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(20)  NOT NULL                COMMENT '用户名（登录账号，唯一）',
    `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 密文，固定 60 位）',
    `phone`       VARCHAR(11)  DEFAULT NULL            COMMENT '手机号（唯一）',
    `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    `nickname`    VARCHAR(30)  DEFAULT NULL            COMMENT '昵称',
    `bio`         VARCHAR(255) DEFAULT NULL            COMMENT '个人简介',
    `avatar`      VARCHAR(255) DEFAULT NULL            COMMENT '头像URL',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0禁用',
    `role`        TINYINT      NOT NULL DEFAULT 0      COMMENT '角色：1管理员 0普通用户（仅管理员可登录管理后台）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ------------------------------------------------------------
-- 商品分类表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id`          INT         NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`        VARCHAR(30) NOT NULL                COMMENT '分类名称（唯一）',
    `sort`        INT         NOT NULL DEFAULT 0      COMMENT '排序值（越小越靠前）',
    `status`      TINYINT     NOT NULL DEFAULT 1      COMMENT '状态：1启用 0禁用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品分类表';

-- ------------------------------------------------------------
-- 二手商品表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `user_id`           BIGINT        NOT NULL                COMMENT '发布用户ID（卖家，关联 user 表）',
    `title`             VARCHAR(100)  NOT NULL                COMMENT '商品标题',
    `description`       VARCHAR(1000) DEFAULT NULL            COMMENT '商品描述',
    `cover_image`       VARCHAR(255)  DEFAULT NULL            COMMENT '封面图URL',
    `category_id`       INT           NOT NULL                COMMENT '分类ID（关联 category 表）',
    `price`             DECIMAL(10,2) NOT NULL                COMMENT '商品价格（元）',
    `original_price`    DECIMAL(10,2) DEFAULT NULL            COMMENT '原价（元），用于展示折扣',
    `product_condition` VARCHAR(10)   NOT NULL                COMMENT '成色：全新/九成新/八成新/七成新及以下',
    `trade_type`        VARCHAR(10)   NOT NULL                COMMENT '交易方式：线上/线下',
    `brand`             VARCHAR(50)   DEFAULT NULL            COMMENT '品牌',
    `model`             VARCHAR(50)   DEFAULT NULL            COMMENT '型号',
    `purchase_time`     VARCHAR(20)   DEFAULT NULL            COMMENT '购买时间（发布页日期选择器的值）',
    `features`          VARCHAR(255)  DEFAULT NULL            COMMENT '商品特色（多个用逗号分隔）',
    `remark`            VARCHAR(255)  DEFAULT NULL            COMMENT '备注说明',
    `contact_name`      VARCHAR(30)   DEFAULT NULL            COMMENT '联系人',
    `contact_phone`     VARCHAR(11)   DEFAULT NULL            COMMENT '联系电话',
    `address`           VARCHAR(200)  DEFAULT NULL            COMMENT '线下交易地址（线上交易时为空）',
    `longitude`         DECIMAL(10,6) DEFAULT NULL            COMMENT '经度（-180~180）',
    `latitude`          DECIMAL(10,6) DEFAULT NULL            COMMENT '纬度（-90~90）',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status`            TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1在售 2已售出 3已下架',
    `view_count`        INT           NOT NULL DEFAULT 0      COMMENT '浏览次数（详情页每次访问累加）',
    `campus_id`         BIGINT        DEFAULT NULL            COMMENT '所属校区ID（校园筛选与面交点推荐，关联 campus 表，可为空）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_longitude` (`longitude`),
    KEY `idx_campus_id` (`campus_id`),
    KEY `idx_status_category` (`status`, `category_id`),
    KEY `idx_status_price` (`status`, `price`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '二手商品表';

-- ------------------------------------------------------------
-- 校区表（校园场景的地理锚点：列表筛选、附近商品默认圆心）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `campus`;
CREATE TABLE `campus` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '校区ID',
    `name`             VARCHAR(50)   NOT NULL                COMMENT '校区名称',
    `center_longitude` DECIMAL(10,6) NOT NULL                COMMENT '中心经度（附近商品默认圆心）',
    `center_latitude`  DECIMAL(10,6) NOT NULL                COMMENT '中心纬度',
    `sort`             INT           NOT NULL DEFAULT 0      COMMENT '排序值（越小越靠前）',
    `status`           TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1启用 0禁用',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '校区表';

-- ------------------------------------------------------------
-- 面交点表（校区内推荐的安全面交位置：快递驿站/图书馆前/校门口等）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `meet_point`;
CREATE TABLE `meet_point` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '面交点ID',
    `campus_id`   BIGINT        NOT NULL                COMMENT '所属校区ID（关联 campus 表）',
    `name`        VARCHAR(50)   NOT NULL                COMMENT '面交点名称',
    `longitude`   DECIMAL(10,6) NOT NULL                COMMENT '经度',
    `latitude`    DECIMAL(10,6) NOT NULL                COMMENT '纬度',
    `description` VARCHAR(200)  DEFAULT NULL            COMMENT '位置说明',
    `sort`        INT           NOT NULL DEFAULT 0      COMMENT '排序值',
    PRIMARY KEY (`id`),
    KEY `idx_campus_id` (`campus_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面交点表';

-- ------------------------------------------------------------
-- 系统消息表（对应 com.example.message.entity.Message => @TableName("system_message")）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `system_message`;
CREATE TABLE `system_message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `receiver_id`  BIGINT       NOT NULL                COMMENT '接收者ID（关联 user 表）',
    `message_type` TINYINT      NOT NULL                COMMENT '消息类型：1系统通知 2公告 3交易消息 4其他',
    `receiver_type` VARCHAR(10) NOT NULL DEFAULT 'user' COMMENT '接收群体：all所有用户 admin仅管理员 user仅普通用户（管理后台群发时标记）',
    `batch_no`     VARCHAR(36)  DEFAULT NULL            COMMENT '群发批次号：同一次群发的所有记录共享，用于聚合展示与整组删除',
    `title`        VARCHAR(100) NOT NULL                COMMENT '消息标题',
    `content`      TEXT         NOT NULL                COMMENT '消息内容',
    `is_read`      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读：0未读 1已读',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_batch_no` (`batch_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统消息表';

-- ------------------------------------------------------------
-- 商品图片表（一个商品可有多张图，sort 最小的作为封面）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product_image`;
CREATE TABLE `product_image` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `product_id`  BIGINT       NOT NULL                COMMENT '商品ID（关联 product 表）',
    `image_url`   VARCHAR(255) NOT NULL                COMMENT '图片访问地址',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值（越小越靠前，第一张即封面）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品图片表';

-- ------------------------------------------------------------
-- 用户-商品关联表（收藏、购物车共用；同表不同 relation_type，可一键互转）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user_product_relation`;
CREATE TABLE `user_product_relation` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `user_id`       BIGINT      NOT NULL                COMMENT '用户ID',
    `product_id`    BIGINT      NOT NULL                COMMENT '商品ID',
    `relation_type` VARCHAR(20) NOT NULL                COMMENT '关系类型：collect-收藏 cart-购物车',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`, `relation_type`),
    KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户-商品关联表';

-- ------------------------------------------------------------
-- 评论表（预留）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '评论用户ID',
    `product_id`  BIGINT   NOT NULL                COMMENT '商品ID',
    `content`     TEXT     NOT NULL                COMMENT '评论内容',
    `rating`      TINYINT  NOT NULL DEFAULT 5      COMMENT '评分 1-5',
    `status`      TINYINT  NOT NULL DEFAULT 1      COMMENT '状态：1正常 0隐藏',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评论表';

-- ------------------------------------------------------------
-- 私信表（同时承载「商品咨询」：product_id 非空即为针对该商品的咨询）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `private_message`;
CREATE TABLE `private_message` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '私信ID',
    `conversation_key` VARCHAR(64)  NOT NULL                COMMENT '会话键：由双方用户ID与商品ID拼成，相同即同一会话，用于按会话聚合',
    `from_user_id`     BIGINT       NOT NULL                COMMENT '发送者ID',
    `to_user_id`       BIGINT       NOT NULL                COMMENT '接收者ID',
    `product_id`       BIGINT       DEFAULT NULL            COMMENT '关联商品ID：商品咨询填写，普通私信为空',
    `content`          VARCHAR(500) NOT NULL                COMMENT '私信内容',
    `is_read`          TINYINT      NOT NULL DEFAULT 0      COMMENT '接收方是否已读：0未读 1已读',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_key` (`conversation_key`),
    KEY `idx_from_user_id` (`from_user_id`),
    KEY `idx_to_user_id` (`to_user_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '私信表（含商品咨询）';

-- ------------------------------------------------------------
-- 订单表
-- 生命周期：待支付 →（模拟支付）已支付 →（买家确认）已完成；待支付可取消
-- 说明：下单即把商品置为「已售出」锁定，避免同一件二手商品被多人买走；
--       订单取消时回滚为「在售」
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product_order`;
CREATE TABLE `product_order` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`     VARCHAR(32)   NOT NULL                COMMENT '订单号（对外展示，唯一）',
    `buyer_id`     BIGINT        NOT NULL                COMMENT '买家ID',
    `total_amount` DECIMAL(10,2) NOT NULL                COMMENT '订单总金额',
    `item_count`   INT           NOT NULL DEFAULT 1      COMMENT '商品件数',
    `status`       TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1待支付 2已支付 3已取消 4已完成',
    `remark`       VARCHAR(255)  DEFAULT NULL            COMMENT '买家留言',
    `pay_time`     DATETIME      DEFAULT NULL            COMMENT '支付时间',
    `finish_time`  DATETIME      DEFAULT NULL            COMMENT '完成时间',
    `cancel_time`  DATETIME      DEFAULT NULL            COMMENT '取消时间',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品订单表';

-- ------------------------------------------------------------
-- 订单明细表
-- 标题、封面、价格均为下单时的快照：商品之后被编辑或删除，历史订单仍然准确
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单明细ID',
    `order_id`    BIGINT        NOT NULL                COMMENT '订单ID',
    `product_id`  BIGINT        NOT NULL                COMMENT '商品ID',
    `seller_id`   BIGINT        NOT NULL                COMMENT '卖家ID',
    `title`       VARCHAR(100)  NOT NULL                COMMENT '商品标题（下单时快照）',
    `cover_image` VARCHAR(255)  DEFAULT NULL            COMMENT '商品封面（下单时快照）',
    `price`       DECIMAL(10,2) NOT NULL                COMMENT '成交单价（下单时快照）',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_seller_id` (`seller_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';

-- ------------------------------------------------------------
-- 举报表（用户对商品/评论发起举报，管理员在后台处理）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '举报ID',
    `reporter_id`     BIGINT       NOT NULL                COMMENT '举报人用户ID（关联 user 表）',
    `target_type`     VARCHAR(10)  NOT NULL                COMMENT '举报对象类型：product商品 comment评论',
    `target_id`       BIGINT       NOT NULL                COMMENT '举报对象ID（商品ID或评论ID）',
    `reason`          VARCHAR(30)  NOT NULL                COMMENT '举报原因',
    `description`     VARCHAR(255) DEFAULT NULL            COMMENT '补充说明',
    `status`          TINYINT      NOT NULL DEFAULT 0      COMMENT '处理状态：0待处理 1已处理 2已驳回',
    `handle_action`   VARCHAR(20)  DEFAULT NULL            COMMENT '处理动作：takeDownProduct下架商品 hideComment隐藏评论 dismiss驳回',
    `handle_note`     VARCHAR(255) DEFAULT NULL            COMMENT '处理备注',
    `handle_admin_id` BIGINT       DEFAULT NULL            COMMENT '处理人（管理员）用户ID',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_reporter_id` (`reporter_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '举报表';

-- ------------------------------------------------------------
-- 系统设置表（单行表，id 固定为 1，设置整体以 JSON 存储）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `system_setting`;
CREATE TABLE `system_setting` (
    `id`          BIGINT NOT NULL COMMENT '主键，固定为1（系统只有一份设置）',
    `config_json` TEXT   NOT NULL COMMENT '设置JSON文本（SystemSettingDTO 序列化结果）',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统设置表（单行）';

-- ============================================================
-- 种子数据
-- ============================================================

-- 基础商品分类
INSERT INTO `category` (`name`, `sort`, `status`) VALUES
    ('手机数码', 1, 1),
    ('电脑办公', 2, 1),
    ('图书教材', 3, 1),
    ('家用电器', 4, 1),
    ('服饰鞋包', 5, 1),
    ('运动户外', 6, 1),
    ('美妆个护', 7, 1),
    ('其他闲置', 8, 1);

-- 默认账号
-- admin   / admin123   （管理员，role=1，可登录管理后台）
-- testuser1 / 123456   （普通用户，有两条在售商品）
-- 说明：下列密文由项目同款 BCryptPasswordEncoder 生成，可直接用于登录
INSERT INTO `user` (`id`, `username`, `password`, `phone`, `nickname`, `status`, `role`) VALUES
    (1, 'admin',     '$2a$10$2PkmBxt2Puc3swXAd6S5Ke2Pbg3dSoaihFx1mLNL5q32Lxr.Gj0t2', '13800138000', '管理员',   1, 1),
    (2, 'testuser1', '$2a$10$1CX3LKmt/AGGOLn6lmtb6eyG1XtMrZYi7PB.9ZdtiLRQoZwUmGvU.', '13800138001', '测试用户1', 1, 0),
    (3, 'testuser2', '$2a$10$1CX3LKmt/AGGOLn6lmtb6eyG1XtMrZYi7PB.9ZdtiLRQoZwUmGvU.', '13800138002', '测试用户2', 1, 0);

-- 校区种子（校园场景的地理锚点；坐标为演示用园区坐标）
INSERT INTO `campus` (`id`, `name`, `center_longitude`, `center_latitude`, `sort`, `status`) VALUES
    (1, '江畔大学·主校区', 114.352000, 30.538000, 1, 1),
    (2, '江畔大学·东校区', 114.365000, 30.545000, 2, 1);

-- 面交点种子（推荐的安全面交位置：人流大、有值班/监控的地点）
INSERT INTO `meet_point` (`id`, `campus_id`, `name`, `longitude`, `latitude`, `description`, `sort`) VALUES
    (1, 1, '东门快递驿站', 114.355000, 30.539000, '主校区东门内侧快递集中取件点，人流大，适合大件面交', 1),
    (2, 1, '图书馆前广场', 114.351000, 30.537200, '图书馆正门广场，白天人流密集，视野开阔', 2),
    (3, 1, '南门校门口', 114.349500, 30.534800, '南门保安亭旁，白天有保安值班', 3),
    (4, 1, '体育馆东侧空地', 114.354800, 30.541500, '体育馆东侧空地，课后人流多', 4),
    (5, 2, '东校区教育超市门口', 114.364000, 30.544000, '东校区教育超市门口，营业时间长', 1);

-- 示例商品（testuser1 发布）
-- 说明：cover_image 指向 frontend/public/images/products/ 下的图片，
-- 这两张图与「二手iPhone 12」「编程书籍套装」对应，改动标题/价格时请同步替换图片
-- 商品 1 的坐标落在主校区东门快递驿站附近，用于「推荐面交点」演示
INSERT INTO `product` (`id`, `user_id`, `title`, `description`, `category_id`, `price`, `original_price`,
                       `product_condition`, `trade_type`, `brand`, `model`, `purchase_time`, `features`,
                       `remark`, `contact_name`, `contact_phone`, `address`, `longitude`, `latitude`,
                       `campus_id`, `status`, `cover_image`, `create_time`) VALUES
    (1, 2, '二手iPhone 12', '95新，功能完好，无拆修，原装配件齐全，电池健康度90%以上。支持当面验机。',
        1, 3500.00, 4599.00, '九成新', '线下', 'Apple', 'iPhone 12 128G', '2023-06-15',
        '支持验机,无拆修记录', '无磕碰无划痕，支持验机', '测试用户1', '13800138001',
        '江畔大学主校区东区9号宿舍楼', 114.356500, 30.540200, 1, 1, '/images/products/iphone-12.png', NOW() - INTERVAL 2 DAY),
    (2, 2, '编程书籍套装', '包含算法、数据结构、计算机网络等经典教材共 6 本，无笔记无划线，书页保存完好。',
        3, 150.00, 420.00, '八成新', '线上', '机械工业出版社', '套装 6 册', '2022-09-01',
        '包装齐全,发票齐全', '整套出不单卖，可小刀', '测试用户1', '13800138001',
        NULL, NULL, NULL, 1, 1, '/images/products/programming-books.png', NOW() - INTERVAL 3 DAY);

-- 历史成交种子（已售出商品 + 订单 + 明细快照）
-- 用途：给「成交行情」提供确定性的样本数据——同分类同成色的中位价因此可预期，
-- 详情页划算指数、发布页行情估价、「我的发布」降价建议开箱即可演示。
-- 全部走真实业务表（product/product_order/order_item），时间用 NOW() 相对值，
-- 保证安装后任意时刻执行都在 90 天统计窗口内。窗口内样本：
--   手机数码·九成新 5 件（中位价 3600）  图书教材·八成新 4 件（中位价 25）
--   电脑办公·九成新 3 件（中位价 220）   运动户外·八成新 3 件（中位价 65）
INSERT INTO `product` (`id`, `user_id`, `title`, `description`, `category_id`, `price`,
                       `product_condition`, `trade_type`, `campus_id`, `address`, `longitude`, `latitude`,
                       `status`, `create_time`, `view_count`) VALUES
    (3,  2, 'iPhone 13 128G 蓝色',        '自用一年，电池健康89%，无拆修，配原装盒。',            1, 3900.00, '九成新', '线下', 1, '江畔大学主校区东区9号宿舍楼', 114.354200, 30.539600, 2, NOW() - INTERVAL 88 DAY, 36),
    (4,  2, 'iPad Air 4 深空灰 64G',      '宿舍吃灰出掉，屏幕无划痕，配保护壳。',                  1, 3600.00, '九成新', '线下', 1, '江畔大学主校区东区5号宿舍楼', 114.353600, 30.541200, 2, NOW() - INTERVAL 73 DAY, 41),
    (5,  2, 'MacBook Air 2019 银色',      '轻度办公使用，循环次数120次，可当面验机。',             1, 4200.00, '九成新', '线下', 1, '江畔大学主校区研究生楼',       114.350800, 30.540500, 2, NOW() - INTERVAL 58 DAY, 55),
    (6,  2, '荣耀平板7 10.1寸',           '网课神器，成色好，配件齐全。',                          1, 2900.00, '九成新', '线下', 1, '江畔大学主校区东区5号宿舍楼', 114.353600, 30.541200, 2, NOW() - INTERVAL 43 DAY, 22),
    (7,  2, '小米平板5 黑色 128G',        '买来看网课的，现在用不上了，95新。',                    1, 3300.00, '九成新', '线下', 1, '江畔大学主校区东区9号宿舍楼', 114.356500, 30.540200, 2, NOW() - INTERVAL 28 DAY, 18),
    (8,  3, '高等数学教材套装（上下册）', '同济第七版，无笔记无划线。',                            3, 30.00,  '八成新', '线上', 1, NULL, NULL, NULL, 2, NOW() - INTERVAL 83 DAY, 14),
    (9,  3, '考研英语一真题（近十年）',   '做完一套，其余全新，含解析。',                          3, 22.00,  '八成新', '线上', 1, NULL, NULL, NULL, 2, NOW() - INTERVAL 63 DAY, 9),
    (10, 3, '数据结构与算法分析教材',     '课程结束出掉，内页干净。',                              3, 28.00,  '八成新', '线上', 1, NULL, NULL, NULL, 2, NOW() - INTERVAL 48 DAY, 7),
    (11, 3, '大学英语综合教程 1-4 册',    '全新未拆封两册，其余九成新。',                          3, 15.00,  '八成新', '线上', 1, NULL, NULL, NULL, 2, NOW() - INTERVAL 33 DAY, 5),
    (12, 3, '罗技K845机械键盘',           '茶轴，宿舍使用半年，无维修。',                          2, 220.00,  '九成新', '线下', 1, '江畔大学主校区东区7号宿舍楼', 114.352900, 30.542300, 2, NOW() - INTERVAL 78 DAY, 27),
    (13, 3, 'AOC 27寸显示器',             '实验室淘汰，面板完美，带原装支架。',                    2, 680.00,  '九成新', '线下', 1, '江畔大学主校区研究生楼',       114.350800, 30.540500, 2, NOW() - INTERVAL 53 DAY, 33),
    (14, 3, '罗技M330无线鼠标',           '静音鼠标，一节电池用到毕业。',                          2, 95.00,   '九成新', '线下', 1, '江畔大学主校区东区7号宿舍楼', 114.352900, 30.542300, 2, NOW() - INTERVAL 38 DAY, 11),
    (15, 2, '斯伯丁篮球 7号球',           '室外场用了一学期，气针齐全。',                          6, 65.00,   '八成新', '线下', 1, '江畔大学主校区体育馆',         114.354800, 30.541500, 2, NOW() - INTERVAL 68 DAY, 13),
    (16, 2, '奥义瑜伽垫 8mm',             '买来用了两次，已清洗晾干。',                            6, 45.00,   '八成新', '线下', 1, '江畔大学主校区体育馆',         114.354800, 30.541500, 2, NOW() - INTERVAL 23 DAY, 8),
    (17, 2, '尤尼克斯羽毛球拍 双拍',      '双拍带拍包，手胶新换。',                                6, 160.00,  '八成新', '线下', 1, '江畔大学主校区体育馆',         114.354800, 30.541500, 2, NOW() - INTERVAL 18 DAY, 19);

-- 历史成交订单（status=4 已完成 / 2 已支付；pay_time 是行情统计口径的时间轴）
INSERT INTO `product_order` (`id`, `order_no`, `buyer_id`, `total_amount`, `item_count`, `status`,
                             `pay_time`, `finish_time`, `create_time`) VALUES
    (1,  'SEED0850001', 3, 3900.00, 1, 4, NOW() - INTERVAL 85 DAY, NOW() - INTERVAL 84 DAY, NOW() - INTERVAL 85 DAY - INTERVAL 2 HOUR),
    (2,  'SEED0700002', 3, 3600.00, 1, 4, NOW() - INTERVAL 70 DAY, NOW() - INTERVAL 69 DAY, NOW() - INTERVAL 70 DAY - INTERVAL 2 HOUR),
    (3,  'SEED0550003', 3, 4200.00, 1, 4, NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 54 DAY, NOW() - INTERVAL 55 DAY - INTERVAL 2 HOUR),
    (4,  'SEED0400004', 3, 2900.00, 1, 2, NOW() - INTERVAL 40 DAY, NULL, NOW() - INTERVAL 40 DAY - INTERVAL 2 HOUR),
    (5,  'SEED0250005', 3, 3300.00, 1, 4, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 24 DAY, NOW() - INTERVAL 25 DAY - INTERVAL 2 HOUR),
    (6,  'SEED0800006', 2, 30.00,   1, 4, NOW() - INTERVAL 80 DAY, NOW() - INTERVAL 79 DAY, NOW() - INTERVAL 80 DAY - INTERVAL 2 HOUR),
    (7,  'SEED0600007', 2, 22.00,   1, 4, NOW() - INTERVAL 60 DAY, NOW() - INTERVAL 59 DAY, NOW() - INTERVAL 60 DAY - INTERVAL 2 HOUR),
    (8,  'SEED0450008', 2, 28.00,   1, 4, NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 44 DAY, NOW() - INTERVAL 45 DAY - INTERVAL 2 HOUR),
    (9,  'SEED0300009', 2, 15.00,   1, 2, NOW() - INTERVAL 30 DAY, NULL, NOW() - INTERVAL 30 DAY - INTERVAL 2 HOUR),
    (10, 'SEED0750010', 2, 220.00,  1, 4, NOW() - INTERVAL 75 DAY, NOW() - INTERVAL 74 DAY, NOW() - INTERVAL 75 DAY - INTERVAL 2 HOUR),
    (11, 'SEED0500011', 2, 680.00,  1, 4, NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 49 DAY, NOW() - INTERVAL 50 DAY - INTERVAL 2 HOUR),
    (12, 'SEED0350012', 2, 95.00,   1, 4, NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 34 DAY, NOW() - INTERVAL 35 DAY - INTERVAL 2 HOUR),
    (13, 'SEED0650013', 3, 65.00,   1, 4, NOW() - INTERVAL 65 DAY, NOW() - INTERVAL 64 DAY, NOW() - INTERVAL 65 DAY - INTERVAL 2 HOUR),
    (14, 'SEED0200014', 3, 45.00,   1, 4, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 19 DAY, NOW() - INTERVAL 20 DAY - INTERVAL 2 HOUR),
    (15, 'SEED0150015', 3, 160.00,  1, 2, NOW() - INTERVAL 15 DAY, NULL, NOW() - INTERVAL 15 DAY - INTERVAL 2 HOUR);

-- 历史成交明细（标题/价格为下单时快照，与 product 表一致）
INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`) VALUES
    (1,  1,  3,  2, 'iPhone 13 128G 蓝色',        3900.00, NOW() - INTERVAL 85 DAY - INTERVAL 2 HOUR),
    (2,  2,  4,  2, 'iPad Air 4 深空灰 64G',      3600.00, NOW() - INTERVAL 70 DAY - INTERVAL 2 HOUR),
    (3,  3,  5,  2, 'MacBook Air 2019 银色',      4200.00, NOW() - INTERVAL 55 DAY - INTERVAL 2 HOUR),
    (4,  4,  6,  2, '荣耀平板7 10.1寸',           2900.00, NOW() - INTERVAL 40 DAY - INTERVAL 2 HOUR),
    (5,  5,  7,  2, '小米平板5 黑色 128G',        3300.00, NOW() - INTERVAL 25 DAY - INTERVAL 2 HOUR),
    (6,  6,  8,  3, '高等数学教材套装（上下册）', 30.00,   NOW() - INTERVAL 80 DAY - INTERVAL 2 HOUR),
    (7,  7,  9,  3, '考研英语一真题（近十年）',   22.00,   NOW() - INTERVAL 60 DAY - INTERVAL 2 HOUR),
    (8,  8,  10, 3, '数据结构与算法分析教材',     28.00,   NOW() - INTERVAL 45 DAY - INTERVAL 2 HOUR),
    (9,  9,  11, 3, '大学英语综合教程 1-4 册',    15.00,   NOW() - INTERVAL 30 DAY - INTERVAL 2 HOUR),
    (10, 10, 12, 3, '罗技K845机械键盘',           220.00,  NOW() - INTERVAL 75 DAY - INTERVAL 2 HOUR),
    (11, 11, 13, 3, 'AOC 27寸显示器',             680.00,  NOW() - INTERVAL 50 DAY - INTERVAL 2 HOUR),
    (12, 12, 14, 3, '罗技M330无线鼠标',           95.00,   NOW() - INTERVAL 35 DAY - INTERVAL 2 HOUR),
    (13, 13, 15, 2, '斯伯丁篮球 7号球',           65.00,   NOW() - INTERVAL 65 DAY - INTERVAL 2 HOUR),
    (14, 14, 16, 2, '奥义瑜伽垫 8mm',             45.00,   NOW() - INTERVAL 20 DAY - INTERVAL 2 HOUR),
    (15, 15, 17, 2, '尤尼克斯羽毛球拍 双拍',      160.00,  NOW() - INTERVAL 15 DAY - INTERVAL 2 HOUR);

-- 滞销演示商品（testuser1 发布，挂牌 20 天 + 定价高于行情，用于「降价建议」演示：
-- 图书教材·八成新中位价 ¥25，当前价 ¥45 > 九折线 ¥22，因此「我的发布」会给出降价建议）
INSERT INTO `product` (`id`, `user_id`, `title`, `description`, `category_id`, `price`, `original_price`,
                       `product_condition`, `trade_type`, `campus_id`, `address`, `longitude`, `latitude`,
                       `status`, `create_time`, `view_count`) VALUES
    (18, 2, '高等数学教材（第七版）', '同济第七版上下册，上册有少量铅笔笔记可擦除。',
        3, 45.00, 68.00, '八成新', '线下', 1, '江畔大学主校区东区9号宿舍楼', 114.353500, 30.539500,
        1, NOW() - INTERVAL 20 DAY, 6);

-- 示例商品图片（与 product.cover_image 保持一致，演示多图链路）
INSERT INTO `product_image` (`product_id`, `image_url`, `sort`) VALUES
    (1, '/images/products/iphone-12.png', 0),
    (2, '/images/products/programming-books.png', 0);

-- 示例系统消息
INSERT INTO `system_message` (`receiver_id`, `message_type`, `title`, `content`, `is_read`) VALUES
    (2, 1, '欢迎使用二手商品交易平台', '您的账号已创建成功，祝您交易愉快！', 0),
    (3, 1, '欢迎使用二手商品交易平台', '您的账号已创建成功，祝您交易愉快！', 0);
