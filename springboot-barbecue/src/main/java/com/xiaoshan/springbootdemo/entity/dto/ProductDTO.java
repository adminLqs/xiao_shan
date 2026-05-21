// 创建商品请求DTO
package com.xiaoshan.springbootdemo.entity.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductDTO {

    @NotNull(message = "商品名称不能为空")
    @Size(min = 1, max = 30, message = "商品名称长度必须在1-50个字符之间")
    private String name;

    @Size(max = 50, message = "商品描述长度不能超过100个字符")
    private String description;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @DecimalMax(value = "999999.99", message = "商品价格不能超过999999.99")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "商品原价必须大于0")
    @DecimalMax(value = "999999.99", message = "商品原价不能超过999999.99")
    private BigDecimal originalPrice;

    @Size(max = 50, message = "分类长度不能超过50个字符")
    private String category;

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();

}