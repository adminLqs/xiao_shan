package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.dto.OrderDTO;
import com.xiaoshan.springbootdemo.entity.vo.OrderWithItemsVO;
import com.xiaoshan.springbootdemo.mapper.*;
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

    private final OrderMapper orderMapper;  // 订单数据库操作
    private final OrderItemMapper orderItemMapper;  // 订单项数据库操作
    private final ProductMapper productMapper;  // 商品数据库操作
    private final CartItemMapper cartItemMapper; // 购物车数据库操作
    private final LogisticsService logisticsService;
    private final ProductImageMapper productImageMapper;  // 商品图片数据库操作
    private final RedisTemplate<String, Object> redisTemplate; // Redis对象操作模板
    private final StringRedisTemplate stringRedisTemplate;    // Redis字符串操作模板（用于Sorted Set）

    /**
     * 创建订单（结算页直接支付）
     * @param userId   用户ID
     * @param orderDTO 订单信息（包含地址ID、支付方式、商品列表）
     * @return 创建的订单
     */
    @Transactional // 开启数据库事务，保证数据一致性
    public Order createOrder(Long userId, OrderDTO orderDTO) {
        // 生成订单号
        String orderNumber = generateOrderNumber();

        // 计算订单总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // ==========遍历商品，Redis原子扣减库存 ==========
        for (OrderItem item : orderDTO.getOrderItems()) {
            // 查询商品信息
            Product product = productMapper.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + item.getProductId()));

            // 检查库存
            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("商品「" + product.getName() + "」库存不足");
            }

            // Redis 预扣库存（原子操作，防止超卖）
            // opsForValue().decrement() 是原子操作，高并发下安全
            // 返回值是扣减后的剩余库存
            String stockKey = "product:stock:" + item.getProductId();
            Long remainStock = redisTemplate.opsForValue().decrement(stockKey, item.getQuantity());

            if (remainStock == null || remainStock < 0) {
                // 库存不足，回滚已扣减的库存
                if (remainStock != null) {
                    redisTemplate.opsForValue().increment(stockKey, item.getQuantity());
                }
                throw new RuntimeException("商品「" + product.getName() + "」库存不足");
            }

            // 记录预扣库存信息到 Redis Hash（用于支付成功确认或超时回滚）
            //     Key: order:prestock:订单号
            //     Field: 商品ID
            //     Value: 购买数量
            String prestockKey = "order:prestock:" + orderNumber;
            redisTemplate.opsForHash().put(prestockKey,
                    String.valueOf(item.getProductId()),
                    String.valueOf(item.getQuantity()));

            // 设置预扣库存记录的过期时间（30分钟）与订单超时时间一致，超时后 Redis 自动删除
            redisTemplate.expire(prestockKey, 30, TimeUnit.MINUTES);

            // 设置订单项完整信息
            item.setSellerId(product.getSellerId()); // 商家ID
            item.setProductName(product.getName()); // 商品名称快照
            item.setProductImage(getProductMainImage(item.getProductId())); // 商品图片快照
            item.setPrice(product.getPrice()); // 下单时单价
            item.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))); // 小计
            item.setCreatedAt(LocalDateTime.now()); // 创建时间

            // 累加总金额
            totalAmount = totalAmount.add(item.getTotalPrice());
            orderItems.add(item);
        }

        // 创建订单（结算页直接支付，状态为PAID）
        Order order = new Order();
        order.setOrderNumber(orderNumber); // 生成订单号
        order.setSource(orderDTO.getSource());  // 保存来源
        order.setUserId(userId); // 用户ID
        order.setAddressId(orderDTO.getAddressId()); // 收货地址ID
        order.setTotalAmount(totalAmount); // 订单总金额

        // 保存订单
        orderMapper.insert(order);

        // 设置订单ID并保存订单项
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.batchInsert(orderItems);

        log.info("订单创建成功: orderId={}, orderNumber={}, userId={}, totalAmount={}",
                order.getId(), order.getOrderNumber(), userId, totalAmount);

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

        // 查询所有订单项
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(order.getId());

        // 扣减库存 + 增加销量
        for (OrderItem item : orderItems) {
            // 扣减库存
            int affected = productMapper.deductStock(item.getProductId(), item.getQuantity());
            if (affected == 0) {
                throw new RuntimeException("商品库存不足: " + item.getProductName());
            }
            // 增加销量
            productMapper.incrementSalesCount(item.getProductId(), item.getQuantity());
            log.info("商品处理成功: productId={}, quantity={}", item.getProductId(), item.getQuantity());
        }

        // 更新订单状态
        order.setStatus(Order.OrderStatus.PAID);
        order.setPaymentMethod(Order.PaymentMethod.valueOf(paymentMethod));
        order.setTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updatePaymentInfo(order);

        log.info("订单支付成功 - 订单号: {}", orderNumber);

        // 如果来源是购物车，删除购物车中的商品
        if ("cart".equals(order.getSource())) {
            List<Long> productIds = orderItems.stream()
                    .map(OrderItem::getProductId)
                    .collect(Collectors.toList());
            cartItemMapper.deleteByUserIdAndProductIds(order.getUserId(), productIds);
            log.info("购物车商品已删除: userId={}, productIds={}", order.getUserId(), productIds);
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
    public List<OrderWithItemsVO> getUserOrdersWithItems(Long userId, int page, int pageSize, String status) {
        // 计算分页偏移量
        int offset = (page - 1) * pageSize;

        // 查询用户订单ID列表
        List<Long> orderIds = orderMapper.findUserOrderIds(userId, offset, pageSize, status);

        // 订单ID为空时返回空列表
        if (orderIds == null || orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询订单详情
        List<Order> orders = orderMapper.findOrdersByIds(orderIds);

        // 批量查询所有订单项
        List<OrderItem> allOrderItems = orderItemMapper.findOrderItemsByOrderIds(orderIds);

        // 将订单项按订单ID分组
        Map<Long, List<OrderItem>> orderItemMap = allOrderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // 将订单信息和订单项列表合并
        return orders.stream().map(order -> new OrderWithItemsVO(
                        order,
                        orderItemMap.getOrDefault(order.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
    }

    /**
     * 统计用户订单总数（含状态筛选）
     *
     * @param userId 用户ID
     * @param status 订单状态
     * @return 订单总数
     */
    public long countUserOrdersWithStatus(Long userId, String status) {
        return orderMapper.countByUserId(userId, status);
    }

    /**
     * 获取各状态订单数量统计
     */
    public Map<String, Long> getOrderCountsByUserId(Long userId) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("total", orderMapper.countByUserId(userId, null));
        counts.put("PENDING", orderMapper.countByUserId(userId, "PENDING"));
        counts.put("PAID", orderMapper.countByUserId(userId, "PAID"));
        counts.put("SHIPPED", orderMapper.countByUserId(userId, "SHIPPED"));
        counts.put("COMPLETED", orderMapper.countByUserId(userId, "COMPLETED"));
        counts.put("CANCELLED", orderMapper.countByUserId(userId, "CANCELLED"));
        return counts;
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
        // 查询订单（带权限校验）
        Order order = orderMapper.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 只有待付款的订单才能取消
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("只有待付款的订单才能取消");
        }

        // 更新订单状态
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateStatus(order.getId(), order.getStatus().name());

        log.info("订单取消成功: orderId={}", orderId);
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

        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateStatus(order.getId(), order.getStatus().name());

        log.info("确认收货成功: orderId={}", orderId);
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

        log.info("订单删除成功: orderId={}", orderId);
    }


    /**
     * 获取商品主图
     */
    private String getProductMainImage(Long productId) {
        // 查询商品主图
        return productImageMapper.findMainImageByProductId(productId);
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
    public List<OrderWithItemsVO> getSellerOrders(Long sellerId, int page, int pageSize, String status) {
        // 计算分页偏移量
        int offset = (page - 1) * pageSize;

        // 查询商家订单ID列表（通过订单项关联商家ID，使用DISTINCT去重）
        List<Long> orderIds = orderMapper.findSellerOrderIds(sellerId, offset, pageSize, status);

        // 订单ID为空时返回空列表
        if (orderIds == null || orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询订单详情
        List<Order> orders = orderMapper.findByIds(orderIds);

        // 批量查询所有订单项
        List<OrderItem> allOrderItems = orderItemMapper.findByOrderIds(orderIds);

        // 将订单项按订单ID分组
        Map<Long, List<OrderItem>> orderItemMap = allOrderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // 组装VO，将订单信息和订单项列表合并
        return orders.stream()
                .map(order -> new OrderWithItemsVO(
                        order,
                        orderItemMap.getOrDefault(order.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
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
    public long countSellerOrders(Long sellerId, String status) {
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

        for (Map<String, Object> item : list) {
            String status = (String) item.get("status");
            Long count = (Long) item.get("count");
            counts.put(status, count);
            counts.put("total", counts.get("total") + count);
        }
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
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateStatus(orderId, Order.OrderStatus.PROCESSING.name());

        log.info("商家处理订单成功: orderId={}, sellerId={}", orderId, sellerId);
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
            log.info("已通知快递鸟，单号已绑定: trackingNumber={}", trackingNumber);
        } catch (Exception e) {
            log.warn("通知快递鸟失败，但不影响发货: {}", e.getMessage());
            // 不影响发货流程
        }

        log.info("商家发货成功: orderId={}, sellerId={}, trackingNumber={}, logisticsName={}",
                orderId, sellerId, trackingNumber, logisticsName);
    }


    /**
     * 根据物流单号更新订单送达时间
     *
     * @param trackingNumber 物流单号
     * @param deliveredTime 送达时间
     */
    @Transactional
    public void updateDeliveredTime(String trackingNumber, String deliveredTime) {
        // 解析送达时间
        LocalDateTime deliveredAt = LocalDateTime.parse(deliveredTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 更新送达时间和订单状态
        int updated = orderMapper.updateDeliveredTimeByTrackingNumber(trackingNumber, deliveredAt);

        // 更新失败时抛出异常
        if (updated == 0) {
            throw new RuntimeException("订单不存在或更新失败");
        }

        log.info("订单送达时间已更新: trackingNumber={}, deliveredAt={}", trackingNumber, deliveredAt);
    }


}