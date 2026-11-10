-- ============================================================
-- 03-upgrade.sql —— 老库一次性升级：校园场景 + 成交行情（2026-11）
-- 定位：已经跑过 01-schema.sql 旧版本（12 张表）的数据库增量升级到本轮能力
-- 与 01-schema.sql 新装路线等效：结构（campus/meet_point/product.campus_id）
-- + 种子（校区、面交点、历史成交样本、滞销演示商品）
-- 幂等性：本脚本可重复执行——加列用 information_schema 判断，
--         种子全部用「标题/单号存在即跳过」的写法，重复执行不产生重复数据
-- ============================================================

USE `secondhand_market`;

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 结构：新表 campus / meet_point（已存在则跳过）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `campus` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '校区ID',
    `name`             VARCHAR(50)   NOT NULL                COMMENT '校区名称',
    `center_longitude` DECIMAL(10,6) NOT NULL                COMMENT '中心经度（附近商品默认圆心）',
    `center_latitude`  DECIMAL(10,6) NOT NULL                COMMENT '中心纬度',
    `sort`             INT           NOT NULL DEFAULT 0      COMMENT '排序值（越小越靠前）',
    `status`           TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1启用 0禁用',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '校区表';

CREATE TABLE IF NOT EXISTS `meet_point` (
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
-- 2. 结构：product 增加校区列（information_schema 判断，已存在则跳过）
-- ------------------------------------------------------------
SET @ddl := (
    IF (EXISTS (SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'secondhand_market'
                  AND TABLE_NAME = 'product'
                  AND COLUMN_NAME = 'campus_id'),
        'SELECT 1',
        'ALTER TABLE `product` ADD COLUMN `campus_id` BIGINT DEFAULT NULL COMMENT ''所属校区ID（校园筛选与面交点推荐，关联 campus 表，可为空）'' AFTER `latitude`, ADD KEY `idx_campus_id` (`campus_id`)'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 3. 种子：校区与面交点（已存在即跳过）
-- ------------------------------------------------------------
INSERT INTO `campus` (`id`, `name`, `center_longitude`, `center_latitude`, `sort`, `status`)
    SELECT 1, '江畔大学·主校区', 114.352000, 30.538000, 1, 1
    WHERE NOT EXISTS (SELECT 1 FROM `campus` WHERE `id` = 1);
INSERT INTO `campus` (`id`, `name`, `center_longitude`, `center_latitude`, `sort`, `status`)
    SELECT 2, '江畔大学·东校区', 114.365000, 30.545000, 2, 1
    WHERE NOT EXISTS (SELECT 1 FROM `campus` WHERE `id` = 2);

INSERT INTO `meet_point` (`id`, `campus_id`, `name`, `longitude`, `latitude`, `description`, `sort`)
    SELECT 1, 1, '东门快递驿站', 114.355000, 30.539000, '主校区东门内侧快递集中取件点，人流大，适合大件面交', 1
    WHERE NOT EXISTS (SELECT 1 FROM `meet_point` WHERE `id` = 1);
INSERT INTO `meet_point` (`id`, `campus_id`, `name`, `longitude`, `latitude`, `description`, `sort`)
    SELECT 2, 1, '图书馆前广场', 114.351000, 30.537200, '图书馆正门广场，白天人流密集，视野开阔', 2
    WHERE NOT EXISTS (SELECT 1 FROM `meet_point` WHERE `id` = 2);
INSERT INTO `meet_point` (`id`, `campus_id`, `name`, `longitude`, `latitude`, `description`, `sort`)
    SELECT 3, 1, '南门校门口', 114.349500, 30.534800, '南门保安亭旁，白天有保安值班', 3
    WHERE NOT EXISTS (SELECT 1 FROM `meet_point` WHERE `id` = 3);
INSERT INTO `meet_point` (`id`, `campus_id`, `name`, `longitude`, `latitude`, `description`, `sort`)
    SELECT 4, 1, '体育馆东侧空地', 114.354800, 30.541500, '体育馆东侧空地，课后人流多', 4
    WHERE NOT EXISTS (SELECT 1 FROM `meet_point` WHERE `id` = 4);
INSERT INTO `meet_point` (`id`, `campus_id`, `name`, `longitude`, `latitude`, `description`, `sort`)
    SELECT 5, 2, '东校区教育超市门口', 114.364000, 30.544000, '东校区教育超市门口，营业时间长', 1
    WHERE NOT EXISTS (SELECT 1 FROM `meet_point` WHERE `id` = 5);

-- ------------------------------------------------------------
-- 4. 种子：历史成交样本（行情数据底座，标题存在即跳过）
--    窗口内样本：手机数码·九成新 5 件（中位价 3600） 图书教材·八成新 4 件（中位价 25）
--               电脑办公·九成新 3 件（中位价 220）  运动户外·八成新 3 件（中位价 65）
--    前置依赖：user 表已有 testuser1(id=2) 与 testuser2(id=3)（01-schema 种子自带）
-- ------------------------------------------------------------
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 2, 'iPhone 13 128G 蓝色', '自用一年，电池健康89%，无拆修，配原装盒。', 1, 3900.00, '九成新', '线下', 1,
           '江畔大学主校区东区9号宿舍楼', 114.354200, 30.539600, 2, NOW() - INTERVAL 88 DAY, 36
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = 'iPhone 13 128G 蓝色');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 2, 'iPad Air 4 深空灰 64G', '宿舍吃灰出掉，屏幕无划痕，配保护壳。', 1, 3600.00, '九成新', '线下', 1,
           '江畔大学主校区东区5号宿舍楼', 114.353600, 30.541200, 2, NOW() - INTERVAL 73 DAY, 41
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = 'iPad Air 4 深空灰 64G');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 2, 'MacBook Air 2019 银色', '轻度办公使用，循环次数120次，可当面验机。', 1, 4200.00, '九成新', '线下', 1,
           '江畔大学主校区研究生楼', 114.350800, 30.540500, 2, NOW() - INTERVAL 58 DAY, 55
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = 'MacBook Air 2019 银色');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 2, '荣耀平板7 10.1寸', '网课神器，成色好，配件齐全。', 1, 2900.00, '九成新', '线下', 1,
           '江畔大学主校区东区5号宿舍楼', 114.353600, 30.541200, 2, NOW() - INTERVAL 43 DAY, 22
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '荣耀平板7 10.1寸');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 2, '小米平板5 黑色 128G', '买来看网课的，现在用不上了，95新。', 1, 3300.00, '九成新', '线下', 1,
           '江畔大学主校区东区9号宿舍楼', 114.356500, 30.540200, 2, NOW() - INTERVAL 28 DAY, 18
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '小米平板5 黑色 128G');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `status`, `create_time`, `view_count`)
    SELECT 3, '高等数学教材套装（上下册）', '同济第七版，无笔记无划线。', 3, 30.00, '八成新', '线上', 1,
           2, NOW() - INTERVAL 83 DAY, 14
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '高等数学教材套装（上下册）');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `status`, `create_time`, `view_count`)
    SELECT 3, '考研英语一真题（近十年）', '做完一套，其余全新，含解析。', 3, 22.00, '八成新', '线上', 1,
           2, NOW() - INTERVAL 63 DAY, 9
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '考研英语一真题（近十年）');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `status`, `create_time`, `view_count`)
    SELECT 3, '数据结构与算法分析教材', '课程结束出掉，内页干净。', 3, 28.00, '八成新', '线上', 1,
           2, NOW() - INTERVAL 48 DAY, 7
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '数据结构与算法分析教材');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `status`, `create_time`, `view_count`)
    SELECT 3, '大学英语综合教程 1-4 册', '全新未拆封两册，其余九成新。', 3, 15.00, '八成新', '线上', 1,
           2, NOW() - INTERVAL 33 DAY, 5
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '大学英语综合教程 1-4 册');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 3, '罗技K845机械键盘', '茶轴，宿舍使用半年，无维修。', 2, 220.00, '九成新', '线下', 1,
           '江畔大学主校区东区7号宿舍楼', 114.352900, 30.542300, 2, NOW() - INTERVAL 78 DAY, 27
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '罗技K845机械键盘');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 3, 'AOC 27寸显示器', '实验室淘汰，面板完美，带原装支架。', 2, 680.00, '九成新', '线下', 1,
           '江畔大学主校区研究生楼', 114.350800, 30.540500, 2, NOW() - INTERVAL 53 DAY, 33
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = 'AOC 27寸显示器');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 3, '罗技M330无线鼠标', '静音鼠标，一节电池用到毕业。', 2, 95.00, '九成新', '线下', 1,
           '江畔大学主校区东区7号宿舍楼', 114.352900, 30.542300, 2, NOW() - INTERVAL 38 DAY, 11
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '罗技M330无线鼠标');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 2, '斯伯丁篮球 7号球', '室外场用了一学期，气针齐全。', 6, 65.00, '八成新', '线下', 1,
           '江畔大学主校区体育馆', 114.354800, 30.541500, 2, NOW() - INTERVAL 68 DAY, 13
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '斯伯丁篮球 7号球');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 2, '奥义瑜伽垫 8mm', '买来用了两次，已清洗晾干。', 6, 45.00, '八成新', '线下', 1,
           '江畔大学主校区体育馆', 114.354800, 30.541500, 2, NOW() - INTERVAL 23 DAY, 8
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '奥义瑜伽垫 8mm');
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `product_condition`,
                       `trade_type`, `campus_id`, `address`, `longitude`, `latitude`, `status`, `create_time`, `view_count`)
    SELECT 2, '尤尼克斯羽毛球拍 双拍', '双拍带拍包，手胶新换。', 6, 160.00, '八成新', '线下', 1,
           '江畔大学主校区体育馆', 114.354800, 30.541500, 2, NOW() - INTERVAL 18 DAY, 19
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '尤尼克斯羽毛球拍 双拍');

