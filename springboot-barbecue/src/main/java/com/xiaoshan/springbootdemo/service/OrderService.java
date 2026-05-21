package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.entity.RefundRecord;
import com.xiaoshan.springbootdemo.entity.dto.OrderDTO;
import com.xiaoshan.springbootdemo.entity.dto.OrderItemDTO;
import com.xiaoshan.springbootdemo.entity.vo.OrderVO;
import com.xiaoshan.springbootdemo.mapper.OrderItemMapper;
import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务层
 * 负责订单的创建、查询、状态更新等业务逻辑
 *
 * @author xiaoshan
 * @date 2026-05-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final WebSocketService webSocketService;
    private final RefundService refundService;

    /**
     * 创建订单
     *
     * @param userId   用户ID
     * @param orderDTO 订单DTO
     * @return 订单实体
     */
    @Transactional
    public Order createOrder(Long userId, OrderDTO orderDTO) {
        // 参数校验
        if (userId == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        String deliveryType = orderDTO.getDeliveryType();
        if (deliveryType == null || deliveryType.trim().isEmpty()) {
            throw new IllegalArgumentException("无效的配送类型");
        }

        // 创建订单对象
        Order order = new Order(userId, orderDTO.getOrderNumber(),
                orderDTO.getTotalAmount(), deliveryType, orderDTO.getRemark());

        // 根据配送类型设置不同字段
        if ("dinein".equals(deliveryType)) {
            order.setPeopleCount(orderDTO.getPeopleCount());
            order.setTablePreference(orderDTO.getTablePreference());
        } else if ("takeaway".equals(deliveryType) || "delivery".equals(deliveryType)) {
            order.setRecipientName(orderDTO.getRecipientName());
            order.setRecipientPhone(orderDTO.getRecipientPhone());
            if ("delivery".equals(deliveryType)) {
                order.setDetailAddress(orderDTO.getDetailAddress());
            }
        }

        // 保存订单
        orderMapper.insertOrder(order);

        // 保存订单项
        if (orderDTO.getOrderItems() != null && !orderDTO.getOrderItems().isEmpty()) {
            List<OrderItem> orderItems = new ArrayList<>();
            for (OrderItemDTO item : orderDTO.getOrderItems()) {
                OrderItem orderItem = new OrderItem(
                        order.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getImage(),
                        item.getQuantity(),
                        item.getPrice()
                );
                orderItems.add(orderItem);
            }
            orderItemMapper.batchInsert(orderItems);
        }

        // 发送新订单通知给商家
        webSocketService.sendNewOrderNotification(order.getOrderNumber(), order.getTotalAmount());

        return order;
    }

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber 订单号
     * @return 订单实体
     */
    public Order findByOrderNumber(String orderNumber) {
        return orderMapper.findByOrderNumber(orderNumber);
    }

    /**
     * 验证待支付订单
     *
     * @param orderNumber    订单号
     * @param expectedAmount 期望金额
     * @return 订单实体，验证失败返回 null
     */
    public Order validatePendingOrder(String orderNumber, BigDecimal expectedAmount) {
        Order order = orderMapper.findByOrderNumber(orderNumber);
        if (order == null) {
            log.warn("订单不存在: {}", orderNumber);
            return null;
        }

        if (!"PENDING".equals(order.getStatus())) {
            log.warn("订单状态异常: {}, 当前状态: {}", orderNumber, order.getStatus());
            return null;
        }

        if (order.getTotalAmount().compareTo(expectedAmount) != 0) {
            log.warn("订单金额不匹配: {}, 期望: {}, 实际: {}", orderNumber, expectedAmount, order.getTotalAmount());
            return null;
        }

        return order;
    }

    /**
     * 更新订单支付成功
     *
     * @param orderNumber   订单号
     * @param paymentMethod 支付方式
     * @param transactionId 交易号
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateOrderPaySuccess(String orderNumber, String paymentMethod, String transactionId) {
        int updated = orderMapper.updateOrderPaySuccess(orderNumber, paymentMethod, transactionId);
        if (updated > 0) {
            log.info("订单支付成功 - 订单号: {}, 支付方式: {}, 交易号: {}", orderNumber, paymentMethod, transactionId);
            return true;
        }
        log.warn("订单支付成功更新失败 - 订单号: {}", orderNumber);
        return false;
    }

    /**
     * 取消订单
     *
     * @param orderNumber 订单号
     * @return 是否取消成功
     */
    @Transactional
    public boolean cancelOrder(String orderNumber) {
        int updated = orderMapper.cancelOrder(orderNumber);
        if (updated > 0) {
            return true;
        }
        return false;
    }

    /**
     * 确认收货
     * <p>
     * 用户确认收货，将订单状态从 SHIPPED 更新为 COMPLETED
     *
     * @param orderNumber 订单号
     * @return 是否成功
     */
    @Transactional
    public boolean confirmReceipt(String orderNumber) {
        // 查询订单
        Order order = orderMapper.findByOrderNumber(orderNumber);
        if (order == null) {
            log.warn("订单不存在: {}", orderNumber);
            return false;
        }

        // 只有已发货状态才能确认收货
        if (!"SHIPPED".equals(order.getStatus())) {
            log.warn("订单状态不是已发货，无法确认收货。订单号: {}, 当前状态: {}", orderNumber, order.getStatus());
            return false;
        }

        // 更新为已完成
        int updated = orderMapper.confirmReceipt(orderNumber);
        if (updated > 0) {
            log.info("确认收货成功 - 订单号: {}", orderNumber);
            return true;
        }
        return false;
    }

    /**
     * 更新订单为退款中
     * <p>
     * 用户申请退款时调用，将订单状态从 PAID 更新为 REFUNDING。
     * 使用乐观锁条件，确保只有已支付的订单才能进入退款流程。
     *
     * @param orderNumber 订单号
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateToRefunding(String orderNumber) {
        int updated = orderMapper.updateToRefunding(orderNumber);
        if (updated > 0) {
            log.info("订单进入退款中 - 订单号: {}", orderNumber);
            return true;
        }
        log.warn("订单退款中更新失败 - 订单号: {}", orderNumber);
        return false;
    }

    /**
     * 更新订单为已退款
     * <p>
     * 退款成功时调用，将订单状态从 REFUNDING 更新为 REFUNDED。
     *
     * @param orderNumber 订单号
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateToRefunded(String orderNumber) {
        int updated = orderMapper.updateToRefunded(orderNumber);
        if (updated > 0) {
            log.info("订单已退款 - 订单号: {}", orderNumber);
            return true;
        }

        return false;
    }

    /**
     * 更新订单为退款失败
     * <p>
     * 退款处理失败时调用，将订单状态从 REFUNDING 更新为 REFUNDFAILED。
     * 使用乐观锁条件，确保只有退款中的订单才能更新。
     *
     * @param orderNumber 订单号
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateToRefundFailed(String orderNumber) {
        int updated = orderMapper.updateToRefundFailed(orderNumber);
        if (updated > 0) {
            log.info("订单退款失败 - 订单号: {}", orderNumber);
            return true;
        }

        return false;
    }

    // ============ 商家 =============

    /**
     * 分页查询商家订单VO列表（含退款信息）
     *
     * @param page     页码，从1开始
     * @param pageSize 每页记录数
     * @param status   订单状态（可选）
     * @return 订单VO列表
     */
    public List<OrderVO> findSellerOrderVOs(int page, int pageSize, String status) {
        int offset = (page - 1) * pageSize;

        List<Order> orders = orderMapper.findAllWithPage(offset, pageSize, status);

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> orderNumbers = orders.stream()
                .map(Order::getOrderNumber)
                .collect(Collectors.toList());

        Map<String, RefundRecord> refundMap = refundService.findRefundMapByOrderNumbers(orderNumbers);

        return orders.stream()
                .map(order -> OrderVO.builder()
                        .order(order)
                        .refundRecord(refundMap.get(order.getOrderNumber()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 统计所有订单总数（商家用）
     *
     * @param status 订单状态（可选）
     * @return 订单总数
     */
    public int countAll(String status) {
        return orderMapper.countAll(status);
    }

    /**
     * 分页查询用户订单列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页记录数
     * @param status 订单状态
     * @return 订单VO列表
     */
    @Transactional
    public List<OrderVO> findUserOrderVOs(Long userId, int page, int pageSize, String status) {
        int offset = (page - 1) * pageSize;

        List<Order> orders = orderMapper.findByUserIdWithPage(userId, offset, pageSize, status);

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> orderNumbers = orders.stream()
                .map(Order::getOrderNumber)
                .collect(Collectors.toList());

        Map<String, RefundRecord> refundMap = refundService.findRefundMapByOrderNumbers(orderNumbers);

        return orders.stream()
                .map(order -> OrderVO.builder()
                        .order(order)
                        .refundRecord(refundMap.get(order.getOrderNumber()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 统计用户订单总数
     *
     * @param userId 用户ID
     * @return 订单总数
     */
    public int countByUserId(Long userId) {
        return orderMapper.countByUserId(userId);
    }

    /**
     * 获取订单详情（包含订单项）
     *
     * @param orderId 订单ID
     * @return 订单项列表
     */
    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        return orderItemMapper.findByOrderId(orderId);
    }

    /**
     * 更新订单状态
     *
     * @param orderNumber 订单号
     * @param status      新状态
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateOrderStatus(String orderNumber, String status) {
        int updated = orderMapper.updateOrderStatus(orderNumber, status);
        if (updated > 0) {
            log.info("订单状态更新成功 - 订单号: {}, 状态: {}", orderNumber, status);
            return true;
        }
        log.warn("订单状态更新失败 - 订单号: {}", orderNumber);
        return false;
    }

    /**
     * 完成订单（核销）
     *
     * @param orderNumber 订单号
     * @return 是否成功
     */
    @Transactional
    public boolean completeOrder(String orderNumber) {
        int updated = orderMapper.completeOrder(orderNumber);
        if (updated > 0) {
            log.info("订单核销成功 - 订单号: {}", orderNumber);
            return true;
        }
        log.warn("订单核销失败 - 订单号: {}", orderNumber);
        return false;
    }

    /**
     * 发货（外卖场景）
     *
     * @param orderNumber 订单号
     * @return 是否成功
     */
    @Transactional
    public boolean shipOrder(String orderNumber) {
        int updated = orderMapper.shipOrder(orderNumber);
        if (updated > 0) {
            log.info("订单发货成功 - 订单号: {}", orderNumber);
            return true;
        }
        log.warn("订单发货失败 - 订单号: {}", orderNumber);
        return false;
    }

    /**
     * 查询超时的待支付订单
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 超时订单列表
     */
    public List<Order> findTimeoutPendingOrders(int timeoutMinutes) {
        return orderMapper.findTimeoutPendingOrders(timeoutMinutes);
    }

    /**
     * 批量取消超时订单
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 取消的订单数量
     */
    @Transactional
    public int cancelTimeoutOrders(int timeoutMinutes) {
        int cancelled = orderMapper.cancelTimeoutOrders(timeoutMinutes);
        if (cancelled > 0) {
            log.info("批量取消超时订单成功，共取消 {} 个订单", cancelled);
        }
        return cancelled;
    }

}