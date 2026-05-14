package com.xiaoshan.springbootdemo.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流信息VO
 */
@Data
public class LogisticsVO {

    // 物流单号
    private String trackingNumber;

    // 物流公司代码
    private String logisticsCode;

    // 物流公司名称
    private String logisticsName;

    // 物流轨迹列表
    private List<TraceVO> traces;

    /**
     * 物流轨迹节点
     */
    @Data
    public static class TraceVO {
        // 轨迹时间
        private LocalDateTime time;

        // 轨迹描述
        private String status;

        // 所在城市
        private String location;
    }
}