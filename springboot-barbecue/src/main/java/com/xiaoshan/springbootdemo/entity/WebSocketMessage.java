package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WebSocket传输对象（不存数据库）
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /** 消息类型：new_order, chat */
    private String type;

    /** 订单号（新订单时使用） */
    private String orderNumber;

    /** 订单金额（新订单时使用） */
    private BigDecimal amount;

    /** 消息内容（聊天时使用） */
    private String content;

    /** 发送者ID（聊天时使用） */
    private Long fromUserId;

    /** 发送时间 */
    private LocalDateTime sendTime = LocalDateTime.now();
}