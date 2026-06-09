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

    private Long skuId;              // SKU ID（指定规格购买时有值）

    private String skuName;          // SKU 规格名称（如"500g"、"红色/M码"）

    private String productName;      // 商品名称

    private String productImage;     // 商品主图URL

    private String brand;            // 商品品牌

    private BigDecimal price;        // 商品价格

    private BigDecimal originalPrice; // 商品原价

    private Integer quantity;        // 购买数量

    private Integer stock;           // 商品库存

    private Boolean isFreeShipping;  // 是否包邮

    private Integer productStatus;   // 商品状态（0-下架，1-上架，2-已删除）

    /** 卖家信息（用于按卖家拆单） */
    private Long sellerId;           // 卖家ID
    private String sellerName;       // 卖家名称
    private String sellerAvatar;     // 卖家头像

}