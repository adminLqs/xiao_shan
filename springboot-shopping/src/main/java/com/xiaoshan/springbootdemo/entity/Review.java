package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


// 商品评价实体
@Data
@NoArgsConstructor
public class Review {

    private Long id;

    // 用户ID
    private Long userId;

    // 商品ID
    private Long productId;

    // 订单ID
    private Long orderItemId;

    // 评分
    private Integer rating;

    // 评论
    private String comment;

    // 评论创建时间
    private LocalDateTime createdAt = LocalDateTime.now();;

}