-- 滞销演示商品（挂牌 20 天 + 定价高于行情，用于「降价建议」演示）
INSERT INTO `product` (`user_id`, `title`, `description`, `category_id`, `price`, `original_price`,
                       `product_condition`, `trade_type`, `campus_id`, `address`, `longitude`, `latitude`,
                       `status`, `create_time`, `view_count`)
    SELECT 2, '高等数学教材（第七版）', '同济第七版上下册，上册有少量铅笔笔记可擦除。', 3, 45.00, 68.00, '八成新', '线下', 1,
           '江畔大学主校区东区9号宿舍楼', 114.353500, 30.539500, 1, NOW() - INTERVAL 20 DAY, 6
    WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `title` = '高等数学教材（第七版）');

-- ------------------------------------------------------------
-- 5. 种子：历史成交订单与明细（单号存在即跳过）
-- ------------------------------------------------------------
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0850001', 3, 3900.00, 1, 4, NOW() - INTERVAL 85 DAY, NOW() - INTERVAL 84 DAY, NOW() - INTERVAL 85 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0850001');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0700002', 3, 3600.00, 1, 4, NOW() - INTERVAL 70 DAY, NOW() - INTERVAL 69 DAY, NOW() - INTERVAL 70 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0700002');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0550003', 3, 4200.00, 1, 4, NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 54 DAY, NOW() - INTERVAL 55 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0550003');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0400004', 3, 2900.00, 1, 2, NOW() - INTERVAL 40 DAY, NULL, NOW() - INTERVAL 40 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0400004');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0250005', 3, 3300.00, 1, 4, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 24 DAY, NOW() - INTERVAL 25 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0250005');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0800006', 2, 30.00, 1, 4, NOW() - INTERVAL 80 DAY, NOW() - INTERVAL 79 DAY, NOW() - INTERVAL 80 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0800006');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0600007', 2, 22.00, 1, 4, NOW() - INTERVAL 60 DAY, NOW() - INTERVAL 59 DAY, NOW() - INTERVAL 60 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0600007');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0450008', 2, 28.00, 1, 4, NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 44 DAY, NOW() - INTERVAL 45 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0450008');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0300009', 2, 15.00, 1, 2, NOW() - INTERVAL 30 DAY, NULL, NOW() - INTERVAL 30 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0300009');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0750010', 2, 220.00, 1, 4, NOW() - INTERVAL 75 DAY, NOW() - INTERVAL 74 DAY, NOW() - INTERVAL 75 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0750010');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0500011', 2, 680.00, 1, 4, NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 49 DAY, NOW() - INTERVAL 50 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0500011');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0350012', 2, 95.00, 1, 4, NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 34 DAY, NOW() - INTERVAL 35 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0350012');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0650013', 3, 65.00, 1, 4, NOW() - INTERVAL 65 DAY, NOW() - INTERVAL 64 DAY, NOW() - INTERVAL 65 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0650013');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0200014', 3, 45.00, 1, 4, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 19 DAY, NOW() - INTERVAL 20 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0200014');
INSERT INTO `product_order` (`order_no`, `buyer_id`, `total_amount`, `item_count`, `status`, `pay_time`, `finish_time`, `create_time`)
    SELECT 'SEED0150015', 3, 160.00, 1, 2, NOW() - INTERVAL 15 DAY, NULL, NOW() - INTERVAL 15 DAY - INTERVAL 2 HOUR
    WHERE NOT EXISTS (SELECT 1 FROM `product_order` WHERE `order_no` = 'SEED0150015');

