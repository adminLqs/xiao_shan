package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// 商品评价视频实体
@Data
@NoArgsConstructor
public class ReviewVideo {

    private Long id;

    // 评论ID
    private Long reviewId;

    // 视频URL
    private String videoUrl;

    // 视频封面图URL
    private String coverUrl;

    // 视频时长（秒）
    private Integer duration;

    // 文件大小（字节）
    private Long size;

    // 排序
    private Integer sortOrder;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

}
