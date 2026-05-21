package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项实体类
 */
@Data
@NoArgsConstructor
public class OrderItem {

    private Long id;

    // 订单ID
    private Long orderId;

    // 商品ID
    private Long productId;

    // 商品名称
    private String productName;

    // 商品数量
    private Integer quantity;

    // 商品图片
    private String image;

    // 商品总价
    private BigDecimal price;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

    // 业务构造
    public OrderItem(Long orderId, Long productId,
                     String productName, String image, Integer quantity, BigDecimal price) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.image = image;
        this.price = price;
        this.createdAt = LocalDateTime.now();
    }
}