package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;


// 商品评价实体
@Data
@NoArgsConstructor
public class Review {

    private Long id;

    // 用户ID
    private Long userId;

    // 商品ID
    private Long productId;

    // 订单ID
    private Long orderId;

    // 订单项ID
    private Long orderItemId;

    // 评分
    private Integer rating;

    // 购买规格快照
    private String skuSpec;

    // 评论
    private String comment;

    // 评论创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

    // 评论IP地址
    private String ip;

    // IP属地（如广东广州）
    private String location;

    // 商品名称（JOIN查询获取）
    private String productName;

    // 商品图片（JOIN查询获取）
    private String productImage;

    // 规格名称（JOIN查询获取）
    private String skuName;

    // 评论图片列表（JOIN查询获取）
    private List<ReviewImage> images;

    // 评论视频列表（JOIN查询获取）
    private List<ReviewVideo> videos;

}
