package com.xiaoshan.springbootdemo.entity.dto;

import lombok.Data;

/**
 * 商家信息数据传输对象
 *
 * @author xiaoshan
 * @date 2026-05-08
 */
@Data
public class SellerProfileDTO {

    /**
     * 店铺名称
     */
    private String storeName;

    /**
     * 店铺标语
     */
    private String slogan;

    /**
     * 店铺详情
     */
    private String storeDetail;

    /**
     * 店铺头像
     */
    private String storeAvatar;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 店铺地址
     */
    private String address;

    /**
     * 营业状态
     */
    private Boolean isOpen;

    /**
     * 营业时间
     */
    private String business;
}