package com.xiaoshan.springbootdemo.entity.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 6,max = 20,message = "用户名长度必须在6-20位之间")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6,max = 20,message = "密码长度必须在6-20位之间")
    private String password;

}
