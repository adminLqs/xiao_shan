package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息ID（雪花ID）
     */
    private Long id;

    /**
     * 发送者用户ID
     */
    private Long senderId;

    /**
     * 接收者用户ID
     */
    private Long receiverId;

    /**
     * 消息内容（文本）
     */
    private String content;

    /**
     * 消息类型: TEXT, IMAGE, VOICE, PRODUCT_CARD, ORDER_CARD, SYSTEM
     */
    private MessageType messageType;

    /**
     * 媒体URL列表（JSON数组格式）
     */
    private String mediaUrls;

    /**
     * 媒体URL列表（解析后的List）
     */
    private List<String> mediaUrlList;

    /**
     * 语音/视频时长（秒）
     */
    private Integer mediaDuration;

    /**
     * 商品ID（商品卡片消息）
     */
    private Long productId;

    /**
     * 订单ID（订单卡片消息）
     */
    private Long orderId;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 是否已撤回
     */
    private Boolean isRecalled;

    /**
     * 撤回时间
     */
    private LocalDateTime recalledAt;

    /**
     * 发送时间
     */
    private LocalDateTime createdAt;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        TEXT,           // 文本消息
        IMAGE,          // 图片消息
        VIDEO,          // 视频消息
        VOICE,          // 语音消息
        PRODUCT_CARD,   // 商品卡片
        ORDER_CARD,     // 订单卡片
        SYSTEM          // 系统消息
    }
}