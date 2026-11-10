-- ============================================================
-- 04-upgrade.sql —— 老库一次性升级：增强搜索与场景推荐（2026-11 第二轮）
-- 内容：4 张新表（search_suggest_word / search_correction / search_log /
--       category_relation）+ 补全词库 / 纠错词表 / 互补品类字典种子
-- 幂等性：CREATE TABLE IF NOT EXISTS + 种子「词存在即跳过」，可重复执行
-- ============================================================

USE `secondhand_market`;

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 表结构
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `search_suggest_word` (
    `id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '词ID',
    `word`      VARCHAR(30)  NOT NULL                COMMENT '词本身',
    `word_type` VARCHAR(20)  NOT NULL DEFAULT 'CATEGORY' COMMENT '词类型：CATEGORY 品类词',
    `tag_label` VARCHAR(20)  DEFAULT NULL            COMMENT '小字标签（所属品类名）',
    `pinyin`    VARCHAR(60)  DEFAULT NULL            COMMENT '拼音（全拼,首字母），供拼音补全',
    `status`    TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
    PRIMARY KEY (`id`),
    KEY `idx_word` (`word`),
    KEY `idx_pinyin` (`pinyin`(20))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '搜索补全词库';

CREATE TABLE IF NOT EXISTS `search_correction` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '纠错项ID',
    `wrong_word`   VARCHAR(30)  NOT NULL                COMMENT '错误写法',
    `correct_word` VARCHAR(30)  NOT NULL                COMMENT '正确写法（实际检索词）',
    `type`         VARCHAR(10)  NOT NULL DEFAULT 'SHAPE' COMMENT '类型：PINYIN 拼音错拼 / SHAPE 形近词',
    `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
    PRIMARY KEY (`id`),
    KEY `idx_wrong_word` (`wrong_word`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '搜索纠错词表（本地词表，不调外部接口）';

CREATE TABLE IF NOT EXISTS `search_log` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '埋点ID',
    `user_id`           BIGINT      DEFAULT NULL            COMMENT '搜索用户ID（游客为空）',
    `keyword`           VARCHAR(50) DEFAULT NULL            COMMENT '原始搜索词',
    `corrected_keyword` VARCHAR(50) DEFAULT NULL            COMMENT '纠错后实际检索词',
    `campus_id`         BIGINT      DEFAULT NULL            COMMENT '搜索时所选校区',
    `result_count`      INT         NOT NULL DEFAULT 0      COMMENT '本次结果条数',
    `clicked_product_id` BIGINT     DEFAULT NULL            COMMENT '点击的结果商品ID（信标回填）',
    `deal_flag`         TINYINT     NOT NULL DEFAULT 0      COMMENT '是否成交：0否 1是（支付后回写）',
    `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    PRIMARY KEY (`id`),
    KEY `idx_keyword` (`keyword`),
    KEY `idx_campus_time` (`campus_id`, `create_time`),
    KEY `idx_clicked_product` (`clicked_product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '搜索埋点（词与ID，不含敏感字段）';

CREATE TABLE IF NOT EXISTS `category_relation` (
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `category_id`         INT         NOT NULL                COMMENT '源品类ID',
    `related_category_id` INT         NOT NULL                COMMENT '互补品类ID',
    `weight`              INT         NOT NULL DEFAULT 0      COMMENT '关联权重（越大越优先）',
    `note`                VARCHAR(100) DEFAULT NULL           COMMENT '关联说明（运营用）',
    `status`              TINYINT     NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_pair` (`category_id`, `related_category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '互补品类关联字典';

-- ------------------------------------------------------------
-- 2. 补全词库种子（word 存在即跳过；pinyin = 全拼,首字母）
-- ------------------------------------------------------------
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '教材', 'CATEGORY', '图书教材', 'jiaocai,jc', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='教材');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '考研真题', 'CATEGORY', '图书教材', 'kaoyanzhenti,kyzt', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='考研真题');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '习题册', 'CATEGORY', '图书教材', 'xitice,xtc', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='习题册');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '笔记本电脑', 'CATEGORY', '电脑办公', 'bijibendiannao,bjdn', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='笔记本电脑');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '显示器', 'CATEGORY', '电脑办公', 'xianshiqi,xsq', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='显示器');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '机械键盘', 'CATEGORY', '电脑办公', 'jixiejianpan,jxjp', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='机械键盘');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '鼠标', 'CATEGORY', '电脑办公', 'shubiao,sb', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='鼠标');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT 'iPhone', 'CATEGORY', '手机数码', 'iphone', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='iPhone');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '平板', 'CATEGORY', '手机数码', 'pingban,pb', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='平板');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '充电宝', 'CATEGORY', '手机数码', 'chongdianbao,cdb', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='充电宝');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '耳机', 'CATEGORY', '手机数码', 'erji,ej', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='耳机');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '自行车', 'CATEGORY', '运动户外', 'zixingche,zxc', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='自行车');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '头盔', 'CATEGORY', '运动户外', 'toukui,tk', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='头盔');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '篮球', 'CATEGORY', '运动户外', 'lanqiu,lq', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='篮球');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '瑜伽垫', 'CATEGORY', '运动户外', 'yujiadian,yjd', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='瑜伽垫');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '羽毛球拍', 'CATEGORY', '运动户外', 'yumaoqiupai,ymqp', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='羽毛球拍');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '台灯', 'CATEGORY', '家用电器', 'taideng,td', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='台灯');
INSERT INTO `search_suggest_word` (`word`, `word_type`, `tag_label`, `pinyin`, `status`)
    SELECT '行李箱', 'CATEGORY', '服饰鞋包', 'xinglixi,xlx', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_suggest_word` WHERE `word`='行李箱');

-- ------------------------------------------------------------
-- 3. 纠错词表种子（wrong_word 存在即跳过）
-- ------------------------------------------------------------
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT 'iphon', 'iPhone', 'PINYIN', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='iphon');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT 'ipone', 'iPhone', 'PINYIN', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='ipone');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT '自形车', '自行车', 'SHAPE', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='自形车');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT '瑜珈垫', '瑜伽垫', 'SHAPE', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='瑜珈垫');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT '蓝芽', '蓝牙', 'SHAPE', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='蓝芽');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT '教才', '教材', 'SHAPE', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='教才');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT '显视器', '显示器', 'SHAPE', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='显视器');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT '键盤', '键盘', 'SHAPE', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='键盤');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT 'jiaocai', '教材', 'PINYIN', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='jiaocai');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT 'pingban', '平板', 'PINYIN', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='pingban');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT 'zixingche', '自行车', 'PINYIN', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='zixingche');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT 'shubiao', '鼠标', 'PINYIN', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='shubiao');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT 'lanqiu', '篮球', 'PINYIN', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='lanqiu');
INSERT INTO `search_correction` (`wrong_word`, `correct_word`, `type`, `status`)
    SELECT 'yujiadian', '瑜伽垫', 'PINYIN', 1 WHERE NOT EXISTS (SELECT 1 FROM `search_correction` WHERE `wrong_word`='yujiadian');

-- ------------------------------------------------------------
-- 4. 互补品类字典种子（品类对唯一，存在即跳过）
--    品类：1 手机数码 2 电脑办公 3 图书教材 4 家用电器 5 服饰鞋包 6 运动户外 7 美妆个护 8 其他闲置
-- ------------------------------------------------------------
INSERT INTO `category_relation` (`category_id`, `related_category_id`, `weight`, `note`, `status`)
    SELECT 3, 3, 10, '教材与配套习题册/真题常一起交易', 1
    WHERE NOT EXISTS (SELECT 1 FROM `category_relation` WHERE `category_id`=3 AND `related_category_id`=3);
INSERT INTO `category_relation` (`category_id`, `related_category_id`, `weight`, `note`, `status`)
    SELECT 6, 6, 10, '球拍与球/护具常一起交易', 1
    WHERE NOT EXISTS (SELECT 1 FROM `category_relation` WHERE `category_id`=6 AND `related_category_id`=6);
INSERT INTO `category_relation` (`category_id`, `related_category_id`, `weight`, `note`, `status`)
    SELECT 1, 2, 8, '数码配件与电脑外设常一起交易', 1
    WHERE NOT EXISTS (SELECT 1 FROM `category_relation` WHERE `category_id`=1 AND `related_category_id`=2);
INSERT INTO `category_relation` (`category_id`, `related_category_id`, `weight`, `note`, `status`)
    SELECT 2, 1, 6, '电脑外设与数码配件常一起交易', 1
    WHERE NOT EXISTS (SELECT 1 FROM `category_relation` WHERE `category_id`=2 AND `related_category_id`=1);
INSERT INTO `category_relation` (`category_id`, `related_category_id`, `weight`, `note`, `status`)
    SELECT 1, 1, 5, '手机与充电宝/手机壳常一起交易', 1
    WHERE NOT EXISTS (SELECT 1 FROM `category_relation` WHERE `category_id`=1 AND `related_category_id`=1);
INSERT INTO `category_relation` (`category_id`, `related_category_id`, `weight`, `note`, `status`)
    SELECT 4, 2, 4, '宿舍电器与电脑外设常一起买', 1
    WHERE NOT EXISTS (SELECT 1 FROM `category_relation` WHERE `category_id`=4 AND `related_category_id`=2);

-- ============================================================
-- 升级完成。回归口径：python tests/api_smoke_test.py 全绿
-- ============================================================
