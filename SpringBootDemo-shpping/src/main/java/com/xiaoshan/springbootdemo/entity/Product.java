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

    // 商品价格
    private BigDecimal price;

    // 商品原价
    private BigDecimal originalPrice;

    // 库存
    private Integer stock;

    // 已售数量
    private Integer salesCount = 0;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();;

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 商品状态：0-下架，1-上架
    private Integer status = 1;

    public Product(Long sellerId, Long categoryId, String name, String brand, String description,
                   BigDecimal price, BigDecimal originalPrice, Integer stock) {
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stock;
    }

    // ======== 映射字段 =========
    private String categoryName;

    private String images;
}
