-- 二手商品交易平台数据库初始化脚本（简化版）
-- 数据库：secondhand_market（MySQL 8.0，字符集 utf8mb4）
-- 提示：完整的建表 + 种子数据请使用 backend/scripts/01-schema.sql

-- 创建数据库（若不存在）
CREATE DATABASE IF NOT EXISTS `secondhand_market`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `secondhand_market`;

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(20)  NOT NULL                COMMENT '用户名（登录账号，唯一）',
    `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt密文）',
    `phone`       VARCHAR(11)  DEFAULT NULL            COMMENT '手机号（唯一）',
    `nickname`    VARCHAR(30)  DEFAULT NULL            COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL            COMMENT '头像URL',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1正常 0禁用',
    `role`        TINYINT      NOT NULL DEFAULT 0      COMMENT '角色：1管理员 0普通用户',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- 商品分类表
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id`          INT         NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`        VARCHAR(30) NOT NULL                COMMENT '分类名称（唯一）',
    `sort`        INT         NOT NULL DEFAULT 0      COMMENT '排序值（数字越小越靠前）',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品分类表';

-- 二手商品表
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `user_id`           BIGINT        NOT NULL                COMMENT '发布用户ID（卖家，关联user表）',
    `title`             VARCHAR(100)  NOT NULL                COMMENT '商品标题',
    `description`       VARCHAR(1000) DEFAULT NULL            COMMENT '商品描述',
    `cover_image`       VARCHAR(255)  DEFAULT NULL            COMMENT '封面图URL（列表页缩略图展示）',
    `category_id`       INT           NOT NULL                COMMENT '分类ID（关联category表）',
    `price`             DECIMAL(10,2) NOT NULL                COMMENT '商品价格（元，必须大于0）',
    `product_condition` VARCHAR(10)   NOT NULL                COMMENT '成色：全新/九成新/八成新/七成新及以下',
    `trade_type`        VARCHAR(10)   NOT NULL                COMMENT '交易方式：线上/线下',
    `address`           VARCHAR(200)  DEFAULT NULL            COMMENT '线下交易地址（线上交易时为空）',
    `longitude`         DECIMAL(10,6) DEFAULT NULL            COMMENT '经度（线下交易地点，-180~180）',
    `latitude`          DECIMAL(10,6) DEFAULT NULL            COMMENT '纬度（线下交易地点，-90~90）',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（发布时间）',
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status`            TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1上架 0下架',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_longitude` (`longitude`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '二手商品表';

-- 系统消息表（对应 com.example.message.entity.Message => @TableName("system_message")）
DROP TABLE IF EXISTS `system_message`;
CREATE TABLE `system_message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `receiver_id`  BIGINT       NOT NULL                COMMENT '接收者ID（关联user表）',
    `message_type` TINYINT      NOT NULL                COMMENT '消息类型：1系统通知 2公告 3交易消息 4其他',
    `title`        VARCHAR(100) NOT NULL                COMMENT '消息标题',
    `content`      TEXT         NOT NULL                COMMENT '消息内容',
    `is_read`      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读：0未读 1已读',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_is_read` (`is_read`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统消息表';

-- 基础分类种子数据
INSERT INTO `category` (`name`, `sort`) VALUES
    ('手机数码', 1),
    ('电脑办公', 2),
    ('图书教材', 3),
    ('家用电器', 4),
    ('服饰鞋包', 5),
    ('运动户外', 6),
    ('美妆个护', 7),
    ('其他闲置', 8);

-- 测试用户数据（密码都是 123456）
INSERT INTO `user` (`username`, `password`, `nickname`, `phone`) VALUES
    ('testuser1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '测试用户1', '13800138001'),
    ('testuser2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITi', '测试用户2', '13800138002');