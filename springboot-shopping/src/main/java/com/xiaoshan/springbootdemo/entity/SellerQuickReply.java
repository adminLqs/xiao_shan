package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商家快捷回复实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerQuickReply {

    /**
     * 快捷回复ID
     */
    private Long id;

    /**
     * 商家用户ID
     */
    private Long sellerId;

    /**
     * 快捷回复内容
     */
    private String content;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}