package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Product {

    private Long id;
    private Long sellerId;
    private Long categoryId;
    private String name;
    private String brand;
    private String description;
    private String detailHtml;
    private BigDecimal weight;
    private Boolean isFreeShipping;
    private String serviceGuarantee;
    private String deliveryCity;
    private Integer viewCount = 0;
    private Integer salesCount = 0;
    private Integer status = 1;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========== 传输字段（非数据库字段，用于列表展示） ==========
    private String images;           // 第一张图片URL（列表展示用）
    private BigDecimal price;        // SKU最低价格（列表展示用）
    private BigDecimal originalPrice; // SKU最高原价（列表展示用）
    private Integer stock;           // SKU库存总和
    private String categoryName;     // 分类名称
    private Integer commentCount;    // 评论数量

    public Product(Long sellerId, Long categoryId, String name, String brand, String description) {
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.name = name;
        this.brand = brand;
        this.description = description;
    }
}