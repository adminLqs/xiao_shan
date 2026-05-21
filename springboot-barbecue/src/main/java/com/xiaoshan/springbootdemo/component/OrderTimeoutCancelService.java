package com.xiaoshan.springbootdemo.component;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableScheduling
public class OrderTimeoutCancelService {

    private final OrderMapper orderMapper;

    // 超时时间：15分钟
    private static final int TIMEOUT_MINUTES = 15;

    /**
     * 定时任务
     * cron表达式：秒 分 时 日 月 星期
     * "0 /30 * * * ?" 表示每30分钟执行一次（当秒数为0时） */
    @Scheduled(cron = "0 */30 * * * ?")
    @Transactional
    public void cancelTimeoutOrders() {
        log.info("开始执行订单超时取消任务，时间：{}", LocalDateTime.now());

        try {
            // 查询超时订单（用于日志记录）
            List<Order> timeoutOrders = orderMapper.findTimeoutPendingOrders(TIMEOUT_MINUTES);

            if (timeoutOrders.isEmpty()) {
                log.info("没有发现超时的待支付订单");
                return;
            }

            log.info("发现 {} 个超时未支付的订单", timeoutOrders.size());

            // 打印超时订单详情
            for (Order order : timeoutOrders) {
                log.info("超时订单 - 订单号: {}, 创建时间: {}",
                        order.getOrderNumber(), order.getCreatedAt());
            }

            // 批量取消超时订单
            int cancelledCount = orderMapper.cancelTimeoutOrders(TIMEOUT_MINUTES);

            log.info("订单超时取消任务完成，共取消 {} 个订单", cancelledCount);

        } catch (Exception e) {
            log.error("订单超时取消任务执行失败", e);
        }
    }

    /**
     * 可选：手动触发取消超时订单的接口（供管理员调用）
     */
//    @Transactional
//    public int manualCancelTimeoutOrders() {
//        log.info("手动触发订单超时取消，时间：{}", LocalDateTime.now());
//        int cancelledCount = orderMapper.cancelTimeoutOrders(TIMEOUT_MINUTES);
//        log.info("手动取消完成，共取消 {} 个订单", cancelledCount);
//        return cancelledCount;
//    }
}