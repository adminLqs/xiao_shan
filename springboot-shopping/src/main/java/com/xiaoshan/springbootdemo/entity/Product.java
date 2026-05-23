package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 商品类
@Data
@NoArgsConstructor
public class Product {

    private Long id;

    // 商家ID
    private Long sellerId;

    // 分类ID
    private Long categoryId;

    // 商品名称
    private String name;

    // 商品品牌
    private String brand;

    // 商品描述
    private String description;

    // 已售数量
    private Integer salesCount = 0;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 商品状态：0-下架，1-上架
    private Integer status = 1;

    // ======== 从 SKU 表汇总的字段（不存数据库，通过查询 SQL 的子查询获取） ========
    private BigDecimal price;      // 最低 SKU 价格

    private BigDecimal originalPrice; // SKU 原价

    private Integer stock;         // SKU 库存总和

    // ======== 映射字段 ========
    private String categoryName;

    private String images;

    public Product(Long sellerId, Long categoryId, String name, String brand, String description) {
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.name = name;
        this.brand = brand;
        this.description = description;
    }
}
