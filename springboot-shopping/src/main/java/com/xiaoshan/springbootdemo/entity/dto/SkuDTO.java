package com.xiaoshan.springbootdemo.entity.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class SkuDTO {

    // 更新时需要传 ID，新增时不传
    private Long id;

    private String skuName;

    private Map<String, String> specInfo;

    @NotNull(message = "SKU价格不能为空")
    @DecimalMin(value = "0.01", message = "SKU价格必须大于0")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @NotNull(message = "SKU库存不能为空")
    @Min(value = 0, message = "SKU库存不能小于0")
    private Integer stock;

    private String skuImage;

    private Boolean skuImageDeleted;

    private Boolean deleted;

    private String skuCode;

    private Integer sortOrder;
}
