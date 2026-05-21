package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.dto.PaymentRequestDTO;
import com.xiaoshan.springbootdemo.entity.dto.PaymentResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付服务层
 * 负责支付订单的创建、支付方式处理等业务逻辑
 *
 * @author xiaoshan
 * @date 2026-05-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderService orderService;
    private final AlipayService alipayService;

    /**
     * 创建支付订单
     *
     * @param request 支付请求
     * @return 支付结果
     */
    @Transactional
    public PaymentResultDTO createPayment(PaymentRequestDTO request) {
        PaymentResultDTO result = new PaymentResultDTO();

        try {
            // 验证订单
            Order order = orderService.validatePendingOrder(request.getOrderNumber(), request.getAmount());
            if (order == null) {
                result.setSuccess(false);
                result.setMessage("订单验证失败");
                return result;
            }

            // 判断支付方式
            if ("WECHAT".equals(request.getPaymentMethod())) {
                result.setSuccess(false);
                result.setMessage("测试阶段仅限于支付宝支付");
                return result;
            }

            // 处理支付宝支付
            if ("ALIPAY".equals(request.getPaymentMethod())) {
                String pageHtml = alipayService.createPagePay(order);
                result.setPageHtml(pageHtml);
                result.setPaymentId(order.getId().toString());
                result.setSuccess(true);
                result.setMessage("支付订单创建成功");
                log.info("创建支付宝支付订单成功 - 订单号: {}, 金额: {}", request.getOrderNumber(), request.getAmount());
                return result;
            }

            result.setSuccess(false);
            result.setMessage("不支持的支付方式");
            return result;

        } catch (Exception e) {
            log.error("创建支付订单失败", e);
            result.setSuccess(false);
            result.setMessage("创建支付订单失败: " + e.getMessage());
            return result;
        }
    }
}