package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;


@Data // 自动创建Getter Setter
@NoArgsConstructor // 无参构造器
public class UserProfile {

    // 主键id
    private Long id;

    // 用户id
    private Long userId;

    // 头像
    private String avatar;

    // 昵称
    private String nickname;

    // 性别
    private Gender gender = Gender.UNKNOWN;

    // 生日
    private LocalDate birthday;

    // 地区
    private String region;

    // 个人简介
    private String bio;

    // 电话
    private String phone;

    // 邮箱
    private String email;

    // 邮箱验证状态
    private Boolean emailVerified = false;

    public enum Gender {
        MALE, // 男
        FEMALE, // 女
        UNKNOWN // 未知
    }
}
