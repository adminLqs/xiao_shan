package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款聊天记录实体
 * 用于记录买家和商家在退款过程中的沟通内容
 *
 * @author xiaoshan
 * @date 2026-05-25
 */
@Data
@NoArgsConstructor
public class RefundChat {

    // 聊天记录ID
    private Long id;

    // 退款ID（关联OrderRefund表）
    private Long refundId;

    // 发送者类型: BUYER-买家, SELLER-商家
    private String senderType;

    // 发送者ID（用户ID或商家ID）
    private Long senderId;

    // 发送者名称（买家昵称或商家店铺名）
    private String senderName;

    // 发送者头像（买家头像或商家店铺头像）
    private String senderAvatar;

    // 消息内容（数据库字段为content）
    private String message;

    // 图片URL列表（查询时根据communication_id关联查询）
    private List<String> images;

    // 沟通轮次
    private Integer round;

    // 发送时间（数据库字段为created_at）
    private LocalDateTime sendTime;

    // 发送者类型枚举
    public enum SenderType {
        BUYER,    // 买家
        SELLER,   // 商家
        SYSTEM    // 系统
    }
}