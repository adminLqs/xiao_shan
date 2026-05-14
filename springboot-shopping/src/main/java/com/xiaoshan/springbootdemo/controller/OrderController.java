package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Address;
import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.entity.dto.OrderDTO;
import com.xiaoshan.springbootdemo.entity.vo.OrderDetailVO;
import com.xiaoshan.springbootdemo.entity.vo.OrderWithItemsVO;
import com.xiaoshan.springbootdemo.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    /**
     * 创建订单
     * POST /api/v1/orders
     */
    @PostMapping("/orders")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> createOrder(
            Authentication authentication,
            @Valid @RequestBody OrderDTO orderDTO
    ) {
        try {
            // 测试阶段仅开放支付宝支付，微信支付暂不支持
            if (!"ALIPAY".equals(orderDTO.getPaymentMethod())) {
                String message = "WECHAT".equals(orderDTO.getPaymentMethod())
                        ? "测试阶段仅限于支付宝支付"
                        : "请选择支付方式";
                return ResponseEntity.ok(Map.of("success", false, "message", message));
            }

            // 校验订单项
            if (orderDTO.getOrderItems() == null || orderDTO.getOrderItems().isEmpty()) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false, "message", "订单项不能为空"
                ));
            }

            // 获取当前用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 创建订单
            Order order = orderService.createOrder(userId, orderDTO);

            // 调用支付宝沙箱API，返回支付页面HTML
            String paymentHtml  = alipayService.createPagePay(order);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "订单创建成功",
                    "data", Map.of(
                            "orderId", order.getId(),
                            "orderNumber", order.getOrderNumber(),
                            "totalAmount", order.getTotalAmount(),
                            "paymentHtml", paymentHtml // 支付网页
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
            @RequestParam(required = false) String status
    ) {
        try {
            // 获取认证用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 查询订单列表
            List<OrderWithItemsVO> orders = orderService.getUserOrdersWithItems(userId, page, pageSize, status);

            // 查询总数
            long total = orderService.countUserOrdersWithStatus(userId, status);

            // 查询各状态数量统计
            Map<String, Long> counts = orderService.getOrderCountsByUserId(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", orders,
                    "total", total,
                    "page", page,
                    "size", pageSize,
                    "totalPages", (int) Math.ceil((double) total / pageSize),
                    "counts", counts
            ));

        } catch (Exception e) {
            log.error("获取订单列表失败: {}", e.getMessage());
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
                    "data", order
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
     * 获取订单详情（完整版：含地址和商品列表）
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

            OrderDetailVO orderDetailVO = new OrderDetailVO(order, orderItems, address);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", orderDetailVO
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
            @RequestParam(required = false) String status
    ) {
        try {
            // 获取当前商家的ID
            Long sellerId = userService.getCurrentUserId(authentication);

            // 调用服务层查询商家订单列表
            List<OrderWithItemsVO> orders = orderService.getSellerOrders(sellerId, page, pageSize, status);

            // 调用服务层查询订单总数
            long total = orderService.countSellerOrders(sellerId, status);

            // 调用服务层查询各状态订单数量统计
            Map<String, Long> counts = orderService.getSellerOrderCounts(sellerId);

            // 返回成功响应，包含订单列表、总数、当前页、每页数量、总页数、状态统计
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", orders,
                    "total", total,
                    "page", page,
                    "pageSize", pageSize,
                    "totalPages", (int) Math.ceil((double) total / pageSize),
                    "counts", counts
            ));

        } catch (Exception e) {
            // 记录错误日志
            log.error("获取商家订单列表失败: {}", e.getMessage());

            // 返回错误响应
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
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

            OrderDetailVO orderDetailVO = new OrderDetailVO(order, orderItems, address);


            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", orderDetailVO
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





}