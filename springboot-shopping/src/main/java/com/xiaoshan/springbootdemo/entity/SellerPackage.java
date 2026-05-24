package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家套餐表实体
 */
@Data
@NoArgsConstructor
public class SellerPackage {

    private Long id;
    private String name;              // 套餐名称
    private String description;       // 套餐描述
    private BigDecimal price;         // 套餐价格
    private Integer durationDays;     // 套餐时长（天）
    private Integer productLimit;     // 商品数量限制（-1表示无限制）
    private String features;          // 套餐功能（JSON格式）
    private Boolean isActive = true; // 是否启用
    private Integer sortOrder = 0;    // 排序
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;   // 更新时间
}
