package com.xiaoshan.springbootdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
// 商品类别
public class Category {

    private Long id;

    // 分类名称
    private String name;

    // 分类状态
    private Boolean isActive = true;

    /** 排序序号，数字越小越靠前 */
    private Integer sortOrder = 0;

    // 父分类 实现多级分类结构（如果为null,表示这是一级分类）
    private Long parentId;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();

}
