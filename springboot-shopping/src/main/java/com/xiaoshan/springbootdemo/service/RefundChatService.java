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

        // 验证发送者权限
        validateSenderPermission(refundId, senderType, senderId);

        // 验证沟通轮次限制（买家最多3轮，商家不受限制）
        int currentRound = refund.getCommunicationRound() != null ? refund.getCommunicationRound() : 0;
        if (!"SELLER".equals(senderType) && currentRound >= 3) {
            throw new RuntimeException("已达到最大沟通次数限制");
        }

        // 发送纯图片消息时，content 设置默认值
        if (message == null || message.trim().isEmpty()) {
            message = "[图片]";
        }

        // 1. 先创建聊天记录（不包含图片信息）
        RefundChat chat = new RefundChat();
        chat.setId(snowflakeIdGenerator.nextId());
        chat.setRefundId(refundId);
        chat.setSenderType(senderType);
        chat.setSenderId(senderId);
        chat.setMessage(message);
        chat.setRound(currentRound);
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
                        .communicationId(chat.getId())  // 关联消息ID
                        .image(url.trim())
                        .imageType(RefundImage.ImageType.CHAT.name())
                        .sortOrder(sortOrder++)
                        .createdAt(LocalDateTime.now())
                        .build();
                refundImageMapper.insert(refundImage);
            }
        }

        // 只有买家发送消息时，沟通轮次 + 1
        if (!"SELLER".equals(senderType)) {
            refund.setCommunicationRound(currentRound + 1);
            orderRefundMapper.updateStatus(refund);
        }

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

        // 验证发送者权限
        validateSenderPermission(refundId, senderType, senderId);

        // 获取当前沟通轮次
        int currentRound = refund.getCommunicationRound() != null ? refund.getCommunicationRound() : 0;

        // 标记是否需要增加轮次
        boolean hasTextMessage = message != null && !message.trim().isEmpty() && !message.trim().equals("[图片]");

        // 验证沟通轮次限制（买家最多3轮）
        if ("BUYER".equals(senderType) && hasTextMessage && currentRound >= 3) {
            throw new RuntimeException("已达到最大沟通次数限制");
        }

        // 确定消息内容
        String content;
        if (hasTextMessage) {
            content = message;
        } else if (files != null && !files.isEmpty()) {
            content = "[图片]";
        } else {
            content = "[空消息]";
        }

        // 1. 先创建聊天记录
        RefundChat chat = new RefundChat();
        chat.setId(snowflakeIdGenerator.nextId());
        chat.setRefundId(refundId);
        chat.setSenderType(senderType);
        chat.setSenderId(senderId);
        chat.setMessage(content);
        chat.setRound(currentRound);
        chat.setSendTime(LocalDateTime.now());

        refundChatMapper.insert(chat);

        // 2. 再处理图片，关联消息ID
        if (files != null && !files.isEmpty()) {
            int sortOrder = 0;
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                // 上传图片获取 URL
                String url = saveFile(file);

                // 插入 refund_images，设置 communication_id
                RefundImage refundImage = RefundImage.builder()
                        .id(snowflakeIdGenerator.nextId())
                        .refundId(refundId)
                        .communicationId(chat.getId())  // 关联消息ID
                        .image(url)
                        .imageType(RefundImage.ImageType.CHAT.name())
                        .sortOrder(sortOrder++)
                        .createdAt(LocalDateTime.now())
                        .build();
                refundImageMapper.insert(refundImage);
            }
        }

        // 3. 买家发送任何消息都触发轮次+1
        if ("BUYER".equals(senderType)) {
            refund.setCommunicationRound(currentRound + 1);
            orderRefundMapper.updateStatus(refund);
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
        result.put("communicationRound", refund.getCommunicationRound());

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