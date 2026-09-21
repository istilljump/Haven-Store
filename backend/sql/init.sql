-- =============================================================
-- 二手商品交易平台数据库初始化脚本
-- 数据库：second_hand_market（MySQL 8.0，字符集 utf8mb4）
-- 执行方式：mysql -uroot -p < init.sql，或在 Navicat 等客户端中直接执行
-- =============================================================

-- 创建数据库（若不存在），指定 utf8mb4 字符集以支持表情符号等完整 Unicode 字符
CREATE DATABASE IF NOT EXISTS `second_hand_market`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `second_hand_market`;

-- -------------------------------------------------------------
-- 用户表
-- -------------------------------------------------------------
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
    PRIMARY KEY (`id`),
    -- 用户名唯一索引：注册唯一性校验的数据库兜底保障
    UNIQUE KEY `uk_username` (`username`),
    -- 手机号唯一索引
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';
