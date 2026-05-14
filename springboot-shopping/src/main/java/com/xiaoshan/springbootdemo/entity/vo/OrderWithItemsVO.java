package com.xiaoshan.springbootdemo.entity.vo;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import lombok.Data;

import java.util.List;

/**
 * 订单VO（包含订单信息和订单项列表）
 */
@Data
public class OrderWithItemsVO {

    // 订单信息
    private Order order;

    // 该订单下的所有商品
    private List<OrderItem> orderItems;

    public OrderWithItemsVO(Order order, List<OrderItem> orderItems) {
        this.order = order;
        this.orderItems = orderItems;
    }
}