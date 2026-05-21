package com.xiaoshan.springbootdemo.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商家资料更新DTO
 */
@Data
public class SellerProfileDTO {

    // 店铺名称
    @NotBlank(message = "店铺名称不能为空")
    private String storeName;

    // 店铺简介
    private String storeDetail;

    // 营业时间
    private String businessHours;

    // 联系电话
    private String contactPhone;
}