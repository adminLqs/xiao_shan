package com.xiaoshan.springbootdemo.entity.vo;

import com.xiaoshan.springbootdemo.entity.Address;
import com.xiaoshan.springbootdemo.entity.LogisticsTrace;
import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderDetailVO {
    // 订单表
    Order order;

    // 订单详情表
    List<OrderItem> orderItems;

    // 地址表
    Address address;

    // 商家名称
    String sellerName;

    // 商家头像
    String sellerAvatar;

    // 物流轨迹（默认节点）
    List<LogisticsTrace> traces;

    // 优惠券名称
    String couponName;

    // 优惠券优惠金额
    BigDecimal couponDiscountAmount;

    public OrderDetailVO(Order order, List<OrderItem> orderItems, Address address) {
        this.order = order;
        this.orderItems = orderItems;
        this.address = address;
    }

    public OrderDetailVO(Order order, List<OrderItem> orderItems, Address address, String sellerName, String sellerAvatar) {
        this.order = order;
        this.orderItems = orderItems;
        this.address = address;
        this.sellerName = sellerName;
        this.sellerAvatar = sellerAvatar;
    }

    public OrderDetailVO(Order order, List<OrderItem> orderItems, Address address, String sellerName, String sellerAvatar, List<LogisticsTrace> traces) {
        this.order = order;
        this.orderItems = orderItems;
        this.address = address;
        this.sellerName = sellerName;
        this.sellerAvatar = sellerAvatar;
        this.traces = traces;
    }
}
