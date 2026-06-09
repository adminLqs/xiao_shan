package com.xiaoshan.springbootdemo.consumer;

import com.xiaoshan.springbootdemo.entity.SellerPackage;
import com.xiaoshan.springbootdemo.entity.SellerPackageOrder;
import com.xiaoshan.springbootdemo.mapper.SellerPackageOrderMapper;
import com.xiaoshan.springbootdemo.service.PackageProductService;
import com.xiaoshan.springbootdemo.service.SellerPackageService;
import com.xiaoshan.springbootdemo.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PackageExpireConsumer {

    private final SellerPackageService sellerPackageService;
    private final SellerPackageOrderMapper sellerPackageOrderMapper;
    private final PackageProductService packageProductService;
    private final WebSocketService webSocketService;

    @RabbitListener(queues = "package.expire.queue")
    public void handlePackageExpire(Map<String, Object> data) {
        log.info("收到套餐到期消息: {}", data);

        Long sellerId = ((Number) data.get("sellerId")).longValue();
        String type = (String) data.get("type");
        Long packageOrderId = ((Number) data.get("packageId")).longValue();
        Long expireTimeMs = data.get("expireTimeMs") != null ? ((Number) data.get("expireTimeMs")).longValue() : null;

        switch (type) {
            case "remind":
                Integer days = (Integer) data.get("days");
                handleRemind(sellerId, days, packageOrderId, expireTimeMs);
                break;
            case "expire":
                handleExpire(sellerId, packageOrderId, expireTimeMs);
                break;
            default:
                log.warn("未知消息类型: {}", type);
        }
    }

    private void handleRemind(Long sellerId, int days, Long packageOrderId, Long expireTimeMs) {
        log.info("发送套餐到期提醒: sellerId={}, days={}, packageOrderId={}, expireTimeMs={}", sellerId, days, packageOrderId, expireTimeMs);

        SellerPackageOrder currentPackage = sellerPackageService.getCurrentPackage(sellerId);
        if (currentPackage == null || !currentPackage.getId().equals(packageOrderId)) {
            log.info("套餐已续费或变更，忽略旧提醒消息: sellerId={}, msgPackageId={}, currentPackageId={}",
                sellerId, packageOrderId, currentPackage != null ? currentPackage.getId() : "null");
            return;
        }

        // 检查到期时间是否匹配（处理同套餐续费的情况）
        long currentExpireMs = currentPackage.getEndDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (expireTimeMs != null && expireTimeMs < currentExpireMs - 5000) {
            log.info("套餐已续费，忽略旧提醒消息: sellerId={}, msgExpireTimeMs={}, currentExpireTimeMs={}",
                sellerId, expireTimeMs, currentExpireMs);
            return;
        }

        String content;
        if (days == 7) {
            content = "您的套餐将在7天后到期，请及时续费。如需降级可能导致商品被冻结";
        } else if (days == 1) {
            content = "您的套餐明天到期，商品即将下架，请及时续费";
        } else {
            content = "您的套餐将在" + days + "天后到期，请及时续费";
        }
        webSocketService.sendPackageNotification(sellerId, "remind", days, content);
    }

    /**
     * 处理套餐到期（完整逻辑）
     * 1. 冻结超配额商品
     * 2. 当前套餐 → EXPIRE
     * 3. 查 PAUSED 套餐，按等级 DESC 排序（最高的先恢复）
     * 4. 恢复最高等级 PAUSED → ACTIVE（使用 remaining_seconds 重建 endDate）
     * 5. 为恢复的套餐发送延迟消息
     * 6. 按恢复套餐的配额解冻商品
     * 7. 如果解冻后仍超配额 → 再次冻结
     */
    @SuppressWarnings("unchecked")
    private void handleExpire(Long sellerId, Long packageOrderId, Long expireTimeMs) {
        log.info("处理套餐到期: sellerId={}, packageOrderId={}, expireTimeMs={}", sellerId, packageOrderId, expireTimeMs);

        Optional<SellerPackageOrder> orderOpt = sellerPackageOrderMapper.findById(packageOrderId);
        if (orderOpt.isEmpty()) {
            log.warn("订单不存在，忽略: packageOrderId={}", packageOrderId);
            return;
        }
        SellerPackageOrder order = orderOpt.get();
        if (!"ACTIVE".equals(order.getStatus())) {
            log.info("套餐订单已变更，忽略旧到期消息: sellerId={}, packageOrderId={}, status={}",
                sellerId, packageOrderId, order.getStatus());
            return;
        }
        
        // 检查到期时间是否匹配（处理同套餐续费的情况），容差5000ms处理精度损失
        if (expireTimeMs != null) {
            long currentExpireMs = order.getEndDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (expireTimeMs < currentExpireMs - 5000) {
                log.info("套餐已续费，忽略旧到期消息: sellerId={}, msgExpireTimeMs={}, currentExpireTimeMs={}",
                    sellerId, expireTimeMs, currentExpireMs);
                return;
            }
        }

        try {
            // 1. 先获取当前套餐配额（在 EXPIRE 前获取）
            SellerPackage currentPkg = sellerPackageService.getPackageById(order.getPackageId());
            int limit = currentPkg.getProductLimit() != null ? currentPkg.getProductLimit() : -1;
            boolean isUnlimited = limit == -1;
            
            // 2. EXPIRE 当前套餐
            order.setStatus("EXPIRED");
            order.setUpdatedAt(LocalDateTime.now());
            sellerPackageOrderMapper.update(order);
            log.info("当前套餐已置为 EXPIRED: sellerId={}, orderId={}, isUnlimited={}", sellerId, order.getId(), isUnlimited);

            // 3. 查 PAUSED：ORDER BY product_limit DESC（通过 JOIN seller_packages）
            List<SellerPackageOrder> pausedOrders = sellerPackageOrderMapper.findPausedBySellerIdOrderByLevel(sellerId);

            // 4. 遍历，取第一个 remaining_seconds > 0 的恢复（查询已过滤）
            for (SellerPackageOrder paused : pausedOrders) {
                SellerPackageOrder resumed = sellerPackageService.resumePausedPackage(sellerId, paused.getId());
                
                if (resumed != null) {
                    // 恢复成功 → break，不继续
                    log.info("降级恢复: sellerId={}, 恢复套餐={}", sellerId, resumed.getPackageName());
                    int remainingDays = (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), resumed.getEndDate()) / 86400;
                    webSocketService.sendPackageNotification(sellerId, "renew", remainingDays,
                            "套餐 " + resumed.getPackageName() + " 已自动恢复，有效期至 " + resumed.getEndDate().toString().substring(0, 16));
                    return;
                }
            }

            // 5. 没有可恢复的套餐 → 全部下架
            Map<String, Object> freezeResult = sellerPackageService.freezeExceedProductsWithDetails(sellerId, 0);
            int frozenCount = (Integer) freezeResult.get("frozenCount");
            List<String> frozenProductNames = (List<String>) freezeResult.get("frozenProductNames");
            log.info("套餐到期，冻结超配额商品: sellerId={}, frozenCount={}", sellerId, frozenCount);

            // 6. 发送到期通知
            String content;
            if (frozenCount > 0) {
                String productList = frozenProductNames.stream().limit(5).collect(Collectors.joining("、"));
                if (frozenCount > 5) {
                    productList += "等" + frozenCount + "个商品";
                } else {
                    productList += "等" + frozenCount + "个商品";
                }
                content = "套餐已到期，" + productList + "已被冻结";
            } else {
                content = "您的套餐已到期";
            }
            webSocketService.sendPackageNotification(sellerId, "expired", 0, content);

            log.info("套餐到期处理完成: sellerId={}, 冻结商品数={}", sellerId, frozenCount);

        } catch (Exception e) {
            log.error("套餐到期处理失败: sellerId={}", sellerId, e);
            throw e;
        }
    }
}