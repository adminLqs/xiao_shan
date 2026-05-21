package com.xiaoshan.springbootdemo.entity.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单项DTO
 */
@Data
public class OrderItemDTO {

    private Long productId;     // 商品ID

    private String productName; // 商品名称

    private String image; // 商品图片

    private Integer quantity;   // 购买数量

    private BigDecimal price;   // 下单时价格
}