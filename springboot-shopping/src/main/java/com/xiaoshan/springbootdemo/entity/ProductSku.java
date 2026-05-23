package com.xiaoshan.springbootdemo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductSku {

    private Long id;
    private Long productId;
    private String skuName;
    private String specInfo;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String skuImage;
    private String skuCode;
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
