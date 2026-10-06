-- ============================================================
-- 二手商品交易平台 —— 数据库增量升级脚本（老库升级用）
-- 说明：
--   1. 全新安装直接执行 01-schema.sql 即可（已包含本脚本的全部结构）；
--   2. 已有老库执行本脚本一次即可升级，请勿重复执行
--      （ALTER ... ADD COLUMN 在列已存在时会报 Duplicate column name）；
--   3. 涉及改动：system_message 增加群发批次号；新增举报表与系统设置表
-- ============================================================

USE `secondhand_market`;

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. system_message 增加群发批次号（管理端按批次聚合展示、整组删除）
-- ------------------------------------------------------------
ALTER TABLE `system_message`
    ADD COLUMN `batch_no` VARCHAR(36) DEFAULT NULL COMMENT '群发批次号：同一次群发的所有记录共享，用于聚合展示与整组删除' AFTER `receiver_type`;

ALTER TABLE `system_message`
    ADD KEY `idx_batch_no` (`batch_no`);

-- ------------------------------------------------------------
-- 2. 举报表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `report` (
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
-- 3. 系统设置表（从此前「存 Redis」改为落库，Redis 清空/重启后设置不丢）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_setting` (
    `id`          BIGINT NOT NULL COMMENT '主键，固定为1（系统只有一份设置）',
    `config_json` TEXT   NOT NULL COMMENT '设置JSON文本（SystemSettingDTO 序列化结果）',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统设置表（单行）';
