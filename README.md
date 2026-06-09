<div align="center">

# 云杉购（shopping-mobile）
### B2C 全场景电商平台 · 前后端分离全栈项目

**用户端 / 商家端 / 管理端 三端合一** · JWT 认证 · 支付宝沙箱支付 · Redis 预扣库存 · WebSocket 实时消息 · 商家套餐订阅

</div>

---

## 目录

- [一、项目概述](#一项目概述)
- [二、技术栈详情](#二技术栈详情)
- [三、数据库设计](#三数据库设计)
- [四、核心业务流程](#四核心业务流程)
- [五、WebSocket 消息设计](#五websocket-消息设计)
- [六、RabbitMQ 设计](#六rabbitmq-设计)
- [七、API 接口文档](#七api-接口文档)
- [八、项目部署](#八项目部署)
- [九、目录结构](#九目录结构)
- [十、开发规范](#十开发规范)

---

## 一、项目概述

### 1.1 项目简介

**云杉购（shopping-mobile）** 是一套面向移动端的 B2C 电商平台，采用前后端分离架构。项目覆盖从用户注册、商品浏览、购物车、下单支付、物流跟踪、退款售后，到商家入驻、商品发布、订单处理、套餐订阅、优惠券营销、即时聊天等完整电商闭环；并配备管理端对商家、商品、分类、轮播图进行统一治理。

系统通过 **RabbitMQ + STOMP 消息中继** 实现下单、支付、发货、退款、聊天、套餐到期等关键节点的实时 WebSocket 推送，并通过 **Redis 预扣库存 + 延迟队列兜底** 解决高并发超卖与订单超时问题。

### 1.2 项目架构图（文字描述）

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                              客户端层 (Client)                            │
│   Vue 3 SPA (PC/移动 H5)         Capacitor 打包 (Android APP)             │
└──────────────┬───────────────────────────────┬──────────────────────────┘
               │ HTTP (axios)                   │ WebSocket (STOMP + SockJS)
               ▼                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          网关 / 代理层 (Nginx)                           │
│   /api  /images  /uploads  →  Spring Boot (8088)                         │
│   /ws                        →  Spring Boot (8088, SockJS 端点)         │
└──────────────┬────────────────────────────────────────────┬────────────┘
               │                                              │
               ▼                                              ▼
┌──────────────────────────────────┐   ┌────────────────────────────────────┐
│      Spring Boot 应用层 (8088)    │   │   消息中间件层                      │
│ ┌──────────────────────────────┐ │   │ ┌────────────────────────────────┐ │
│ │ Controller (REST API)        │ │   │ │ RabbitMQ (AMQP 5672)          │ │
│ │ Service (业务逻辑)            │ │   │ │  - order.delay.queue          │ │
│ │ Mapper (MyBatis)             │ │   │ │  - package.expire.exchange     │ │
│ │ Filter (JWT 认证)            │ │   │ │    (x-delayed-message 插件)    │ │
│ │ Schedule (定时兜底)          │ │   │ │ RabbitMQ STOMP (61613)        │ │
│ └──────────────┬───────────────┘ │   │ │  - amq.topic 主题交换机        │ │
│                │                  │   │ └────────────────────────────────┘ │
└────────────────┼──────────────────┘   └────────────────────────────────────┘
                 │
        ┌────────┼─────────┐
        ▼        ▼         ▼
┌──────────┐ ┌────────┐ ┌────────────────┐
│  MySQL   │ │ Redis  │ │  外部服务        │
│ (5.7+)   │ │ (缓存/ │ │ - 支付宝沙箱    │
│ Flyway   │ │ 预扣)  │ │ - 快递鸟 API    │
│ 迁移     │ │        │ │ - 文件存储       │
└──────────┘ └────────┘ └────────────────┘
```

### 1.3 功能模块总览表

| 端 | 模块 | 说明 |
| :-- | :-- | :-- |
| 用户端 | 账号 | 注册 / 登录 / JWT 认证 / 角色切换 / 个人资料 |
| 用户端 | 商品 | 首页轮播 / 分类筛选 / 关键词搜索 / 详情 / 推荐 / 浏览历史 |
| 用户端 | 交易 | 购物车 / 结算 / 下单 / 支付宝沙箱支付 / 订单管理 |
| 用户端 | 售后 | 仅退款 / 退货退款 / 退款沟通聊天 / 物流查询 |
| 用户端 | 互动 | 商品评价（图文视频）/ 收藏 / 关注商家 / 店铺页 |
| 用户端 | 营销 | 优惠券领取 / 我的券 / 订单应用券 |
| 用户端 | 沟通 | 与商家实时聊天（文本/图片/视频/商品卡片/订单卡片）|
| 用户端 | 消息 | 通知中心（订单/系统/物流）+ WebSocket 实时推送 |
| 商家端 | 入驻 | 资质申请 / 管理员审核 / 开通 |
| 商家端 | 商品 | 四步表单发布（基本信息→规格SKU→图片详情→其他信息）/ 编辑 / 上下架 |
| 商家端 | 订单 | 接单（PAID→PROCESSING）/ 发货（PROCESSING→SHIPPED）/ 物流填写 |
| 商家端 | 售后 | 退款审核 / 确认退货 / 退款聊天 |
| 商家端 | 营销 | 优惠券创建 / 启停 / 商家快捷回复 |
| 商家端 | 套餐 | 购买 / 到期提醒 / 冻结商品 / 续费恢复 / 降级 |
| 商家端 | 分析 | 销售数据分析（ECharts）|
| 管理端 | 商家 | 入驻审核 / 商家列表 / 重置密码 / 停用 |
| 管理端 | 用户 | 用户列表 / 状态管理 / 管理员增删改 |
| 管理端 | 商品 | 商品审核 / 上下架 / 删除 |
| 管理端 | 内容 | 分类管理 / 轮播图管理 |
| 公共 | 文件 | 图片/视频上传（富文本编辑器、商品图、聊天媒体）|

---

## 二、技术栈详情

### 2.1 前端技术栈

| 技术 | 版本 | 说明 |
| :-- | :-- | :-- |
| Vue 3 | ^3.5.26 | 渐进式框架，组合式 API |
| TypeScript | ~5.9.3 | 类型安全 |
| Vite | ^7.3.0 | 构建工具 / 开发服务器 |
| Vue Router | ^4.6.4 | 前端路由（模块化：user/seller/admin）|
| Pinia | ^3.0.4 | 状态管理（auth / home / orders）|
| Vant | ^4.9.24 | 移动端 UI 组件库 |
| Element Plus | ^2.13.0 | 管理端 UI 组件库 |
| Axios | ^1.13.2 | HTTP 请求 |
| @stomp/stompjs + sockjs-client | ^7.3 / ^1.6 | WebSocket（STOMP + SockJS）|
| ECharts | ^6.0.0 | 商家数据分析图表 |
| Capacitor | ^8.4.0 | Android 原生打包 |
| html5-qrcode / qr-scanner | ^2.3.8 / ^1.4.2 | 扫码功能 |
| Playwright | ^1.57.0 | E2E 测试 |
| Vitest | ^4.0.16 | 单元测试 |
| ESLint + Prettier | 9.x / 3.7 | 代码规范与格式化 |

### 2.2 后端技术栈

| 技术 | 版本 | 说明 |
| :-- | :-- | :-- |
| Spring Boot | 3.5.0 | 核心框架 |
| Java | 17 | 编译与运行时（pom.xml 配置 `java.version=17`）|
| Spring Security | 6.x | 认证授权 / CSRF / CORS |
| JWT (jjwt) | 0.11.5 | Token 生成与校验 |
| MyBatis | 3.0.3 (starter) | ORM 框架 |
| MySQL | 5.7+ | 关系型数据库 |
| Flyway | 内置 | 数据库版本迁移 |
| Redis (Lettuce) | 内置 | 缓存 / 预扣库存 / 验证码 |
| RabbitMQ | 内置 (spring-boot-starter-amqp) | 延迟队列 / 消息推送 |
| WebSocket (STOMP) | 内置 (starter-websocket) | 实时消息，RabbitMQ STOMP 中继 |
| 支付宝 SDK | alipay-sdk-java 4.38.157 | 沙箱支付 / 退款回调 |
| 快递鸟 API | 外部 | 物流轨迹 / 电子面单 |
| Undertow | 内置 | 替代 Tomcat 的高性能 NIO 容器 |
| JCodec | 0.2.5 | 视频处理 |
| Lombok | 1.18.30 | 实体类简化 |
| Jackson | 内置 | JSON 序列化（Asia/Shanghai 时区）|
| Spring Cache + Validation + Thymeleaf | 内置 | 缓存 / 校验 / 模板 |
| Actuator + Prometheus | 内置 | 监控指标导出 |

---

## 三、数据库设计

数据库 `shopping-mobile`，字符集 `utf8mb4`，引擎 `InnoDB`。表结构由 Flyway 在 `src/main/resources/db/migration/V1__Create_complete_ecommerce_schema.sql` 中初始化，含初始角色、管理员账号与默认套餐数据。

> 下表列出各表的字段、类型、含义、索引与外键。`PK` 主键，`UK` 唯一索引，`IDX` 普通索引，`FK` 外键。

### 3.1 用户与角色

#### users — 用户表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 用户ID | PK |
| account | VARCHAR(50) | 用户账号 | NOT NULL，UNIQUE，IDX(idx_account) |
| password | VARCHAR(255) | 密码（BCrypt） | NOT NULL |
| status | TINYINT(1) | 账号状态（1启用 0禁用） | DEFAULT 1 |
| role | VARCHAR(20) | 角色 ROLE_USER/ROLE_SELLER/ROLE_ADMIN | DEFAULT 'ROLE_USER' |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

#### roles — 角色表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT | 角色ID（1=USER 2=SELLER 3=ADMIN） | PK |
| name | VARCHAR(50) | 角色名称 | NOT NULL，UNIQUE，IDX(idx_name) |
| description | VARCHAR(200) | 角色描述 | - |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

#### user_roles — 用户角色关联表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| user_id | BIGINT | 用户ID | PK(user_id, role_id)，FK→users(id) ON DELETE CASCADE |
| role_id | BIGINT | 角色ID | PK，FK→roles(id) ON DELETE CASCADE |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

### 3.2 用户资料

#### user_profiles — 用户资料表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| user_id | BIGINT | 用户ID | NOT NULL，UK(uk_user_id)，FK→users(id) ON DELETE CASCADE |
| avatar | VARCHAR(500) | 头像URL | - |
| nickname | VARCHAR(100) | 昵称 | - |
| gender | VARCHAR(10) | 性别 MALE/FEMALE/UNKNOWN | DEFAULT 'UNKNOWN' |
| birthday | DATE | 生日 | - |
| region | VARCHAR(100) | 地区 | - |
| bio | VARCHAR(200) | 个人简介 | - |
| phone | VARCHAR(20) | 手机号 | - |
| email | VARCHAR(100) | 邮箱 | - |
| email_verified | TINYINT(1) | 邮箱验证状态 | DEFAULT 0 |
| created_at / updated_at | DATETIME | 创建/更新时间 | 自动维护 |

#### seller_profiles — 商家资料表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| user_id | BIGINT | 用户ID | NOT NULL，UK(uk_user_id)，FK→users(id) ON DELETE CASCADE |
| store_name | VARCHAR(200) | 店铺名称 | - |
| store_avatar | VARCHAR(500) | 店铺头像 | - |
| store_detail | TEXT | 店铺简介 | - |
| address | VARCHAR(500) | 店铺详细地址 | - |
| business_hours | VARCHAR(100) | 营业时间 | - |
| contact_phone | VARCHAR(20) | 联系电话 | - |
| created_at / updated_at | DATETIME | 创建/更新时间 | 自动维护 |

### 3.3 商品模块

#### categories — 商品分类表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 分类ID | PK |
| name | VARCHAR(100) | 分类名称 | NOT NULL |
| parent_id | BIGINT | 父分类ID（NULL=一级） | IDX(idx_parent_id) |
| sort_order | INT | 排序（小靠前） | DEFAULT 0，IDX(idx_sort_order) |
| is_active | TINYINT(1) | 状态 1启用 0禁用 | DEFAULT 1，IDX(idx_is_active) |
| created_at / updated_at | DATETIME | 时间戳 | 自动维护 |

#### products — 商品表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 商品ID | PK |
| category_id | BIGINT | 分类ID | NOT NULL，FK→categories(id)，IDX(idx_products_category_id) |
| seller_id | BIGINT | 商家用户ID | NOT NULL，FK→users(id)，IDX(idx_products_seller_id) |
| name | VARCHAR(255) | 商品名称 | NOT NULL |
| brand | VARCHAR(100) | 品牌 | - |
| description | TEXT | 商品描述 | - |
| detail_html | TEXT | 商品详情（富文本HTML） | - |
| weight | DECIMAL(10,3) | 重量(kg) | - |
| is_free_shipping | TINYINT(1) | 是否包邮 | DEFAULT 0 |
| service_guarantee | VARCHAR(500) | 服务保障标签（逗号分隔） | - |
| delivery_city | VARCHAR(100) | 发货城市 | - |
| sales_count | INT | 已售数量 | DEFAULT 0 |
| view_count | INT | 浏览量 | DEFAULT 0 |
| status | TINYINT(1) | 状态 0下架 1上架 2已删除 | NOT NULL DEFAULT 1，IDX(idx_products_status) |
| created_at / updated_at | DATETIME | 时间戳 | 自动维护 |

#### product_skus — 商品SKU表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | SKU ID | PK |
| product_id | BIGINT | 商品ID | NOT NULL，FK→products(id) ON DELETE CASCADE，IDX(idx_product_id) |
| sku_name | VARCHAR(200) | SKU名称（如"红色-128GB"） | NOT NULL |
| spec_info | JSON | 规格信息（颜色/内存） | - |
| price | DECIMAL(10,2) | SKU价格 | NOT NULL |
| original_price | DECIMAL(10,2) | SKU原价 | - |
| stock | INT | SKU库存 | NOT NULL DEFAULT 0 |
| sku_image | VARCHAR(500) | SKU主图 | - |
| sort_order | INT | 排序 | DEFAULT 0 |
| created_at / updated_at | TIMESTAMP | 时间戳 | 自动维护 |

#### product_params — 商品参数表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| product_id | BIGINT | 商品ID | NOT NULL，FK→products(id) ON DELETE CASCADE，IDX(idx_product_id) |
| param_name | VARCHAR(100) | 参数名（屏幕尺寸） | NOT NULL |
| param_value | VARCHAR(500) | 参数值（6.7英寸） | NOT NULL |
| sort_order | INT | 排序 | DEFAULT 0 |

#### product_images — 商品图片表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| product_id | BIGINT | 商品ID | NOT NULL，FK→products(id) ON DELETE CASCADE |
| image | VARCHAR(500) | 图片URL | NOT NULL |
| sort_order | INT | 图片排序 | DEFAULT 0 |
| created_at | DATETIME | 创建时间 | 自动维护 |

### 3.4 订单模块

#### orders — 订单表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 订单ID | PK |
| order_number | VARCHAR(64) | 订单号 | UNIQUE NOT NULL，IDX(idx_order_number) |
| user_id | BIGINT | 买家ID | NOT NULL，FK→users(id) ON DELETE RESTRICT，IDX(idx_user_id) |
| total_amount | DECIMAL(10,2) | 订单总金额 | NOT NULL |
| discount_amount | DECIMAL(10,2) | 优惠金额 | DEFAULT 0 |
| status | VARCHAR(20) | PENDING/PAID/PROCESSING/SHIPPED/COMPLETED/CANCELLED | DEFAULT 'PENDING'，IDX(idx_status) |
| source | VARCHAR(20) | 来源 cart购物车 / product直接购买 | DEFAULT 'cart' |
| address_id | BIGINT | 收货地址ID | NOT NULL，IDX(idx_address_id) |
| payment_method | VARCHAR(50) | ALIPAY/WECHAT | - |
| transaction_id | VARCHAR(100) | 支付交易编号 | - |
| paid_at | DATETIME | 支付时间 | - |
| processing_at | DATETIME | 商家接单时间 | - |
| tracking_number | VARCHAR(100) | 物流单号 | - |
| logistics_code | VARCHAR(20) | 物流公司代码 | - |
| logistics_name | VARCHAR(50) | 物流公司名称 | - |
| shipped_at | DATETIME | 发货时间 | IDX(idx_shipped_at) |
| completed_at | DATETIME | 完成时间 | - |
| cancelled_at | DATETIME | 取消时间 | - |
| created_at / updated_at | DATETIME | 时间戳 | 自动维护 |
| is_deleted | TINYINT(1) | 软删除标记 | DEFAULT 0 |

> 复合索引：`idx_orders_user_id_status_created(user_id,status,created_at)`、`idx_orders_seller_id_status_created(seller_id,status,created_at)`

#### order_items — 订单项表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 订单项ID | PK |
| order_id | BIGINT | 订单ID | NOT NULL，FK→orders(id) ON DELETE CASCADE，IDX(idx_order_id) |
| product_id | BIGINT | 商品ID | NOT NULL，FK→products(id) ON DELETE RESTRICT，IDX(idx_product_id) |
| sku_id | BIGINT | SKU ID | FK→product_skus(id) ON DELETE SET NULL |
| seller_id | BIGINT | 商家ID | IDX(idx_seller_id) |
| product_name | VARCHAR(255) | 商品名称快照 | NOT NULL |
| product_image | VARCHAR(500) | 商品图片快照 | - |
| sku_name | VARCHAR(200) | SKU规格快照 | - |
| quantity | INT | 购买数量 | NOT NULL |
| price | DECIMAL(10,2) | 下单时单价 | NOT NULL |
| total_price | DECIMAL(10,2) | 小计金额 | NOT NULL |
| is_reviewed | TINYINT(1) | 是否已评论 | DEFAULT 0，IDX(idx_is_reviewed) |
| reviewed_at | DATETIME | 评论时间 | - |
| refund_status | VARCHAR(20) | 售后状态 REFUNDING/AFTER_SALE/WAITING_RETURN/COMPLETED | - |
| created_at | DATETIME | 创建时间 | 自动维护 |

### 3.5 退款售后

#### order_refunds — 退款记录表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 退款ID | PK |
| order_id | BIGINT | 订单ID | NOT NULL，FK→orders(id) ON DELETE RESTRICT，IDX(idx_order_id) |
| order_item_id | BIGINT | 订单项ID | IDX(idx_order_item_id) |
| order_number | VARCHAR(64) | 订单号冗余 | IDX(idx_order_number) |
| user_id | BIGINT | 用户ID | IDX(idx_user_id) |
| refund_amount | DECIMAL(10,2) | 退款金额 | NOT NULL |
| refund_reason | VARCHAR(500) | 退款原因 | - |
| return_method | VARCHAR(20) | 退货方式 PICKUP上门取件/SELF自寄 | - |
| return_tracking_number | VARCHAR(100) | 退货物流单号 | - |
| return_logistics_name | VARCHAR(50) | 退货物流公司 | - |
| return_status | VARCHAR(20) | 退货状态 NULL/RETURNING/RECEIVED | IDX(idx_return_status) |
| seller_address_id | BIGINT | 商家收货地址ID | - |
| return_apply_time / return_receive_time | DATETIME | 退货申请/收货时间 | - |
| refund_status | VARCHAR(20) | PROCESSING/WAITING_RETURN/RETURNING/SUCCESS/FAILED | DEFAULT 'PROCESSING'，IDX(idx_refund_status) |
| refund_type | VARCHAR(20) | REFUND仅退款/AFTER_SALE退货退款 | DEFAULT 'REFUND'，IDX(idx_refund_type) |
| description | VARCHAR(500) | 退款描述 | - |
| refund_transaction_id | VARCHAR(100) | 退款交易编号 | - |
| apply_time / review_time / complete_time | DATETIME | 申请/审核/完成时间 | IDX(idx_apply_time) |
| review_notes | VARCHAR(500) | 审核备注 | - |
| reviewed_by | BIGINT | 审核人ID | - |

#### refund_images — 退款凭证图片表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| refund_id | BIGINT | 退款记录ID | NOT NULL，FK→order_refunds(id) ON DELETE CASCADE，IDX(idx_refund_id) |
| communication_id | BIGINT | 关联消息ID | IDX(idx_communication_id) |
| image | VARCHAR(500) | 图片URL | NOT NULL |
| image_type | VARCHAR(20) | EVIDENCE凭证/APPEAL申诉/CHAT聊天 | DEFAULT 'EVIDENCE'，IDX(idx_image_type) |
| sort_order | INT | 排序 | DEFAULT 0 |
| created_at | DATETIME | 创建时间 | 自动维护 |

#### refund_videos — 退款凭证视频表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| refund_id | BIGINT | 退款记录ID | NOT NULL，FK→order_refunds(id) ON DELETE CASCADE，IDX(idx_refund_id) |
| video_url | VARCHAR(500) | 视频URL | NOT NULL |
| cover_url | VARCHAR(500) | 封面图URL | - |
| duration | INT | 视频时长（秒） | - |
| size | BIGINT | 文件大小（字节） | - |
| sort_order | INT | 排序 | DEFAULT 0 |
| created_at | DATETIME | 创建时间 | 自动维护 |

#### refund_communications — 退款沟通记录表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| refund_id | BIGINT | 退款记录ID | NOT NULL，FK→order_refunds(id) ON DELETE CASCADE，IDX(idx_refund_id) |
| sender_type | VARCHAR(20) | BUYER/SELLER | NOT NULL |
| sender_id | BIGINT | 发送者ID | NOT NULL |
| content | VARCHAR(1000) | 沟通内容 | NOT NULL |
| round | INT | 第几轮沟通 | DEFAULT 1 |
| created_at | DATETIME | 创建时间 | 自动维护 |

### 3.6 购物 / 收藏 / 地址

#### cart_items — 购物车表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| user_id | BIGINT | 用户ID | NOT NULL，FK→users(id) ON DELETE CASCADE |
| product_id | BIGINT | 商品ID | NOT NULL，FK→products(id) ON DELETE CASCADE |
| sku_id | BIGINT | SKU ID | FK→product_skus(id) ON DELETE SET NULL，IDX(idx_sku_id) |
| quantity | INT | 数量 | NOT NULL DEFAULT 1 |
| added_at | DATETIME | 添加时间 | 自动维护 |
| - | - | 唯一约束 | UK(uk_user_product_sku)(user_id,product_id,sku_id) |
| - | - | 复合索引 | IDX(idx_product_sku)(product_id,sku_id) |

#### favorites — 用户收藏表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| user_id | BIGINT | 用户ID | NOT NULL，FK→users(id) ON DELETE CASCADE，IDX(idx_favorites_user_id) |
| product_id | BIGINT | 商品ID | NOT NULL，FK→products(id) ON DELETE CASCADE |
| created_at | DATETIME | 收藏时间 | 自动维护 |
| - | - | 唯一约束 | UK(uk_user_product)(user_id,product_id) |

#### addresses — 收货地址表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 地址ID | PK |
| user_id | BIGINT | 用户ID | NOT NULL，FK→users(id) ON DELETE CASCADE，IDX(idx_user_id) |
| recipient_name | VARCHAR(100) | 收件人姓名 | NOT NULL |
| recipient_phone | VARCHAR(20) | 收件人电话 | NOT NULL |
| province / city / district | VARCHAR(50) | 省/市/区县 | NOT NULL |
| detail_address | VARCHAR(500) | 详细地址 | NOT NULL |
| label | VARCHAR(50) | 地址标签（家/公司/学校） | - |
| is_default | TINYINT(1) | 是否默认地址 | DEFAULT 0，IDX(idx_is_default) |
| created_at / updated_at | DATETIME | 时间戳 | 自动维护 |

### 3.7 评价模块

#### reviews — 商品评价表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| user_id | BIGINT | 用户ID | NOT NULL，FK→users(id)，IDX(idx_reviews_user_id) |
| product_id | BIGINT | 商品ID | NOT NULL，FK→products(id)，IDX(idx_reviews_product_id) |
| order_id | BIGINT | 订单ID | NOT NULL，FK→orders(id) |
| order_item_id | BIGINT | 订单项ID | - |
| rating | INT | 评分 1-5 | NOT NULL |
| comment | TEXT | 评论内容 | - |
| is_anonymous | TINYINT(1) | 是否匿名 | DEFAULT 0 |
| sku_spec | VARCHAR(200) | 购买规格快照 | - |
| ip | VARCHAR(50) | 评论IP | - |
| location | VARCHAR(100) | IP属地 | - |
| created_at | DATETIME | 评论时间 | 自动维护 |

#### review_images — 评论图片表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| review_id | BIGINT | 评论ID | NOT NULL，FK→reviews(id) ON DELETE CASCADE |
| image | VARCHAR(500) | 图片URL | NOT NULL |
| sort_order | INT | 图片排序 | DEFAULT 0 |
| created_at | DATETIME | 创建时间 | 自动维护 |

#### review_videos — 评论视频表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| review_id | BIGINT | 评论ID | NOT NULL，FK→reviews(id) ON DELETE CASCADE，IDX(idx_review_id) |
| video_url | VARCHAR(500) | 视频URL | NOT NULL |
| cover_url | VARCHAR(500) | 封面图URL | - |
| duration | INT | 时长（秒） | - |
| size | BIGINT | 文件大小（字节） | - |
| sort_order | INT | 排序 | DEFAULT 0 |
| created_at | DATETIME | 创建时间 | 自动维护 |

### 3.8 优惠券模块

#### coupons — 优惠券模板表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| seller_id | BIGINT | 商家ID | NOT NULL，IDX(idx_seller_id) |
| name | VARCHAR(100) | 优惠券名称 | NOT NULL |
| type | VARCHAR(20) | FULL_REDUCTION/DISCOUNT/NO_THRESHOLD | NOT NULL |
| min_amount | DECIMAL(10,2) | 满减门槛 | DEFAULT 0 |
| discount_amount | DECIMAL(10,2) | 减免金额 | - |
| discount_rate | DECIMAL(3,2) | 折扣率（0.85=85折） | - |
| total_count | INT | 发行总量（-1不限） | NOT NULL DEFAULT 1 |
| received_count | INT | 已领取数量 | DEFAULT 0 |
| used_count | INT | 已使用数量 | DEFAULT 0 |
| per_user_limit | INT | 每人限领 | DEFAULT 1 |
| start_time / end_time | DATETIME | 生效起止 | NOT NULL，IDX(idx_start_end) |
| status | TINYINT(1) | 1启用 0禁用 | DEFAULT 1，IDX(idx_status) |
| created_at / updated_at | DATETIME | 时间戳 | 自动维护 |

#### user_coupons — 用户优惠券表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| user_id | BIGINT | 用户ID | NOT NULL，IDX(idx_user_id) |
| coupon_id | BIGINT | 优惠券ID | NOT NULL，IDX(idx_coupon_id) |
| status | VARCHAR(20) | UNUSED/USED/EXPIRED | DEFAULT 'UNUSED' |
| order_id | BIGINT | 使用的订单ID | - |
| received_at | DATETIME | 领取时间 | 自动维护 |
| used_at | DATETIME | 使用时间 | - |
| expire_at | DATETIME | 过期时间 | NOT NULL |
| - | - | 复合索引 | IDX(idx_user_status)(user_id,status) |

#### order_coupons — 订单优惠券表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| order_id | BIGINT | 订单ID | NOT NULL，IDX(idx_order_id) |
| user_coupon_id | BIGINT | 用户优惠券ID | NOT NULL |
| coupon_id | BIGINT | 优惠券ID | NOT NULL，IDX(idx_coupon_id) |
| coupon_name | VARCHAR(100) | 优惠券名称 | NOT NULL |
| coupon_type | VARCHAR(20) | 优惠券类型 | NOT NULL |
| discount_amount | DECIMAL(10,2) | 优惠金额 | NOT NULL |
| created_at | DATETIME | 创建时间 | 自动维护 |

### 3.9 消息通知

#### notifications — 消息通知表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT | 消息ID | PK |
| user_id | BIGINT | 接收者用户ID | NOT NULL，IDX(idx_user_id) |
| type | VARCHAR(30) | ORDER/SYSTEM/LOGISTICS/REFUND/CHAT | NOT NULL，IDX(idx_type) |
| title | VARCHAR(200) | 消息标题 | NOT NULL |
| content | VARCHAR(1000) | 消息内容 | - |
| extra_data | JSON | 扩展数据（orderId/trackingNumber/amount） | - |
| is_read | TINYINT | 0未读 1已读 | DEFAULT 0，IDX(idx_is_read) |
| created_at | DATETIME | 创建时间 | IDX(idx_created_at) |

> 复合索引：`idx_notifications_user_id_read(user_id,is_read)`、`idx_notifications_user_id_type(user_id,type)`

### 3.10 聊天模块

#### chat_messages — 聊天消息表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT | 消息ID | PK |
| sender_id | BIGINT | 发送者ID | NOT NULL，IDX(idx_sender) |
| receiver_id | BIGINT | 接收者ID | NOT NULL，IDX(idx_receiver) |
| content | TEXT | 消息内容 | - |
| message_type | VARCHAR(20) | TEXT/IMAGE/PRODUCT_CARD/ORDER_CARD | DEFAULT 'TEXT' |
| media_urls | TEXT | 媒体URL列表 | - |
| product_id | BIGINT | 商品ID | - |
| order_id | BIGINT | 订单ID | - |
| is_read | TINYINT(1) | 是否已读 | DEFAULT 0 |
| is_recalled | TINYINT(1) | 是否撤回 | DEFAULT 0 |
| recalled_at | DATETIME | 撤回时间 | - |
| created_at | DATETIME | 创建时间 | 自动维护，IDX(idx_created_at) |

#### chat_sessions — 会话表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT | 会话ID | PK |
| user_id | BIGINT | 用户ID | NOT NULL，IDX(idx_user_id) |
| target_id | BIGINT | 对方ID | NOT NULL |
| last_message | TEXT | 最后一条消息 | - |
| last_message_time | DATETIME | 最后消息时间 | - |
| unread_count | INT | 未读数 | DEFAULT 0 |
| is_top | TINYINT(1) | 是否置顶 | DEFAULT 0 |
| is_muted | TINYINT(1) | 是否免打扰 | DEFAULT 0 |
| created_at / updated_at | DATETIME | 时间戳 | 自动维护，IDX(idx_updated_at) |
| - | - | 唯一约束 | UK(uk_user_target)(user_id,target_id) |

#### seller_quick_replies — 商家快捷回复表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| seller_id | BIGINT | 商家ID | IDX(idx_seller_id) |
| content | TEXT | 快捷回复内容 | NOT NULL |
| sort_order | INT | 排序 | DEFAULT 0 |
| created_at / updated_at | DATETIME | 时间戳 | 自动维护 |

### 3.11 商品冻结 / 套餐

#### product_freeze_log — 商品冻结记录表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 冻结记录ID | PK |
| seller_id | BIGINT | 商家ID | NOT NULL，FK→users(id) ON DELETE CASCADE，IDX(idx_seller_id) |
| product_id | BIGINT | 商品ID | NOT NULL，FK→products(id) ON DELETE CASCADE，IDX(idx_product_id) |
| freeze_reason | VARCHAR(100) | 冻结原因（套餐到期等） | - |
| freeze_time | DATETIME | 冻结时间 | 自动维护，IDX(idx_freeze_time) |
| unfreeze_time | DATETIME | 解冻时间 | - |
| unfreeze_reason | VARCHAR(100) | 解冻原因（续费等） | - |
| created_at | DATETIME | 创建时间 | 自动维护 |

#### seller_packages — 商家套餐表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 套餐ID | PK |
| name | VARCHAR(50) | 套餐名称（基础/标准/高级/旗舰） | NOT NULL |
| description | VARCHAR(500) | 套餐描述 | - |
| price | DECIMAL(10,2) | 套餐价格 | NOT NULL |
| duration_days | INT | 套餐时长（天） | NOT NULL |
| product_limit | INT | 商品数量限制（-1无限制） | NOT NULL |
| features | TEXT | 套餐功能（JSON） | - |
| is_active | TINYINT(1) | 是否启用 | DEFAULT 1 |
| sort_order | INT | 排序 | DEFAULT 0 |
| created_at / updated_at | DATETIME | 时间戳 | 自动维护 |

#### seller_package_orders — 商家套餐购买记录表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 记录ID | PK |
| seller_id | BIGINT | 商家用户ID | NOT NULL，FK→users(id) ON DELETE CASCADE |
| package_id | BIGINT | 套餐ID | NOT NULL，FK→seller_packages(id) |
| package_name | VARCHAR(50) | 套餐名称 | NOT NULL |
| price | DECIMAL(10,2) | 购买价格 | NOT NULL |
| start_date | DATETIME | 开始日期 | - |
| end_date | DATETIME | 结束日期 | - |
| remaining_seconds | INT | 暂停时剩余秒数（PAUSED时使用） | DEFAULT 0 |
| STATUS | VARCHAR(20) | PENDING/ACTIVE/PAUSED/EXPIRED/CANCELLED | DEFAULT 'PENDING' |
| payment_method | VARCHAR(20) | ALIPAY/WECHAT | - |
| transaction_id | VARCHAR(100) | 交易单号 | - |
| created_at / updated_at | DATETIME | 时间戳 | 自动维护 |
| - | - | 复合索引 | IDX(idx_seller_status)(seller_id,STATUS)、IDX(idx_status) |

### 3.12 其他业务表

#### banners — 首页轮播图表
> 由 `BannerController` / `Banner` 实体管理，对应字段如下：

| 字段 | 类型 | 含义 |
| :-- | :-- | :-- |
| id | BIGINT | 主键 |
| title | VARCHAR | 标题 |
| image_url | VARCHAR(500) | 图片URL |
| link_url | VARCHAR(500) | 跳转链接 |
| sort_order | INT | 排序（默认0） |
| status | INT | 状态（1启用 0禁用，默认1） |
| start_time / end_time | DATETIME | 投放起止时间 |
| position | VARCHAR | 投放位置（默认 HOME） |
| created_at / updated_at | DATETIME | 时间戳 |

#### merchant_apply — 商家入驻申请表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| user_id | BIGINT | 申请人用户ID | NOT NULL，FK→users(id) ON DELETE CASCADE |
| contact_name | VARCHAR(50) | 联系人姓名 | NOT NULL |
| contact_phone | VARCHAR(20) | 联系电话 | NOT NULL |
| contact_email | VARCHAR(100) | 联系邮箱 | NOT NULL |
| store_name | VARCHAR(100) | 店铺名称 | NOT NULL |
| store_detail | TEXT | 店铺详细描述 | - |
| address | VARCHAR(500) | 详细地址 | - |
| business_type | VARCHAR(20) | 经营类型 | NOT NULL |
| main_category | VARCHAR(20) | 主营类目 | NOT NULL |
| business_license | VARCHAR(500) | 营业执照路径 | - |
| id_card_front | VARCHAR(500) | 身份证正面路径 | - |
| id_card_back | VARCHAR(500) | 身份证反面路径 | - |
| status | VARCHAR(20) | PENDING/APPROVED/REJECTED | DEFAULT 'PENDING' |
| review_notes | TEXT | 审核备注 | - |
| reviewed_by | BIGINT | 审核人用户ID | FK→users(id) ON DELETE SET NULL |
| created_at / updated_at / reviewed_at | DATETIME | 时间戳 | 自动维护 |

#### follow_sellers — 用户关注商家表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 关注ID | PK |
| user_id | BIGINT | 用户ID | NOT NULL，FK→users(id) ON DELETE CASCADE，IDX(idx_user_id) |
| seller_id | BIGINT | 商家用户ID | NOT NULL，FK→users(id) ON DELETE CASCADE，IDX(idx_seller_id) |
| created_at | DATETIME | 关注时间 | 自动维护 |
| - | - | 唯一约束 | UK(uk_user_seller)(user_id,seller_id) |

#### user_browse_history — 用户浏览记录表
| 字段 | 类型 | 含义 | 约束/索引 |
| :-- | :-- | :-- | :-- |
| id | BIGINT AUTO_INCREMENT | 主键 | PK |
| user_id | BIGINT | 用户ID | NOT NULL，FK→users(id) ON DELETE CASCADE |
| product_id | BIGINT | 商品ID | NOT NULL |
| product_name | VARCHAR(255) | 商品名称（冗余） | - |
| product_image | VARCHAR(500) | 商品图片（冗余） | - |
| product_price | DECIMAL(10,2) | 浏览时价格 | - |
| browse_time | DATETIME | 浏览时间 | 自动维护 |
| - | - | 复合索引 | IDX(idx_user_time)(user_id, browse_time DESC) |

---

## 四、核心业务流程

### 4.1 用户注册 / 登录 / JWT 认证

```text
1. 注册：POST /api/v1/auth/register → BCrypt 加密密码 → 写 users + user_roles(默认 ROLE_USER)
2. 登录：POST /api/v1/auth/login → BCrypt 校验 → 生成 JWT → 写入 HttpOnly Cookie（防 XSS）
3. 请求：浏览器自动携带 Cookie → JwtAuthenticationFilter 解析校验 → SecurityContextHolder 写入认证信息
4. 角色切换：POST /api/v1/user/switch-role（ROLE_USER/ROLE_SELLER/ROLE_ADMIN）
5. 踢下线：WebSocketService 向 /exchange/amq.topic/kickout.user.{userId} 推送 → 前端清除登录态并跳转登录页
```

### 4.2 商家入驻 → 审核 → 开通

```text
1. 用户提交：POST /api/v1/merchant/applications（FormData：资质材料、营业执照、身份证）
   → merchant_apply 写入 PENDING
2. 管理员审核：PUT /api/v1/admin/merchant/applications/{id}/approve | /reject
3. 审核通过：用户角色升级为 ROLE_SELLER（user_roles），seller_profiles 初始化
4. 开通后商家可登录商家端，发布商品、购买套餐
```

### 4.3 商品发布（四步表单）

```text
步骤1 基本信息：名称、分类、品牌、描述、发货城市、服务保障
步骤2 规格SKU：定义规格（颜色/内存）→ 生成 product_skus（含价格、原价、库存、主图）
步骤3 图片详情：主图/轮播图 → product_images；富文本详情 → products.detail_html；参数 → product_params
步骤4 其他信息：重量、是否包邮、上架状态
→ POST /api/v1/seller/products（FormData）→ 需先通过套餐发布权限校验（check-publish）
```

### 4.4 商品搜索 / 浏览 / 详情查看

```text
首页：GET /api/v1/banners（轮播）+ GET /api/v1/products/recommend（推荐，基于浏览历史/热销）
列表：GET /api/v1/products?page&pageSize&keyword&categoryId&sellerId（分页+筛选）
搜索：GET /api/v1/products/suggest?keyword（搜索建议）
详情：GET /api/v1/products/{id} + /skus + /params + /coupons + /seller + /reviews + /reviews/statistics
浏览记录：POST /api/v1/user/browse-history（写 user_browse_history，冗余商品快照）
```

### 4.5 购物车 → 结算 → 下单 → 支付

```text
1. 加购：POST /api/v1/cart/items（user_id+product_id+sku_id 唯一约束）
2. 结算：POST /api/v1/checkout/cart（按购物车项ID）或 /checkout/product（立即购买）
3. 选券：GET /api/v1/checkout/coupons?orderAmount
4. 下单：POST /api/v1/orders
   → Redis 预扣库存（decrement + Hash 30分钟过期）
   → orders + order_items 写入，status=PENDING
   → RabbitMQ order.delay.queue 投递（30分钟 TTL）
   → 返回支付宝支付表单
5. 支付：用户扫码支付 → 支付宝异步回调 /api/v1/payment/callback
   → 验签 → orders.status=PAID → 扣减 MySQL 真实库存 + sales_count → 删购物车
   → WebSocket 推送 seller.payment-success / user.payment
```

### 4.6 订单状态流转

```text
PENDING（待付款）──支付──▶ PAID（已付款）──商家接单──▶ PROCESSING（处理中）
   │                              │                          │
   │ 超时30min                     │ 用户取消                 │ 商家发货
   ▼                              ▼                          ▼
CANCELLED                    CANCELLED                SHIPPED（已发货）──确认收货/7天自动──▶ COMPLETED
```

### 4.7 退款售后流程

```text
1. 用户申请：POST /api/v1/refunds/submit/{orderItemId}（FormData：图片+视频）
   → order_refunds 写入 refund_type=REFUND（仅退款）/AFTER_SALE（退货退款）
2. 商家处理：POST /api/v1/seller/refunds/{refundId}/approve（同意）
   · 仅退款：直接退款（调用支付宝退款）
   · 退货退款：用户提交退货 POST /refunds/{refundId}/return-submit（上门取件/自寄）
3. 退货物流：商家收货 POST /api/v1/seller/refunds/{refundId}/confirm-receive → 退款完成 SUCCESS
4. 沟通：退款聊天 POST /api/v1/refunds/{refundId}/chat/send（支持图片）
5. 申诉/介入：PUT /refunds/{refundId}/appeal、POST /refunds/{refundId}/intervene
6. 实时推送：seller.refund / seller.return / user.refund / user.refund.chat / seller.refund.chat
```

### 4.8 商家套餐：购买 → 生效 → 到期提醒 → 冻结 → 续费/降级恢复

```text
1. 购买：POST /api/v1/seller/packages/{packageId}/buy → seller_package_orders STATUS=PENDING
2. 生效：支付成功后 ACTIVE，设置 start_date / end_date
3. 到期提醒：RabbitMQ x-delayed-message 投递 package.expire.remind（7天/1天前）
   → WebSocket seller.package 推送 + 前端音频播报
4. 到期处理：package.expire.process
   → 当前套餐 EXPIRED → 查 PAUSED 套餐按等级 DESC 恢复最高等级（resumePausedPackage，用 remaining_seconds 重建 endDate）
   → 无可恢复套餐：冻结超配额商品（freezeExceedProductsWithDetails）→ product_freeze_log
5. 续费/降级恢复：购买新套餐 → 解冻商品 → 发送 renew 通知
```

### 4.9 优惠券：创建 → 陯领 → 使用 → 核销

```text
1. 商家创建：POST /api/v1/seller/coupons（满减/折扣/无门槛）→ coupons
2. 用户领取：POST /api/v1/coupons/{id}/receive（校验 per_user_limit、total_count）→ user_coupons(UNUSED)
3. 结算选券：GET /api/v1/checkout/coupons?orderAmount → POST /orders/{orderId}/apply-coupon
4. 核销：下单使用 → user_coupons(USED) + order_coupons 记录；过期 → user_coupons(EXPIRED)
```

### 4.10 消息通知（WebSocket 实时推送 + 数据库存储）

```text
产生事件 → WebSocketService.convertAndSend 推送到 RabbitMQ amq.topic（路由键路由）
        → 同时写 notifications 表（type=ORDER/SYSTEM/LOGISTICS/REFUND/CHAT）
前端订阅 → 收到推送播放音频 + Message 提示 + window.dispatchEvent 触发组件刷新
历史查询 → GET /api/v1/notifications?page&pageSize&type（分页）+ /notifications/summary（汇总）
已读     → PUT /api/v1/notifications/{id}/read、/notifications/read-all
```

### 4.11 聊天系统（用户与商家实时沟通）

```text
发送文本：POST /api/v1/chat/send/text?targetId&content
发送媒体：POST /api/v1/chat/send/image | /video | /with-media（FormData）
发送卡片：POST /api/v1/chat/send/product | /order
→ 写 chat_messages + 更新 chat_sessions（last_message、unread_count）
→ WebSocket 推送 user.chat.{userId} / seller.chat.{sellerId}
会话管理：GET /chat/sessions、置顶、免打扰、删除、撤回 /chat/{messageId}/recall
快捷回复：seller_quick_replies 增删改查
```

### 4.12 库存管理（Redis 预扣 + 真实库存同步）

```text
下单：
  1. Redis 原子扣减库存（decrement，返回值校验防超卖）
  2. 预扣存入 Redis Hash（key 含 orderId，TTL 30 分钟）
支付成功：
  3. 扣减 MySQL product_skus.stock + 累加 products.sales_count
  4. 删除购物车项
超时未支付：
  5. order.delay.queue 消费 / 定时任务兜底（每天 02:00）取消订单 + 回滚 Redis 库存
```

---

## 五、WebSocket 消息设计

### 5.1 连接配置

- **端点**：`/ws`（SockJS，允许跨域）
- **消息代理**：RabbitMQ STOMP 中继（`enableStompBrokerRelay`），端口 `61613`
- **Broker 前缀**：`/exchange`、`/queue`
- **应用目的地前缀**：`/app`
- **用户目的地前缀**：`/user`
- **底层交换机**：RabbitMQ 内置 `amq.topic`（topic 交换机）

### 5.2 应用端点（@MessageMapping）

| 客户端发送目的地 | 方法 | 说明 |
| :-- | :-- | :-- |
| `/app/heartbeat` | `WebSocketController.heartbeat` | 客户端心跳（前端每 60s 发送一次）|

### 5.3 订阅 Topic 列表

前端通过 STOMP 订阅以下 `/exchange/amq.topic/` 路由键，后端 `WebSocketService.convertAndSend` 推送。

#### 用户端订阅（connectUser）

| Topic | 触发场景 | 前端处理 |
| :-- | :-- | :-- |
| `/exchange/amq.topic/kickout.user.{userId}` | 账号被踢下线 | 清除登录态、断开 WS、跳转登录页 |
| `/exchange/amq.topic/user.payment.{userId}` | 订单支付成功 | 播放音频、Toast、刷新订单 |
| `/exchange/amq.topic/user.shipment.{userId}` | 商家已发货 | 播放音频、物流提示、刷新 |
| `/exchange/amq.topic/user.refund.{userId}` | 退款处理结果 | 播放音频、退款状态变更事件 |
| `/exchange/amq.topic/user.refund.chat.{userId}` | 退款沟通新消息 | 播放音频、刷新退款聊天 |
| `/exchange/amq.topic/user.chat.{userId}` | 商家客服消息 | 播放音频、刷新会话 |

#### 商家端订阅（connectSeller）

| Topic | 触发场景 | 前端处理 |
| :-- | :-- | :-- |
| `/exchange/amq.topic/kickout.user.{userId}` | 账号被踢下线 | 清除登录态、跳转登录页 |
| `/exchange/amq.topic/seller.new-order` | 收到新订单 | 播放音频、新订单事件 |
| `/exchange/amq.topic/seller.payment-success` | 买家支付成功 | 播放音频、刷新订单 |
| `/exchange/amq.topic/seller.refund.{userId}` | 收到退款申请 | 播放音频、退款申请事件 |
| `/exchange/amq.topic/seller.return.{userId}` | 买家提交退货 | 物流单号提示、刷新通知 |
| `/exchange/amq.topic/seller.refund.chat.{userId}` | 退款沟通新消息 | 播放音频、刷新退款聊天 |
| `/exchange/amq.topic/seller.package.{userId}` | 套餐到期提醒/续费恢复 | 按剩余天数播放对应音频 |
| `/exchange/amq.topic/seller.chat.{userId}` | 用户客服消息 | 播放音频、刷新会话 |

#### 管理端订阅（connectAdmin）

| Topic | 触发场景 |
| :-- | :-- |
| `/exchange/amq.topic/kickout.user.{userId}` | 账号被踢下线 |

> 套餐音频策略：`days===7` 播放"套餐7天到期"；`days===1` 播放"套餐一天到期"；`type===expired` 播放"套餐已到期"；`type===renew` 播放"套餐续费恢复"。

---

## 六、RabbitMQ 设计

### 6.1 订单延迟队列（order.delay.queue）

| 配置项 | 值 | 说明 |
| :-- | :-- | :-- |
| 队列 | `order.delay.queue` | durable，订单超时取消 |
| 交换机 | `order.delay.exchange` | DirectExchange |
| 绑定路由键 | `order.delay` | queue ↔ exchange |
| TTL | 30 分钟（业务配置 `business.order.timeout-minutes=30`）| 订单超时自动取消 + 回滚 Redis 库存 |
| 兜底 | `OrderSchedule.cancelExpiredOrders` | 每日 02:00 cron 兜底检查 |

### 6.2 套餐到期队列（package.expire.queue，x-delayed-message 插件）

| 配置项 | 值 | 说明 |
| :-- | :-- | :-- |
| 队列 | `package.expire.queue` | durable，套餐到期处理 |
| 交换机 | `package.expire.exchange` | 类型 `x-delayed-message`，参数 `x-delayed-type=direct` |
| 绑定1 路由键 | `package.expire.remind` | 到期前提醒（7天/1天）|
| 绑定2 路由键 | `package.expire.process` | 到期处理（冻结商品 / 降级恢复）|
| 消费者 | `PackageExpireConsumer.handlePackageExpire` | 处理 remind/expire 两类消息 |
| 兜底 | `PackageExpireTask.checkExpiredPackages` | 每日 03:00 cron 兜底检查 |

### 6.3 可靠性配置（application.yml）

- `publisher-confirm-type=correlated`：发布确认（异步）
- `publisher-returns=true` + `template.mandatory=true`：不可路由消息返回
- `listener.simple.acknowledge-mode=manual`：手动 ACK
- `prefetch=1`，`concurrency=5`，`max-concurrency=20`
- 重试：`max-attempts=3`，`initial-interval=1000ms`，`multiplier=2`，`max-interval=10000ms`
- 消息序列化：`Jackson2JsonMessageConverter`（生产者 + 消费者均使用）

---

## 七、API 接口文档

> 统一前缀 `/api/v1`。返回格式：`{ success: boolean, data: T, message?: string }`。
> 鉴权：JWT 通过 HttpOnly Cookie 自动携带；放行路径见 `SecurityConfig`（login/register/products/categories/upload/payment-callback/logistics-callback/ws）。

### 7.1 用户端接口

#### 认证与账号

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| POST | `/auth/login` | 用户登录（账号+密码）|
| POST | `/auth/register` | 用户注册 |
| GET | `/auth/logout` | 登出 |
| POST | `/auth/refresh` | 刷新 Token |
| GET | `/account/profile` | 获取账号信息（角色、状态）|
| POST | `/user/switch-role` | 切换活跃角色 |
| GET | `/user/profile` | 获取个人资料 |
| PUT | `/user/profile` | 修改资料（昵称/性别/生日）|
| POST | `/user/avatar` | 修改头像 |
| GET | `/user/{userId}/online-status` | 用户在线状态 |

#### 商品浏览

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/products` | 商品列表（分页/关键词/分类/商家筛选）|
| GET | `/products/{id}` | 商品详情 |
| GET | `/products/{id}/skus` | 商品 SKU 列表 |
| GET | `/products/{id}/params` | 商品参数 |
| GET | `/products/{id}/coupons` | 商品可领优惠券 |
| GET | `/products/{id}/seller` | 商品所属商家 |
| GET | `/products/{id}/reviews` | 商品评价列表 |
| GET | `/products/{id}/reviews/statistics` | 评价统计 |
| GET | `/products/recommend` | 推荐商品 |
| GET | `/products/suggest?keyword=` | 搜索建议 |
| GET | `/products/{id}/quality-stats` | 商品质量统计 |
| GET | `/categories` | 所有分类 |
| GET | `/banners` | 首页轮播（公开）|

#### 购物车

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| POST | `/cart/items` | 加入购物车 |
| GET | `/cart/items` | 购物车列表 |
| PUT | `/cart/items` | 修改数量 |
| DELETE | `/cart/items/{cartItemId}` | 删除单项 |
| DELETE | `/cart/items/batch` | 批量删除 |
| GET | `/cart/count` | 购物车数量 |
| POST | `/cart/items/batch` | 按 ID 批量查询 |

#### 收藏与浏览历史

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| POST | `/favorites` | 添加收藏 |
| DELETE | `/favorites/product/{productId}` | 取消收藏 |
| DELETE | `/favorites/batch` | 批量取消 |
| GET | `/favorites` | 收藏列表（分页）|
| GET | `/favorites/check?productId=` | 检查是否收藏 |
| GET | `/favorites/count` | 收藏数量 |
| POST | `/user/browse-history` | 记录浏览历史 |
| GET | `/user/browse-history` | 浏览历史 |
| DELETE | `/user/browse-history` | 清空浏览历史 |

#### 地址管理

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/addresses` | 地址列表 |
| GET | `/addresses/{addressId}` | 地址详情 |
| GET | `/addresses/default` | 默认地址 |
| POST | `/addresses` | 新增地址 |
| PUT | `/addresses/{addressId}` | 更新地址 |
| PUT | `/addresses/{addressId}/default` | 设为默认 |
| DELETE | `/addresses/{addressId}` | 删除地址 |
| GET | `/user/addresses` | 用户收货地址列表 |

#### 结算与订单

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| POST | `/checkout/cart` | 购物车结算 |
| POST | `/checkout/product` | 立即购买结算 |
| GET | `/checkout/coupons?orderAmount=` | 结算可用券 |
| POST | `/orders` | 创建订单 |
| POST | `/orders/{orderId}/apply-coupon` | 订单应用券 |
| POST | `/orders/{orderId}/pay` | 发起支付 |
| GET | `/orders` | 订单列表（分页/状态）|
| GET | `/orders/{id}` | 订单概要 |
| GET | `/orders/{id}/detail` | 订单详情（含地址+商品）|
| PUT | `/orders/{id}/cancel` | 取消订单 |
| PUT | `/orders/{id}/confirm` | 确认收货 |
| DELETE | `/orders/{id}` | 删除订单（软删除）|
| GET | `/orders/counts` | 订单状态统计 |
| GET | `/orders/review-items?type=` | 评价相关订单项 |
| GET | `/orderItems/{orderItemId}` | 订单项详情 |

#### 退款售后（用户）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| POST | `/orders/{orderId}/refund` | 申请退款（旧/JSON）|
| POST | `/refunds/submit/{orderItemId}` | 提交退款（FormData：图+视频）|
| GET | `/user/refunds?page&pageSize` | 用户退款列表 |
| GET | `/user/refunds/pending-count` | 进行中退款数 |
| GET | `/orders/{orderId}/refunds` | 订单退款记录 |
| GET | `/orders/{orderId}/refund/status` | 退款状态 |
| GET | `/refunds/{refundId}` | 退款详情 |
| GET | `/refunds/order-item/{orderItemId}` | 按订单项查退款 |
| GET | `/refunds/{refundId}/return-info` | 退货信息 |
| POST | `/refunds/{refundId}/return-submit` | 提交退货 |
| POST | `/refunds/cancel-pickup` | 取消取件 |
| PUT | `/refunds/{refundId}/appeal` | 提交申诉 |
| POST | `/refunds/{refundId}/intervene` | 申请平台介入 |
| GET | `/refunds/{refundId}/chat` | 退款聊天记录 |
| POST | `/refunds/{refundId}/chat/send` | 发送退款消息 |
| POST | `/refunds/{refundId}/messages` | 退款消息（带文件）|
| GET | `/refunds/{refundId}/detail-with-chat` | 退款详情（含聊天）|

#### 评价

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| POST | `/reviews` | 提交评价（FormData：图+视频）|
| PUT | `/reviews` | 更新评价 |
| GET | `/reviews/orderItem/{orderItemId}` | 按订单项查评价 |
| GET | `/reviews/user?page&pageSize` | 用户评价列表 |
| GET | `/reviews/pending?page&pageSize` | 待评价列表 |
| GET | `/reviews/{reviewId}/images` | 评论图片 |
| DELETE | `/reviews/images/{imageId}` | 删除评论图片 |
| GET | `/reviews/product/{productId}/tags` | 评价标签 |

#### 物流（用户）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/orders/{orderId}/logistics` | 物流信息 |
| POST | `/orders/{orderId}/logistics/refresh` | 刷新物流 |
| GET | `/logistics/query?trackingNumber=&logisticsName=&refundId=` | 按单号查询 |

#### 优惠券（用户）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/coupons/available` | 可领取券列表 |
| GET | `/seller/{sellerId}/coupons/available` | 店铺可领券 |
| POST | `/coupons/{id}/receive` | 领取优惠券 |
| GET | `/user/coupons?status=` | 我的券（UNUSED/USED/EXPIRED）|

#### 商家与关注（公开/用户）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/seller/info/{sellerId}` | 商家信息（公开）|
| GET | `/seller/{sellerId}/address` | 商家收货地址 |
| GET | `/seller/{userId}/qualification` | 商家资质 |
| GET | `/seller/{sellerId}/reviews?page&pageSize` | 商家评价列表 |
| POST | `/seller/{sellerId}/follow` | 关注商家 |
| DELETE | `/seller/{sellerId}/follow` | 取消关注 |
| GET | `/seller/{sellerId}/follow/check` | 检查关注状态 |

#### 聊天（用户/通用）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/chat/{targetId}/info` | 目标用户信息 |
| GET | `/chat/{targetId}/messages?page&pageSize` | 聊天记录 |
| POST | `/chat/send/text?targetId&content` | 文本消息 |
| POST | `/chat/send/image` | 图片消息 |
| POST | `/chat/send/video` | 视频消息 |
| POST | `/chat/send/with-media` | 文本+媒体 |
| POST | `/chat/send/product?targetId&productId` | 商品卡片 |
| POST | `/chat/send/order?targetId&orderId` | 订单卡片 |
| POST | `/chat/{messageId}/recall` | 撤回消息 |
| GET | `/chat/sessions` | 会话列表 |
| POST | `/chat/session/{sessionId}/top?isTop=` | 置顶 |
| POST | `/chat/session/{sessionId}/mute?isMuted=` | 免打扰 |
| DELETE | `/chat/session/{targetId}` | 删除会话 |
| GET | `/chat/unread-count` | 未读总数 |

#### 消息通知（用户）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/notifications?page&pageSize&type` | 通知列表 |
| GET | `/notifications/summary` | 通知汇总 |
| GET | `/notifications/unread-count` | 未读数 |
| PUT | `/notifications/{id}/read` | 标记已读 |
| PUT | `/notifications/read-all` | 全部已读 |
| GET | `/messages` | 消息列表 |
| DELETE | `/messages` | 清空消息 |

#### 商家入驻（用户提交）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| POST | `/merchant/applications` | 提交入驻申请（FormData）|
| GET | `/merchant/apply/status` | 查询申请状态 |

### 7.2 商家端接口

#### 商品管理

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/seller/products?page&pageSize&keyword&status` | 商家商品列表 |
| POST | `/seller/products` | 发布商品（FormData）|
| PUT | `/seller/products/{productId}` | 更新商品 |
| PATCH | `/seller/products/{productId}` | 修改状态（上下架）|
| DELETE | `/seller/products/{productId}` | 删除商品 |
| DELETE | `/seller/products/batch` | 批量删除 |
| POST | `/seller/products/{productId}/restore` | 恢复已删除 |
| GET | `/seller/products/{productId}` | 商家商品详情 |
| GET | `/seller/products/{productId}/skus` | 商家 SKU 列表 |

#### 订单管理

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/seller/orders?page&pageSize&status` | 商家订单列表 |
| GET | `/seller/orders/{orderId}` | 商家订单详情 |
| PUT | `/seller/orders/{orderId}/process` | 接单（PAID→PROCESSING）|
| PUT | `/seller/orders/{orderId}/ship` | 发货（含物流信息）|
| PUT | `/seller/orders/{orderId}/cancel` | 商家取消订单 |
| GET | `/seller/orders/{orderId}/logistics` | 物流信息 |
| POST | `/seller/orders/{orderId}/logistics/refresh` | 刷新物流 |

#### 退款处理（商家）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/seller/refunds` | 待处理退款列表 |
| GET | `/seller/refunds/by-order-item/{orderItemId}` | 按订单项查退款 |
| POST | `/seller/refunds/{refundId}/approve` | 同意退款 |
| PUT | `/seller/orders/{orderId}/refund/{refundId}/reject` | 拒绝退款 |
| POST | `/seller/refunds/{refundId}/confirm-receive` | 确认收货并退款 |
| PUT | `/seller/refunds/{refundId}/respond` | 商家回复 |
| GET | `/seller/refunds/{refundId}/logistics` | 退货物流查询 |

#### 评论回复（商家）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/seller/reviews/pending?page&pageSize` | 待回复评论 |
| POST | `/reviews/{reviewId}/reply` | 回复评论 |
| DELETE | `/reviews/{reviewId}/reply` | 删除回复 |

#### 优惠券管理（商家）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/seller/coupons?page&pageSize` | 优惠券列表 |
| GET | `/seller/coupons/{id}` | 优惠券详情 |
| POST | `/seller/coupons` | 创建优惠券 |
| PUT | `/seller/coupons/{id}` | 更新优惠券 |
| PUT | `/seller/coupons/{id}/status` | 启用/禁用 |
| DELETE | `/seller/coupons/{id}` | 删除 |

#### 套餐（商家）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/seller/packages` | 所有可用套餐 |
| GET | `/seller/packages/{packageId}` | 套餐详情 |
| GET | `/seller/packages/current` | 当前套餐与使用情况 |
| POST | `/seller/packages/{packageId}/buy` | 购买套餐 |
| GET | `/seller/packages/check-publish` | 发布权限校验 |
| GET | `/seller/packages/history` | 购买历史 |
| GET | `/seller/packages/orders/{orderId}` | 套餐订单状态 |
| GET | `/seller/packages/status` | 是否有有效套餐 |

#### 资料与数据分析

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/seller/profile` | 商家资料 |
| PUT | `/seller/profile` | 更新资料（FormData：基本信息+头像+横幅）|
| GET | `/seller/analytics?days=7/30/90` | 数据分析 |

#### 快捷回复与通知（商家）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/seller/quick-replies` | 快捷回复列表 |
| POST | `/seller/quick-replies?content=` | 添加 |
| PUT | `/seller/quick-replies/{id}?content=` | 更新 |
| DELETE | `/seller/quick-replies/{id}` | 删除 |
| GET | `/seller/notifications?page&pageSize&type` | 商家通知列表 |
| GET | `/seller/notifications/summary` | 商家通知汇总 |
| GET | `/seller/notifications/unread-count` | 商家未读数 |
| PUT | `/seller/notifications/read-all` | 全部已读 |

### 7.3 管理端接口

#### 商家审核与管理

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/admin/merchant/applications` | 入驻申请列表 |
| GET | `/admin/merchant/applications/stats` | 申请统计 |
| GET | `/admin/merchant/applications/{id}` | 申请详情 |
| PUT | `/admin/merchant/applications/{id}/approve` | 批准 |
| PUT | `/admin/merchant/applications/{id}/reject` | 拒绝 |
| GET | `/admin/sellers` | 商家列表 |
| GET | `/admin/sellers/{id}` | 商家详情 |
| PUT | `/admin/sellers/{id}/reset-password` | 重置商家密码 |
| GET | `/merchant/applications` | 入驻申请（通用）|
| GET | `/merchant/applications/status` | 申请状态汇总 |
| GET | `/merchant/applications/{applicationId}` | 申请详情 |

#### 用户与管理员管理

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/admin/users` | 用户列表 |
| GET | `/admin/users/{id}` | 用户详情 |
| PUT | `/admin/users/{userId}/status` | 用户状态 |
| PUT | `/admin/users/{id}/reset-password` | 重置用户密码 |
| GET | `/admin/admins` | 管理员列表 |
| POST | `/admin/admins` | 新增管理员 |
| DELETE | `/admin/admins/{id}` | 删除管理员 |
| PUT | `/admin/admins/{id}` | 更新管理员 |
| PUT | `/admin/admins/{id}/roles` | 修改管理员角色 |

#### 商品管理

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/admin/products` | 商品列表 |
| GET | `/admin/products/{id}` | 商品详情 |
| PUT | `/admin/products/{id}/status` | 上下架 |
| DELETE | `/admin/products/{id}` | 删除商品 |

#### 分类管理（admin）

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/admin/categories/tree` | 分类树 |
| GET | `/admin/categories` | 分类列表 |
| GET | `/admin/categories/level1` | 一级分类 |
| GET | `/admin/categories/{parentId}/children` | 子分类 |
| GET | `/admin/categories/{id}` | 分类详情 |
| POST | `/admin/categories` | 新增 |
| PUT | `/admin/categories/{id}` | 更新 |
| DELETE | `/admin/categories/{id}` | 删除 |
| PUT | `/admin/categories/{id}/status` | 启停 |
| PUT | `/admin/categories/{id}/sort` | 排序 |

#### 轮播图管理

| 方法 | 路径 | 说明 |
| :-- | :-- | :-- |
| GET | `/admin/banners` | 列表 |
| GET | `/admin/banners/{id}` | 详情 |
| POST | `/admin/banners` | 新增 |
| PUT | `/admin/banners/{id}` | 更新 |
| DELETE | `/admin/banners/{id}` | 删除 |
| PUT | `/admin/banners/{id}/status` | 启停 |
| PUT | `/admin/banners/{id}/sort` | 排序 |

### 7.4 公共接口

| 方法 | 路径 | 说明 | 鉴权 |
| :-- | :-- | :-- | :-- |
| GET | `/categories` | 所有分类 | 公开 |
| GET | `/products/**` | 商品相关 | 公开 |
| GET | `/banners` | 轮播 | 公开 |
| GET | `/seller/info/{sellerId}` | 商家信息 | 公开 |
| GET | `/seller/{sellerId}/reviews` | 商家评价 | 公开 |
| POST | `/upload` | 文件上传 | 公开 |
| GET | `/api/v1/files/{filename}` | 文件访问 | - |
| POST | `/payment/callback` | 支付宝异步回调 | 关闭 CSRF |
| POST | `/logistics/callback` | 快递鸟回调 | 关闭 CSRF |

---

## 八、项目部署

### 8.1 环境要求

| 组件 | 版本要求 | 说明 |
| :-- | :-- | :-- |
| JDK | 17+ | pom.xml 配置 `java.version=17`，Dockerfile 使用 openjdk-17 |
| Maven | 3.8+ | 后端构建 |
| Node.js | ^20.19.0 或 ≥22.12.0 | 前端构建（见 package.json engines）|
| MySQL | 5.7+（推荐 8.0）| 数据库，字符集 utf8mb4 |
| Redis | 7.x | 缓存 / 预扣库存 |
| RabbitMQ | 3.x（含 STOMP 与延迟插件）| 消息队列 |
| 内网穿透（可选）| natapp 等 | 支付宝沙箱回调 |

### 8.2 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE `shopping-mobile` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Flyway 会在后端首次启动时自动执行 `src/main/resources/db/migration/V1__Create_complete_ecommerce_schema.sql`，完成全部表结构、初始角色（ROLE_USER/ROLE_SELLER/ROLE_ADMIN）、管理员账号与默认套餐（基础/标准/高级/旗舰）的初始化。

> 默认管理员账号（见迁移脚本）：`13295370591`（密码 BCrypt 加密）。生产环境请立即修改。

### 8.3 RabbitMQ 配置（含延迟插件 + STOMP）

1. **启用 Web STOMP 插件**（端口 61613）：

```bash
rabbitmq-plugins enable rabbitmq_stomp
rabbitmq-plugins enable rabbitmq_web_stomp
```

2. **安装延迟消息插件** `rabbitmq_delayed_message_exchange`（套餐到期队列依赖）：

   - 下载与 RabbitMQ 版本匹配的 `rabbitmq_delayed_message_exchange.ez`
   - 放入 RabbitMQ `plugins` 目录
   - 执行 `rabbitmq-plugins enable rabbitmq_delayed_message_exchange`
   - 重启 RabbitMQ

3. **连接配置**（`application.yml`）：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest        # 生产环境请改为 admin/admin123
    password: guest
    virtual-host: /
    stomp:
      port: 61613          # STOMP 中继端口
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1
        concurrency: 5
        max-concurrency: 20
```

### 8.4 Redis 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:              # 无密码留空
    database: 0
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
```

> Redis 必须启动，否则预扣库存、验证码等功能失效。

### 8.5 后端打包部署

**方式一：Maven 本地运行**

```bash
cd springboot-shopping
# 修改 src/main/resources/application.yml（数据库、Redis、RabbitMQ、支付宝、快递鸟参数）
mvn spring-boot:run
# 访问 http://localhost:8088
```

**方式二：Jar 打包**

```bash
mvn clean package -DskipTests
java -jar target/springboot-shopping-0.0.1-SNAPSHOT.jar
```

**方式三：Docker**

```bash
cd springboot-shopping
docker build -t shopping-backend .
docker run -d -p 8088:8088 --name shopping-backend shopping-backend
```

### 8.6 前端构建部署

**开发环境**

```bash
cd vue-shopping
npm install
npm run dev          # http://localhost:5173
```

**生产构建**

```bash
npm run build        # 输出至 dist/
```

**Nginx 部署 + 反向代理**（参考前端 Dockerfile）

```nginx
server {
    listen 80;
    server_name your-domain;
    root /usr/share/nginx/html;          # dist 内容
    index index.html;

    location / { try_files $uri $uri /index.html; }

    location /api/   { proxy_pass http://localhost:8088; }
    location /images/{ proxy_pass http://localhost:8088; }
    location /uploads/{ proxy_pass http://localhost:8088; }
    location /ws     { proxy_pass http://localhost:8088;
                       proxy_http_version 1.1;
                       proxy_set_header Upgrade $http_upgrade;
                       proxy_set_header Connection "upgrade"; }
}
```

**Android 打包（Capacitor）**

```bash
npm run build
npx cap sync android
npx cap open android    # 在 Android Studio 中构建 APK
```

### 8.7 关键业务配置（application.yml）

| 配置项 | 默认值 | 说明 |
| :-- | :-- | :-- |
| `server.port` | 8088 | 后端端口 |
| `business.order.timeout-minutes` | 30 | 订单超时（分钟）|
| `business.order.auto-confirm-days` | 7 | 自动确认收货天数 |
| `business.coupon.user-daily-limit` | 10 | 每人每日领券上限 |
| `business.security.login-lock-minutes` | 15 | 登录失败锁定时间 |
| `business.security.max-login-attempts` | 5 | 最大登录失败次数 |
| `app.file.upload-dir` | uploads | 文件上传目录 |
| `alipay.*` | 沙箱参数 | 支付宝沙箱支付 |
| `kdniao.*` | 快递鸟 | 物流查询 |

### 8.8 支付宝沙箱与回调

- `alipay.app-id` / `private-key` / `alipay-public-key`：沙箱密钥
- `alipay.notify-url`：异步回调（需内网穿透，例如 `http://xxx.natappfree.cc/api/v1/payment/callback`）
- `alipay.return-url`：同步跳转
- `alipay.refund-notify-url`：退款回调
- 回调路径已在 `SecurityConfig` 中关闭 CSRF 校验

---

## 九、目录结构

### 9.1 后端目录树（`springboot-shopping`）

```text
springboot-shopping/
├── src/main/java/com/xiaoshan/springbootdemo/
│   ├── SpringBootApplication.java          # 启动类（@MapperScan）
│   ├── SecurityConfig.java                # Spring Security 配置（JWT/CSRF/CORS）
│   ├── component/                         # 初始化组件
│   │   ├── AdminInitializer.java          #   管理员初始化
│   │   └── StockInitializer.java          #   库存初始化（Redis）
│   ├── config/                             # 配置类
│   │   ├── JacksonConfig.java             #   JSON 序列化
│   │   ├── RabbitMQConfig.java            #   队列/交换机/绑定/延迟插件
│   │   ├── SnowflakeIdInterceptor.java    #   雪花 ID 拦截器
│   │   ├── StaticResourceConfig.java      #   静态资源映射（uploads/images）
│   │   ├── WebSocketConfig.java           #   STOMP + RabbitMQ 中继
│   │   ├── WebSocketEventListener.java    #   连接事件监听
│   │   └── WebSocketHandlerConfig.java    #   WS 处理器
│   ├── consumer/
│   │   └── PackageExpireConsumer.java     # 套餐到期消息消费者
│   ├── controller/                        # 控制器层（30 个）
│   │   ├── AdminController.java           #   管理端（商家审核/用户/商品）
│   │   ├── AddressController.java          #   收货地址
│   │   ├── BannerController.java           #   轮播图
│   │   ├── CartItemController.java         #   购物车
│   │   ├── CategoryController.java         #   分类管理
│   │   ├── ChatController.java            #   即时聊天
│   │   ├── CheckoutController.java         #   结算
│   │   ├── CouponController.java           #   优惠券
│   │   ├── FavoriteController.java         #   收藏
│   │   ├── FileController.java            #   文件访问
│   │   ├── FollowController.java           #   关注商家
│   │   ├── LogisticsController.java       #   物流（快递鸟）
│   │   ├── MerchantApplyController.java   #   商家入驻申请
│   │   ├── NotificationController.java     #   用户通知
│   │   ├── OrderController.java           #   订单
│   │   ├── OrderItemController.java       #   订单项
│   │   ├── OrderRefundController.java     #   退款售后
│   │   ├── PaymentCallbackController.java #   支付宝回调
│   │   ├── ProductController.java         #   商品
│   │   ├── RefundChatController.java      #   退款聊天
│   │   ├── ReviewController.java          #   评价
│   │   ├── SellerNotificationController.java # 商家通知
│   │   ├── SellerPackageController.java   #   商家套餐
│   │   ├── SellerProfileController.java   #   商家资料
│   │   ├── SellerQuickReplyController.java#   快捷回复
│   │   ├── UploadController.java          #   文件上传
│   │   ├── UserBrowseHistoryController.java # 浏览历史
│   │   ├── UserController.java            #   用户/认证
│   │   └── WebSocketController.java       #   WS 消息（@MessageMapping /heartbeat）
│   ├── entity/                            # 实体类
│   │   ├── dto/                           #   请求 DTO（Login/Register/Order/Product/Sku...）
│   │   ├── vo/                            #   响应 VO（Product/Order/Checkout/Logistics...）
│   │   └── *.java                         #   数据库实体（30+ 张表对应）
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java   # JWT 认证过滤器
│   ├── handler/
│   │   └── NotificationWebSocketHandler.java # 通知 WS 处理器
│   ├── mapper/                            # MyBatis Mapper（35 个）
│   ├── schedule/
│   │   └── OrderSchedule.java             # 订单超时 + 套餐过期定时兜底
│   ├── service/                           # 业务服务层（25 个）
│   │   ├── AlipayService.java             #   支付宝支付/退款
│   │   ├── OrderService.java             #   订单核心
│   │   ├── SellerPackageService.java     #   套餐/冻结/恢复
│   │   ├── WebSocketService.java          #   WS 消息推送
│   │   └── ...
│   ├── task/
│   │   └── PackageExpireTask.java         # 套餐到期兜底任务
│   └── util/                              # 工具类
│       ├── GlobalExceptionHandler.java   #   全局异常
│       ├── IpLocationUtil.java / IpUtils.java # IP/属地
│       ├── JwtUtil.java                   #   JWT 工具
│       ├── RedisConfig.java              #   Redis 配置
│       └── SnowflakeIdGenerator.java     #   雪花 ID
├── src/main/resources/
│   ├── application.yml                     # 主配置
│   ├── mapper/                             # MyBatis XML
│   └── db/migration/
│       └── V1__Create_complete_ecommerce_schema.sql # Flyway 迁移
├── Dockerfile                              # 后端 Docker 镜像
└── pom.xml                                 # Maven 依赖
```

### 9.2 前端目录树（`vue-shopping`）

```text
vue-shopping/
├── src/
│   ├── api/                                # API 接口定义
│   │   ├── adminAPI.ts                     #   管理端接口
│   │   ├── authAPI.ts                      #   认证 + 用户/商家通用接口
│   │   └── sellerAPI.ts                    #   商家端接口
│   ├── components/                         # 公共组件
│   │   ├── Chat.vue                        #   聊天组件
│   │   ├── ImagePreview.vue                #   图片预览
│   │   └── user/AddressSelector.vue        #   地址选择
│   ├── constants/
│   │   └── images.ts                       #   图片常量
│   ├── router/                             # 路由
│   │   ├── index.ts                        #   主入口 + 守卫
│   │   └── modules/
│   │       ├── admin.ts                    #   管理端路由
│   │       ├── seller.ts                   #   商家端路由
│   │       └── user.ts                     #   用户端路由
│   ├── static/                             # 静态资源
│   │   ├── audio/                          #   WS 推送音频（订单/支付/发货/套餐…）
│   │   ├── css/                            #   各页面样式（admin/seller/user/common）
│   │   ├── images/                         #   图片
│   │   └── js/js.cookie.min.js            #   Cookie 工具
│   ├── stores/                             # Pinia 状态
│   │   ├── auth.ts                         #   认证状态
│   │   ├── home.ts                         #   首页状态
│   │   └── orders.ts                       #   订单状态
│   ├── types/
│   │   └── refund.ts                       #   退款类型定义
│   ├── utils/                              # 工具
│   │   ├── axios-config.ts                 #   Axios 封装
│   │   ├── message.ts                      #   消息提示
│   │   ├── vant.ts                         #   Vant 注册
│   │   └── websocket.ts                    #   STOMP 客户端（connectUser/Seller/Admin）
│   ├── views/                              # 页面
│   │   ├── admin/                          #   管理端页面（Dashboard/Users/Sellers/Products/Category/Banner...）
│   │   ├── seller/                         #   商家端页面（Dashboard/Products/AddProduct/OrderDetail/Package/Coupons/Analytics...）
│   │   └── user/                           #   用户端页面（首页/Cart/Checkout/Orders/Refund/Messages/Chat/...）
│   ├── App.vue
│   └── main.ts
├── android/                                # Capacitor Android 工程
├── e2e/ + playwright.config.ts             # E2E 测试
├── public/ (favicon/manifest)
├── Dockerfile                              # 前端 Docker（Node 构建 + Nginx 托管）
├── vite.config.ts                          # Vite 配置（代理 /api /images /uploads /ws）
├── capacitor.config.ts                     # Capacitor 配置（appId: 云杉购）
├── eslint.config.ts / .prettierrc.json     # 代码规范
└── package.json
```

---

## 十、开发规范

### 10.1 代码风格

- **后端**：遵循阿里巴巴 Java 开发手册；Lombok 简化实体类；Controller → Service → Mapper 三层架构；DTO 入参校验使用 `spring-boot-starter-validation`。
- **前端**：Vue 3 `<script setup>` + TypeScript；组合式 API；按职责划分 `api/`、`stores/`、`views/`、`components/`、`utils/`。
- **日志**：统一 Slf4j，包级配置见 `application.yml`（`com.xiaoshan.springbootdemo: INFO`，第三方库 `WARN`）。
- **异常**：`GlobalExceptionHandler` 统一捕获并返回 `{ success, data, message }`。

### 10.2 命名规范

| 类别 | 规范 | 示例 |
| :-- | :-- | :-- |
| 数据库表/字段 | 蛇形命名 `snake_case` | `order_items`、`created_at` |
| Java 类/方法 | 大驼峰 / 小驼峰 | `OrderService`、`createOrder()` |
| MyBatis 实体 | 大驼峰，开启驼峰自动映射 | `OrderItem` → `order_items` |
| Vue 组件 | 大驼峰文件名 | `OrderDetail.vue` |
| TS 变量/函数 | 小驼峰 | `getSellerOrders` |
| API 路径 | RESTful，复数名词，统一 `/api/v1` 前缀 | `/api/v1/orders/{id}` |
| 常量 | 全大写下划线 | `ROLE_USER`、`PENDING` |

### 10.3 Git 提交规范

采用 Conventional Commits 规范：

```text
<type>(<scope>): <subject>

<body>
```

| type | 含义 |
| :-- | :-- |
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档变更 |
| `style` | 代码格式（不影响功能）|
| `refactor` | 重构 |
| `perf` | 性能优化 |
| `test` | 测试 |
| `chore` | 构建/依赖/配置 |
| `build` | 构建系统或依赖 |

示例：

```text
feat(order): 支持订单项级别退款与退货物流
fix(websocket): 修复套餐续费后旧到期消息误触发冻结
docs(readme): 新增企业级 README 文档
```

### 10.4 分支管理

| 分支 | 用途 |
| :-- | :-- |
| `main` | 生产稳定版本，受保护，仅接受 PR 合并 |
| `develop` | 集成测试主干 |
| `feature/<模块>-<简述>` | 功能开发，如 `feature/refund-chat` |
| `fix/<问题>-<简述>` | 缺陷修复，如 `fix/order-stock-rollback` |
| `hotfix/<简述>` | 生产紧急修复，基于 `main` 切出并回合并 |

> 提交前执行 `npm run lint`、`npm run type-check`；后端执行 `mvn clean package -DskipTests` 保证构建通过。

---

<div align="center">

**云杉购** · 完整 B2C 电商平台 · Spring Boot 3.5.0 + Vue 3 + WebSocket + RabbitMQ

</div>
