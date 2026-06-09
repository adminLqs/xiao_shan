package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OrderCoupon {

    private Long id;
    private Long orderId;
    private Long userCouponId;
    private Long couponId;
    private String couponName;
    private String couponType;
    private BigDecimal discountAmount;
    private LocalDateTime createdAt;
}
