package com.xiaoshan.springbootdemo.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 评论提交DTO
 */
@Data
public class ReviewSubmitDTO {

    // 订单项ID
    @NotNull(message = "订单项ID不能为空")
    private Long orderItemId;

    // 评分（1-5）
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围为1-5")
    @Max(value = 5, message = "评分范围为1-5")
    private Integer rating;

    // 评论内容
    @Size(max = 500, message = "评论内容最多500字")
    private String comment;

    // 是否匿名（true-匿名，false-不匿名）
//    private Boolean isAnonymous;
}