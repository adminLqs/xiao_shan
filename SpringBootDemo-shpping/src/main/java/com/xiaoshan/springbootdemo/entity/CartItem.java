package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
// 购物车实体项
public class CartItem {

    private Long id;

    // 用户id
    private Long userId;

    // 商品id
    private Long productId;

    // 数量
    private Integer quantity;

    // 添加时间
    private LocalDateTime addedAt = LocalDateTime.now();

 }
