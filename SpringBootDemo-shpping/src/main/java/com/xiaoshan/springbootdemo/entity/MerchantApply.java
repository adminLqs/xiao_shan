package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MerchantApply {

    // 主键ID
    private Long id;

    // 申请人用户ID
    private Long userId;

    // 联系人姓名
    private String contactName;

    // 联系电话
    private String contactPhone;

    // 联系邮箱
    private String contactEmail;

    // 店铺名称
    private String storeName;

    // 店铺详细描述
    private String storeDetail;

    // 经营类型
    private String businessType;

    // 主营类目
    private String mainCategory;

    // 营业执照文件路径
    private String businessLicense;

    // 身份证正面照片路径
    private String idCardFront;

    // 身份证反面照片路径
    private String idCardBack;

    // 申请状态
    private String status;

    // 审核备注
    private String reviewNotes;

    // 审核人用户ID
    private Long reviewedBy;

    private LocalDateTime createdAt  = LocalDateTime.now();   // 创建时间
    private LocalDateTime updatedAt  = LocalDateTime.now();   // 更新时间
    private LocalDateTime reviewedAt;      // 审核时间

    // 状态枚举
    public enum Status {
        PENDING,   // 待审核
        APPROVED,  // 已通过
        REJECTED   // 已拒绝
    }

    // 经营类型枚举
    public enum BusinessType {
        INDIVIDUAL,    // 个体工商户
        COMPANY        // 企业商家
    }

    // 主营类目枚举
    public enum MainCategory {
        ELECTRONICS,      // 手机数码
        COMPUTER,         // 电脑办公
        FOOD,             // 食品生鲜
        BEAUTY,           // 美妆个护
        CLOTHING,         // 服饰鞋包
        HOME,             // 家居家装
        SPORTS            // 运动户外
    }
}