package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.beans.Transient;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
// 收获地址实体
public class Address {

    // 地址ID
    private Long id;

    // 用户id
    private Long userId;

    // 收件人姓名
    private String recipientName;

    // 收件人电话电话
    private String recipientPhone;

    // 省份
    private String province;

    // 城市 - 二级行政区域
    private String city;

    // 区县 - 三级行政区域
    private String district;

    // 详细地址 - 具体的街道
    private String detailAddress;

    // 地址标签 - 学校 家 公司
    private String label;

    // 默认地址
    private Boolean isDefault = false;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

    // 修改时间
    private LocalDateTime updatedAt = LocalDateTime.now();

}
