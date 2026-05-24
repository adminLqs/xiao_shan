package com.xiaoshan.springbootdemo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // ==================== 公共 ====================

    public void kickOut(Long userId, String reason) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/kickout.user." + userId,
            Map.of("type", "kickout", "content", reason));
        log.info("踢出用户: userId={}", userId);
    }

    // ==================== 用户端 ====================

    public void sendUserPaymentSuccess(Long userId, String orderNumber) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.payment." + userId,
            Map.of("type", "payment", "orderNumber", orderNumber));
        log.info("通知用户支付成功: userId={}, orderNumber={}", userId, orderNumber);
    }

    public void sendUserShipment(Long userId, String orderNumber, String trackingNumber) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.shipment." + userId,
            Map.of("type", "shipment", "orderNumber", orderNumber,
                   "content", "您的订单已发货，物流单号：" + trackingNumber));
        log.info("通知用户发货: userId={}, orderNumber={}", userId, orderNumber);
    }

    public void sendUserRefundResult(Long userId, String content) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.refund." + userId,
            Map.of("type", "refund", "content", content));
        log.info("通知用户退款结果: userId={}, content={}", userId, content);
    }

    public void sendRefundChatMessage(Long userId, String content) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.refund.chat." + userId,
            Map.of("type", "refund-chat", "content", content));
        log.info("发送退款沟通消息: userId={}", userId);
    }

    public void sendChatToUser(Long userId, String content) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.chat." + userId,
            Map.of("type", "chat", "content", content));
        log.info("发送客服消息给用户: userId={}", userId);
    }

    // ==================== 商家端 ====================

    public void sendNewOrder(String orderNumber, BigDecimal amount) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.new-order",
            Map.of("type", "new-order", "orderNumber", orderNumber, "amount", amount));
        log.info("发送新订单广播: orderNumber={}", orderNumber);
    }

    public void sendSellerPaymentSuccess(String orderNumber, BigDecimal amount) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.payment-success",
            Map.of("type", "payment-success", "orderNumber", orderNumber, "amount", amount));
        log.info("发送支付成功广播: orderNumber={}", orderNumber);
    }

    public void sendRefundApplication(Long sellerId, String orderNumber, BigDecimal amount) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.refund." + sellerId,
            Map.of("type", "refund-application", "orderNumber", orderNumber, "amount", amount));
        log.info("通知商家退款申请: sellerId={}, orderNumber={}", sellerId, orderNumber);
    }

    public void sendSellerRefundChat(Long sellerId, String content) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.refund.chat." + sellerId,
            Map.of("type", "refund-chat", "content", content));
        log.info("通知商家退款沟通: sellerId={}", sellerId);
    }

    public void sendChatToSeller(Long sellerId, String content, Long fromUserId) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.chat." + sellerId,
            Map.of("type", "chat", "content", content, "fromUserId", fromUserId));
        log.info("发送消息给商家: sellerId={}", sellerId);
    }

    public void sendPackageNotification(Long sellerId, String type, int days, String content) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.package." + sellerId,
            Map.of("type", type, "days", days, "content", content));
        log.info("通知商家套餐: sellerId={}, type={}, days={}", sellerId, type, days);
    }
}