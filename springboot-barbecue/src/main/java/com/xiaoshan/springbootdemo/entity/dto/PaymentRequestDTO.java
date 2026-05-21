package com.xiaoshan.springbootdemo.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付请求DTO
 */
@Data
public class PaymentRequestDTO {
    private String orderNumber; // 订单号
    private BigDecimal amount; // 总金额
    private String paymentMethod; // WECHAT(微信),ALIPAY(支付宝)
}
