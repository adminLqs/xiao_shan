package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 用户与用户商品多对多关系实体表 (收藏表)
@Data
@NoArgsConstructor
public class Favorite {
    // 收藏ID
    private Long id;

    // 收藏用户
    private Long userId;

    // 被收藏商品
    private Long productId;

    // 收藏时间
    private LocalDateTime createdAt = LocalDateTime.now();

}
