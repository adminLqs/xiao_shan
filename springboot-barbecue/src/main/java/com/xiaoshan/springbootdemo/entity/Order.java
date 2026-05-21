package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * 对应数据库表 orders
 *
 * @author xiaoshan
 * @date 2026-05-08
 */
@Data
@NoArgsConstructor
public class Order {

    private Long id;

    private Long userId;

    private String orderNumber;

    private BigDecimal totalAmount;

    /** 配送类型 dinein-到店用餐, takeaway-打包自取, delivery-外卖配送 */
    private String deliveryType;

    private String recipientName = null;

    private String recipientPhone = null;

    private String detailAddress = null;

    private Integer peopleCount = null;

    private String tablePreference = null;

    private String remark = null;

    /** 支付方式 WECHAT-微信支付, ALIPAY-支付宝支付 */
    private String paymentMethod;

    private String transactionId;

    private LocalDateTime paidAt;

    /**
     * 订单状态 PENDING-待支付, PAID-已支付, SHIPPED-已发货, COMPLETED-已完成,
     * CANCELLED-已取消, REFUNDING-退款中, REFUNDED-已退款, REFUNDFAILED-退款失败
     */
    private String status;

    /** 已退款总额 */
    private BigDecimal refundedAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 创建订单时的构造函数
     *
     * @param userId       用户ID
     * @param orderNumber  订单号
     * @param totalAmount  总金额
     * @param deliveryType 配送类型
     * @param remark       订单备注
     */
    public Order(Long userId, String orderNumber, BigDecimal totalAmount,
                 String deliveryType, String remark) {
        this.userId = userId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.deliveryType = deliveryType;
        this.remark = remark;
        this.status = "PENDING";
        this.refundedAmount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }
}