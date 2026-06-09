package com.xiaoshan.springbootdemo.schedule;  // 包路径

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.mapper.OrderItemMapper;
import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import com.xiaoshan.springbootdemo.service.OrderService;
import com.xiaoshan.springbootdemo.service.SellerPackageService;
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
    private final OrderItemMapper orderItemMapper;  // 订单项数据库操作
    private final OrderService orderService;    // 订单业务服务
    private final SellerPackageService sellerPackageService;  // 商家套餐服务
    private final RedisTemplate<String, Object> redisTemplate;  // Redis 操作模板

    /**
     * 订单超时兜底：每天凌晨2点执行
     * 并回滚 Redis 预扣库存
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cancelExpiredOrders() {


        // 查询30分钟前创建的未支付订单
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);
        List<Order> expiredOrders = orderMapper.findExpiredOrders(expireTime);



        for (Order order : expiredOrders) {
            try {
                orderService.cancelOrder(order.getUserId(), order.getId());
                log.info("定时任务取消过期订单: orderNumber={}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("定时任务取消订单失败: orderNumber={}, error={}", order.getOrderNumber(), e.getMessage());
            }
        }


    }

    /**
     * 每天凌晨1点执行，检查并更新过期的商家套餐
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateExpiredPackages() {

        
        try {
            sellerPackageService.updateExpiredPackages();

        } catch (Exception e) {
            log.error("商家套餐过期检查任务执行失败", e);
        }
    }
}