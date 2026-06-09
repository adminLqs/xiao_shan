package com.xiaoshan.springbootdemo.entity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class User {

    private Long id;

    private String account;

    private String password;

    private Boolean status = true;

    private LocalDateTime createdAt;

    private String role = "ROLE_USER";

    private List<com.xiaoshan.springbootdemo.entity.Role> roles;

    public enum UserRoleType {
        ROLE_USER,
        ROLE_SELLER,
        ROLE_ADMIN
    }

    public User(String account,String password){
        this.account = account;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

}
