package com.xiaoshan.springbootdemo.entity.vo;

import lombok.Data;

@Data
public class ProductParamVO {
    private Long id;
    private String paramName;
    private String paramValue;
    private Integer sortOrder;
}