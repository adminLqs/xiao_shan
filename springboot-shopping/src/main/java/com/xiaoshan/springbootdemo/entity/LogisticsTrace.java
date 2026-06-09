package com.xiaoshan.springbootdemo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsTrace {
    private String time;
    private String description;
    private String status;
    private String location;
}
