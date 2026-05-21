package com.xiaoshan.springbootdemo.entity.dto;

import lombok.Data;

import java.util.Map;

/**
 * 支付结果DTO
 */
@Data
public class PaymentResultDTO {
    private boolean success;           // 是否成功
    private String message;            // 消息
    private String paymentId;          // 订单ID
    private String pageHtml;      // 支付宝网页支付HTML（网页支付模式）
    private Map<String, String> payParams;  // 支付参数（生产模式）
}
