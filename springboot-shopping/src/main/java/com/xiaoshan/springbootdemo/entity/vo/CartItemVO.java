package com.xiaoshan.springbootdemo.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车项视图对象（包含商品信息）
 */
@Data
public class CartItemVO {

    private Long id;                    // 购物车项ID

    private Long userId;                // 用户ID

    private Long productId;             // 商品ID

    private Integer quantity;           // 数量

    private LocalDateTime addedAt;      // 添加时间

    // 关联商品信息
    private String productName;         // 商品名称

    private String brand;               // 商品品牌

    private BigDecimal price;           // 商品价格

    private BigDecimal originalPrice;   // 商品原价

    private Integer stock;              // 库存

    private String productImage;        // 商品主图
}