-- 订单明细：按标题反查商品 ID，与订单号配对写入（存在即跳过）
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = 'iPhone 13 128G 蓝色'
    WHERE o.order_no = 'SEED0850001'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = 'iPad Air 4 深空灰 64G'
    WHERE o.order_no = 'SEED0700002'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = 'MacBook Air 2019 银色'
    WHERE o.order_no = 'SEED0550003'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '荣耀平板7 10.1寸'
    WHERE o.order_no = 'SEED0400004'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '小米平板5 黑色 128G'
    WHERE o.order_no = 'SEED0250005'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '高等数学教材套装（上下册）'
    WHERE o.order_no = 'SEED0800006'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '考研英语一真题（近十年）'
    WHERE o.order_no = 'SEED0600007'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '数据结构与算法分析教材'
    WHERE o.order_no = 'SEED0450008'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '大学英语综合教程 1-4 册'
    WHERE o.order_no = 'SEED0300009'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '罗技K845机械键盘'
    WHERE o.order_no = 'SEED0750010'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = 'AOC 27寸显示器'
    WHERE o.order_no = 'SEED0500011'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '罗技M330无线鼠标'
    WHERE o.order_no = 'SEED0350012'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '斯伯丁篮球 7号球'
    WHERE o.order_no = 'SEED0650013'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '奥义瑜伽垫 8mm'
    WHERE o.order_no = 'SEED0200014'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);
