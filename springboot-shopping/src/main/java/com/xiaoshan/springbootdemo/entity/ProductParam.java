package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ProductParam {

    private Long id;

    // 商品ID
    private Long productId;

    // 参数名
    private String paramName;

    // 参数值
    private String paramValue;

    // 排序顺序
    private Integer sortOrder = 0;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ProductParam(Long productId, String paramName, String paramValue) {
        this.productId = productId;
        this.paramName = paramName;
        this.paramValue = paramValue;
    }
}