package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录表实体
 *
 * @author xiaoshan
 * @date 2026-04-14
 */
@Data
@NoArgsConstructor
public class OrderRefund {

    // 退款ID
    private Long id;

    // ========== 关联订单信息 ==========
    // 订单ID
    private Long orderId;

    // 订单号（冗余，便于查询）
    private String orderNumber;

    // 用户ID
    private Long userId;

    // ========== 退款信息 ==========
    // 退款金额
    private BigDecimal refundAmount;

    // 退款状态: PROCESSING-退款中, SUCCESS-退款成功, FAILED-退款失败
    private RefundStatus refundStatus = RefundStatus.PROCESSING;

    // 退款原因
    private String refundReason;

    // 退款交易编号（支付宝/微信退款单号）
    private String refundTransactionId;

    // ========== 审核信息 ==========
    // 申请时间
    private LocalDateTime applyTime = LocalDateTime.now();

    // 审核时间
    private LocalDateTime reviewTime;

    // 退款完成时间
    private LocalDateTime completeTime;

    // 审核备注
    private String reviewNotes;

    // 审核人ID
    private Long reviewedBy;

    // ========== 退款状态枚举 ==========
    public enum RefundStatus {
        PROCESSING, // 退款中
        SUCCESS,    // 退款成功
        FAILED      // 退款失败
    }
}