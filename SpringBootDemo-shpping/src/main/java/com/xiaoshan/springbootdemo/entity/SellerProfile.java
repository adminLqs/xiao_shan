package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 商家资料实体类
 */
@Data
@NoArgsConstructor
public class SellerProfile {

    // 商家资料ID
    private Long id;

    // 用户ID
    private Long userId;

    // 店铺名称
    private String storeName;

    // 店铺Logo
    private String storeAvatar;

    // 店铺横幅
    private String storeBanner;

    // 店铺简介
    private String storeDetail;

    // 营业时间
    private String businessHours;

    // 联系电话
    private String contactPhone;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();
}