INSERT INTO `order_item` (`order_id`, `product_id`, `seller_id`, `title`, `price`, `create_time`)
    SELECT o.id, p.id, p.user_id, p.title, p.price, o.create_time
    FROM `product_order` o
    JOIN `product` p ON p.title = '尤尼克斯羽毛球拍 双拍'
    WHERE o.order_no = 'SEED0150015'
      AND NOT EXISTS (SELECT 1 FROM `order_item` oi WHERE oi.order_id = o.id AND oi.product_id = p.id);

-- ------------------------------------------------------------
-- 6. 老库种子商品同步校园场景：把 01-schema 旧版自带的演示商品挂到主校区
--    （新库由 01-schema 直接写入校园坐标；老库原本是北京坐标，这里幂等更新）
-- ------------------------------------------------------------
UPDATE `product`
SET `campus_id` = 1,
    `address` = '江畔大学主校区东区9号宿舍楼',
    `longitude` = 114.356500,
    `latitude` = 30.540200
WHERE `title` = '二手iPhone 12';
UPDATE `product` SET `campus_id` = 1 WHERE `title` = '编程书籍套装';

-- ------------------------------------------------------------
-- 7. 品牌叙事同步（可选）：把已有系统设置里的站点名/描述换成新定位文案。
--    只在 config_json 是合法 JSON 时执行，不合法则跳过（后台「系统设置」里也可手改）
-- ------------------------------------------------------------
UPDATE `system_setting`
SET `config_json` = JSON_SET(`config_json`,
        '$.site.name', '闲置港湾 Haven-Store',
        '$.site.description', '面向校园与同城的闲置循环平台——用真实成交行情定对价，用校区与面交点让面交更省心')
WHERE JSON_VALID(`config_json`);

-- ============================================================
-- 升级完成。回归口径：python tests/api_smoke_test.py 全绿
-- ============================================================
