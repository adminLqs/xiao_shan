package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表实体
 */
@Data
@NoArgsConstructor
public class Order {

    // 订单ID
    private Long id;

    // ========== 订单基本信息 ==========
    // 订单号
    private String orderNumber;

    // 买家ID
    private Long userId;

    // 订单总金额
    private BigDecimal totalAmount;

    // 订单状态
    private OrderStatus status = OrderStatus.PENDING;

    // 订单来源：cart-购物车，product-直接购买
    private String source;

    // ========== 收货地址信息 ==========
    // 地址ID（可选，优先使用）
    private Long addressId;

    // ========== 支付信息 ==========
    // 支付方式（ALIPAY:支付宝, WECHAT:微信支付）
    private PaymentMethod paymentMethod;

    // 支付交易编号
    private String transactionId;

    // 支付时间
    private LocalDateTime paidAt;

    // ========== 物流信息 ==========
    // 物流单号（必须）
    private String trackingNumber;

    // 物流公司代码（SF、YTO、ZTO、EMS） - 用于API调用
    private String logisticsCode;

    // 物流公司名称（顺丰速运、圆通速递）
    private String logisticsName;

    // 发货时间
    private LocalDateTime shippedAt;

    // 送达时间
    private LocalDateTime deliveredAt;

    // 完成时间
    private LocalDateTime completedAt;

    // ========== 时间戳 ==========
    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 软删除标记
    private Boolean isDeleted = false;


    // ========== 订单状态枚举 ==========
    public enum OrderStatus {
        PENDING,    // 待付款
        PAID,       // 已付款
        PROCESSING, // 处理中
        SHIPPED,    // 已发货
        DELIVERED,  // 已送达
        COMPLETED,  // 已完成
        CANCELLED,  // 已取消
        REFUNDED    // 已退款
    }

    // ========== 支付方式枚举 ==========
    public enum PaymentMethod {
        ALIPAY,     // 支付宝
        WECHAT,     // 微信支付
    }
}