package com.xiaoshan.springbootdemo.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址请求DTO
 */
@Data
public class AddressDTO {

    // 地址ID（更新时必填，新增时不填）
    private Long id;

    @NotBlank(message = "收件人姓名不能为空")
    @Size(max = 100, message = "收件人姓名不能超过100个字符")
    private String recipientName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号码")
    private String recipientPhone;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "区县不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 500, message = "详细地址不能超过500个字符")
    private String detailAddress;

    // 地址标签（家/公司/学校），可选
    private String label;

    // 是否默认地址
    private Boolean isDefault = false;
}