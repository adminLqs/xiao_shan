package com.xiaoshan.springbootdemo.entity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
// 商品图片实体
public class ProductImage {

    private Long id;

    // 图片路径
    private String image;

    // 图片排序
    private Integer sortOrder = 0;

    // 商品和商品图片的外键
    private Long productId;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

}
