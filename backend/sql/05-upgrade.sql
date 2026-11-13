-- ============================================================
-- 05-upgrade.sql —— 老库一次性升级：业务流程升级（2026-11 第三轮）
-- 内容：交易事件通知（复用 system_message，type=3 交易消息，不加表）+
--       私信议价报价字段 + 订单面交确认码字段 + 评价标签与卖家回评字段
-- 幂等性：列存在即跳过（information_schema 检查），可重复执行
-- 设计裁定：docs/015（引导式审定 9 项结论）
-- ============================================================

USE `secondhand_market`;

SET NAMES utf8mb4;

-- 列存在则跳过的通用过程（MySQL 8.0 无 ADD COLUMN IF NOT EXISTS）
DROP PROCEDURE IF EXISTS `hs_add_column`;
DELIMITER $$
CREATE PROCEDURE `hs_add_column`(
    IN in_table   VARCHAR(64),
    IN in_column  VARCHAR(64),
    IN in_ddl     TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = in_table
          AND COLUMN_NAME = in_column
    ) THEN
        SET @ddl = in_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ------------------------------------------------------------
-- 1. 私信议价：报价三字段（仅卖家报价消息非空）
--    offer_status：0 待处理 / 1 已接受 / 2 已失效（过期/商品锁定/被新报价顶替）
-- ------------------------------------------------------------
CALL hs_add_column('private_message', 'offer_price',
    'ALTER TABLE `private_message` ADD COLUMN `offer_price` DECIMAL(10,2) DEFAULT NULL COMMENT ''议价报价金额（仅卖家报价消息非空）'' AFTER `content`');
CALL hs_add_column('private_message', 'offer_expire_time',
    'ALTER TABLE `private_message` ADD COLUMN `offer_expire_time` DATETIME DEFAULT NULL COMMENT ''报价失效时间（发送+30分钟）'' AFTER `offer_price`');
CALL hs_add_column('private_message', 'offer_status',
    'ALTER TABLE `private_message` ADD COLUMN `offer_status` TINYINT DEFAULT NULL COMMENT ''报价状态：0待处理 1已接受 2已失效；非报价消息为NULL'' AFTER `offer_expire_time`');
-- 报价接受/失效按状态检索
CALL hs_add_column('private_message', 'idx_offer',
    'ALTER TABLE `private_message` ADD INDEX `idx_offer` (`offer_status`, `offer_expire_time`)');

-- ------------------------------------------------------------
-- 2. 订单面交确认码：支付成功生成，卖家输码确认交付（一次性）
--    confirmed_by：1 买家确认收货 / 2 卖家输码面交 / 3 超时自动完成
-- ------------------------------------------------------------
CALL hs_add_column('product_order', 'confirm_code',
    'ALTER TABLE `product_order` ADD COLUMN `confirm_code` CHAR(6) DEFAULT NULL COMMENT ''面交确认码（支付成功生成，买家持码卖家输码，一次性）'' AFTER `cancel_time`');
CALL hs_add_column('product_order', 'confirm_fail_count',
    'ALTER TABLE `product_order` ADD COLUMN `confirm_fail_count` INT NOT NULL DEFAULT 0 COMMENT ''面交码错误尝试次数（达5次当日锁定）'' AFTER `confirm_code`');
CALL hs_add_column('product_order', 'confirmed_by',
    'ALTER TABLE `product_order` ADD COLUMN `confirmed_by` TINYINT DEFAULT NULL COMMENT ''完成确认方式：1买家确认 2卖家输码 3超时自动'' AFTER `confirm_fail_count`');

-- ------------------------------------------------------------
-- 3. 评价标签与卖家回评
--    tags：逗号分隔预置正向标签（≤3 个）：准时赴约/成色如一/沟通顺畅/描述相符
--    reply/reply_time：卖家回评（每条买家评价仅一条）
-- ------------------------------------------------------------
CALL hs_add_column('comment', 'tags',
    'ALTER TABLE `comment` ADD COLUMN `tags` VARCHAR(200) DEFAULT NULL COMMENT ''评价标签，逗号分隔（预置正向标签，≤3个）'' AFTER `rating`');
CALL hs_add_column('comment', 'reply',
    'ALTER TABLE `comment` ADD COLUMN `reply` VARCHAR(500) DEFAULT NULL COMMENT ''卖家回评内容（每条评价仅一条）'' AFTER `tags`');
CALL hs_add_column('comment', 'reply_time',
    'ALTER TABLE `comment` ADD COLUMN `reply_time` DATETIME DEFAULT NULL COMMENT ''卖家回评时间'' AFTER `reply`');

DROP PROCEDURE IF EXISTS `hs_add_column`;

-- ============================================================
-- 说明：
-- 1. 交易事件通知不新建表——复用 system_message（message_type=3「交易消息」，
--    轮前为空壳枚举），接收人定向写入，通知中心/顶栏角标自动生效；
-- 2. 议价总开关（tradeNegotiationEnabled）与面交码总开关
--    （tradeConfirmCodeEnabled）落在 system_setting JSON 的 trade 分组，
--    缺省开启，无需 DDL；
-- 3. 老数据兼容：三处新列均为 NULL/默认值，不触碰既有行为。
-- ============================================================
