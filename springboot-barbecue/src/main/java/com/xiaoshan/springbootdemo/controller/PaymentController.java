package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.dto.PaymentRequestDTO;
import com.xiaoshan.springbootdemo.entity.dto.PaymentResultDTO;
import com.xiaoshan.springbootdemo.service.OrderService;
import com.xiaoshan.springbootdemo.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    // ==================== 支付相关 ====================

    /**
     * 支付订单
     * <p>
     * 根据请求信息创建支付订单，返回支付参数供前端调起支付。
     *
     * @param request 支付请求DTO，包含订单号、支付方式等
     * @return 支付参数或错误信息
     */
    @PostMapping("/order/payment")
    public ResponseEntity<?> createPayment(@RequestBody PaymentRequestDTO request) {
        try {
            PaymentResultDTO result = paymentService.createPayment(request);

            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "支付订单创建成功",
                        "data", Map.of("paymentResult", result)
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", result.getMessage(),
                        "data", Map.of()
                ));
            }

        } catch (Exception e) {
            log.error("创建支付订单失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "创建支付订单失败: " + e.getMessage()
            ));
        }
    }

}
