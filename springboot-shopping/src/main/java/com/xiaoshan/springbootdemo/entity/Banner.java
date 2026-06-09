package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Banner {

    private Long id;

    private String title;

    private String imageUrl;

    private String linkUrl;

    private Integer sortOrder = 0;

    private Integer status = 1;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String position = "HOME";

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}