package com.xiaoshan.springbootdemo.entity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 用户
@Data
@NoArgsConstructor // 无参构造器 1.首先通过无参构造器创建对象实例 2.通过反射各个字段的值
public class User {

    private Long id = generateSnowflakeId();

    // 用户专用(设备标识符)
    private String deviceId;

    // 手机号（用于找回账号）
    private String phone;

    // 创建时间
    private LocalDateTime createdAt;

    public User(String deviceId) {
        this.deviceId = deviceId;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 简易雪花算法生成唯一 Long ID
     */
    private static long generateSnowflakeId() {
        long timestamp = System.currentTimeMillis();
        long random = (long) (Math.random() * 10000);
        return timestamp * 10000 + random;
    }

}
