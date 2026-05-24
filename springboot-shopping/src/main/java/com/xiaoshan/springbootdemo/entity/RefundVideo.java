package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundVideo {

    private Long id;

    private Long refundId;

    private String videoUrl;

    private String coverUrl;

    private Integer duration;

    private Long size;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

}