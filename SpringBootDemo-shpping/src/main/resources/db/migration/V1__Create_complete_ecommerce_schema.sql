-- =============================================
-- 完整电商系统数据库初始化
-- 版本: 1
-- 描述: 创建所有核心业务表结构
-- =============================================

-- 用户相关表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account VARCHAR(50) UNIQUE NOT NULL COMMENT '用户账号',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    status TINYINT(1) DEFAULT 1 COMMENT '账号状态',
    role VARCHAR(20) DEFAULT 'ROLE_USER' COMMENT '用户角色: ROLE_USER, ROLE_SELLER, ROLE_ADMIN',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_account (account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户信息表
CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    avatar VARCHAR(500) COMMENT '头像URL',
    nickname VARCHAR(100) COMMENT '昵称',
    gender VARCHAR(10) DEFAULT 'UNKNOWN' COMMENT '性别: MALE(男), FEMALE(女), UNKNOWN(未知)',
    birthday DATE COMMENT '生日',
    region VARCHAR(100) COMMENT '地区',
    bio VARCHAR(200) COMMENT '个人简介',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    email_verified TINYINT(1) DEFAULT 0 COMMENT '邮箱验证状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料表';

-- 商家信息表
CREATE TABLE seller_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    store_name VARCHAR(200) COMMENT '店铺名称',
    store_avatar VARCHAR(500) COMMENT '店铺头像',
    store_banner VARCHAR(500) COMMENT '店铺横幅',
    store_detail TEXT COMMENT '店铺简介',
    business_hours VARCHAR(100) COMMENT '营业时间',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家资料表';

-- 商品分类表
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父分类ID，NULL表示一级分类',
    sort_order INT DEFAULT 0 COMMENT '排序序号，数字越小越靠前',
    is_active TINYINT(1) DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_parent_id (parent_id),
    INDEX idx_is_active (is_active),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品表
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL COMMENT '分类ID',
    seller_id BIGINT NOT NULL COMMENT '商家用户ID',
    name VARCHAR(255) NOT NULL COMMENT '商品名称',
    brand VARCHAR(100) COMMENT '品牌',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) COMMENT '商品原价',
    stock INT DEFAULT 0 COMMENT '库存',
    sales_count INT DEFAULT 0 COMMENT '已售数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status TINYINT(1) DEFAULT 1 NOT NULL COMMENT '商品状态：0-下架，1-上架',

    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 商品图片
CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    image VARCHAR(500) NOT NULL COMMENT '图片URL',
    sort_order INT DEFAULT 0 COMMENT '图片排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

