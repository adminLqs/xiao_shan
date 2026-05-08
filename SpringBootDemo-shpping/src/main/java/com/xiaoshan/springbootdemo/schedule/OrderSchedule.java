package com.xiaoshan.springbootdemo.schedule;  // 包路径

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import com.xiaoshan.springbootdemo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSchedule {

    private final OrderMapper orderMapper;      // 订单数据库操作
    private final OrderService orderService;    // 订单业务服务
    private final RedisTemplate<String, Object> redisTemplate;  // Redis 操作模板

    /**
     * 每5分钟执行一次，取消超时未支付的订单
     * 并回滚 Redis 预扣库存
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelExpiredOrders() {
        log.info("========== 开始执行订单超时取消任务 ==========");

        // 查询30分钟前创建的未支付订单
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);
        List<Order> expiredOrders = orderMapper.findExpiredOrders(expireTime);

        log.info("发现 {} 个超时未支付订单", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // 1. 回滚 Redis 库存（先回滚，再取消订单）
                String prestockKey = "order:prestock:" + order.getOrderNumber();
                Map<Object, Object> prestockMap = redisTemplate.opsForHash().entries(prestockKey);

                for (Map.Entry<Object, Object> entry : prestockMap.entrySet()) {
                    Long productId = Long.valueOf(entry.getKey().toString());
                    Integer quantity = Integer.valueOf(entry.getValue().toString());

                    String stockKey = "product:stock:" + productId;
                    redisTemplate.opsForValue().increment(stockKey, quantity);
                    log.info("回滚Redis库存: productId={}, quantity={}", productId, quantity);
                }

                // 2. 删除 Redis 预扣记录
                redisTemplate.delete(prestockKey);

                // 3. 调用订单服务取消订单（复用已有方法）
                //    注意：定时任务没有 userId，需要从 order 对象获取
                orderService.cancelOrder(order.getUserId(), order.getId());

                log.info("超时订单已取消: orderNumber={}", order.getOrderNumber());

            } catch (Exception e) {
                log.error("取消订单失败: orderNumber={}, error={}", order.getOrderNumber(), e.getMessage());
            }
        }

        log.info("订单超时取消任务执行完成，共处理 {} 个订单", expiredOrders.size());
    }
}