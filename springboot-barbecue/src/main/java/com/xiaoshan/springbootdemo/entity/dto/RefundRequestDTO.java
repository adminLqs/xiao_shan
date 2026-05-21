package com.xiaoshan.springbootdemo.entity.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 退款请求DTO
 */
@Data
public class RefundRequestDTO {

    /**
     * 订单号
     */
    private String orderNumber;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String refundReason;
}