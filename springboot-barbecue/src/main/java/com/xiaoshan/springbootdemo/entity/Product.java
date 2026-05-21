package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 */
@Data
@NoArgsConstructor
public class Product {

    private Long id;

    // 商品名称
    private String name;

    // 商品描述
    private String description;

    // 价格
    private BigDecimal price;

    // 商品原价
    private BigDecimal originalPrice;

    // 商品图片URL
    private String image;

    // 分类
    private String category;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 业务构造
    public Product(String name, BigDecimal price, String category, String image) {
        this.name = name;
        this.price = price;
        this.originalPrice = price;
        this.image = image;
        this.category = category;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}