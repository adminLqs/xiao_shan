package com.xiaoshan.springbootdemo.entity.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileDTO {

    @Size(max = 16, message = "昵称长度不能超过16个字符")
    private String nickname; // 昵称

    @Pattern(regexp = "^(MALE|FEMALE|UNKNOWN)$", message = "性别必须是男、女或未知")
    private String gender; // 性别

    @Past(message = "生日必须是过去的日期")
    private LocalDate birthday; // 生日

    private String region; // 地区

    private String bio; // 个人简介

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$",message = "手机号格式不正确")
    private String phone; // 手机号

    @Email(message = "邮箱格式不正确")
    private String email;

}
