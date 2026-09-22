-- H2数据库初始化脚本
-- 创建用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    nickname VARCHAR(100),
    avatar VARCHAR(255),
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    status INT DEFAULT 1 COMMENT '状态：1-正常，2-禁用',
    INDEX idx_username (username),
    INDEX idx_phone (phone)
);

-- 创建商品分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    sort INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_sort (sort)
);

-- 创建商品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    cover_image VARCHAR(255),
    category_id BIGINT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    product_condition VARCHAR(20) NOT NULL COMMENT '商品成色',
    trade_type VARCHAR(20) NOT NULL COMMENT '交易方式',
    address VARCHAR(500),
    longitude DECIMAL(12,9),
    latitude DECIMAL(12,9),
    status INT DEFAULT 1 COMMENT '状态：1-在售，2-已售出，3-已下架',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id),
    INDEX idx_price (price),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (category_id) REFERENCES category(id)
);

-- 插入默认商品分类数据
INSERT INTO category (id, name, sort, create_time, update_time) VALUES
(1, '手机数码', 1, NOW(), NOW()),
(2, '电脑办公', 2, NOW(), NOW()),
(3, '图书教材', 3, NOW(), NOW()),
(4, '家用电器', 4, NOW(), NOW()),
(5, '服饰鞋包', 5, NOW(), NOW()),
(6, '运动户外', 6, NOW(), NOW()),
(7, '美妆个护', 7, NOW(), NOW()),
(8, '其他闲置', 8, NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 插入默认管理员用户
INSERT INTO user (id, username, password, phone, nickname, create_time, update_time) VALUES
(1, 'admin', '$2a$10$rO3mXG7K0J5I9S1U7h8nZOs7xW9G8YXQl6E9V2r4T9N6wD5Y8t2e', '13800138000', '管理员', NOW(), NOW())
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname);

-- 插入测试商品数据
INSERT INTO product (id, user_id, title, description, cover_image, category_id, price, product_condition, trade_type, address, longitude, latitude, status, create_time, update_time) VALUES
(1, 1, 'iPhone 13 Pro 256G 深空灰', '95新，无拆修，原装充电器，发票齐全', '/images/iphone13pro.jpg', 1, 6999.00, '九成新', 'offline', '北京市朝阳区国贸CBD', 116.466240, 39.920800, 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
(2, 1, 'MacBook Pro 13寸 2020款', '性能完好，轻度使用，适合办公学习', '/images/macbookpro.jpg', 2, 8999.00, '八成新', 'online', '', NULL, NULL, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY)
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- 创建用户-商品关联表（用于收藏等）
CREATE TABLE IF NOT EXISTS user_product_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    relation_type VARCHAR(20) NOT NULL COMMENT '关系类型：collect-收藏',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_user_product (user_id, product_id, relation_type),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- 创建评论表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    rating INT DEFAULT 5 COMMENT '评分1-5',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    status INT DEFAULT 1 COMMENT '状态：1-正常，2-隐藏',
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- 创建消息通知表
CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read INT DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    message_type VARCHAR(20) NOT NULL COMMENT '消息类型：comment-评论，collect-收藏，trade-交易',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_from_user_id (from_user_id),
    INDEX idx_to_user_id (to_user_id),
    INDEX idx_is_read (is_read),
    FOREIGN KEY (from_user_id) REFERENCES user(id),
    FOREIGN KEY (to_user_id) REFERENCES user(id)
);

-- 创建索引优化查询
CREATE INDEX idx_product_status_category ON product(status, category_id);
CREATE INDEX idx_product_status_price ON product(status, price);
CREATE INDEX idx_product_create_time ON product(status, create_time);

-- 提交事务
COMMIT;