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
    store_detail TEXT COMMENT '店铺简介',
    address VARCHAR(500) COMMENT '店铺详细地址',
    business_hours VARCHAR(100) COMMENT '营业时间',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家资料表';

-- 商品分类表
CREATE TABLE categories (
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
    detail_html TEXT COMMENT '商品详情（富文本HTML）',
    weight DECIMAL(10,3) COMMENT '商品重量（kg）',
    is_free_shipping TINYINT(1) DEFAULT 0 COMMENT '是否包邮',
    service_guarantee VARCHAR(500) COMMENT '服务保障标签（逗号分隔）',
    delivery_city VARCHAR(100) COMMENT '发货城市',
    sales_count INT DEFAULT 0 COMMENT '已售数量',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    status TINYINT(1) DEFAULT 1 NOT NULL COMMENT '商品状态：0-下架，1-上架',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 商品SKU表
CREATE TABLE product_skus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'SKU ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_name VARCHAR(200) NOT NULL COMMENT 'SKU名称，如"红色-128GB"或"单品"',
    spec_info JSON COMMENT '规格信息，如{"颜色":"红色","内存":"128GB"}或{"默认":"单品"}',
    price DECIMAL(10,2) NOT NULL COMMENT 'SKU价格',
    original_price DECIMAL(10,2) COMMENT 'SKU原价',
    stock INT NOT NULL DEFAULT 0 COMMENT 'SKU库存',
    sku_image VARCHAR(500) COMMENT 'SKU主图',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- 商品参数表
CREATE TABLE product_params (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    param_name VARCHAR(100) NOT NULL COMMENT '参数名（如：屏幕尺寸）',
    param_value VARCHAR(500) NOT NULL COMMENT '参数值（如：6.7英寸）',
    sort_order INT DEFAULT 0 COMMENT '排序',

    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品参数表';

-- 商品图片
CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    image VARCHAR(500) NOT NULL COMMENT '图片URL',
    sort_order INT DEFAULT 0 COMMENT '图片排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

-- 订单表（完整版）
CREATE TABLE orders (
    -- 主键
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    -- 订单基本信息
    order_number VARCHAR(64) UNIQUE NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '买家ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    -- 订单状态
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '订单状态: PENDING-待付款, PAID-已付款, PROCESSING-处理中, SHIPPED-已发货, DELIVERED-已送达, COMPLETED-已完成, CANCELLED-已取消, REFUNDED-已退款',
    source VARCHAR(20) DEFAULT 'cart' COMMENT '订单来源: cart-购物车, product-直接购买',
    -- 收货地址信息
    address_id BIGINT NOT NULL COMMENT '收货地址ID',
    -- 支付信息
    payment_method VARCHAR(50) COMMENT '支付方式: ALIPAY-支付宝, WECHAT-微信',
    transaction_id VARCHAR(100) COMMENT '支付交易编号',
    paid_at DATETIME NULL COMMENT '支付时间',
    -- 处理信息
    processing_at DATETIME NULL COMMENT '处理时间（商家接单）',
    -- 物流信息
    tracking_number VARCHAR(100) COMMENT '物流单号',
    logistics_code VARCHAR(20) COMMENT '物流公司代码（SF、YTO、ZTO、EMS）',
    logistics_name VARCHAR(50) COMMENT '物流公司名称（顺丰速运、圆通速递）',
    shipped_at DATETIME NULL COMMENT '发货时间',
    delivered_at DATETIME NULL COMMENT '送达时间（物流签收/用户确认）',
    -- 完成与取消
    completed_at DATETIME NULL COMMENT '完成时间（用户确认收货/自动完成）',
    cancelled_at DATETIME NULL COMMENT '取消时间',
    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 软删除
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '软删除标记: 0-未删除, 1-已删除',

    -- 索引优化
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_user_id (user_id),
    INDEX idx_address_id (address_id),
    INDEX idx_order_number (order_number),
    INDEX idx_status (status),
    INDEX idx_shipped_at (shipped_at)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单项表
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单项ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NULL COMMENT 'SKU ID',
    seller_id BIGINT NOT NULL COMMENT '商家ID',
    product_name VARCHAR(255) NOT NULL COMMENT '商品名称（冗余快照）',
    product_image VARCHAR(500) COMMENT '商品图片（冗余快照）',
    sku_name VARCHAR(200) NULL COMMENT 'SKU规格名称（快照）',
    quantity INT NOT NULL COMMENT '购买数量',
    price DECIMAL(10,2) NOT NULL COMMENT '下单时单价',
    total_price DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    is_reviewed TINYINT(1) DEFAULT 0 COMMENT '是否已评论',
    reviewed_at DATETIME NULL COMMENT '评论时间',
    refund_status VARCHAR(20) NULL COMMENT '售后状态: REFUNDING-退款中, AFTER_SALE-售后退款中, WAITING_RETURN-待退货, COMPLETED-已退款',
    refund_id BIGINT NULL COMMENT '退款记录ID（关联order_refunds表）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    FOREIGN KEY (sku_id) REFERENCES product_skus(id) ON DELETE SET NULL,
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_is_reviewed (is_reviewed),
    INDEX idx_refund_id (refund_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

-- 商品冻结记录表
CREATE TABLE product_freeze_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '冻结记录ID',
    seller_id BIGINT NOT NULL COMMENT '商家ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    freeze_reason VARCHAR(100) COMMENT '冻结原因（套餐到期等）',
    freeze_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '冻结时间',
    unfreeze_time DATETIME NULL COMMENT '解冻时间',
    unfreeze_reason VARCHAR(100) COMMENT '解冻原因（续费等）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_seller_id (seller_id),
    INDEX idx_product_id (product_id),
    INDEX idx_freeze_time (freeze_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品冻结记录表';

-- 退款记录表
CREATE TABLE order_refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '退款ID',
    -- 关联订单
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_item_id BIGINT NULL COMMENT '订单项ID（支持订单项级别的退款）',
    order_number VARCHAR(64) NOT NULL COMMENT '订单号（冗余，便于查询）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    -- 退款信息
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    refund_reason VARCHAR(500) COMMENT '退款原因',
    -- 退货信息（售后场景）
    return_method VARCHAR(20) DEFAULT NULL COMMENT '退货方式: PICKUP-上门取件, SELF-自寄',
    return_tracking_number VARCHAR(100) COMMENT '退货物流单号',
    return_logistics_name VARCHAR(50) COMMENT '退货物流公司',
    return_status VARCHAR(20) DEFAULT NULL COMMENT '退货状态: NULL-未退货, RETURNING-退货中, RECEIVED-已收货',
    seller_address_id BIGINT NULL COMMENT '商家收货地址ID',
    return_apply_time DATETIME NULL COMMENT '买家提交退货时间',
    return_receive_time DATETIME NULL COMMENT '商家确认收货时间',
    -- 退款状态和类型
    refund_status VARCHAR(20) DEFAULT 'PROCESSING' COMMENT '退款状态: PROCESSING-处理中, APPROVED-已同意, SUCCESS-退款成功, FAILED-已拒绝',
    refund_type VARCHAR(20) DEFAULT 'REFUND' COMMENT '退款类型: REFUND-仅退款, AFTER_SALE-退货退款',
    description VARCHAR(500) COMMENT '退款描述',
    refund_transaction_id VARCHAR(100) COMMENT '退款交易编号（支付宝/微信退款单号）',
    -- 审核信息
    apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    review_time DATETIME NULL COMMENT '审核时间',
    complete_time DATETIME NULL COMMENT '退款完成时间',
    review_notes VARCHAR(500) COMMENT '审核备注',
    reviewed_by BIGINT COMMENT '审核人ID',
    -- 沟通信息
    communication_round INT DEFAULT 1 COMMENT '沟通轮次（最多3轮）',

    -- 索引
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    INDEX idx_order_id (order_id),
    INDEX idx_order_item_id (order_item_id),
    INDEX idx_order_number (order_number),
    INDEX idx_user_id (user_id),
    INDEX idx_refund_status (refund_status),
    INDEX idx_refund_type (refund_type),
    INDEX idx_return_status (return_status),
    INDEX idx_apply_time (apply_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款记录表';

-- 退款凭证图片表
CREATE TABLE refund_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '图片ID',
    refund_id BIGINT NOT NULL COMMENT '退款记录ID',
    communication_id BIGINT NULL COMMENT '关联的消息ID',
    image VARCHAR(500) NOT NULL COMMENT '图片URL',
    image_type VARCHAR(20) DEFAULT 'EVIDENCE' COMMENT '图片类型: EVIDENCE-凭证, APPEAL-申诉, CHAT-聊天',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (refund_id) REFERENCES order_refunds(id) ON DELETE CASCADE,
    INDEX idx_refund_id (refund_id),
    INDEX idx_communication_id (communication_id),
    INDEX idx_image_type (image_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款凭证图片表';

-- 退款凭证视频表
CREATE TABLE refund_videos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '视频ID',
    refund_id BIGINT NOT NULL COMMENT '退款记录ID',
    video_url VARCHAR(500) NOT NULL COMMENT '视频URL',
    cover_url VARCHAR(500) COMMENT '封面图URL',
    duration INT COMMENT '视频时长（秒）',
    size BIGINT COMMENT '文件大小（字节）',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (refund_id) REFERENCES order_refunds(id) ON DELETE CASCADE,
    INDEX idx_refund_id (refund_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款凭证视频表';

-- 沟通记录表
CREATE TABLE refund_communications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '沟通记录ID',
    refund_id BIGINT NOT NULL COMMENT '退款记录ID',
    sender_type VARCHAR(20) NOT NULL COMMENT '发送者类型: BUYER, SELLER',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    content VARCHAR(1000) NOT NULL COMMENT '沟通内容',
    round INT DEFAULT 1 COMMENT '第几轮沟通',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (refund_id) REFERENCES order_refunds(id) ON DELETE CASCADE,
    INDEX idx_refund_id (refund_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款沟通记录表';

-- 购物车项
CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NULL COMMENT 'SKU ID（可选，关联 product_skus 表）',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (sku_id) REFERENCES product_skus(id) ON DELETE SET NULL,
    UNIQUE KEY uk_user_product_sku (user_id, product_id, sku_id),
    INDEX idx_sku_id (sku_id),
    INDEX idx_product_sku (product_id, sku_id)
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
    order_item_id BIGINT NULL COMMENT '订单项ID',
    rating INT NOT NULL COMMENT '评分: 1-5',
    comment TEXT COMMENT '评论内容',
    is_anonymous TINYINT(1) DEFAULT 0 COMMENT '是否匿名评价',
    sku_spec VARCHAR(200) COMMENT '购买规格快照',
    ip VARCHAR(50) COMMENT '评论IP地址',
    location VARCHAR(100) COMMENT 'IP属地（如广东广州）',
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

-- 评论视频表
CREATE TABLE review_videos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '视频ID',
    review_id BIGINT NOT NULL COMMENT '评论ID',
    video_url VARCHAR(500) NOT NULL COMMENT '视频URL',
    cover_url VARCHAR(500) COMMENT '视频封面图URL',
    duration INT COMMENT '视频时长（秒）',
    size BIGINT COMMENT '文件大小（字节）',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
    INDEX idx_review_id (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论视频表';

-- 商家申请表
CREATE TABLE merchant_apply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '申请人用户ID',
    contact_name VARCHAR(50) NOT NULL COMMENT '联系人姓名',
    contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    contact_email VARCHAR(100) NOT NULL COMMENT '联系邮箱',
    store_name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    store_detail TEXT COMMENT '店铺详细描述',
    address VARCHAR(500) NULL COMMENT '详细地址',
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
    reviewed_at DATETIME COMMENT '审核时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
) COMMENT='商家申请表';

-- 关注表
CREATE TABLE IF NOT EXISTS follow_sellers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关注ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    seller_id BIGINT NOT NULL COMMENT '商家用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_seller (user_id, seller_id),
    INDEX idx_user_id (user_id),
    INDEX idx_seller_id (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注商家表';

-- 消息通知表
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '接收者用户ID',
    sender_id BIGINT COMMENT '发送者用户ID',
    type VARCHAR(30) NOT NULL COMMENT 'ORDER/REFUND/SHIPMENT/CHAT/SYSTEM',
    title VARCHAR(200) NOT NULL COMMENT '消息标题',
    content VARCHAR(1000) COMMENT '消息内容',
    extra_data JSON COMMENT '扩展数据（orderId、trackingNumber、amount、isDeleted等）',
    is_read TINYINT DEFAULT 0 COMMENT '0-未读, 1-已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_read (user_id, is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- =============================================
-- 商家套餐相关表
-- =============================================

-- 商家套餐表（定义各种套餐）
CREATE TABLE seller_packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '套餐ID',
    name VARCHAR(50) NOT NULL COMMENT '套餐名称',
    description VARCHAR(500) COMMENT '套餐描述',
    price DECIMAL(10, 2) NOT NULL COMMENT '套餐价格',
    duration_days INT NOT NULL COMMENT '套餐时长（天）',
    product_limit INT NOT NULL COMMENT '商品数量限制（-1表示无限制）',
    features TEXT COMMENT '套餐功能（JSON格式）',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家套餐表';

-- 商家套餐购买记录表
CREATE TABLE seller_package_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    seller_id BIGINT NOT NULL COMMENT '商家用户ID',
    package_id BIGINT NOT NULL COMMENT '套餐ID',
    package_name VARCHAR(50) NOT NULL COMMENT '套餐名称（冗余）',
    price DECIMAL(10, 2) NOT NULL COMMENT '购买价格',
    start_date DATETIME NULL COMMENT '开始日期（支付成功后设置）',
    end_date DATETIME NULL COMMENT '结束日期（支付成功后设置）',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING(待支付), ACTIVE(生效中), EXPIRED(已到期), CANCELLED(已取消)',
    payment_method VARCHAR(20) COMMENT '支付方式',
    transaction_id VARCHAR(100) COMMENT '交易单号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (package_id) REFERENCES seller_packages(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家套餐购买记录表';

-- 插入默认套餐数据
INSERT INTO seller_packages (name, description, price, duration_days, product_limit, features, sort_order) VALUES
('基础版', '适合新手卖家，最多发布10个商品', 99.00, 30, 10, '{"ads": false, "analytics": true, "priority": 1}', 1),
('标准版', '适合成长型店铺，最多发布50个商品', 299.00, 30, 50, '{"ads": true, "analytics": true, "priority": 3}', 2),
('高级版', '适合成熟商家，最多发布200个商品', 799.00, 30, 200, '{"ads": true, "analytics": true, "priority": 5, "support": true}', 3),
('旗舰版', '适合大型商家，商品数量无限制', 1999.00, 30, -1, '{"ads": true, "analytics": true, "priority": 10, "support": true, "api": true}', 4);
