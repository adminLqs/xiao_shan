package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 评论图片表实体
 */
@Data
@NoArgsConstructor
public class ReviewImage {

    // 主键ID
    private Long id;

    // 评论ID
    private Long reviewId;

    // 图片URL
    private String image;

    // 图片排序（数字越小越靠前）
    private Integer sortOrder;

    // 创建时间
    private LocalDateTime createdAt;
}