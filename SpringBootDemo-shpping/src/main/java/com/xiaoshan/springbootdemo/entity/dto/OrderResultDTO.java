package com.xiaoshan.springbootdemo.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付结果DTO
 */
@Data
public class OrderResultDTO {
    private Long orderId;          // 订单ID
    private String orderNumber;    // 订单号
    private BigDecimal totalAmount;// 订单金额
    private String pageHtml;      // 支付宝网页支付HTML（网页支付模式）
    private Map<String, String> payParams;  // 支付参数（生产模式）
}
