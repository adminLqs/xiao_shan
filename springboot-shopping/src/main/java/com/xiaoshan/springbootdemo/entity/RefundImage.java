package com.xiaoshan.springbootdemo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 退款图片实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundImage {

    private Long id;
    private Long refundId;
    private Long communicationId;  // 新增：关联的消息ID
    private String image;
    private String imageType;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    /**
     * 图片类型枚举
     */
    public enum ImageType {
        EVIDENCE,  // 证据图片
        APPEAL,    // 申诉图片
        CHAT       // 聊天图片
    }
}
