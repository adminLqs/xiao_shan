# 🛒 电商平台 - 前后端分离全栈项目

一个功能完整的B2C电商平台，采用Spring Boot + Vue 3技术栈，实现了完整的购物流程和商家管理功能。

## 📋 项目简介

本项目是一个完整的电商平台，包含用户端、商家端和管理员端三大模块。实现了从商品浏览、购物车、订单支付到物流跟踪的完整电商闭环。

### 核心亮点
- 🔐 **JWT + HttpOnly Cookie** 安全认证，防止XSS攻击
- 🚀 **Redis预扣库存** 方案，解决高并发超卖问题
- 💳 **支付宝沙箱支付** 完整接入，支持支付回调
- 🚚 **快递鸟API** 物流轨迹实时追踪
- 📝 **评论系统** 支持图片上传和评分
- ⏰ **定时任务** 自动取消超时未支付订单

## 🛠️ 技术栈

### 后端
| 技术            | 版本  | 说明            |
| --------------- | ----- | --------------- |
| Spring Boot     | 2.7.x | 核心框架        |
| Spring Security | -     | 认证授权        |
| MyBatis         | -     | ORM框架         |
| MySQL           | 8.0   | 数据库          |
| Redis           | 7.x   | 缓存 + 预扣库存 |
| JWT             | -     | Token生成       |
| 支付宝SDK       | -     | 支付接入        |
| 快递鸟API       | -     | 物流查询        |
| Flyway          | -     | 数据库版本管理  |

### 前端
| 技术       | 说明         |
| ---------- | ------------ |
| Vue 3      | 渐进式框架   |
| TypeScript | 类型安全     |
| Pinia      | 状态管理     |
| Vue Router | 路由管理     |
| Vant 4     | 移动端组件库 |
| Axios      | HTTP请求     |
| Vite       | 构建工具     |

## 📁 项目结构

xiaoshan-springboot/
├── src/main/java/com/xiaoshan/springbootdemo/
│ ├── config/ # 配置类（Security、Redis、CORS）
│ ├── controller/ # 控制器层（14个）
│ │ ├── UserController # 用户认证/个人资料
│ │ ├── ProductController # 商品管理
│ │ ├── OrderController # 订单管理
│ │ ├── CartItemController # 购物车
│ │ ├── ReviewController # 评论系统
│ │ ├── LogisticsController # 物流查询
│ │ └── ...
│ ├── service/ # 业务逻辑层
│ ├── mapper/ # 数据访问层（MyBatis）
│ ├── entity/ # 实体类/DTO/VO
│ ├── filter/ # JWT过滤器
│ ├── schedule/ # 定时任务
│ └── util/ # 工具类
├── src/main/resources/
│ ├── application-dev.yml # 配置文件
│ └── db/migration/ # Flyway数据库脚本
└── pom.xml # Maven依赖

## 🚀 快速启动

### 前置条件
- JDK 17+
- MySQL 8.0+
- Redis 7.x+
- Maven 3.8+
- Node.js 18+（前端）

### 1. 克隆项目
```bash
git clone https://github.com/adminLqs/xiao_shan.git
```

### 2. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE shopping CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Flyway会自动执行迁移脚本
-- 脚本位置: src/main/resources/db/migration/
```

### 3. 后端启动

```bash
cd xiao_shan

# 修改配置文件
vim src/main/resources/application-dev.yml
# 配置：数据库密码、Redis、支付宝参数、快递鸟密钥

# 启动项目
mvn spring-boot:run
# 访问: http://localhost:8088
```

### 4. 前端启动

```bash
cd vue-shopping

# 安装依赖
npm install

# 开发环境启动
npm run dev
# 访问: http://localhost:5173
```



## 📖 核心功能

### 用户端

| 功能模块   | 说明                       |
| :--------- | :------------------------- |
| 登录/注册  | JWT + HttpOnly Cookie      |
| 商品浏览   | 分类筛选、关键词搜索、分页 |
| 购物车     | 添加/删除/修改数量         |
| 订单结算   | 地址选择、金额计算         |
| 支付宝支付 | 沙箱环境完整流程           |
| 订单管理   | 查询/取消/确认收货         |
| 物流追踪   | 快递鸟API实时轨迹          |
| 商品评价   | 评分+图文评论              |
| 收藏功能   | 关注心仪商品               |
| 个人资料   | 头像上传、信息修改         |

### 商家端

| 功能模块 | 说明                |
| :------- | :------------------ |
| 商品管理 | 发布/编辑/上架/下架 |
| 订单处理 | 确认处理/发货       |
| 物流填写 | 支持快递鸟自动识别  |
| 店铺信息 | 头像/横幅/简介设置  |
| 入驻申请 | 提交营业执照等材料  |

### 管理员端

| 功能模块 | 说明         |
| :------- | :----------- |
| 商家审核 | 审核入驻申请 |
| 分类管理 | 商品分类维护 |



## 🔧 核心设计

### 1. 安全认证架构

```text
用户登录 → JWT生成 → HttpOnly Cookie存储 → 
每次请求携带Cookie → JWT过滤器验证 → 
SecurityContextHolder设置认证信息
```



### 2. 高并发库存方案

```text
下单流程：
1. Redis原子扣减库存（decrement）
2. 预扣库存存入Hash（30分钟过期）
3. 支付成功 → 扣减MySQL库存
4. 支付超时 → 定时任务回滚Redis库存
```



### 3. 支付流程

```text
创建订单 → 返回支付宝HTML表单 → 
用户扫码支付 → 支付宝异步回调 → 
验签 → 更新订单状态 → 扣库存/增销量/删购物车
```



## 📊 数据库设计

核心表结构（15张表）：

- `users` / `user_profiles` - 用户及资料
- `seller_profiles` - 商家信息
- `categories` - 商品分类
- `products` / `product_images` - 商品及图片
- `orders` / `order_items` - 订单及商品快照
- `cart_items` - 购物车
- `addresses` - 收货地址
- `reviews` / `review_images` - 评论
- `favorites` - 收藏
- `merchant_apply` - 商家入驻申请

## 🎯 环境配置说明

### 支付宝沙箱（测试用）

yaml

```yaml
alipay:
  app-id: 你的APPID
  private-key: 应用私钥
  alipay-public-key: 支付宝公钥
  notify-url: 你的回调地址（需内网穿透）
