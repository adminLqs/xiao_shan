package com.xiaoshan.springbootdemo.entity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 用户
@Data
@NoArgsConstructor // 无参构造器 1.首先通过无参构造器创建对象实例 2.通过反射各个字段的值
public class User {

    private Long id;

    private String account;

    private String password;

    private Boolean status = true; // 默认值为 true

    private LocalDateTime createdAt; // 自动映射到 created_at 列

    private Role role = Role.ROLE_USER;

    public enum Role {
        ROLE_USER,    // 普通用户
        ROLE_SELLER,  // 商家
        ROLE_ADMIN    // 管理员
    }

    public User(String account,String password){
        this.account = account;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

}
