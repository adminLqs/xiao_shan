package com.xiaoshan.springbootdemo.entity.dto;

import com.xiaoshan.springbootdemo.entity.OrderItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建订单请求DTO
 */
@Data
@NoArgsConstructor
public class OrderDTO {

    // 收货地址ID
    @NotNull(message = "请选择收货地址")
    private Long addressId;

    // 订单来源：cart-购物车，product-直接购买
    @NotBlank(message = "订单来源有误")
    private String source;

    // 支付方式
    private String paymentMethod;        // 支付方式：ALIPAY / WECHAT

    // 商品列表
    @NotNull(message = "订单项不能为空")
    @Size(min = 1, message = "至少有一个商品")
    private List<OrderItem> orderItems = new ArrayList<>();  // 订单项列表
}