-- =============================================
-- 订单状态枚举说明
-- =============================================
-- PENDING    : 待付款 - 订单已创建，等待用户支付
-- PAID       : 已付款 - 用户已支付，等待商家处理
-- PROCESSING : 处理中 - 商家正在备货/打包
-- SHIPPED    : 已发货 - 商品已发出，等待收货
-- DELIVERED  : 已送达 - 商品已签收
-- COMPLETED  : 已完成 - 订单完成（可评价）
-- CANCELLED  : 已取消 - 订单被取消（未支付时）
-- REFUNDED   : 已退款 - 订单已退款（支付后取消
-- 订单表（完整版）
CREATE TABLE IF NOT EXISTS orders (
    -- 主键
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',

    -- 订单基本信息
    order_number VARCHAR(64) UNIQUE NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '买家ID',

    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',

    -- 订单状态
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '订单状态: PENDING-待付款, PAID-已付款, PROCESSING-处理中, SHIPPED-已发货, DELIVERED-已送达, COMPLETED-已完成, CANCELLED-已取消, REFUNDED-已退款',

    -- 收货地址信息
    address_id BIGINT NOT NULL COMMENT '收货地址ID',

    -- 支付信息
    payment_method VARCHAR(50) COMMENT '支付方式: ALIPAY-支付宝, WECHAT-微信',
    transaction_id VARCHAR(100) COMMENT '支付交易编号',
    paid_at DATETIME NULL COMMENT '支付时间',

    -- 物流信息
    tracking_number VARCHAR(100) COMMENT '物流单号',
    logistics_code VARCHAR(20) COMMENT '物流公司代码（SF、YTO、ZTO、EMS）',
    logistics_name VARCHAR(50) COMMENT '物流公司名称（顺丰速运、圆通速递）',
    shipped_at DATETIME NULL COMMENT '发货时间',
    delivered_at DATETIME NULL COMMENT '送达时间（物流签收/用户确认）',
    completed_at DATETIME NULL COMMENT '完成时间（用户确认收货/自动完成）',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 软删除
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '软删除标记: 0-未删除, 1-已删除',

    -- 索引优化
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE RESTRICT,

    INDEX idx_user_id (user_id),
    INDEX idx_address_id (address_id),
    INDEX idx_order_number (order_number),
    INDEX idx_status (status),
    INDEX idx_shipped_at (shipped_at)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单项表
CREATE TABLE IF NOT EXISTS order_items (
    -- 主键
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单项ID',

    -- 关联信息
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    seller_id BIGINT NOT NULL COMMENT '商家ID',

    -- 商品快照（防止商品信息变更后订单数据错误）
    product_name VARCHAR(255) NOT NULL COMMENT '商品名称（冗余快照）',
    product_image VARCHAR(500) COMMENT '商品图片（冗余快照）',

    -- 数量与价格
    quantity INT NOT NULL COMMENT '购买数量',
    price DECIMAL(10,2) NOT NULL COMMENT '下单时单价',
    total_price DECIMAL(10,2) NOT NULL COMMENT '小计金额 = price * quantity',

    -- 评论状态
    is_reviewed TINYINT(1) DEFAULT 0 COMMENT '是否已评论: 0-未评论, 1-已评论',
    reviewed_at DATETIME NULL COMMENT '评论时间',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 外键约束
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,

    -- 索引优化
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_is_reviewed (is_reviewed)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

-- 退款记录表（独立）
CREATE TABLE IF NOT EXISTS order_refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '退款ID',

    -- 关联订单
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_number VARCHAR(64) NOT NULL COMMENT '订单号（冗余，便于查询）',
    user_id BIGINT NOT NULL COMMENT '用户ID',

    -- 退款信息
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    refund_status VARCHAR(20) DEFAULT 'PROCESSING' COMMENT '退款状态: PROCESSING-退款中, SUCCESS-退款成功, FAILED-退款失败',
    refund_reason VARCHAR(500) COMMENT '退款原因',
    refund_transaction_id VARCHAR(100) COMMENT '退款交易编号（支付宝/微信退款单号）',

    -- 审核信息
    apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    review_time DATETIME NULL COMMENT '审核时间',
    complete_time DATETIME NULL COMMENT '退款完成时间',
    review_notes VARCHAR(500) COMMENT '审核备注',
    reviewed_by BIGINT COMMENT '审核人ID',

    -- 索引
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    INDEX idx_order_id (order_id),
    INDEX idx_order_number (order_number),
    INDEX idx_user_id (user_id),
    INDEX idx_refund_status (refund_status),
    INDEX idx_apply_time (apply_time)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款记录表';

-- 购物车项
CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 收藏表
CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- 用户地址表
CREATE TABLE addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    recipient_name VARCHAR(100) NOT NULL COMMENT '收件人姓名',
    recipient_phone VARCHAR(20) NOT NULL COMMENT '收件人电话',
    province VARCHAR(50) NOT NULL COMMENT '省份',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    district VARCHAR(50) NOT NULL COMMENT '区县',
    detail_address VARCHAR(500) NOT NULL COMMENT '详细地址',
    label VARCHAR(50) COMMENT '地址标签（家、公司、学校）',
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 商品评论
CREATE TABLE reviews (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     user_id BIGINT NOT NULL COMMENT '用户ID',
     product_id BIGINT NOT NULL COMMENT '商品ID',
     order_id BIGINT NOT NULL COMMENT '订单ID',
     rating INT NOT NULL COMMENT '评分: 1-5',
     comment TEXT COMMENT '评论内容',
     created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',

     FOREIGN KEY (user_id) REFERENCES users(id),
     FOREIGN KEY (product_id) REFERENCES products(id),
     FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- 评论图片表
CREATE TABLE review_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL COMMENT '评论ID',
    image VARCHAR(500) NOT NULL COMMENT '图片URL',
    sort_order INT DEFAULT 0 COMMENT '图片排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='评论图片表';

-- 商家申请表（添加邮箱字段）
CREATE TABLE merchant_apply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '申请人用户ID',
    contact_name VARCHAR(50) NOT NULL COMMENT '联系人姓名',
    contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    contact_email VARCHAR(100) NOT NULL COMMENT '联系邮箱',
    store_name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    store_detail TEXT COMMENT '店铺详细描述',
    business_type VARCHAR(20) NOT NULL COMMENT '经营类型',
    main_category VARCHAR(20) NOT NULL COMMENT '主营类目',
    business_license VARCHAR(500) COMMENT '营业执照文件路径',
    id_card_front VARCHAR(500) COMMENT '身份证正面照片路径',
    id_card_back VARCHAR(500) COMMENT '身份证反面照片路径',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '申请状态',
    review_notes TEXT COMMENT '审核备注',
    reviewed_by BIGINT COMMENT '审核人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    reviewed_at DATETIME COMMENT '审核时间'
) COMMENT='商家申请表';