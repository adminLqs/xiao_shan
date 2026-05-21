package com.xiaoshan.springbootdemo.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏视图对象
 */
@Data
public class FavoriteVO {
    // 收藏ID
    private Long id;

    // 用户ID
    private Long userId;

    // 商品ID
    private Long productId;

    // 收藏时间
    private LocalDateTime createdAt;

    // ======= 关联商品信息 =======
    // 商品名称
    private String productName;

    // 商品品牌
    private String brand;

    // 商品价格
    private BigDecimal price;

    // 商品原价
    private BigDecimal originalPrice;

    // 库存
    private Integer stock;

    // 商品主图
    private String productImage;
}