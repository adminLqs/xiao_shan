package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体类
 * 对应数据库表 refund_records
 *
 * @author xiaoshan
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
public class RefundRecord {

    private Long id;

    private String orderNumber;

    private BigDecimal refundAmount;

    private String refundReason;

    private String refundTransactionId;

    /** 退款状态 PROCESSING-处理中, SUCCESS-成功, FAILED-失败 */
    private String status = "PROCESSING";

    private String failReason;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;


    /**
     * 构造函数（创建退款申请时使用）
     *
     * @param orderNumber   订单号
     * @param refundAmount  退款金额
     * @param refundReason  退款原因
     */
    public RefundRecord(String orderNumber, BigDecimal refundAmount, String refundReason) {
        this.orderNumber = orderNumber;
        this.refundAmount = refundAmount;
        this.refundReason = refundReason;
        this.status = "PROCESSING";
        this.createdAt = LocalDateTime.now();
    }
}