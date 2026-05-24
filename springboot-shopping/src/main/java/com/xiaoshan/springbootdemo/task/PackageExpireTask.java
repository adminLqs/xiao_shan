package com.xiaoshan.springbootdemo.task;

import com.xiaoshan.springbootdemo.mapper.SellerPackageOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 套餐到期检测定时任务（兜底机制，主要由 RabbitMQ 延迟消息处理）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PackageExpireTask {

    private final SellerPackageOrderMapper sellerPackageOrderMapper;

    /**
     * 每天凌晨3点执行，作为 RabbitMQ 延迟消息的兜底检查
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void checkExpiredPackages() {
        log.info("开始检测到期套餐（兜底检查）...");

        try {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

            List<Long> expiredSellerIds = sellerPackageOrderMapper.findExpiredSellerIds(startOfDay, endOfDay);

            if (expiredSellerIds.isEmpty()) {
                log.info("今日无到期套餐");
                return;
            }

            log.info("发现 {} 个商家的套餐今日到期（由 RabbitMQ 延迟消息处理）", expiredSellerIds.size());

        } catch (Exception e) {
            log.error("检测到期套餐任务失败", e);
        }
    }
}