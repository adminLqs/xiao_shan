package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品冻结记录表实体
 */
@Data
@NoArgsConstructor
public class ProductFreezeLog {

    private Long id;
    private Long productId;           // 商品ID
    private Long sellerId;            // 商家ID
    private LocalDateTime freezeTime; // 冻结时间
    private LocalDateTime unfreezeTime; // 解冻时间
    private String freezeReason;      // 冻结原因
    private String unfreezeReason;    // 解冻原因

    public ProductFreezeLog(Long id, Long productId, Long sellerId, String freezeReason) {
        this.id = id;
        this.productId = productId;
        this.sellerId = sellerId;
        this.freezeTime = LocalDateTime.now();
        this.freezeReason = freezeReason;
    }
}