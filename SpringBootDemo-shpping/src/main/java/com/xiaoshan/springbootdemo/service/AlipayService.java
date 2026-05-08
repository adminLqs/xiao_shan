package com.xiaoshan.springbootdemo.service;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.mapper.CartItemMapper;
import com.xiaoshan.springbootdemo.mapper.OrderItemMapper;
import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付宝支付服务类
 * 负责与支付宝进行支付交互，使用网页支付模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final OrderService orderService;

    // ========== 支付宝配置参数，从application配置文件中注入 ==========

    // 让支付宝知道你是谁
    @Value("${alipay.app-id:}")
    private String appId;

    // 请求要发到的地址
    @Value("${alipay.gateway-url:}")
    private String gatewayUrl;

    // 签名证明是你本人
    @Value("${alipay.private-key:}")
    private String privateKey;

    // 验证支付宝返回的结果
    @Value("${alipay.alipay-public-key:}")
    private String alipayPublicKey;

    // 后端接收支付结果的地址
    @Value("${alipay.notify-url:}")
    private String notifyUrl;

    // 支付成功后前端跳转的页面
    @Value("${alipay.return-url:}")
    private String returnUrl;

    // 支付宝客户端实例
    private AlipayClient alipayClient;

    /**
     * 获取支付宝客户端实例
     */
    private AlipayClient getAlipayClient() {
        if (alipayClient == null) {
            alipayClient = new DefaultAlipayClient(
                    gatewayUrl,
                    appId,
                    privateKey,
                    "json",
                    "utf-8",
                    alipayPublicKey,
                    "RSA2"
            );
        }
        return alipayClient;
    }

    /**
     * 创建支付宝网页支付订单（返回支付页面HTML）
     *
     * 业务流程：
     * 1. 调用支付宝网页支付接口（alipay.trade.page.pay）
     * 2. 返回完整的HTML表单，前端直接渲染即可跳转到支付宝登录页面
     *
     * @param order 订单实体对象
     * @return String 返回支付宝支付页面的HTML代码，前端直接渲染即可
     */
    public String createPagePay(Order order) {
        try {
            // ========== 步骤1：构建支付宝网页支付请求 ==========
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

            // 设置异步通知地址（支付结果通知）
            request.setNotifyUrl(notifyUrl);
            // 设置同步跳转地址（支付完成后前端跳转）
            request.setReturnUrl(returnUrl);

            // 构建业务参数
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", order.getOrderNumber());           // 商户订单号
            bizContent.put("total_amount", order.getTotalAmount().toString()); // 订单金额
            bizContent.put("subject", "购物清单");   // 订单标题
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");         // 产品码，网页支付必填

            request.setBizContent(com.alibaba.fastjson.JSON.toJSONString(bizContent));

            // ========== 步骤2：调用支付宝接口 ==========
            AlipayTradePagePayResponse response = getAlipayClient().pageExecute(request);

            if (response.isSuccess()) {
                // 获取支付页面HTML
                String pageHtml = response.getBody();
                log.info("支付宝网页支付订单创建成功 - 订单号: {}", order.getOrderNumber());
                return pageHtml;
            } else {
                log.error("支付宝创建订单失败: {}", response.getMsg());
                throw new RuntimeException(response.getMsg());
            }

        } catch (AlipayApiException e) {
            log.error("支付宝支付失败", e);
            throw new RuntimeException("支付宝支付失败: " + e.getMessage());
        }
    }

    /**
     * 处理支付宝支付回调
     */
    public boolean handleAlipayCallback(Map<String, String> params) {
        try {
            // 验签（支付宝专用）
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params, alipayPublicKey, "utf-8", "RSA2"
            );
            if (!signVerified) {
                log.error("支付宝回调签名验证失败");
                return false;
            }

            // 获取关键参数
            String orderNumber = params.get("out_trade_no");
            String transactionId = params.get("trade_no");
            String tradeStatus = params.get("trade_status");

            log.info("支付宝回调 - 订单号: {}, 交易号: {}, 状态: {}", orderNumber, transactionId, tradeStatus);

            // 只处理支付成功状态
            if ("TRADE_SUCCESS".equals(tradeStatus)) {
                // 调用 OrderService 处理支付成功业务
                orderService.handlePaymentSuccess(orderNumber, transactionId, "ALIPAY");
            }

            return true;

        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return false;
        }
    }

    /**
     * 支付宝退款
     *
     * @param orderNumber 商户订单号
     * @param refundAmount 退款金额（单位：元）
     * @param refundReason 退款原因
     * @return 退款结果
     */
    public boolean refund(String orderNumber, BigDecimal refundAmount, String refundReason) {
        try {
            // 构建退款请求
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

            // 构建业务参数
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderNumber);           // 商户订单号
            bizContent.put("refund_amount", refundAmount.toString()); // 退款金额
            bizContent.put("refund_reason", refundReason);         // 退款原因

            request.setBizContent(JSON.toJSONString(bizContent));

            // 调用支付宝退款接口
            AlipayTradeRefundResponse response = getAlipayClient().execute(request);

            if (response.isSuccess()) {
                log.info("退款成功 - 订单号: {}, 退款金额: {}", orderNumber, refundAmount);
                return true;
            } else {
                log.error("退款失败: {}", response.getMsg());
                return false;
            }

        } catch (AlipayApiException e) {
            log.error("退款异常", e);
            return false;
        }
    }
}