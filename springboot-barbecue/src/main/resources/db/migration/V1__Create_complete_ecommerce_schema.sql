-- =============================================
-- 完整购物小程序系统数据库初始化
-- 版本: 1
-- 描述: 创建所有核心业务表结构
-- =============================================

-- 用户相关表
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    device_id VARCHAR(50) UNIQUE COMMENT '设备标识符',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商家信息表
CREATE TABLE seller_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_name VARCHAR(50) COMMENT '店铺名称',
    slogan VARCHAR(50) COMMENT '店铺标语',
    store_detail TEXT COMMENT '店铺详情',
    store_avatar VARCHAR(100) COMMENT '头像URL',
    phone varchar(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '店铺地址',
    is_open TINYINT(1) DEFAULT 1 COMMENT '手动开关: 1-营业 0-打烊',
    business varchar(50) COMMENT '营业时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家资料表';

-- 商品表
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) NOT NULL COMMENT '商品原价',
    image VARCHAR(100) COMMENT '商品图片',
    category VARCHAR(100) NOT NULL COMMENT '分类',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 订单相关表
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_number VARCHAR(64) UNIQUE NOT NULL COMMENT '订单号',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    delivery_type VARCHAR(50) COMMENT '配送类型：dinein/takeaway/delivery',

    -- 配送相关字段
    recipient_name VARCHAR(50) COMMENT '收货人姓名',
    recipient_phone VARCHAR(20) COMMENT '收货人电话',
    detail_address VARCHAR(500) COMMENT '详细地址',

    people_count INT DEFAULT 1 COMMENT '用餐人数（到店用餐）',
    table_preference VARCHAR(100) COMMENT '桌号偏好',
    remark VARCHAR(500) COMMENT '订单备注',

    -- 支付信息
    payment_method VARCHAR(50) COMMENT '支付方式',
    transaction_id VARCHAR(100) COMMENT '交易ID',
    paid_at DATETIME NULL COMMENT '支付时间',

    -- 订单状态
    -- PENDING-待支付, PAID-已支付, SHIPPED-已发货, COMPLETED-已完成, CANCELLED-已取消
    -- REFUNDING-退款中, REFUNDED-已退款, REFUNDFAILED-退款失败
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING-待支付, PAID-已支付, SHIPPED-已发货, COMPLETED-已完成, CANCELLED-已取消, REFUNDING-退款中, REFUNDED-已退款, REFUNDFAILED-退款失败',

    -- 退款汇总
    refunded_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '已退款总额（汇总字段）',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_order_number (order_number),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),

    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单项
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称（快照）',
    image VARCHAR(500) COMMENT '商品图片（快照）',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    price DECIMAL(10,2) NOT NULL COMMENT '下单时价格',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- 退款记录表（添加失败原因字段）
CREATE TABLE refund_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL COMMENT '订单号',

    -- 退款金额
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '本次退款金额',

    -- 退款信息
    refund_reason VARCHAR(500) COMMENT '退款原因',
    refund_transaction_id VARCHAR(100) COMMENT '支付宝/微信退款交易号',

    -- 退款状态
    status VARCHAR(20) DEFAULT 'PROCESSING' COMMENT '退款状态：PROCESSING-处理中, SUCCESS-成功, FAILED-失败',

    -- 失败信息
    fail_reason VARCHAR(500) COMMENT '失败原因（退款失败时记录）',

    -- 时间
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    completed_at DATETIME COMMENT '完成时间',

    INDEX idx_order_number (order_number),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';

-- 客服消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT COMMENT '发送者ID（用户ID）',
    receiver_id BIGINT COMMENT '接收者ID（用户ID或商家ID）',
    content TEXT NOT NULL COMMENT '消息内容',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读（针对接收者）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',

    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_conversation (sender_id, receiver_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服消息表';

