package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Notification {
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String extraData;
    private Integer isRead;
    private LocalDateTime createdAt;
}