package com.xiaoshan.springbootdemo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final  NotificationService notificationService;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    // ==================== 公共 ====================

    public void kickOut(Long userId, String reason) {
        notificationService.create(userId, "SYSTEM", "账号安全提醒", reason, null);
        messagingTemplate.convertAndSend("/exchange/amq.topic/kickout.user." + userId,
            Map.of("type", "kickout", "content", reason));
        log.info("踢出用户: userId={}", userId);
    }

    // ==================== 用户端 ====================

    public void sendUserPaymentSuccess(Long userId, Long orderId, String orderNumber) {
        String extraData = "{\"orderId\":" + orderId + ",\"orderNumber\":\"" + orderNumber + "\"}";
        notificationService.create(userId, "ORDER", "支付成功", "订单 " + orderNumber + " 支付成功", extraData);
        
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.payment." + userId,
            Map.of("type", "payment", "orderNumber", orderNumber));
        log.info("通知用户支付成功: userId={}, orderId={}, orderNumber={}", userId, orderId, orderNumber);
    }

    public void sendUserShipment(Long userId, Long orderId, String orderNumber, String trackingNumber) {
        String extraData = "{\"orderId\":" + orderId + ",\"orderNumber\":\"" + orderNumber + "\",\"trackingNumber\":\"" + trackingNumber + "\"}";
        notificationService.create(userId, "ORDER", "订单已发货", "物流单号: " + trackingNumber, extraData);
        
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.shipment." + userId,
            Map.of("type", "shipment", "orderNumber", orderNumber,
                   "content", "您的订单已发货，物流单号：" + trackingNumber));
        log.info("通知用户发货: userId={}, orderId={}, orderNumber={}", userId, orderId, orderNumber);
    }

    public void sendUserRefundResult(Long userId, Long refundId, Long orderId, String content) {
        String extraData = "{\"refundId\":" + refundId + ",\"orderId\":" + orderId + "}";
        notificationService.create(userId, "ORDER", "退款通知", content, extraData);
        
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.refund." + userId,
            Map.of("type", "refund", "content", content));
        log.info("通知用户退款结果: userId={}, refundId={}, orderId={}", userId, refundId, orderId);
    }

    public void sendRefundChatMessage(Long userId, String content) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.refund.chat." + userId,
            Map.of("type", "refund-chat", "content", content));
        log.info("发送退款沟通消息: userId={}", userId);
    }

    public void sendRefundChatMessage(Long userId, String content, Long refundId) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.refund.chat." + userId,
            Map.of("type", "refund-chat", "content", content, "refundId", refundId));
        log.info("发送退款沟通消息: userId={}, refundId={}", userId, refundId);
    }

    public void sendRefundChatMessage(Long userId, String content, Long refundId, String senderType) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.refund.chat." + userId,
            Map.of("type", "refund-chat", "content", content, "refundId", refundId, "senderType", senderType));
        log.info("发送退款沟通消息: userId={}, refundId={}, senderType={}", userId, refundId, senderType);
    }

    public void sendChatToUser(Long userId, String content) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.chat." + userId,
            Map.of(
                "type", "TEXT",
                "text", content,
                "sendTime", java.time.LocalDateTime.now().toString()
            ));
        log.info("发送客服消息给用户: userId={}", userId);
    }

    /**
     * 发送消息给用户（支持媒体类型）
     */
    public void sendChatToUser(Long userId, String content, String messageType) {
        String notifyText = content != null ? content : "";
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.chat." + userId,
            Map.of(
                "type", messageType != null ? messageType : "TEXT",
                "text", notifyText,
                "sendTime", java.time.LocalDateTime.now().toString()
            ));
        log.info("发送客服消息给用户: userId={}, messageType={}", userId, messageType);
    }

    /**
     * 发送消息给用户（支持媒体类型和URL）
     */
    public void sendChatToUser(Long userId, String content, String messageType, List<String> mediaUrls) {
        String notifyText = content != null ? content : "";
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("type", messageType != null ? messageType : "TEXT");
        messageData.put("text", notifyText);
        messageData.put("sendTime", java.time.LocalDateTime.now().toString());
        if ("IMAGE".equals(messageType)) {
            messageData.put("images", mediaUrls != null ? mediaUrls : new ArrayList<>());
        } else if ("VIDEO".equals(messageType)) {
            messageData.put("videos", mediaUrls != null ? mediaUrls : new ArrayList<>());
        }
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.chat." + userId, messageData);
        log.info("发送客服消息给用户: userId={}, messageType={}, mediaUrls={}", userId, messageType, mediaUrls);
    }

    /**
     * 发送消息给用户（支持媒体类型和URL，包含senderId和senderName）
     */
    public void sendChatToUser(Long userId, Long senderId, String content, String messageType, List<String> mediaUrls, String senderName) {
        String notifyText = content != null ? content : "";
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", senderId);
        messageData.put("receiverId", userId);
        messageData.put("type", messageType != null ? messageType : "TEXT");
        messageData.put("text", notifyText);
        messageData.put("senderName", senderName);
        messageData.put("sendTime", java.time.LocalDateTime.now().toString());
        if ("IMAGE".equals(messageType)) {
            messageData.put("images", mediaUrls != null ? mediaUrls : new ArrayList<>());
        } else if ("VIDEO".equals(messageType)) {
            messageData.put("videos", mediaUrls != null ? mediaUrls : new ArrayList<>());
        }
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.chat." + userId, messageData);
        log.info("发送客服消息给用户: userId={}, senderId={}, messageType={}, mediaUrls={}, senderName={}", userId, senderId, messageType, mediaUrls, senderName);
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

    public void sendRefundApplication(Long sellerId, Long refundId, Long orderId, String orderNumber, BigDecimal amount) {
        String extraData = "{\"refundId\":" + refundId + ",\"orderId\":" + orderId + ",\"orderNumber\":\"" + orderNumber + "\",\"amount\":\"" + amount + "\"}";
        notificationService.create(sellerId, "ORDER", "退款申请", "订单号: " + orderNumber + " | 退款金额: ¥" + amount, extraData);
        
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.refund." + sellerId,
            Map.of("type", "refund-application", "orderNumber", orderNumber, "amount", amount));
        log.info("通知商家退款申请: sellerId={}, refundId={}, orderId={}, orderNumber={}", sellerId, refundId, orderId, orderNumber);
    }

    public void sendReturnSubmitted(Long sellerId, Map<String, Object> data) {
        messagingTemplate.convertAndSend(
                "/exchange/amq.topic/seller.return." + sellerId,
                data
        );
        log.info("通知商家退货提交: sellerId={}", sellerId);
    }

    public void sendSellerRefundChat(Long sellerId, String content) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.refund.chat." + sellerId,
            Map.of("type", "refund-chat", "content", content));
        log.info("通知商家退款沟通: sellerId={}", sellerId);
    }

    public void sendSellerRefundChat(Long sellerId, String content, Long refundId) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.refund.chat." + sellerId,
            Map.of("type", "refund-chat", "content", content, "refundId", refundId));
        log.info("通知商家退款沟通: sellerId={}, refundId={}", sellerId, refundId);
    }

    public void sendSellerRefundChat(Long sellerId, String content, Long refundId, String senderType) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.refund.chat." + sellerId,
            Map.of("type", "refund-chat", "content", content, "refundId", refundId, "senderType", senderType));
        log.info("通知商家退款沟通: sellerId={}, refundId={}, senderType={}", sellerId, refundId, senderType);
    }

    /**
     * 发送消息给商家（支持媒体类型和URL，包含senderName）
     */
    public void sendChatToSeller(Long sellerId, String content, Long fromUserId, String messageType, List<String> mediaUrls, String senderName) {
        String notifyText = content != null ? content : "";
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("type", messageType != null ? messageType : "TEXT");
        messageData.put("senderId", fromUserId);
        messageData.put("receiverId", sellerId);
        messageData.put("text", notifyText);
        messageData.put("senderName", senderName);
        messageData.put("sendTime", java.time.LocalDateTime.now().toString());
        if ("IMAGE".equals(messageType)) {
            messageData.put("images", mediaUrls != null ? mediaUrls : new ArrayList<>());
        } else if ("VIDEO".equals(messageType)) {
            messageData.put("videos", mediaUrls != null ? mediaUrls : new ArrayList<>());
        }
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.chat." + sellerId, messageData);
        log.info("发送消息给商家: sellerId={}, messageType={}, mediaUrls={}, senderName={}", sellerId, messageType, mediaUrls, senderName);
    }

    /**
     * 发送消息给商家（支持媒体类型，包含senderName）
     */
    public void sendChatToSeller(Long sellerId, String content, Long fromUserId, String messageType, String senderName) {
        String notifyText = content != null ? content : "";
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.chat." + sellerId,
            Map.of(
                "type", messageType != null ? messageType : "TEXT",
                "senderId", fromUserId,
                "receiverId", sellerId,
                "text", notifyText,
                "senderName", senderName,
                "sendTime", java.time.LocalDateTime.now().toString()
            ));
        log.info("发送消息给商家: sellerId={}, messageType={}, senderName={}", sellerId, messageType, senderName);
    }

    /**
     * 发送文本消息给商家，包含senderName
     */
    public void sendChatToSeller(Long sellerId, String content, Long fromUserId, String senderName) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.chat." + sellerId,
            Map.of(
                "type", "TEXT",
                "senderId", fromUserId,
                "receiverId", sellerId,
                "text", content,
                "senderName", senderName,
                "sendTime", java.time.LocalDateTime.now().toString()
            ));
        log.info("发送消息给商家: sellerId={}, senderName={}", sellerId, senderName);
    }

    public void sendPackageNotification(Long sellerId, String type, int days, String content) {
        String extraData = "{\"type\":\"" + type + "\",\"days\":" + days + "}";
        notificationService.create(sellerId, "SYSTEM", "套餐提醒", content, extraData);
        
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.package." + sellerId,
            Map.of("type", type, "days", days, "content", content));
        log.info("通知商家套餐: sellerId={}, type={}, days={}", sellerId, type, days);
    }

    /**
     * 发送聊天消息给目标用户
     */
    public void sendChatMessage(Long targetId, Object message) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/user.chat." + targetId, message);
        log.info("发送聊天消息: targetId={}", targetId);
    }

    /**
     * 发送聊天消息给商家
     */
    public void sendChatMessageToSeller(Long sellerId, Object message) {
        messagingTemplate.convertAndSend("/exchange/amq.topic/seller.chat." + sellerId, message);
        log.info("发送聊天消息给商家: sellerId={}", sellerId);
    }

    /**
     * 上传文件（支持图片和视频）
     * @param file 文件
     * @return 文件访问URL，上传失败返回null
     */
    public String uploadFile(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                log.warn("上传文件为空");
                return null;
            }

            String contentType = file.getContentType();
            if (contentType == null ||
                (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                log.warn("文件类型不允许: {}", contentType);
                return null;
            }

            // 根据文件类型设置大小限制
            long maxSize;
            if (contentType.startsWith("video/")) {
                maxSize = 100 * 1024 * 1024; // 视频：100MB
            } else {
                maxSize = 5 * 1024 * 1024; // 图片：5MB
            }

            if (file.getSize() > maxSize) {
                log.warn("文件大小超过限制: {} bytes, 最大允许: {} bytes", file.getSize(), maxSize);
                return null;
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            LocalDate now = LocalDate.now();
            String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
            String month = now.format(DateTimeFormatter.ofPattern("MM"));
            String day = now.format(DateTimeFormatter.ofPattern("dd"));

            Path uploadPath = Paths.get(uploadDir, "chat", year, month, day).toAbsolutePath().normalize();
            File destDir = uploadPath.toFile();

            if (!destDir.exists()) {
                boolean created = destDir.mkdirs();
                if (!created) {
                    log.error("创建上传目录失败: {}", destDir.getAbsolutePath());
                    return null;
                }
            }

            File destFile = new File(destDir, fileName);
            file.transferTo(destFile);

            String fileUrl = "/uploads/chat/" + year + "/" + month + "/" + day + "/" + fileName;
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return null;
        }
    }
}