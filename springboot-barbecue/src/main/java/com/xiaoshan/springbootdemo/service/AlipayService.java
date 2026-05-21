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
import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付服务类
 * 负责与支付宝进行支付交互、验签、回调处理
 *
 * @author xiaoshan
 * @date 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {

    private final OrderMapper orderMapper;
    private final WebSocketService webSocketService;

    // ========== 支付宝配置参数，从application配置文件中注入 ==========

    @Value("${alipay.app-id:}")
    private String appId;

    @Value("${alipay.gateway-url:}")
    private String gatewayUrl;

    @Value("${alipay.private-key:}")
    private String privateKey;

    @Value("${alipay.alipay-public-key:}")
    private String alipayPublicKey;

    @Value("${alipay.notify-url:}")
    private String notifyUrl;

    @Value("${alipay.return-url:}")
    private String returnUrl;

    /**
     * 沙箱环境APPID前缀
     * 沙箱APPID通常以902100开头
     */
    private static final String SANDBOX_APPID_PREFIX = "902100";

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

    // ==================== 支付相关 ====================

    /**
     * 创建支付宝网页支付订单（返回支付页面HTML）
     *
     * @param order 订单实体对象
     * @return 支付宝支付页面的HTML代码
     */
    public String createPagePay(Order order) {
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(notifyUrl);
            request.setReturnUrl(returnUrl);

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", order.getOrderNumber());
            bizContent.put("total_amount", order.getTotalAmount().toString());
            bizContent.put("subject", "烧烤订单");
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

            request.setBizContent(JSON.toJSONString(bizContent));

            AlipayTradePagePayResponse response = getAlipayClient().pageExecute(request);

            if (response.isSuccess()) {
                log.info("支付宝网页支付订单创建成功 - 订单号: {}", order.getOrderNumber());
                return response.getBody();
            } else {
                log.error("支付宝创建订单失败: {}", response.getMsg());
                throw new RuntimeException(response.getMsg());
            }

        } catch (AlipayApiException e) {
            log.error("支付宝支付失败", e);
            throw new RuntimeException("支付宝支付失败: " + e.getMessage());
        }
    }

    // ==================== 回调验签相关 ====================

    /**
     * 验证支付宝回调签名
     *
     * @param params     回调参数（支付宝POST过来的所有参数）
     * @return 签名是否有效
     */
    public boolean verifyCallback(Map<String, String> params) {
        try {
            // 使用支付宝沙箱公钥验证签名
            AlipaySignature.rsaCheckV1(
                    params,
                    alipayPublicKey,
                    "UTF-8",
                    "RSA2"
            );
            return true;

        } catch (AlipayApiException e) {
            log.error("验证支付宝回调签名异常", e);
            return false;
        }
    }

    /**
     * 处理支付宝回调通知
     * 包含验签和业务逻辑处理
     *
     * @param params 回调参数
     * @return 处理结果
     */
    public CallbackResult handleCallback(Map<String, String> params) {
        try {
            // 验证签名
            boolean verified = verifyCallback(params);
            if (!verified) {
                return CallbackResult.fail("签名验证失败");
            }

            // 获取关键参数
            String tradeStatus = params.get("trade_status");
            String orderNumber = params.get("out_trade_no");
            String transactionId = params.get("trade_no");
            String appId = params.get("app_id");

            log.info("支付宝回调 - 订单号: {}, 交易号: {}, 状态: {}, AppId: {}",
                    orderNumber, transactionId, tradeStatus, appId);

            // 参数校验
            if (orderNumber == null || transactionId == null) {
                return CallbackResult.fail("订单号或交易号为空");
            }

            // 根据交易状态处理
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 支付成功，更新订单状态
                int updated = orderMapper.updateOrderPaySuccess(orderNumber, "ALIPAY", transactionId);
                if (updated > 0) {
                    log.info("支付宝回调处理成功 - 订单支付成功: {}", orderNumber);

                    // 从回调参数中获取金额
                    String totalAmount = params.get("total_amount");
                    webSocketService.sendPaymentSuccessNotification(orderNumber, new BigDecimal(totalAmount));
                    return CallbackResult.success("支付成功", orderNumber, transactionId);
                } else {
                    log.warn("支付宝回调 - 订单更新失败，可能已处理过: {}", orderNumber);
                    return CallbackResult.success("订单已处理", orderNumber, transactionId);
                }

            } else if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
                log.info("订单等待付款: {}", orderNumber);
                return CallbackResult.success("等待付款", orderNumber, transactionId);

            } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                log.info("订单已关闭: {}", orderNumber);
                orderMapper.cancelOrder(orderNumber);
                return CallbackResult.success("订单已关闭", orderNumber, transactionId);

            } else {
                log.info("支付宝回调 - 未处理的交易状态: {}", tradeStatus);
                return CallbackResult.success("未处理状态", orderNumber, transactionId);
            }

        } catch (Exception e) {
            log.error("处理支付宝回调失败", e);
            return CallbackResult.fail("处理异常: " + e.getMessage());
        }
    }
    // ==================== 退款相关 ====================

    /**
     * 支付宝退款
     *
     * @param orderNumber  商户订单号
     * @param refundAmount 退款金额（单位：元）
     * @param refundReason 退款原因
     * @return 退款结果
     */
    public boolean refund(String orderNumber, BigDecimal refundAmount, String refundReason) {
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderNumber); // 商户单号
            bizContent.put("refund_amount", refundAmount.toString()); // 退款金额
            bizContent.put("refund_reason", refundReason); // 退款原因

            request.setBizContent(JSON.toJSONString(bizContent));

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

    // ==================== 回调结果封装类 ====================

    /**
     * 回调处理结果内部类
     */
    public static class CallbackResult {
        private boolean success;
        private String message;
        private String orderNumber;
        private String transactionId;

        private CallbackResult(boolean success, String message, String orderNumber, String transactionId) {
            this.success = success;
            this.message = message;
            this.orderNumber = orderNumber;
            this.transactionId = transactionId;
        }

        public static CallbackResult success(String message, String orderNumber, String transactionId) {
            return new CallbackResult(true, message, orderNumber, transactionId);
        }

        public static CallbackResult fail(String message) {
            return new CallbackResult(false, message, null, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getOrderNumber() { return orderNumber; }
        public String getTransactionId() { return transactionId; }
    }
}