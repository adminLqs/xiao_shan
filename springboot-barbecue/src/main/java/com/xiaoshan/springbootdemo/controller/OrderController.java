package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.entity.RefundRecord;
import com.xiaoshan.springbootdemo.entity.dto.OrderDTO;
import com.xiaoshan.springbootdemo.entity.dto.RefundHandleDTO;
import com.xiaoshan.springbootdemo.entity.vo.OrderDetailVO;
import com.xiaoshan.springbootdemo.entity.vo.OrderVO;
import com.xiaoshan.springbootdemo.service.OrderItemService;
import com.xiaoshan.springbootdemo.service.OrderService;
import com.xiaoshan.springbootdemo.service.RefundService;
import com.xiaoshan.springbootdemo.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final RefundService refundService;

    // ==================== 订单管理 ====================

    /**
     * 创建订单
     * <p>
     * 用户下单创建新订单，订单初始状态为待支付（PENDING）。
     *
     * @param orderDTO 订单信息DTO，包含商品列表、配送方式等
     * @param userId   用户ID
     * @return 创建结果，包含订单ID和订单号
     */
    @PostMapping("/orders/{userId}")
    public ResponseEntity<?> createOrder(
            @RequestBody OrderDTO orderDTO,
            @PathVariable("userId") Long userId
    ) {
        try {
            Order order = orderService.createOrder(userId, orderDTO);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "订单创建成功",
                    "data", Map.of(
                            "orderId", order.getId(),
                            "orderNumber", order.getOrderNumber()
                    )
            ));
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "订单创建失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取用户订单列表（分页）
     * <p>
     * 查询当前用户的订单历史记录，支持分页。
     *
     * @param userId   用户ID
     * @param page     页码，从1开始，默认值为1
     * @param pageSize 每页记录数
     * @return 订单列表及分页信息
     */
    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<?> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        try {
            List<OrderVO> orderVOs = orderService.findUserOrderVOs(userId, page, pageSize, status);
            int total = orderService.countByUserId(userId);
            int offset = (page - 1) * pageSize;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "orders", orderVOs,
                            "total", total,
                            "page", page,
                            "pageSize", pageSize,
                            "hasMore", (offset + pageSize) < total
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取订单列表失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 用户取消订单
     * <p>
     * 用户主动取消待支付的订单。仅当订单状态为待支付（PENDING）时才能取消。
     *
     * @param orderNumber 订单号
     * @return 取消结果
     */
    @PutMapping("/orders/cancel/{orderNumber}")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderNumber) {
        try {
            boolean updated = orderService.cancelOrder(orderNumber);
            if (!updated) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "取消失败，订单不存在或状态异常"
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "订单已取消"
            ));

        } catch (Exception e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "取消失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 确认收货
     * <p>
     * 用户确认收到商品，将订单状态从 SHIPPED 更新为 COMPLETED
     *
     * @param orderNumber 订单号
     * @return 操作结果
     */
    @PutMapping("/orders/confirm/{orderNumber}")
    public ResponseEntity<?> confirmReceipt(@PathVariable String orderNumber) {
        try {
            boolean success = orderService.confirmReceipt(orderNumber);
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "确认收货成功"
                ));
            } else {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "确认收货失败，订单状态异常"
                ));
            }
        } catch (Exception e) {
            log.error("确认收货失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "确认收货失败: " + e.getMessage()
            ));
        }
    }


    /**
     * 获取订单详情
     * <p>
     * 根据订单号查询订单的详细信息，包括订单项（商品明细）。
     *
     * @param orderNumber 订单号
     * @return 订单详情，包含订单信息和商品列表
     */
    @GetMapping("/orders/detail/{orderNumber}")
    public ResponseEntity<?> getOrderDetail(@PathVariable String orderNumber) {
        try {
            Order order = orderService.findByOrderNumber(orderNumber);
            if (order == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "订单不存在"
                ));
            }

            List<OrderItem> orderItems = orderItemService.findByOrderId(order.getId());
            List<RefundRecord> refundRecords = refundService.findByOrderNumber(orderNumber);

            // 取第一条退款记录
            RefundRecord refundRecord = (refundRecords != null && !refundRecords.isEmpty())
                    ? refundRecords.get(0) : null;

            OrderDetailVO orderDetailVO = OrderDetailVO.builder()
                    .order(order)
                    .orderItems(orderItems)
                    .refundRecords(refundRecord)
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("orderDetail", orderDetailVO)
            ));

        } catch (Exception e) {
            log.error("获取订单详情失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取订单详情失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 查询支付结果（前端轮询用）
     * <p>
     * 支付宝支付后，前端需要轮询查询支付状态。
     * 返回订单的支付状态和是否已支付。
     *
     * @param orderNumber 订单号
     * @return 支付结果
     */
    @GetMapping("/payments/result/{orderNumber}")
    public ResponseEntity<?> queryPaymentResult(@PathVariable String orderNumber) {
        try {
            Order order = orderService.findByOrderNumber(orderNumber);
            if (order == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "订单不存在"
                ));
            }

            // 判断是否已支付
            boolean paid = "PAID".equals(order.getStatus())
                    || "SHIPPED".equals(order.getStatus())
                    || "COMPLETED".equals(order.getStatus());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "orderNumber", order.getOrderNumber(),
                            "status", order.getStatus(),
                            "paid", paid,
                            "totalAmount", order.getTotalAmount()
                    )
            ));
        } catch (Exception e) {
            log.error("查询支付结果失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "查询失败: " + e.getMessage()
            ));
        }
    }

    // ==================== 商家订单管理 ====================

    /**
     * 获取商家订单列表（分页）
     * <p>
     * 商家后台获取所有订单，支持按状态筛选和分页查询。
     *
     * @param page     页码，从1开始，默认值为1
     * @param pageSize 每页记录数
     * @param status   订单状态（可选），如 PENDING、PAID、COMPLETED、CANCELLED
     * @return 响应结果，包含订单列表、总数、分页信息
     */
    @GetMapping("/seller/orders")
    public ResponseEntity<?> getSellerOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        try {
            List<OrderVO> orderVOs = orderService.findSellerOrderVOs(page, pageSize, status);
            int total = orderService.countAll(status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "orders", orderVOs,
                            "total", total,
                            "page", page,
                            "pageSize", pageSize
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取订单失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 订单发货（外卖配送）
     *
     * @param orderNumber 订单号
     * @return 发货结果
     */
    @PutMapping("/orders/ship/{orderNumber}")
    public ResponseEntity<?> shipOrder(@PathVariable String orderNumber) {
        try {
            boolean success = orderService.shipOrder(orderNumber);
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "发货成功"
                ));
            } else {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "发货失败，订单状态异常"
                ));
            }
        } catch (Exception e) {
            log.error("发货失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "发货失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 核销/完成订单（到店用餐/打包自取）
     *
     * @param orderNumber 订单号
     * @return 核销结果
     */
    @PutMapping("/orders/complete/{orderNumber}")
    public ResponseEntity<?> completeOrder(@PathVariable String orderNumber) {
        try {
            boolean success = orderService.completeOrder(orderNumber);
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "核销成功"
                ));
            } else {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "核销失败，订单状态异常"
                ));
            }
        } catch (Exception e) {
            log.error("核销失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "核销失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 商家处理退款
     *
     * @param request 退款处理请求
     * @return 处理结果
     */
    @PutMapping("/orders/refund/handle")
    public ResponseEntity<?> handleRefund(@RequestBody RefundHandleDTO request) {
        try {
            refundService.handleRefundResult(
                    request.getOrderNumber(),
                    request.getResult(),
                    request.getFailReason()
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "处理成功"));
        } catch (Exception e) {
            log.error("处理退款失败", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }


}
