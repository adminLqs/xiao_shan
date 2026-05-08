package com.xiaoshan.springbootdemo.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    // 在子包中定义传输对象
    @NotBlank(message = "用户名不能为空") // 用于验证字符串字段不为 null 且去除首尾空格后长度大于 0
    @Size(min = 6,max = 20,message = "用户名长度必须在6-20位之间")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6,max = 20,message = "密码长度必须在6-20位之间")
    private String password;

    @NotBlank
    @Size(min = 6,max = 20,message = "密码长度必须在6-20位之间")
    private String confirmPassword;
}
