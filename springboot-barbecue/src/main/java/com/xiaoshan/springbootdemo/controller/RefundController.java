package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.RefundRecord;
import com.xiaoshan.springbootdemo.entity.dto.RefundRequestDTO;
import com.xiaoshan.springbootdemo.service.OrderService;
import com.xiaoshan.springbootdemo.service.RefundService;
import com.xiaoshan.springbootdemo.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 退款控制器
 *
 * @author xiaoshan
 * @date 2026-05-10
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RefundController {

    private final OrderService orderService;
    private final RefundService refundService;
    private final WebSocketService webSocketService;

    // ==================== 退款相关 ====================

    /**
     * 申请退款
     * <p>
     * 用户发起退款申请，创建退款记录。
     * 仅已支付（PAID）的订单可以申请退款。
     * 退款申请创建后异步处理。
     *
     * @param request 退款请求DTO
     * @return 退款申请结果
     */
    @PostMapping("/orders/refund")
    public ResponseEntity<?> refundOrder(@RequestBody RefundRequestDTO request) {
        try {
            RefundRecord refund = refundService.processRefundRequest(request);

            webSocketService.sendRefundApplicationNotification(refund.getOrderNumber(), refund.getRefundAmount(), refund.getRefundReason());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "退款申请已提交，请等待处理",
                    "data", Map.of("refund", refund)
            ));

        } catch (Exception e) {
            log.error("申请退款失败 - 订单号: {}", request.getOrderNumber(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "退款申请失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 查询订单的退款记录
     *
     * @param orderNumber 订单号
     * @return 退款记录列表
     */
    @GetMapping("/orders/refund/records/{orderNumber}")
    public ResponseEntity<?> getRefundRecords(@PathVariable String orderNumber) {
        try {
            List<RefundRecord> refundRecords = refundService.findByOrderNumber(orderNumber);
            BigDecimal totalRefunded = refundService.getTotalRefundedAmount(orderNumber);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "records", refundRecords,
                            "totalRefunded", totalRefunded
                    )
            ));

        } catch (Exception e) {
            log.error("查询退款记录失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "查询失败: " + e.getMessage()
            ));
        }
    }
}