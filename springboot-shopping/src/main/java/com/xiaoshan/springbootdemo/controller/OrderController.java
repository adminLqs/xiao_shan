package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Address;
import com.xiaoshan.springbootdemo.entity.LogisticsTrace;
import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderCoupon;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.entity.dto.OrderDTO;
import com.xiaoshan.springbootdemo.entity.vo.OrderDetailVO;
import com.xiaoshan.springbootdemo.entity.vo.OrderWithItemsVO;
import com.xiaoshan.springbootdemo.mapper.OrderCouponMapper;
import com.xiaoshan.springbootdemo.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final UserService userService;
    private final OrderService orderService;
    private final AddressService addressService;
    private final OrderItemService orderItemService;
    private final AlipayService alipayService;
    private final LogisticsService logisticsService;
    private final OrderRefundService orderRefundService;
    private final OrderCouponMapper orderCouponMapper;

    /**
     * 创建订单（按卖家拆单）
     * POST /api/v1/orders
     */
    @PostMapping("/orders")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> createOrder(
            Authentication authentication,
            @Valid @RequestBody OrderDTO orderDTO
    ) {
        try {
            if (!"ALIPAY".equals(orderDTO.getPaymentMethod())) {
                String message = "WECHAT".equals(orderDTO.getPaymentMethod())
                        ? "测试阶段仅限于支付宝支付"
                        : "请选择支付方式";
                return ResponseEntity.ok(Map.of("success", false, "message", message));
            }

            if (orderDTO.getOrderItems() == null || orderDTO.getOrderItems().isEmpty()) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false, "message", "订单项不能为空"
                ));
            }

            Long userId = userService.getCurrentUserId(authentication);

            List<Order> orders = orderService.createOrdersBySeller(userId, orderDTO);

            if (orders.isEmpty()) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false, "message", "创建订单失败"
                ));
            }

            BigDecimal totalAmount = orders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String paymentHtml = alipayService.createPagePayForMultipleOrders(orders, totalAmount);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "订单创建成功",
                    "data", Map.of(
                            "orders", orders,
                            "totalAmount", totalAmount,
                            "paymentHtml", paymentHtml
                    )
            ));

        } catch (Exception e) {
            log.error("创建订单失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 订单支付Api
    @PostMapping("/orders/{orderId}/pay")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> payOrder(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);

            // 查询订单
            Order order = orderService.getOrderDetail(userId, orderId);

            // 检验订单处理
            if (order == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "订单不存在"));
            }

            if (order.getStatus() != Order.OrderStatus.PENDING) {
                return ResponseEntity.ok(Map.of("success", false, "message", "订单状态异常，无法支付"));
            }

            // 调用支付宝支付
            String paymentHtml = alipayService.createPagePay(order);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "orderId", order.getId(),
                            "orderNumber", order.getOrderNumber(),
                            "totalAmount", order.getTotalAmount(),
                            "paymentHtml", paymentHtml
                    )
            ));

        } catch (Exception e) {
            log.error("支付失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取订单列表（分页） 包含订单信息和订单项列表
     * GET /api/v1/orders?page=1&size=10&status=PENDING
     */
    @GetMapping("/orders")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String[] status
    ) {
        try {
            // 获取认证用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 查询订单列表
            List<OrderWithItemsVO> orders = orderService.getUserOrdersWithItems(userId, page, pageSize, status);

            // 查询总数
            long total = orderService.countUserOrdersWithStatus(userId, status);

            Map<String, Long> counts = orderService.getOrderCountsByUserId(userId);

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("records", orders);
            dataMap.put("total", total);
            dataMap.put("page", page);
            dataMap.put("size", pageSize);
            dataMap.put("totalPages", (int) Math.ceil((double) total / pageSize));
            dataMap.put("counts", counts != null ? counts : new HashMap<>());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dataMap);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("获取订单列表失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage() != null ? e.getMessage() : "服务器内部错误");
            return ResponseEntity.ok().body(errorResult);
        }
    }

    /**
     * 获取订单状态统计
     * GET /api/v1/orders/counts
     */
    @GetMapping("/orders/counts")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getOrderCounts(Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);

            Map<String, Long> counts = orderService.getOrderCountsByUserId(userId);

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("counts", counts != null ? counts : new HashMap<>());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dataMap);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取订单统计失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage() != null ? e.getMessage() : "服务器内部错误");
            return ResponseEntity.ok().body(errorResult);
        }
    }

    /**
     * 获取评价相关订单项列表
     * GET /api/v1/orders/review-items?type=pending|reviewed
     *
     * @param authentication 认证信息
     * @param type 类型（pending: 待评价, reviewed: 已评价）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 订单项列表
     */
    @GetMapping("/orders/review-items")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getReviewItems(
            Authentication authentication,
            @RequestParam String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            
            Map<String, Object> result = orderService.getReviewItems(userId, type, page, pageSize);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result
            ));
        } catch (Exception e) {
            log.error("获取评价订单项失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取订单信息
     * GET /api/v1/orders/{orderId}
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getOrderDetail(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);

            Order order = orderService.getOrderDetail(userId, orderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("order", order)
            ));

        } catch (Exception e) {
            log.error("获取订单详情失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取订单详情（完整版：含地址和商品列表和默认物流）
     * GET /api/v1/orders/{orderId}/detail
     */
    @GetMapping("/orders/{orderId}/detail")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getOrderFullDetail(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            // 获取当前登录用户ID（用于权限校验，只能查询自己的订单）
            Long userId = userService.getCurrentUserId(authentication);

            // 查询订单信息
            Order order = orderService.getOrderFullDetail(userId, orderId);

            // 查询订单项列表
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);

            // 查询地址表信息
            Address address = addressService.getAddressByUserIdAndAddressId(userId, order.getAddressId());

            // 查询商家信息
            String sellerName = "商家";
            String sellerAvatar = null;
            if (!orderItems.isEmpty() && orderItems.get(0).getSellerId() != null) {
                String[] sellerInfo = orderService.getSellerInfo(orderItems.get(0).getSellerId());
                sellerName = sellerInfo[0];
                sellerAvatar = sellerInfo[1];
            }

            // 生成默认物流轨迹
            List<LogisticsTrace> traces = generateDefaultTraces(order);

            OrderDetailVO orderDetailVO = new OrderDetailVO(order, orderItems, address, sellerName, sellerAvatar, traces);

            List<OrderCoupon> orderCoupons = orderCouponMapper.findByOrderId(orderId);
            if (orderCoupons != null && !orderCoupons.isEmpty()) {
                OrderCoupon orderCoupon = orderCoupons.get(0);
                orderDetailVO.setCouponName(orderCoupon.getCouponName());
                orderDetailVO.setCouponDiscountAmount(orderCoupon.getDiscountAmount());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("orderDetail", orderDetailVO)
            ));

        } catch (Exception e) {
            log.error("获取订单详情失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 生成默认物流轨迹
     * @param order 订单
     * @return 物流轨迹列表
     */
    private List<LogisticsTrace> generateDefaultTraces(Order order) {
        String status = order.getStatus() != null ? order.getStatus().name() : "";
        String time = getOrderTime(order);
        String description = "";
        String location = "";

        switch (status) {
            case "PENDING":
                description = "订单已提交，等待支付";
                break;
            case "PAID":
                description = "订单已付款，等待发货";
                break;
            case "PROCESSING":
                description = "商家正在备货中";
                break;
            case "SHIPPED":
                description = "商品已发货，运输中";
                break;
            case "COMPLETED":
                description = "订单已完成";
                break;
            case "CANCELLED":
                description = "订单已取消";
                break;
            default:
                description = "订单已提交";
        }

        LogisticsTrace trace = new LogisticsTrace(time, description, description, location);
        return Collections.singletonList(trace);
    }

    /**
     * 获取订单相关时间
     * @param order 订单
     * @return 时间字符串
     */
    private String getOrderTime(Order order) {
        if (order.getPaidAt() != null) {
            return formatDateTime(order.getPaidAt());
        } else if (order.getShippedAt() != null) {
            return formatDateTime(order.getShippedAt());
        } else if (order.getCreatedAt() != null) {
            return formatDateTime(order.getCreatedAt());
        }
        return formatDateTime(LocalDateTime.now());
    }

    /**
     * 格式化日期时间
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 取消订单
     * PUT /api/v1/orders/{orderId}/cancel
     */
    @PutMapping("/orders/{orderId}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> cancelOrder(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);

            orderService.cancelOrder(userId, orderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "订单已取消"
            ));

        } catch (Exception e) {
            log.error("取消订单失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 确认收货
     * PUT /api/v1/orders/{orderId}/confirm
     */
    @PutMapping("/orders/{orderId}/confirm")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> confirmReceive(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);

            orderService.confirmReceive(userId, orderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "确认收货成功"
            ));

        } catch (Exception e) {
            log.error("确认收货失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除订单（软删除）
     * DELETE /api/v1/orders/{orderId}
     */
    @DeleteMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> deleteOrder(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);

            orderService.deleteOrder(userId, orderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "删除成功"
            ));

        } catch (Exception e) {
            log.error("删除订单失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    // ========== 商家逻辑 ==========
    /**
     * 获取订单列表（分页）
     * <p>返回当前用户的订单列表，包含订单信息和订单项列表</p>
     *
     * @param authentication 认证信息，用于获取当前用户ID
     * @param page 当前页码，默认1
     * @param pageSize 每页数量，默认10
     * @param status 订单状态筛选（PENDING/PAID/SHIPPED等），可选
     * @return 包含订单列表、总数、分页信息、状态统计的响应
     */
    @GetMapping("/seller/orders")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getSellerOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String[] status
    ) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);

            String[] refundStatus = null;
            
            if (status != null && status.length > 0) {
                if ("REFUNDING".equals(status[0])) {
                    refundStatus = new String[]{"PROCESSING", "WAITING_RETURN", "RETURNING"};
                    status = null;
                } else if ("REFUNDED".equals(status[0])) {
                    refundStatus = new String[]{"SUCCESS"};
                    status = null;
                }
            }

            List<OrderWithItemsVO> orders = orderService.getSellerOrders(sellerId, page, pageSize, status, refundStatus);

            long total = orderService.countSellerOrders(sellerId, status, refundStatus);

            Map<String, Long> counts = orderService.getSellerOrderCounts(sellerId);

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("records", orders);
            dataMap.put("total", total);
            dataMap.put("page", page);
            dataMap.put("size", pageSize);
            dataMap.put("totalPages", (int) Math.ceil((double) total / pageSize));
            dataMap.put("counts", counts != null ? counts : new HashMap<>());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dataMap);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("获取商家订单列表失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage() != null ? e.getMessage() : "服务器内部错误");
            return ResponseEntity.ok().body(errorResult);
        }
    }

    /**
     * 获取商家订单详情（含地址和商品列表）
     * GET /api/v1/seller/orders/{orderId}
     */
    @GetMapping("/seller/orders/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getSellerOrderDetail(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);

            // 查询订单信息
            Order order = orderService.getSellerOrder(sellerId, orderId);

            // 订单项信息
            List<OrderItem> orderItems = orderItemService.getOrderItemsByOrderId(orderId);

            // 地址信息
            Address address = addressService.getAddressById(order.getAddressId());

            // 查询商家信息（商家端查看自己的订单，显示自己的店铺信息）
            String[] sellerInfo = orderService.getSellerInfo(sellerId);
            String sellerName = sellerInfo[0];
            String sellerAvatar = sellerInfo[1];

            OrderDetailVO orderDetailVO = new OrderDetailVO(order, orderItems, address, sellerName, sellerAvatar);

            List<OrderCoupon> orderCoupons = orderCouponMapper.findByOrderId(orderId);
            if (orderCoupons != null && !orderCoupons.isEmpty()) {
                OrderCoupon orderCoupon = orderCoupons.get(0);
                orderDetailVO.setCouponName(orderCoupon.getCouponName());
                orderDetailVO.setCouponDiscountAmount(orderCoupon.getDiscountAmount());
            }


            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("orderDetail", orderDetailVO)
            ));

        } catch (Exception e) {
            log.error("获取商家订单详情失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 商家处理订单（PAID → PROCESSING）
     * PUT /api/v1/seller/orders/{orderId}/process
     *
     * @param authentication 认证信息
     * @param orderId 订单ID
     * @return 处理结果
     */
    @PutMapping("/seller/orders/{orderId}/process")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> processOrder(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            // 获取当前商家ID
            Long sellerId = userService.getCurrentUserId(authentication);

            // 调用服务层处理订单
            orderService.processOrder(sellerId, orderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "订单已处理"
            ));

        } catch (Exception e) {
            log.error("处理订单失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 商家发货
     * PUT /api/v1/seller/orders/{orderId}/ship
     */
    @PutMapping("/seller/orders/{orderId}/ship")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> shipOrder(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request
    ) {
        try {
            // 获取当前登录用户的ID（商家ID）
            Long userId = userService.getCurrentUserId(authentication);

            // 获取请求参数
            String trackingNumber = request.get("trackingNumber");
            String logisticsCode = request.get("logisticsCode");
            String logisticsName = request.get("logisticsName");

            // 调用服务层执行发货操作
            orderService.shipOrder(userId, orderId, trackingNumber, logisticsCode, logisticsName);

            // 返回成功响应
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "发货成功"
            ));

        } catch (Exception e) {
            // 记录错误日志
            log.error("发货失败: {}", e.getMessage());

            // 返回失败响应
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取用户的退款记录列表
     * GET /api/v1/refunds
     */
    @GetMapping("/refunds")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getUserRefunds(Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            var refunds = orderRefundService.getUserRefunds(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", refunds
            ));

        } catch (Exception e) {
            log.error("获取退款记录失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }

    /**
     * 获取订单的退款记录
     * GET /api/v1/orders/{orderId}/refunds
     */
    @GetMapping("/orders/{orderId}/refunds")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getOrderRefunds(@PathVariable Long orderId) {
        try {
            var refunds = orderRefundService.getOrderRefunds(orderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", refunds
            ));

        } catch (Exception e) {
            log.error("获取退款记录失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }

    // ========== 商家退款处理接口 ==========

    /**
     * 获取商家待处理的退款列表
     * GET /api/v1/seller/refunds
     */
    @GetMapping("/seller/refunds")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getSellerPendingRefunds(Authentication authentication) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            var refunds = orderRefundService.getPendingRefunds(sellerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", refunds,
                    "count", refunds.size()
            ));

        } catch (Exception e) {
            log.error("获取待处理退款失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }

    /**
     * 商家同意退款
     * POST /api/v1/seller/refunds/{refundId}/approve
     */
    @PostMapping("/seller/refunds/{refundId}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> approveRefund(
            Authentication authentication,
            @PathVariable Long refundId,
            @RequestBody Map<String, String> request
    ) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            String notes = request.getOrDefault("notes", "");

            var result = orderRefundService.approveRefund(refundId, sellerId, notes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "退款成功",
                    "data", result
            ));

        } catch (RuntimeException e) {
            log.warn("处理退款失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("处理退款系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 商家拒绝退款
     * POST /api/v1/seller/refunds/{refundId}/reject
     */
    @PostMapping("/seller/refunds/{refundId}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> rejectRefund(
            Authentication authentication,
            @PathVariable Long refundId,
            @RequestBody Map<String, String> request
    ) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            String notes = request.get("notes");

            if (notes == null || notes.trim().isEmpty()) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "请填写拒绝原因"
                ));
            }

            var result = orderRefundService.rejectRefund(refundId, sellerId, notes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "已拒绝退款申请",
                    "data", result
            ));

        } catch (RuntimeException e) {
            log.warn("拒绝退款失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("拒绝退款系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }
}