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
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_longitude` (`longitude`),
    KEY `idx_status_category` (`status`, `category_id`),
    KEY `idx_status_price` (`status`, `price`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '二手商品表';

-- ------------------------------------------------------------
-- 系统消息表（对应 com.example.message.entity.Message => @TableName("system_message")）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `system_message`;
CREATE TABLE `system_message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `receiver_id`  BIGINT       NOT NULL                COMMENT '接收者ID（关联 user 表）',
    `message_type` TINYINT      NOT NULL                COMMENT '消息类型：1系统通知 2公告 3交易消息 4其他',
    `receiver_type` VARCHAR(10) NOT NULL DEFAULT 'user' COMMENT '接收群体：all所有用户 admin仅管理员 user仅普通用户（管理后台群发时标记）',
    `title`        VARCHAR(100) NOT NULL                COMMENT '消息标题',
    `content`      TEXT         NOT NULL                COMMENT '消息内容',
    `is_read`      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读：0未读 1已读',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_create_time` (`create_time`)
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

-- 示例商品（testuser1 发布）
-- 说明：cover_image 指向 frontend/public/images/products/ 下的图片，
-- 这两张图与「二手iPhone 12」「编程书籍套装」对应，改动标题/价格时请同步替换图片
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `original_price`,
                       `product_condition`, `trade_type`, `brand`, `model`, `purchase_time`, `features`,
                       `remark`, `contact_name`, `contact_phone`, `address`, `longitude`, `latitude`,
                       `status`, `cover_image`) VALUES
    (2, '二手iPhone 12', '95新，功能完好，无拆修，原装配件齐全，电池健康度90%以上。支持当面验机。',
        1, 3500.00, 4599.00, '九成新', '线下', 'Apple', 'iPhone 12 128G', '2023-06-15',
        '支持验机,无拆修记录', '无磕碰无划痕，支持验机', '测试用户1', '13800138001',
        '北京市朝阳区国贸CBD', 116.466240, 39.920800, 1, '/images/products/iphone-12.png'),
    (2, '编程书籍套装', '包含算法、数据结构、计算机网络等经典教材共 6 本，无笔记无划线，书页保存完好。',
        3, 150.00, 420.00, '八成新', '线上', '机械工业出版社', '套装 6 册', '2022-09-01',
        '包装齐全,发票齐全', '整套出不单卖，可小刀', '测试用户1', '13800138001',
        NULL, NULL, NULL, 1, '/images/products/programming-books.png');

-- 示例商品图片（与 product.cover_image 保持一致，演示多图链路）
INSERT INTO `product_image` (`product_id`, `image_url`, `sort`) VALUES
    (1, '/images/products/iphone-12.png', 0),
    (2, '/images/products/programming-books.png', 0);

-- 示例系统消息
INSERT INTO `system_message` (`receiver_id`, `message_type`, `title`, `content`, `is_read`) VALUES
    (2, 1, '欢迎使用二手商品交易平台', '您的账号已创建成功，祝您交易愉快！', 0),
    (3, 1, '欢迎使用二手商品交易平台', '您的账号已创建成功，祝您交易愉快！', 0);
