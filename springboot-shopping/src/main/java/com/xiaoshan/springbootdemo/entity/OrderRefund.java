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

    // 商家ID
    private Long sellerId;

    // ========== 退款信息 ==========
    // 退款金额
    private BigDecimal refundAmount;

    // 退款状态: PROCESSING-退款中, SUCCESS-退款成功, FAILED-退款失败
    private RefundStatus refundStatus = RefundStatus.PROCESSING;

    // 退款原因
    private String refundReason;

    // 退款类型: REFUND-退款, AFTER_SALE-售后
    private String refundType = "REFUND";

    // 退款交易编号（支付宝/微信退款单号）
    private String refundTransactionId;

    // 订单项ID
    private Long orderItemId;

    // 退款描述
    private String description;

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

    // ========== 退货信息 ==========
    // 退货方式: PICKUP-上门取件, STORE-到店寄件, SELF-自寄
    private String returnMethod;

    // 退货物流公司
    private String returnLogisticsName;

    // 退货单号
    private String returnTrackingNumber;

    // 退货状态: RETURNING-退货中, RECEIVED-已收货
    private String returnStatus;

    // 退货说明
    private String returnNote;

    // 退货申请时间
    private LocalDateTime returnApplyTime;

    // 商家确认收货时间
    private LocalDateTime returnReceiveTime;

    // ========== 退款状态枚举 ==========
    public enum RefundStatus {
        PROCESSING,      // 退款中（待处理）
        WAITING_RETURN,  // 待退货
        RETURNING,       // 退货中
        SUCCESS,         // 退款成功
        FAILED           // 退款失败
    }
}