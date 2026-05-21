package com.xiaoshan.springbootdemo.entity.vo;

import com.xiaoshan.springbootdemo.entity.Address;
import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import lombok.Data;

import java.util.List;

@Data
public class OrderDetailVO {
    // 订单表
    Order order;

    // 订单详情表
    List<OrderItem> orderItems;

    // 地址表
    Address address;

    public OrderDetailVO(Order order, List<OrderItem> orderItems, Address address) {
        this.order = order;
        this.orderItems = orderItems;
        this.address = address;
    }
}
