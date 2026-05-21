package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项表实体
 */
@Data
@NoArgsConstructor
public class OrderItem {

    // ========== 主键 ==========
    // 订单项ID
    private Long id;

    // ========== 关联信息 ==========
    // 订单ID
    private Long orderId;

    // 商品ID
    private Long productId;

    // 卖家ID（商家ID，冗余字段，提升查询性能）
    private Long sellerId;

    // ========== 商品快照（防止商品信息变更后订单数据错误）==========
    // 商品名称
    private String productName;

    // 商品图片
    private String productImage;

    // 购买数量
    private Integer quantity;

    // 下单时单价
    private BigDecimal price;

    // 小计金额 = price * quantity
    private BigDecimal totalPrice;

    // ========== 评论状态 ==========
    // 是否已评论: 0-未评论, 1-已评论
    private Boolean isReviewed = false;

    // 评论时间
    private LocalDateTime reviewedAt;

    // ========== 时间戳 ==========
    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();
}