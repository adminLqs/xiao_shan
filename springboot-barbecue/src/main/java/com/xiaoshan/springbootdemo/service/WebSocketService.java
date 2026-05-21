package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 发送新订单通知
     */
    public void sendNewOrderNotification(String orderNumber, BigDecimal amount) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("new_order");
        message.setOrderNumber(orderNumber);
        message.setAmount(amount);
        message.setSendTime(LocalDateTime.now());

        // 广播到 /topic/new-order，所有订阅的客户端都能收到
        messagingTemplate.convertAndSend("/topic/new-order", message);

        log.info("发送新订单通知广播: orderNumber={}, amount={}", orderNumber, amount);
    }

    /**
     * 用户发送消息给商家（广播方式）
     */
    public void sendChatToSeller(Long userId, String content) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("chat");
        message.setContent(content);
        message.setFromUserId(userId);
        message.setSendTime(LocalDateTime.now());

        // 广播到 /topic/chat，商家端订阅即可收到
        messagingTemplate.convertAndSend("/topic/chat", message);

        log.info("发送客服消息广播: userId={}, content={}", userId, content);
    }

    /**
     * 商家回复消息给用户（点对点）
     */
    public void sendChatToUser(Long userId, String content) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("chat");
        message.setContent(content);
        message.setFromUserId(null);  // 商家不设ID
        message.setSendTime(LocalDateTime.now());

        // 点对点发送给指定用户
        String destination = "/topic/chat/user/" + userId;
        messagingTemplate.convertAndSend(destination, message);

        log.info("商家回复消息给用户: userId={}, content={}", userId, content);
    }

    /**
     * 发送支付成功通知（通知商家订单已支付）
     */
    public void sendPaymentSuccessNotification(String orderNumber, BigDecimal amount) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("payment_success");
        message.setOrderNumber(orderNumber);
        message.setAmount(amount);
        message.setSendTime(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/payment-success", message);
        log.info("发送支付成功通知广播: orderNumber={}, amount={}", orderNumber, amount);
    }

    /**
     * 发送商家发货通知（通知用户订单已发货）
     *
     * @param userId 用户ID
     * @param orderNumber 订单号
     * @param trackingNumber 物流单号
     */
    public void sendShipmentNotification(Long userId, String orderNumber, String trackingNumber) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("shipment");
        message.setOrderNumber(orderNumber);
        message.setContent("您的订单已发货，物流单号：" + trackingNumber);
        message.setAmount(null);
        message.setSendTime(LocalDateTime.now());

        // 发送给指定用户
        String destination = "/topic/shipment/user/" + userId;
        messagingTemplate.convertAndSend(destination, message);

        log.info("发送发货通知给用户: userId={}, orderNumber={}, trackingNumber={}", userId, orderNumber, trackingNumber);
    }

    /**
     * 发送退款申请通知（通知商家有新退款申请）
     *
     * @param orderNumber 订单号
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     */
    public void sendRefundApplicationNotification(String orderNumber, BigDecimal refundAmount, String refundReason) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("refund_application");
        message.setOrderNumber(orderNumber);
        message.setAmount(refundAmount);
        message.setContent(refundReason);
        message.setSendTime(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/refund-application", message);
        log.info("发送退款申请通知广播: orderNumber={}, refundAmount={}, refundReason={}",
                orderNumber, refundAmount, refundReason);
    }



}