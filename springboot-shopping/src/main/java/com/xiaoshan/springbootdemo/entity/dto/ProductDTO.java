// 创建商品请求DTO
package com.xiaoshan.springbootdemo.entity.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {

    @NotNull(message = "商品名称不能为空")
    @Size(min = 1, max = 30, message = "商品名称长度必须在1-50个字符之间")
    private String name;

    @Size(max = 50, message = "商品品牌长度不能超过50个字符")
    private String brand;

    @Size(max = 500, message = "商品描述长度不能超过500个字符")
    private String description;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @DecimalMax(value = "999999.99", message = "商品价格不能超过999999.99")
    private BigDecimal price;

    @DecimalMax(value = "999999.99", message = "商品原价不能超过999999.99")
    private BigDecimal originalPrice;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    @Max(value = 999999, message = "库存不能超过999999")
    private Integer stock;

    @NotNull(message = "分类不能为空")
    private Long categoryId;  // 分类ID

    private Integer status; // 商品状态
}