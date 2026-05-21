package com.xiaoshan.springbootdemo.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 客服消息实体（数据库存储）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    private Long id;

    // 发送者ID
    private Long senderId;

    // 接收者ID
    private Long receiverId;

    private String content;

    private Boolean isRead;

    private LocalDateTime createdAt;
}