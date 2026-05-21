package com.xiaoshan.springbootdemo.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户DTO
 */
@Data
public class UserDTO {

    /**
     * 第三方平台用户唯一标识
     * - 微信登录: 微信 OpenID (通过微信授权获取)
     * - 支付宝登录: 支付宝用户 ID (通过支付宝授权获取)
     * - 说明: 两者不互通，根据实际登录渠道传入对应的标识
     */
    private String openId;

    /**
     * 设备唯一标识
     * - 用于 H5 匿名登录或设备绑定
     * - 优先级: 当 openId 为空时，使用 deviceId 进行设备登录
     */
    private String deviceId;
}