package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.*;
import com.xiaoshan.springbootdemo.entity.dto.OrderDTO;
import com.xiaoshan.springbootdemo.entity.vo.OrderWithItemsVO;
import com.xiaoshan.springbootdemo.mapper.*;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CartItemMapper cartItemMapper;
    private final LogisticsService logisticsService;
    private final ProductImageMapper productImageMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final WebSocketService webSocketService;
    private final NotificationService notificationService;
    private final SellerProfileMapper sellerProfileMapper;
    private final UserProfileMapper userProfileMapper;
    private final CouponService couponService;
    private final UserCouponMapper userCouponMapper;
    private final OrderCouponMapper orderCouponMapper;
    private final CouponMapper couponMapper;

    /**
     * 创建订单（结算页直接支付）- 单订单版本
     * @param userId   用户ID
     * @param orderDTO 订单信息（包含地址ID、支付方式、商品列表）
     * @return 创建的订单
     */
    @Transactional
    public Order createOrder(Long userId, OrderDTO orderDTO) {
        return createOrderInternal(userId, orderDTO.getAddressId(), orderDTO.getSource(), orderDTO.getOrderItems());
    }

    /**
     * 按卖家拆单创建多个订单
     * @param userId   用户ID
     * @param orderDTO 订单信息
     * @return 创建的订单列表
     */
    @Transactional
    public List<Order> createOrdersBySeller(Long userId, OrderDTO orderDTO) {
        List<Order> orders = new ArrayList<>();

        Map<Long, List<OrderItem>> itemsBySeller = orderDTO.getOrderItems().stream()
                .collect(Collectors.groupingBy(item -> {
                    if (item.getSellerId() != null) {
                        return item.getSellerId();
                    }
                    Product product = productMapper.findById(item.getProductId()).orElse(null);
                    return product != null ? product.getSellerId() : 0L;
                }));

        UserCoupon userCoupon = null;
        Coupon coupon = null;
        Long couponSellerId = null;

        if (orderDTO.getUserCouponId() != null) {
            userCoupon = userCouponMapper.findById(orderDTO.getUserCouponId())
                    .orElseThrow(() -> new RuntimeException("优惠券不存在"));
            if (!userId.equals(userCoupon.getUserId())) {
                throw new RuntimeException("无权使用该优惠券");
            }
            if (!"UNUSED".equals(userCoupon.getStatus())) {
                throw new RuntimeException("优惠券状态异常");
            }
            coupon = couponMapper.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new RuntimeException("优惠券模板不存在"));
            couponSellerId = coupon.getSellerId();
        }

        for (Map.Entry<Long, List<OrderItem>> entry : itemsBySeller.entrySet()) {
            Long sellerId = entry.getKey();
            List<OrderItem> sellerItems = entry.getValue();
            Order order = createOrderInternal(userId, orderDTO.getAddressId(), orderDTO.getSource(), sellerItems);

            if (userCoupon != null && coupon != null && sellerId.equals(couponSellerId)) {
                BigDecimal discountAmount = couponService.calculateDiscount(coupon, order.getTotalAmount());
                if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                    order.setDiscountAmount(discountAmount);
                    order.setTotalAmount(order.getTotalAmount().subtract(discountAmount).max(BigDecimal.ZERO));
                    orderMapper.updateDiscountAmount(order.getId(), discountAmount, order.getTotalAmount());

                    OrderCoupon orderCoupon = new OrderCoupon();
                    orderCoupon.setId(snowflakeIdGenerator.nextId());
                    orderCoupon.setOrderId(order.getId());
                    orderCoupon.setUserCouponId(userCoupon.getId());
                    orderCoupon.setCouponId(coupon.getId());
                    orderCoupon.setCouponName(coupon.getName());
                    orderCoupon.setCouponType(coupon.getType());
                    orderCoupon.setDiscountAmount(discountAmount);
                    orderCouponMapper.insert(orderCoupon);

                    userCouponMapper.markUsed(userCoupon.getId(), order.getId());
                    couponMapper.incrementUsedCount(coupon.getId());

                    log.info("订单使用优惠券: orderId={}, userCouponId={}, couponId={}, discountAmount={}",
                            order.getId(), userCoupon.getId(), coupon.getId(), discountAmount);
                }
            }

            orders.add(order);
        }

        return orders;
    }

    /**
     * 内部方法：创建单个订单
     */
    private Order createOrderInternal(Long userId, Long addressId, String source, List<OrderItem> orderItems) {
        String orderNumber = generateOrderNumber();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> processedItems = new ArrayList<>();

        for (OrderItem item : orderItems) {
            Product product = productMapper.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + item.getProductId()));

            if (item.getSkuId() != null) {
                String realStockKey = "sku:stock:" + item.getSkuId();
                String reservedStockKey = "sku:stock:reserved:" + item.getSkuId();

                Object realStockObj = redisTemplate.opsForValue().get(realStockKey);
                Long realStock = realStockObj instanceof Number 
                    ? ((Number) realStockObj).longValue() 
                    : null;
                
                if (realStock == null) {
                    ProductSku sku = productSkuMapper.findById(item.getSkuId())
                            .orElseThrow(() -> new RuntimeException("SKU不存在"));
                    realStock = sku.getStock() != null ? sku.getStock().longValue() : 0L;
                    redisTemplate.opsForValue().set(realStockKey, realStock);
                }

                Object reservedStockObj = redisTemplate.opsForValue().get(reservedStockKey);
                Long reservedStock = reservedStockObj instanceof Number 
                    ? ((Number) reservedStockObj).longValue() 
                    : 0L;

                Long availableStock = realStock - reservedStock;
                if (availableStock < item.getQuantity()) {
                    throw new RuntimeException("商品「" + product.getName() + "」库存不足");
                }

                redisTemplate.opsForValue().increment(reservedStockKey, item.getQuantity());
                log.info("预扣 SKU 库存: skuId={}, quantity={}", item.getSkuId(), item.getQuantity());

                String prestockKey = "order:prestock:" + orderNumber;
                redisTemplate.opsForHash().put(prestockKey, "sku:" + item.getSkuId(),
                        String.valueOf(item.getQuantity()));
                redisTemplate.expire(prestockKey, 30, TimeUnit.MINUTES);

            } else {
                String stockKey = "product:stock:" + item.getProductId();

                Boolean keyExists = redisTemplate.hasKey(stockKey);
                if (keyExists == null || !keyExists) {
                    Integer dbStock = productSkuMapper.getTotalStockByProductId(product.getId());
                    redisTemplate.opsForValue().set(stockKey, dbStock != null ? dbStock : 0);
                }

                Long remainStock = redisTemplate.opsForValue().decrement(stockKey, item.getQuantity());

                if (remainStock == null || remainStock < 0) {
                    if (remainStock != null) {
                        redisTemplate.opsForValue().increment(stockKey, item.getQuantity());
                    }
                    throw new RuntimeException("商品「" + product.getName() + "」库存不足");
                }

                String prestockKey = "order:prestock:" + orderNumber;
                redisTemplate.opsForHash().put(prestockKey,
                        String.valueOf(item.getProductId()),
                        String.valueOf(item.getQuantity()));
                redisTemplate.expire(prestockKey, 30, TimeUnit.MINUTES);
            }

            item.setSellerId(product.getSellerId());
            item.setProductName(product.getName());
            
            String productMainImage = getProductMainImage(item.getProductId());
            
            if (item.getSkuId() != null) {
                ProductSku sku = productSkuMapper.findById(item.getSkuId()).orElse(null);
                if (sku != null) {
                    item.setSkuName(sku.getSkuName());
                    item.setPrice(sku.getPrice() != null ? sku.getPrice() : BigDecimal.ZERO);
                    if (sku.getSkuImage() != null && !sku.getSkuImage().isEmpty()) {
                        item.setProductImage(sku.getSkuImage());
                    } else {
                        item.setProductImage(productMainImage);
                    }
                } else {
                    item.setProductImage(productMainImage);
                    BigDecimal productPrice = productSkuMapper.getMinPriceByProductId(product.getId());
                    item.setPrice(productPrice != null ? productPrice : BigDecimal.ZERO);
                }
            } else {
                item.setProductImage(productMainImage);
                BigDecimal productPrice = productSkuMapper.getMinPriceByProductId(product.getId());
                item.setPrice(productPrice != null ? productPrice : BigDecimal.ZERO);
            }
            
            BigDecimal itemPrice = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
            item.setTotalPrice(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            item.setCreatedAt(LocalDateTime.now());
            item.setId(snowflakeIdGenerator.nextId());

            totalAmount = totalAmount.add(item.getTotalPrice());
            processedItems.add(item);
        }

        Order order = new Order();
        order.setId(snowflakeIdGenerator.nextId());
        order.setOrderNumber(orderNumber);
        order.setSource(source);
        order.setUserId(userId);
        order.setAddressId(addressId);
        order.setTotalAmount(totalAmount);

        orderMapper.insert(order);

        for (OrderItem item : processedItems) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.batchInsert(processedItems);

        log.info("订单创建成功: orderId={}, orderNumber={}, userId={}, totalAmount={}",
                order.getId(), order.getOrderNumber(), userId, totalAmount);

        webSocketService.sendNewOrder(order.getOrderNumber(), totalAmount);

        Map<Long, List<OrderItem>> sellerItems = processedItems.stream()
                .filter(item -> item.getSellerId() != null)
                .collect(Collectors.groupingBy(OrderItem::getSellerId));

        for (Map.Entry<Long, List<OrderItem>> entry : sellerItems.entrySet()) {
            Long sellerId = entry.getKey();
            BigDecimal sellerAmount = entry.getValue().stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String extraData = "{\"orderId\":" + order.getId() + "}";
            notificationService.create(sellerId, "ORDER", "新订单通知",
                    "您有一个新订单：" + order.getOrderNumber() + "，金额：¥" + sellerAmount, extraData);
        }

        return order;
    }

    /**
     * 处理支付成功业务逻辑
     * 职责：更新订单状态、扣库存、增销量、删除购物车
     * 这个方法可以被多种支付方式复用：
     * - 支付宝回调调用
     * - 微信回调调用
     * - 其他支付方式调用
     *
     * @param orderNumber    订单号
     * @param transactionId  支付平台交易号
     * @param paymentMethod  支付方式（ALIPAY / WECHAT）
     */
    @Transactional
    public void handlePaymentSuccess(String orderNumber, String transactionId, String paymentMethod) {
        // 查询订单
        Order order = orderMapper.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderNumber));

        // 幂等性处理：防止重复回调
        if (order.getStatus() == Order.OrderStatus.PAID) {
            log.info("订单已支付，无需重复处理: {}", orderNumber);
            return;
        }

        // 校验 Redis 预扣库存记录存在（下单时已经预扣，这里只做确认，不再重复扣减）
        String prestockKey = "order:prestock:" + orderNumber;
        Boolean prestockExists = redisTemplate.hasKey(prestockKey);
        if (prestockExists == null || !prestockExists) {
            log.warn("未找到 Redis 预扣库存记录，可能已超时或非本系统订单: orderNumber={}", orderNumber);
        }

        // 查询所有订单项（用于删除购物车商品）
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(order.getId());

        // 更新订单状态：PENDING → PAID，以及支付时间、交易单号
        order.setStatus(Order.OrderStatus.PAID);
        order.setPaymentMethod(Order.PaymentMethod.valueOf(paymentMethod));
        order.setTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updatePaymentInfo(order);

        // 支付成功后清除预扣记录（Redis 预扣即真正消耗，不再回滚）
        if (prestockExists != null && prestockExists) {
            redisTemplate.delete(prestockKey);
            log.info("清除预扣库存记录: orderNumber={}", orderNumber);
        }

        // 增加商品销量
        for (OrderItem item : orderItems) {
            productMapper.incrementSalesCount(item.getProductId(), item.getQuantity());
            log.info("增加商品销量 - productId: {}, quantity: {}", item.getProductId(), item.getQuantity());
        }

        log.info("订单支付成功 - 订单号: {}", orderNumber);

        // 发送支付成功通知给用户
        webSocketService.sendUserPaymentSuccess(order.getUserId(), order.getId(), orderNumber);

        // 发送支付成功通知给商家（广播）
        webSocketService.sendSellerPaymentSuccess(orderNumber, order.getTotalAmount());

        // 如果来源是购物车，删除购物车中的商品
        if ("cart".equals(order.getSource())) {
            List<Long> productIds = orderItems.stream()
                    .map(OrderItem::getProductId)
                    .collect(Collectors.toList());
            cartItemMapper.deleteByUserIdAndProductIds(order.getUserId(), productIds);

        }
    }

    /**
     * 获取用户订单列表（含订单项）
     * 参考商家订单列表的实现逻辑，返回包含订单项的完整订单信息
     *
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 订单状态
     * @return 订单VO列表（包含订单信息和订单项）
     */
    public List<OrderWithItemsVO> getUserOrdersWithItems(Long userId, int page, int pageSize, String[] status) {
        // 计算分页偏移量
        int offset = (page - 1) * pageSize;

        // 查询用户订单ID列表
        List<Long> orderIds = orderMapper.findUserOrderIds(userId, offset, pageSize, status);

        // 订单ID为空时返回空列表
        if (orderIds == null || orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询订单详情
        List<Order> orders = orderMapper.findByIds(orderIds);

        List<OrderItem> allOrderItems = orderItemMapper.findOrderItemsByOrderIds(orderIds);

        final Map<Long, List<OrderItem>> orderItemMap = allOrderItems != null 
                ? allOrderItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId))
                : new HashMap<>();

        // 将订单信息和订单项列表合并，并查询商家信息
        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemMap.getOrDefault(order.getId(), Collections.emptyList());
            String sellerName = "商家";
            String sellerAvatar = null;
            
            if (!items.isEmpty() && items.get(0).getSellerId() != null) {
                Long sellerId = items.get(0).getSellerId();
                String[] sellerInfo = getSellerInfo(sellerId);
                sellerName = sellerInfo[0];
                sellerAvatar = sellerInfo[1];
            }
            
            return new OrderWithItemsVO(order, items, sellerName, sellerAvatar);
        }).collect(Collectors.toList());
    }

    /**
     * 统计用户订单总数（含状态筛选）
     *
     * @param userId 用户ID
     * @param status 订单状态
     * @return 订单总数
     */
    public long countUserOrdersWithStatus(Long userId, String[] status) {
        return orderMapper.countByUserId(userId, status);
    }

    /**
     * 获取各状态订单数量统计
     */
    public Map<String, Long> getOrderCountsByUserId(Long userId) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("total", getOrDefault(orderMapper.countByUserId(userId, null), 0L));
        counts.put("PENDING", getOrDefault(orderMapper.countByUserId(userId, new String[]{"PENDING"}), 0L));
        counts.put("PAID", getOrDefault(orderMapper.countByUserId(userId, new String[]{"PAID"}), 0L));
        counts.put("PROCESSING", getOrDefault(orderMapper.countByUserId(userId, new String[]{"PROCESSING"}), 0L));
        counts.put("SHIPPED", getOrDefault(orderMapper.countByUserId(userId, new String[]{"SHIPPED"}), 0L));
        counts.put("COMPLETED", getOrDefault(orderMapper.countByUserId(userId, new String[]{"COMPLETED"}), 0L));
        counts.put("CANCELLED", getOrDefault(orderMapper.countByUserId(userId, new String[]{"CANCELLED"}), 0L));
        counts.put("pendingReview", getOrDefault(orderItemMapper.countPendingReviewByUserId(userId), 0L));
        return counts;
    }

    private Long getOrDefault(Long value, Long defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * 获取订单详情
     */
    public Order getOrderDetail(Long userId, Long orderId) {
        return orderMapper.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    /**
     * 获取订单信息
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 订单
     */
    public Order getOrderFullDetail(Long userId, Long orderId) {
        return orderMapper.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("获取订单信息失败"));
    }

    /**
     * 取消订单
     * @param userId 用户ID
     * @param orderId 订单ID
     */
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("只有待付款的订单才能取消");
        }

        String prestockKey = "order:prestock:" + order.getOrderNumber();
        Map<Object, Object> prestockMap = redisTemplate.opsForHash().entries(prestockKey);

        if (prestockMap != null && !prestockMap.isEmpty()) {
            for (Map.Entry<Object, Object> entry : prestockMap.entrySet()) {
                String key = (String) entry.getKey();
                Integer quantity = Integer.valueOf((String) entry.getValue());
                handleStockRollback(key, quantity);
            }
            redisTemplate.delete(prestockKey);
        } else {
            log.warn("预扣记录不存在或已过期，从数据库查询订单项进行回滚: orderId={}", orderId);
            List<OrderItem> orderItems = orderItemMapper.findByOrderId(orderId);
            for (OrderItem item : orderItems) {
                if (item.getSkuId() != null) {
                    String key = "sku:" + item.getSkuId();
                    handleStockRollback(key, item.getQuantity());
                } else {
                    String key = String.valueOf(item.getProductId());
                    handleStockRollback(key, item.getQuantity());
                }
            }
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateStatus(order.getId(), Order.OrderStatus.CANCELLED.name());
        orderMapper.updateCancelledTime(order.getId(), order.getCancelledAt());
    }

    private void handleStockRollback(String key, Integer quantity) {
        if (key.startsWith("sku:")) {
            Long skuId = Long.valueOf(key.substring(4));
            String reservedStockKey = "sku:stock:reserved:" + skuId;

            Object reservedObj = redisTemplate.opsForValue().get(reservedStockKey);
            Long currentReserved = reservedObj instanceof Number 
                ? ((Number) reservedObj).longValue() 
                : null;

            if (currentReserved == null || currentReserved < quantity) {
                log.warn("Redis预扣库存异常，从数据库重新计算: skuId={}, currentReserved={}, quantity={}", 
                        skuId, currentReserved, quantity);
                Long dbReserved = orderItemMapper.sumReservedBySkuId(skuId);
                if (dbReserved == null) dbReserved = 0L;
                redisTemplate.opsForValue().set(reservedStockKey, dbReserved);
                currentReserved = dbReserved;
            }

            if (currentReserved >= quantity) {
                redisTemplate.opsForValue().increment(reservedStockKey, -quantity);
                log.info("回滚 SKU 预扣库存: skuId={}, quantity={}", skuId, quantity);
            } else {
                log.warn("预扣库存不足，无法回滚: skuId={}, currentReserved={}, quantity={}", 
                        skuId, currentReserved, quantity);
            }
        } else {
            Long productId = Long.valueOf(key);
            String stockKey = "product:stock:" + productId;
            redisTemplate.opsForValue().increment(stockKey, quantity);
            log.info("回滚商品库存: productId={}, quantity={}", productId, quantity);
        }
    }

    /**
     * 确认收货
     */
    @Transactional
    public void confirmReceive(Long userId, Long orderId) {
        Order order = orderMapper.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getStatus() != Order.OrderStatus.SHIPPED) {
            throw new RuntimeException("只有已发货的订单才能确认收货");
        }

        orderMapper.confirmReceive(orderId);

        // 处理订单项的售后状态
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            String refundStatus = item.getRefundStatus();
            
            if (refundStatus == null || refundStatus.isEmpty()) {
                // 无退款记录 → 正常确认收货，无需处理
                continue;
            }
            
            // 根据不同售后状态处理：
            // 1. SUCCESS（已退款完成）→ 保持不变
            // 2. PROCESSING（退款中）→ 确认收货不影响退款流程，保持不变
            // 3. FAILED（卖家拒绝退款）→ 买家确认收货后关闭售后，更新为SUCCESS
            if ("FAILED".equals(refundStatus)) {
                orderItemMapper.updateRefundStatus(item.getId(), "SUCCESS");
            }
            // 其他状态（WAITING_RETURN, RETURNING）保持不变
        }
    }

    /**
     * 删除订单（软删除）
     */
    @Transactional
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderMapper.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getStatus() != Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("只有已取消的订单才能删除");
        }

        orderMapper.softDeleteById(orderId, userId);


    }


    /**
     * 获取商品主图
     */
    private String getProductMainImage(Long productId) {
        // 查询商品主图
        return productImageMapper.findMainImageByProductId(productId);
    }

    /**
     * 获取评价相关订单项列表
     *
     * @param userId 用户ID
     * @param type 类型（pending: 待评价, reviewed: 已评价）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 订单项列表和分页信息
     */
    public Map<String, Object> getReviewItems(Long userId, String type, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> items;
        long total;
        
        if ("pending".equalsIgnoreCase(type)) {
            // 查询已完成订单中未评价的订单项
            items = orderItemMapper.findPendingReviewItems(userId, offset, pageSize);
            total = orderItemMapper.countPendingReviewByUserId(userId);
        } else {
            // 查询已评价的订单项
            items = orderItemMapper.findReviewedItems(userId, offset, pageSize);
            total = orderItemMapper.countReviewedByUserId(userId);
        }
        
        int totalPages = (int) Math.ceil((double) total / pageSize);
        
        return Map.of(
                "records", items,
                "total", total,
                "page", page,
                "pageSize", pageSize,
                "totalPages", totalPages,
                "hasMore", page < totalPages
        );
    }


    /**
     * 生成订单号
     * 格式: ORD + yyyyMMddHHmmss + 4位随机数
     */
    private String generateOrderNumber() {
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.now());
        int random = (int) (Math.random() * 10000);
        return "ORD" + timestamp + String.format("%04d", random);
    }


    // ================= 商家逻辑 ===============

    /**
     * 根据商家ID和订单ID查询订单
     *
     * @param sellerId 商家ID
     * @param orderId 订单ID
     * @return 订单信息
     */
    public Order getOrderBySellerId(Long sellerId, Long orderId) {
        return orderMapper.findByIdAndSellerId(orderId, sellerId)
                .orElse(null);
    }

    /**
     * 获取商家订单详情
     * 商家端使用：返回订单信息
     *
     * @param sellerId 商家ID
     * @param orderId 订单ID
     * @return 订单信息
     */
    public Order getSellerOrder(Long sellerId, Long orderId) {
        // 查询订单基本信息
        return orderMapper.findByIdAndSellerId(orderId, sellerId)
                .orElseThrow(() -> new RuntimeException("订单不存在或无权操作"));
    }

    /**
     * 获取商家订单列表（分页）
     *
     * @param sellerId 商家ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 订单状态
     * @return 订单VO列表
     */
    public List<OrderWithItemsVO> getSellerOrders(Long sellerId, int page, int pageSize, String[] status) {
        return getSellerOrders(sellerId, page, pageSize, status, null);
    }

    public List<OrderWithItemsVO> getSellerOrders(Long sellerId, int page, int pageSize, String[] status, String[] refundStatus) {
        int offset = (page - 1) * pageSize;

        List<Long> orderIds;
        if (refundStatus != null && refundStatus.length > 0) {
            orderIds = orderMapper.findSellerOrderIdsByRefundStatus(sellerId, offset, pageSize, refundStatus);
        } else {
            orderIds = orderMapper.findSellerOrderIds(sellerId, offset, pageSize, status);
        }

        if (orderIds == null || orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Order> orders = orderMapper.findByIds(orderIds);

        List<OrderItem> allOrderItems = orderItemMapper.findByOrderIds(orderIds);

        final Map<Long, List<OrderItem>> orderItemMap = allOrderItems != null 
                ? allOrderItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId))
                : new HashMap<>();

        final String[] sellerInfo = getSellerInfo(sellerId);
        final String name = sellerInfo[0];
        final String avatar = sellerInfo[1];

        return orders.stream()
                .map(order -> new OrderWithItemsVO(
                        order,
                        orderItemMap.getOrDefault(order.getId(), Collections.emptyList()),
                        name,
                        avatar
                ))
                .collect(Collectors.toList());
    }

    /**
     * 查询商家名称和头像
     * @param sellerId 商家ID
     * @return String数组，[0]为商家名称，[1]为商家头像
     */
    public String[] getSellerInfo(Long sellerId) {
        String sellerName = "商家";
        String sellerAvatar = null;
        
        Optional<SellerProfile> sellerProfile = sellerProfileMapper.findByUserId(sellerId);
        if (sellerProfile.isPresent()) {
            SellerProfile sp = sellerProfile.get();
            sellerName = sp.getStoreName();
            sellerAvatar = sp.getStoreAvatar();
        }
        
        if (sellerName == null || sellerName.isEmpty()) {
            Optional<UserProfile> userProfile = userProfileMapper.findByUserId(sellerId);
            if (userProfile.isPresent()) {
                UserProfile up = userProfile.get();
                sellerName = up.getNickname();
                if (sellerAvatar == null) {
                    sellerAvatar = up.getAvatar();
                }
            }
        }
        
        if (sellerName == null || sellerName.isEmpty()) {
            sellerName = "商家";
        }
        
        return new String[]{sellerName, sellerAvatar};
    }

    // 辅助方法：枚举转字符串
    private String convertEnum(Enum<?> enumValue) {
        return enumValue != null ? enumValue.name() : null;
    }

    /**
     * 统计商家订单总数
     *
     * @param sellerId 商家ID
     * @param status 订单状态
     * @return 订单总数
     */
    public long countSellerOrders(Long sellerId, String[] status) {
        return countSellerOrders(sellerId, status, null);
    }

    public long countSellerOrders(Long sellerId, String[] status, String[] refundStatus) {
        if (refundStatus != null && refundStatus.length > 0) {
            return orderMapper.countSellerOrdersByRefundStatus(sellerId, refundStatus);
        }
        return orderMapper.countSellerOrders(sellerId, status);
    }

    /**
     * 统计商家各状态订单数量
     *
     * @param sellerId 商家ID
     * @return 各状态订单数量统计
     */
    public Map<String, Long> getSellerOrderCounts(Long sellerId) {
        List<Map<String, Object>> list = orderMapper.getSellerOrderCounts(sellerId);
        Map<String, Long> counts = new HashMap<>();
        counts.put("total", 0L);
        counts.put("PENDING", 0L);
        counts.put("PAID", 0L);
        counts.put("SHIPPED", 0L);
        counts.put("COMPLETED", 0L);
        counts.put("CANCELLED", 0L);
        counts.put("REFUNDING", 0L);

        if (list != null) {
            for (Map<String, Object> item : list) {
                String status = (String) item.get("status");
                Long count = (Long) item.get("count");
                counts.put(status, count);
                counts.put("total", counts.get("total") + count);
            }
        }

        Long refundingCount = orderMapper.countRefundingItems(sellerId);
        if (refundingCount == null) {
            refundingCount = 0L;
        }
        counts.put("REFUNDING", refundingCount);

        return counts;
    }


    /**
     * 商家处理订单（PAID → PROCESSING）
     *
     * @param sellerId 商家ID
     * @param orderId 订单ID
     */
    @Transactional
    public void processOrder(Long sellerId, Long orderId) {
        // 查询订单（校验订单存在且属于该商家）
        Order order = orderMapper.findByIdAndSellerId(orderId, sellerId)
                .orElseThrow(() -> new RuntimeException("订单不存在或无权操作"));

        // 校验订单状态
        if (order.getStatus() != Order.OrderStatus.PAID) {
            throw new RuntimeException("订单状态异常，当前状态：" + order.getStatus());
        }

        // 更新订单状态为处理中
        order.setStatus(Order.OrderStatus.PROCESSING);
        LocalDateTime processingTime = LocalDateTime.now();
        order.setProcessingAt(processingTime);
        order.setUpdatedAt(processingTime);
        orderMapper.updateStatus(orderId, Order.OrderStatus.PROCESSING.name());
        orderMapper.updateProcessingTime(orderId, processingTime);


    }

    /**
     * 商家发货（PROCESSING → SHIPPED）
     *
     * @param sellerId 商家ID
     * @param orderId 订单ID
     * @param trackingNumber 物流单号
     * @param logisticsCode 物流公司代码
     * @param logisticsName 物流公司名称
     */
    @Transactional
    public void shipOrder(Long sellerId, Long orderId, String trackingNumber,
                          String logisticsCode, String logisticsName) {
        // 查询订单（校验订单存在且属于该商家）
        Order order = orderMapper.findByIdAndSellerId(orderId, sellerId)
                .orElseThrow(() -> new RuntimeException("订单不存在或无权操作"));

        // 校验订单状态
        if (order.getStatus() != Order.OrderStatus.PROCESSING) {
            throw new RuntimeException("订单状态异常，当前状态：" + order.getStatus());
        }

        // 更新物流信息
        order.setTrackingNumber(trackingNumber);
        order.setLogisticsCode(logisticsCode);
        order.setLogisticsName(logisticsName);

        // 更新订单状态为已发货，记录发货时间
        order.setStatus(Order.OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 调用更新发货信息方法
        orderMapper.updateShipInfo(order);

        // 【关键】调用快递鸟查询API，让快递鸟知道这个单号属于您
        // 这一步不是为了获取物流信息，而是为了让快递鸟"认识"这个单号
        try {
            logisticsService.queryLogistics(trackingNumber, logisticsCode, orderId);

        } catch (Exception e) {
            log.warn("通知快递鸟失败，但不影响发货: {}", e.getMessage());
            // 不影响发货流程
        }

        // 发送发货通知给用户
        webSocketService.sendUserShipment(order.getUserId(), order.getId(), order.getOrderNumber(), trackingNumber);

    }



}