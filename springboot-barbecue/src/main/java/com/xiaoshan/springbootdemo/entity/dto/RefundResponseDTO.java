package com.xiaoshan.springbootdemo.entity.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款响应DTO
 */
@Data
public class RefundResponseDTO {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款状态（NO_REFUND-未退款, PARTIAL-部分退款, FULL-全额退款）
     */
    private String refundStatus;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 支付宝退款交易号
     */
    private String refundTransactionId;

    /**
     * 退款状态文本
     */
    public String getRefundStatusText() {
        switch (refundStatus) {
            case "NO_REFUND": return "未退款";
            case "PARTIAL_REFUND": return "部分退款";
            case "FULL_REFUND": return "全额退款";
            default: return refundStatus;
        }
    }
}