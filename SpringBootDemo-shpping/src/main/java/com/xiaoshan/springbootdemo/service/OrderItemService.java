package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.mapper.OrderItemMapper;
import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemMapper orderItemMapper;  // 订单项数据库操作
    private final OrderMapper orderMapper; // 订单表数据库操作


    /**
     * 根据ID查询订单项（带权限校验）
     *
     * @param userId 用户ID
     * @param orderItemId 订单项ID
     * @return 订单项
     */
    public OrderItem getOrderItemById(Long userId, Long orderItemId) {
        // 查询订单项
        OrderItem orderItem = orderItemMapper.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        // 查询订单
        Order order = orderMapper.findById(orderItem.getOrderId()).orElse(null);
        if (order == null) {
            return null;
        }

        // 校验订单所属用户
        if (!order.getUserId().equals(userId)) {
            return null;
        }

        return orderItem;
    }

    /**
     * 根据订单ID查询订单项列表
     *
     * @param orderId 订单ID
     * @return 订单项列表
     */
    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        return orderItemMapper.findByOrderId(orderId);
    }


}
