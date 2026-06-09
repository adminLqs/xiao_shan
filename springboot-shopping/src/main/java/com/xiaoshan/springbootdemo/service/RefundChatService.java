package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.entity.OrderRefund;
import com.xiaoshan.springbootdemo.entity.RefundChat;
import com.xiaoshan.springbootdemo.entity.RefundImage;
import com.xiaoshan.springbootdemo.mapper.OrderItemMapper;
import com.xiaoshan.springbootdemo.mapper.OrderRefundMapper;
import com.xiaoshan.springbootdemo.mapper.RefundChatMapper;
import com.xiaoshan.springbootdemo.mapper.RefundImageMapper;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 退款聊天服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundChatService {

    private final RefundChatMapper refundChatMapper;
    private final OrderRefundMapper orderRefundMapper;
    private final OrderItemMapper orderItemMapper;
    private final RefundImageMapper refundImageMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final WebSocketService webSocketService;

    /**
     * 发送消息
     * @param refundId 退款ID
     * @param senderType 发送者类型
     * @param senderId 发送者ID
     * @param message 消息内容
     * @param images 图片URL（逗号分隔）
     */
    @Transactional
    public RefundChat sendMessage(Long refundId, String senderType, Long senderId, String message, String images) {
        // 验证退款记录存在
        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        // 验证发送者权限（系统消息不验证）
        if (!"SYSTEM".equals(senderType)) {
            validateSenderPermission(refundId, senderType, senderId);
        }

        // 1. 先创建聊天记录（不包含图片信息）
        RefundChat chat = new RefundChat();
        chat.setId(snowflakeIdGenerator.nextId());
        chat.setRefundId(refundId);
        chat.setSenderType(senderType);
        chat.setSenderId(senderId != null ? senderId : 0L);
        chat.setMessage(message != null ? message : "");
        chat.setSendTime(LocalDateTime.now());

        refundChatMapper.insert(chat);

        // 2. 再插入图片，关联消息ID
        if (images != null && !images.trim().isEmpty()) {
            String[] imageUrls = images.split(",");
            int sortOrder = 0;
            for (String url : imageUrls) {
                if (url.trim().isEmpty()) continue;
                RefundImage refundImage = RefundImage.builder()
                        .id(snowflakeIdGenerator.nextId())
                        .refundId(refundId)
                        .communicationId(chat.getId())
                        .image(url.trim())
                        .imageType(RefundImage.ImageType.CHAT.name())
                        .sortOrder(sortOrder++)
                        .createdAt(LocalDateTime.now())
                        .build();
                refundImageMapper.insert(refundImage);
            }
        }

        // 发送消息通知给接收方
        sendNotificationToReceiver(refund, senderType, senderId, message);

        return chat;
    }

    /**
     * 发送消息（带文件）
     * @param refundId 退款ID
     * @param senderType 发送者类型
     * @param senderId 发送者ID
     * @param message 消息内容
     * @param files 图片文件列表
     */
    @Transactional
    public void sendMessageWithFiles(Long refundId, String senderType, Long senderId, String message, List<MultipartFile> files) {
        // 验证退款记录存在
        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        // 验证发送者权限（系统消息不验证）
        if (!"SYSTEM".equals(senderType)) {
            validateSenderPermission(refundId, senderType, senderId);
        }

        // 确定消息内容
        String content = message != null ? message.trim() : "";

        // 1. 先创建聊天记录
        RefundChat chat = new RefundChat();
        chat.setId(snowflakeIdGenerator.nextId());
        chat.setRefundId(refundId);
        chat.setSenderType(senderType);
        chat.setSenderId(senderId != null ? senderId : 0L);
        chat.setMessage(content);
        chat.setSendTime(LocalDateTime.now());

        refundChatMapper.insert(chat);

        // 2. 再处理图片，关联消息ID
        if (files != null && !files.isEmpty()) {
            int sortOrder = 0;
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String url = saveFile(file);

                RefundImage refundImage = RefundImage.builder()
                        .id(snowflakeIdGenerator.nextId())
                        .refundId(refundId)
                        .communicationId(chat.getId())
                        .image(url)
                        .imageType(RefundImage.ImageType.CHAT.name())
                        .sortOrder(sortOrder++)
                        .createdAt(LocalDateTime.now())
                        .build();
                refundImageMapper.insert(refundImage);
            }
        }

        // 发送消息通知给接收方
        sendNotificationToReceiver(refund, senderType, senderId, content);
    }

    /**
     * 发送消息通知给接收方
     */
    private void sendNotificationToReceiver(OrderRefund refund, String senderType, Long senderId, String message) {
        // 获取接收方ID
        Long receiverId = null;
        String receiverType = null;

        if ("SELLER".equals(senderType)) {
            // 商家发送消息，接收方是用户
            receiverId = refund.getUserId();
            receiverType = "USER";
        } else {
            // 用户发送消息，接收方是商家
            List<OrderItem> orderItems = orderItemMapper.findByOrderId(refund.getOrderId());
            if (!orderItems.isEmpty()) {
                receiverId = orderItems.get(0).getSellerId();
                receiverType = "SELLER";
            }
        }

        // 发送通知
        if (receiverId != null) {
            if ("SELLER".equals(receiverType)) {
                webSocketService.sendSellerRefundChat(receiverId, message, refund.getId(), senderType);
            } else {
                webSocketService.sendRefundChatMessage(receiverId, message, refund.getId(), senderType);
            }
            log.info("发送退款沟通消息通知: receiverType={}, receiverId={}, refundId={}", receiverType, receiverId, refund.getId());
        }
    }

    /**
     * 保存文件到服务器
     * @param file 上传的文件
     * @return 文件访问URL
     */
    private String saveFile(MultipartFile file) {
        try {
            // 定义存储目录（相对于应用运行目录）
            String uploadDir = "uploads/refund/";
            Path uploadPath = Paths.get(uploadDir);
            
            // 如果目录不存在，创建目录
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) 
                    + "_" + snowflakeIdGenerator.nextId() + extension;

            // 保存文件
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // 返回访问URL
            return "/api/v1/files/" + newFilename;

        } catch (IOException e) {
            log.error("保存文件失败", e);
            throw new RuntimeException("文件保存失败");
        }
    }

    /**
     * 商家回复退款申请
     * @param refundId 退款ID
     * @param sellerId 商家ID
     * @param response 回复内容
     * @param images 图片证据
     */
    @Transactional
    public RefundChat sellerResponse(Long refundId, Long sellerId, String response, String images) {
        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        // 验证商家权限
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(refund.getOrderId());
        if (orderItems.isEmpty() || !orderItems.get(0).getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作此退款申请");
        }

        // 发送商家消息
        RefundChat chat = sendMessage(refundId, "SELLER", sellerId, response, images);

        return chat;
    }

    /**
     * 获取退款聊天记录
     * @param refundId 退款ID
     * @param userId 用户ID（用于权限验证）
     */
    public List<RefundChat> getChatHistory(Long refundId, Long userId) {
        // 验证权限
        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        // 用户只能查看自己的退款记录
        if (!refund.getUserId().equals(userId)) {
            // 检查是否为商家
            List<OrderItem> orderItems = orderItemMapper.findByOrderId(refund.getOrderId());
            boolean isSeller = !orderItems.isEmpty() && orderItems.get(0).getSellerId().equals(userId);
            if (!isSeller) {
                throw new RuntimeException("无权查看此退款聊天记录");
            }
        }

        // 先查消息列表
        List<RefundChat> chatList = refundChatMapper.findByRefundId(refundId);

        // 对每条消息，查询关联的图片
        for (RefundChat chat : chatList) {
            List<RefundImage> images = refundImageMapper.findByCommunicationId(chat.getId());
            if (images != null && !images.isEmpty()) {
                List<String> imageUrls = new ArrayList<>();
                for (RefundImage img : images) {
                    imageUrls.add(img.getImage());
                }
                chat.setImages(imageUrls);
            }
        }

        return chatList;
    }

    /**
     * 获取退款详情（含聊天记录和订单信息）
     * @param refundId 退款ID
     * @param userId 用户ID
     */
    public Map<String, Object> getRefundDetailWithChat(Long refundId, Long userId) {
        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        // 权限验证
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(refund.getOrderId());
        boolean isOwner = refund.getUserId().equals(userId);
        boolean isSeller = !orderItems.isEmpty() && orderItems.get(0).getSellerId().equals(userId);

        if (!isOwner && !isSeller) {
            throw new RuntimeException("无权查看此退款记录");
        }

        // 获取聊天记录（含图片）
        List<RefundChat> chatHistory = getChatHistory(refundId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("refund", refund);
        result.put("orderItems", orderItems);
        result.put("chatHistory", chatHistory);

        return result;
    }

    /**
     * 验证发送者权限
     */
    private void validateSenderPermission(Long refundId, String senderType, Long senderId) {
        OrderRefund refund = orderRefundMapper.findById(refundId).orElse(null);
        if (refund == null) {
            throw new RuntimeException("退款记录不存在");
        }

        List<OrderItem> orderItems = orderItemMapper.findByOrderId(refund.getOrderId());

        if ("BUYER".equals(senderType)) {
            // 买家只能发送自己的退款消息
            if (!refund.getUserId().equals(senderId)) {
                throw new RuntimeException("无权发送此消息");
            }
        } else if ("SELLER".equals(senderType)) {
            // 商家只能回复自己订单的退款消息
            if (orderItems.isEmpty() || !orderItems.get(0).getSellerId().equals(senderId)) {
                throw new RuntimeException("无权发送此消息");
            }
        }
    }
}