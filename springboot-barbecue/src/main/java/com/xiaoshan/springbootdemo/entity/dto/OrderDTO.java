package com.xiaoshan.springbootdemo.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建订单DTO
 */
@Data
public class OrderDTO {

    // 订单号（前端生成）
    @NotBlank(message = "订单号不可为空")
    private String orderNumber;

    // 总金额
    @NotNull(message = "总金额不能为空")
    private BigDecimal totalAmount;

    // 配送类型：dinein(到店用餐) / takeaway(打包自取) / delivery(外卖配送)
    @NotBlank(message = "配送类型不能为空")
    private String deliveryType;

    // 收货人姓名
    private String recipientName;

    // 收货人电话
    private String recipientPhone;

    // 详细地址
    private String detailAddress;

    // 用餐人数
    private Integer peopleCount;

    // 桌号偏好
    private String tablePreference;

    // 订单备注
    private String remark;

    // 订单项列表（遍历购物车得到）
    @NotNull(message = "订单项不能为空")
    @Size(min = 1, message = "至少有一个商品")
    private List<OrderItemDTO> orderItems;
}