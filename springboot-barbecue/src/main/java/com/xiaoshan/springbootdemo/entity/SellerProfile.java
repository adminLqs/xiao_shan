package com.xiaoshan.springbootdemo.entity;

import lombok.Data;

@Data
public class SellerProfile {

    private Long id;

    // 店铺名称
    private String storeName;

    // 店铺标语
    private String slogan;

    // 店铺详情
    private String storeDetail;

    // 店铺头像
    private String storeAvatar;

    // 联系电话
    private String phone;

    // 店铺地址
    private String address;

    // 营业状态
    private Boolean isOpen = true;

    // 营业时间
    private String business;

    // 带参数构造方法
    public SellerProfile(String storeName, String storeDetail,
                         String phone, String business,
                         String address, String slogan) {
        this.storeName = storeName;
        this.storeDetail = storeDetail;
        this.phone = phone;
        this.business = business;
        this.address = address;
        this.slogan = slogan;
        this.isOpen = true;
    }

    public SellerProfile() {

    }
}
