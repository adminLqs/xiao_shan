package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserCoupon {

    private Long id;
    private Long userId;
    private Long couponId;
    private String status;
    private Long orderId;
    private LocalDateTime receivedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expireAt;

    private Coupon coupon;
}
