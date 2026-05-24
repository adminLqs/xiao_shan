package com.xiaoshan.springbootdemo.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {

    // ======== 基础信息（来自 products 表） ========
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
    private Integer viewCount;
    private Integer salesCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ======== SKU 汇总字段 ========
    private BigDecimal price;        // SKU最低价格
    private BigDecimal originalPrice; // SKU最高原价
    private Integer stock;           // SKU 库存总和

    // ======== 关联查询字段 ========
    private String categoryName;
    private Integer commentCount;
    private List<ProductImageVO> productImages;  // 商品图片列表
    private List<ProductParamVO> params;         // 商品参数列表
    private List<com.xiaoshan.springbootdemo.entity.ProductSku> skus;  // SKU 列表
}