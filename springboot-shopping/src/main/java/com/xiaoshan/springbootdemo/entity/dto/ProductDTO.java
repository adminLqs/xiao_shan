package com.xiaoshan.springbootdemo.entity.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class ProductDTO {

    @NotNull(message = "商品名称不能为空")
    @Size(min = 1, max = 30, message = "商品名称长度必须在1-50个字符之间")
    private String name;

    @Size(max = 50, message = "商品品牌长度不能超过50个字符")
    private String brand;

    @Size(max = 500, message = "商品描述长度不能超过500个字符")
    private String description;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Integer status;

    // SKU 列表（所有商品必须有 SKU）
    private List<SkuDTO> skus;
}
