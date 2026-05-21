package com.xiaoshan.springbootdemo.entity.vo;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.entity.RefundRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单详情 VO
 * 包含订单基本信息、商品列表、退款记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailVO {

    /** 订单基本信息 */
    private Order order;

    /** 订单商品列表 */
    private List<OrderItem> orderItems;

    /** 退款记录列表 */
    private RefundRecord refundRecords;
}