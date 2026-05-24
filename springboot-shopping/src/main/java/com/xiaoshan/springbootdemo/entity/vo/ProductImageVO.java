package com.xiaoshan.springbootdemo.entity.vo;

import lombok.Data;

@Data
public class ProductImageVO {
    private Long id;
    private String image;
    private Integer sortOrder;
}