```



### 快递鸟配置

```yaml
kdniao:
  app:
    key: 1918612  # 用户ID
    secret: 你的密钥
```



## 📝 注意事项

1. **支付宝回调地址**需要使用内网穿透工具（如natapp）
2. **快递鸟物流**顺丰快递需要传收件人手机号后4位
3. **Redis必须启动**，否则预扣库存功能失效
4. **图片上传目录**默认为项目根目录的`uploads`文件夹

## 👨‍💻 作者

- GitHub: [@adminLqs](https://github.com/adminLqs)

## 📄 许可证

MIT License

------

## 🔗 相关链接

- [GitHub仓库](https://github.com/adminLqs/xiao_shan)
- [前端项目](https://github.com/adminLqs/vue-shopping)（如有）

```text
这个README涵盖了：
- ✅ 项目简介和亮点
- ✅ 完整技术栈表格
- ✅ 项目结构说明
- ✅ 快速启动步骤
- ✅ 功能模块清单
- ✅ 核心设计思路
- ✅ 数据库设计
- ✅ 环境配置说明
```





============================================
云杉购 全局字体规范表
============================================

一、首页 / 分类浏览 / 搜索 / 店铺 / 收藏（商品列表卡片）
--------------------------------------------
商品名称      15px   font-weight 500   #1e293b
当前价格      16px   font-weight 600   #ef4444
原价划线      12px   #94a3b8           text-decoration: line-through
已售文字      11px   #94a3b8


二、商品详情页
--------------------------------------------
商品名称      17px   font-weight 500   #1e293b
当前价格      24px   font-weight 700   #ef4444
原价划线      16px   #94a3b8           text-decoration: line-through
已售文字      13px   #94a3b8


三、购物车
--------------------------------------------
商品名称      14px   font-weight 500   #1e293b
规格标签      12px   #64748b
当前价格      15px   font-weight 600   #ef4444
原价划线      12px   #94a3b8           text-decoration: line-through
结算栏总价    20px   font-weight 700   #4a6491（渐变）


四、订单列表（用户端 + 商家端）
--------------------------------------------
商品名称      14px   font-weight 500   #1e293b
规格标签      12px   #64748b
单价          15px   font-weight 600   #ef4444
实付款标签    12px   #64748b
实付款金额    18px   font-weight 700   #ef4444


五、订单详情（用户端 + 商家端）
--------------------------------------------
商品名称      14px   font-weight 500   #1e293b
规格标签      12px   #64748b
单价          15px   font-weight 600   #ef4444
实付款标签    12px   #64748b
实付款金额    20px   font-weight 700   #ef4444


六、支付结算页
--------------------------------------------
商品名称      14px   font-weight 500   #1e293b
规格标签      12px   #64748b
单价          15px   font-weight 600   #ef4444
合计金额      22px   font-weight 700   #ef4444


七、商家端商品管理页
--------------------------------------------
商品名称      15px   font-weight 500   #1e293b
价格          16px   font-weight 600   #ef4444


八、导航栏/标题
--------------------------------------------
页面标题      17px   font-weight 700   #4a6491（渐变）
品牌名        16px   font-weight 700   #4a6491（渐变）





```
============================================
统一规范（以列表页为准）
============================================

已完成 → 灰 #e2e8f0 / #64748b
已退款 → 红 #fee2e2 / #dc2626
退款中 → 橙 #fef3c7 / #d97706
已付款 → 蓝 #dbeafe / #2563eb
处理中 → 紫 #f3e8ff / #9333ea
已发货 → 青 #cffafe / #0891b2
已送达 → 靛蓝 #e0e7ff / #4f46e5
已取消 → 红 #fee2e2 / #dc2626

============================================
需修改的文件
============================================

1. 订单详情页（用户端 OrderDetail.vue + 商家端 SellerOrderDetail.vue）
   - 已完成 → 改为灰色
   - 其他状态对照上面规范统一

2. 商家端订单管理页 SellerOrders.vue
   - 已退款 → 确认是红色
   - 已完成 → 确认是灰色

3. 搜索所有 CSS 文件中 .status- 开头的样式
   - 确保同一状态在列表页和详情页颜色一致
```