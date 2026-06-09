package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.ChatMessage;
import com.xiaoshan.springbootdemo.entity.ChatSession;
import com.xiaoshan.springbootdemo.entity.SellerProfile;
import com.xiaoshan.springbootdemo.entity.SellerQuickReply;
import com.xiaoshan.springbootdemo.entity.User;
import com.xiaoshan.springbootdemo.entity.UserProfile;
import com.xiaoshan.springbootdemo.mapper.ChatMessageMapper;
import com.xiaoshan.springbootdemo.mapper.ChatSessionMapper;
import com.xiaoshan.springbootdemo.mapper.SellerProfileMapper;
import com.xiaoshan.springbootdemo.mapper.SellerQuickReplyMapper;
import com.xiaoshan.springbootdemo.mapper.UserMapper;
import com.xiaoshan.springbootdemo.mapper.UserProfileMapper;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 聊天服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final SellerQuickReplyMapper sellerQuickReplyMapper;
    private final UserMapper userMapper;
    private final SellerProfileMapper sellerProfileMapper;
    private final UserProfileMapper userProfileMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ObjectMapper objectMapper;

    /**
     * 发送消息
     */
    @Transactional
    public ChatMessage sendMessage(Long senderId, Long receiverId, String content,
                                   ChatMessage.MessageType messageType, List<String> mediaUrls,
                                   Integer mediaDuration, Long productId, Long orderId) {
        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setId(snowflakeIdGenerator.nextId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setIsRead(false);
        message.setIsRecalled(false);
        message.setCreatedAt(LocalDateTime.now());

        // 设置媒体URL
        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            try {
                message.setMediaUrls(objectMapper.writeValueAsString(mediaUrls));
            } catch (JsonProcessingException e) {
                log.error("序列化媒体URL失败", e);
            }
        }

        message.setMediaDuration(mediaDuration);
        message.setProductId(productId);
        message.setOrderId(orderId);

        // 保存消息
        chatMessageMapper.insert(message);

        // 生成会话预览文字
        String sessionPreview = generateSessionPreview(content, messageType);

        // 更新或创建会话
        updateOrCreateSession(senderId, receiverId, sessionPreview);
        updateOrCreateSession(receiverId, senderId, sessionPreview);

        // 增加接收方的未读消息数
        chatSessionMapper.incrementUnreadCount(receiverId, senderId);

        return message;
    }

    /**
     * 生成会话预览文字
     */
    private String generateSessionPreview(String content, ChatMessage.MessageType messageType) {
        if (content != null && !content.trim().isEmpty()) {
            return content;
        }
        if (messageType == ChatMessage.MessageType.IMAGE) {
            return "[图片]";
        }
        if (messageType == ChatMessage.MessageType.VIDEO) {
            return "[视频]";
        }
        if (messageType == ChatMessage.MessageType.PRODUCT_CARD) {
            return "[商品]";
        }
        if (messageType == ChatMessage.MessageType.ORDER_CARD) {
            return "[订单]";
        }
        return "";
    }

    /**
     * 更新或创建会话
     */
    private void updateOrCreateSession(Long userId, Long targetId, String lastMessage) {
        ChatSession existing = chatSessionMapper.findByUserAndTarget(userId, targetId);
        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            // 更新现有会话
            chatSessionMapper.updateLastMessage(userId, targetId, lastMessage, now, now);
        } else {
            // 创建新会话
            ChatSession session = new ChatSession();
            session.setId(snowflakeIdGenerator.nextId());
            session.setUserId(userId);
            session.setTargetId(targetId);
            session.setLastMessage(lastMessage);
            session.setLastMessageTime(now);
            session.setUnreadCount(0);
            session.setIsTop(false);
            session.setIsMuted(false);
            session.setCreatedAt(now);
            session.setUpdatedAt(now);
            chatSessionMapper.insert(session);
        }
    }

    /**
     * 获取用户之间的消息列表（分页）
     */
    public List<ChatMessage> getMessages(Long userId, Long targetId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<ChatMessage> messages = chatMessageMapper.findMessagesBetweenUsers(userId, targetId, offset, pageSize);
        
        // 解析媒体URL
        for (ChatMessage msg : messages) {
            msg.setMediaUrlList(parseMediaUrls(msg.getMediaUrls()));
        }
        
        return messages;
    }

    /**
     * 获取消息总数
     */
    public int getMessageCount(Long userId, Long targetId) {
        return chatMessageMapper.countMessagesBetweenUsers(userId, targetId);
    }

    /**
     * 标记消息为已读
     */
    @Transactional
    public void markMessagesAsRead(Long userId, Long targetId) {
        chatMessageMapper.markMessagesAsRead(userId, targetId);
        chatSessionMapper.clearUnreadCount(userId, targetId);
    }

    /**
     * 撤回消息（2分钟内）
     */
    @Transactional
    public boolean recallMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageMapper.findById(messageId);
        if (message == null) {
            return false;
        }

        // 只能撤回自己发送的消息
        if (!message.getSenderId().equals(userId)) {
            return false;
        }

        // 只能在2分钟内撤回
        LocalDateTime now = LocalDateTime.now();
        long minutesDiff = ChronoUnit.MINUTES.between(message.getCreatedAt(), now);
        if (minutesDiff > 2) {
            return false;
        }

        chatMessageMapper.recallMessage(messageId, now);
        return true;
    }

    /**
     * 获取用户的会话列表
     */
    public List<Map<String, Object>> getSessions(Long userId) {
        List<ChatSession> sessions = chatSessionMapper.findByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ChatSession session : sessions) {
            Map<String, Object> sessionMap = new HashMap<>();

            // 获取目标用户信息
            User target = userMapper.findById(session.getTargetId()).orElse(null);
            if (target != null) {
                sessionMap.put("id", target.getId());
                
                // 根据角色获取名称和头像
                if ("ROLE_SELLER".equals(target.getRole())) {
                    // 商家：获取店铺信息
                    var sellerProfile = sellerProfileMapper.findByUserId(target.getId()).orElse(null);
                    if (sellerProfile != null) {
                        sessionMap.put("name", sellerProfile.getStoreName() != null && !sellerProfile.getStoreName().isEmpty() 
                                ? sellerProfile.getStoreName() : "商家");
                        if (sellerProfile.getStoreAvatar() != null) {
                            sessionMap.put("avatar", sellerProfile.getStoreAvatar());
                        }
                    } else {
                        sessionMap.put("name", "商家");
                    }
                } else {
                    // 普通用户：获取用户资料
                    var userProfile = userProfileMapper.findByUserId(target.getId()).orElse(null);
                    if (userProfile != null) {
                        // 优先使用昵称，没有则使用账号
                        sessionMap.put("name", userProfile.getNickname() != null && !userProfile.getNickname().isEmpty() 
                                ? userProfile.getNickname() : target.getAccount());
                        if (userProfile.getAvatar() != null && !userProfile.getAvatar().contains("default-admin-avatar")) {
                            sessionMap.put("avatar", userProfile.getAvatar());
                        }
                    } else {
                        sessionMap.put("name", target.getAccount());
                    }
                }
                
                sessionMap.put("online", false); // 默认离线
            }

            // 统一的消息相关字段
            sessionMap.put("latest", session.getLastMessage());
            sessionMap.put("unread", session.getUnreadCount());
            sessionMap.put("time", session.getLastMessageTime() != null 
                    ? session.getLastMessageTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
            sessionMap.put("isTop", session.getIsTop());
            sessionMap.put("isMuted", session.getIsMuted());

            result.add(sessionMap);
        }

        return result;
    }

    /**
     * 设置会话置顶
     */
    public void setSessionTop(Long sessionId, Boolean isTop) {
        chatSessionMapper.updateTopStatus(sessionId, isTop);
    }

    /**
     * 设置会话免打扰
     */
    public void setSessionMuted(Long sessionId, Boolean isMuted) {
        chatSessionMapper.updateMuteStatus(sessionId, isMuted);
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(Long userId, Long targetId) {
        chatSessionMapper.deleteByUserAndTarget(userId, targetId);
    }

    /**
     * 获取未读消息总数
     */
    public int getUnreadCount(Long userId) {
        return chatSessionMapper.sumUnreadCount(userId);
    }

    /**
     * 添加快捷回复
     */
    public SellerQuickReply addQuickReply(Long sellerId, String content) {
        SellerQuickReply reply = new SellerQuickReply();
        reply.setSellerId(sellerId);
        reply.setContent(content);
        reply.setSortOrder(0);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());
        sellerQuickReplyMapper.insert(reply);
        return reply;
    }

    /**
     * 获取商家的快捷回复列表
     */
    public List<SellerQuickReply> getQuickReplies(Long sellerId) {
        return sellerQuickReplyMapper.findBySellerId(sellerId);
    }

    /**
     * 更新快捷回复
     */
    public void updateQuickReply(Long id, String content) {
        SellerQuickReply reply = sellerQuickReplyMapper.findById(id);
        if (reply != null) {
            reply.setContent(content);
            reply.setUpdatedAt(LocalDateTime.now());
            sellerQuickReplyMapper.update(reply);
        }
    }

    /**
     * 删除快捷回复
     */
    public void deleteQuickReply(Long id) {
        sellerQuickReplyMapper.deleteById(id);
    }

    /**
     * 初始化商家默认快捷回复
     */
    @Transactional
    public void initDefaultQuickReplies(Long sellerId) {
        List<String> defaultReplies = Arrays.asList(
                "亲，有什么可以帮您的？",
                "您的订单已发货，请注意查收",
                "感谢您的支持，有问题随时联系我们",
                "抱歉给您带来不便，我们会尽快处理",
                "您好，请问有什么可以帮助您的？"
        );

        List<SellerQuickReply> existing = sellerQuickReplyMapper.findBySellerId(sellerId);
        if (!existing.isEmpty()) {
            return; // 已有快捷回复，不重复初始化
        }

        int sortOrder = 0;
        for (String content : defaultReplies) {
            SellerQuickReply reply = new SellerQuickReply();
            reply.setSellerId(sellerId);
            reply.setContent(content);
            reply.setSortOrder(sortOrder++);
            reply.setCreatedAt(LocalDateTime.now());
            reply.setUpdatedAt(LocalDateTime.now());
            sellerQuickReplyMapper.insert(reply);
        }
    }

    /**
     * 解析媒体URL
     */
    private List<String> parseMediaUrls(String mediaUrlsJson) {
        if (mediaUrlsJson == null || mediaUrlsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(mediaUrlsJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("解析媒体URL失败", e);
            return Collections.emptyList();
        }
    }
}