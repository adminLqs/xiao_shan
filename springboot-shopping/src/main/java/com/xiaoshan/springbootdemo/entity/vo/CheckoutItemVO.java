package com.xiaoshan.springbootdemo.entity.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 结算页商品信息VO（视图对象）
 * 用于返回给前端展示的商品信息
 */
@Data
public class CheckoutItemVO {

    private Long cartItemId;        // 购物车项ID（从购物车结算时有值，立即购买时为null）

    /** 关联商品信息 */
    private Long productId;          // 商品ID

    private String productName;      // 商品名称

    private String productImage;     // 商品主图URL

    private String brand;            // 商品品牌

    private BigDecimal price;        // 商品价格

    private BigDecimal originalPrice; // 商品原价

    private Integer quantity;        // 购买数量

    private Integer stock;           // 商品库存


}