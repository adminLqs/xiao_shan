package com.xiaoshan.springbootdemo.entity.dto;

import lombok.Data;

@Data
public class RefundHandleDTO {
    private String orderNumber;
    private String result;      // SUCCESS / FAIL
    private String failReason;
}
