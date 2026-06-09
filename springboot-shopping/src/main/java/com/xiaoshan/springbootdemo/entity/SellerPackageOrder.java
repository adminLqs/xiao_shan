package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家套餐购买记录表实体
 */
@Data
@NoArgsConstructor
public class SellerPackageOrder {

    private Long id;
    private Long sellerId;             // 商家用户ID
    private Long packageId;           // 套餐ID
    private String packageName;       // 套餐名称（冗余）
    private BigDecimal price;          // 购买价格
    private LocalDateTime startDate;  // 开始日期
    private LocalDateTime endDate;    // 结束日期
    private String status = "ACTIVE";  // 状态: PENDING, ACTIVE, PAUSED, EXPIRED, CANCELLED
    private String paymentMethod;      // 支付方式
    private String transactionId;     // 交易单号
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间
    
    private Integer remainingSeconds = 0;  // PAUSED时存剩余秒数，恢复时用它重建endDate
    private Integer priority = 0;          // 优先级（从product_limit映射，越大越高级，-1=99）

    // 状态枚举
    public enum Status {
        PENDING,      // 待支付
        ACTIVE,       // 生效中
        PAUSED,       // 暂停中
        EXPIRED,      // 已到期
        CANCELLED     // 已取消
    }
}