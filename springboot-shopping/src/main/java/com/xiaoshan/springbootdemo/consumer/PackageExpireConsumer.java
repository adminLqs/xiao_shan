package com.xiaoshan.springbootdemo.consumer;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.ProductFreezeLog;
import com.xiaoshan.springbootdemo.entity.SellerPackageOrder;
import com.xiaoshan.springbootdemo.mapper.ProductFreezeLogMapper;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.SellerPackageOrderMapper;
import com.xiaoshan.springbootdemo.service.WebSocketService;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PackageExpireConsumer {

    private final SellerPackageOrderMapper packageOrderMapper;
    private final ProductMapper productMapper;
    private final ProductFreezeLogMapper productFreezeLogMapper;
    private final WebSocketService webSocketService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @RabbitListener(queues = "package.expire.queue")
    @Transactional
    public void handlePackageExpire(Map<String, Object> data) {
        log.info("收到套餐到期消息: {}", data);

        Long sellerId = ((Number) data.get("sellerId")).longValue();
        String type = (String) data.get("type");
        Long packageId = ((Number) data.get("packageId")).longValue();

        // 1. 幂等校验：当前生效套餐是否匹配
        SellerPackageOrder current = packageOrderMapper.findCurrentActiveBySellerId(sellerId).orElse(null);
        if (current == null || !current.getId().equals(packageId)) {
            log.info("套餐已变更，忽略旧消息: sellerId={}, msgPackageId={}, currentPackageId={}",
                    sellerId, packageId, current != null ? current.getId() : null);
            return;
        }

        switch (type) {
            case "remind":
                Integer days = (Integer) data.get("days");
                handleRemind(sellerId, days);
                break;
            case "expire":
                handleExpire(sellerId, packageId, current);
                break;
            default:
                log.warn("未知消息类型: {}", type);
        }
    }

    private void handleRemind(Long sellerId, int days) {
        log.info("发送套餐到期提醒: sellerId={}, days={}", sellerId, days);
        String content;
        if (days == 7) {
            content = "您的套餐将在7天后到期，请及时续费";
        } else if (days == 1) {
            content = "您的套餐明天到期，商品即将下架";
        } else {
            content = "您的套餐将在" + days + "天后到期";
        }
        webSocketService.sendPackageNotification(sellerId, "remind", days, content);
    }

    private void handleExpire(Long sellerId, Long packageId, SellerPackageOrder current) {
        log.info("处理套餐到期: sellerId={}, packageId={}", sellerId, packageId);

        try {
            // 2. 再次确认套餐已到期（防止时间误差）
            if (current.getEndDate() != null && current.getEndDate().isAfter(LocalDateTime.now())) {
                log.info("套餐尚未到期，跳过: sellerId={}", sellerId);
                return;
            }

            // 3. 批量下架该商家所有上架商品
            List<Product> products = productMapper.findBySellerIdAndStatus(sellerId, 1);
            log.info("商家 {} 有 {} 个上架商品需要下架", sellerId, products.size());

            if (!products.isEmpty()) {
                List<Long> productIds = new ArrayList<>();
                List<ProductFreezeLog> freezeLogs = new ArrayList<>();

                for (Product product : products) {
                    product.setStatus(0);
                    productMapper.updateById(product);
                    productIds.add(product.getId());

                    // 记录冻结日志
                    ProductFreezeLog freezeLog = new ProductFreezeLog();
                    freezeLog.setId(snowflakeIdGenerator.nextId());
                    freezeLog.setSellerId(sellerId);
                    freezeLog.setProductId(product.getId());
                    freezeLog.setReason("套餐到期自动下架");
                    freezeLog.setFreezeTime(LocalDateTime.now());
                    freezeLogs.add(freezeLog);
                }

                productFreezeLogMapper.batchInsert(freezeLogs);
                log.info("商家 {} 已记录 {} 条冻结日志", sellerId, freezeLogs.size());
            }

            // 4. 更新套餐状态为已到期
            packageOrderMapper.expireOrder(packageId);
            log.info("商家 {} 套餐状态已更新为 EXPIRED", sellerId);

            // 5. 通知商家
            String content = "您的套餐已到期，已自动下架" + products.size() + "件商品";
            webSocketService.sendPackageNotification(sellerId, "expired", 0, content);

            log.info("套餐到期降级完成: sellerId={}, 下架商品数={}", sellerId, products.size());

        } catch (Exception e) {
            log.error("套餐到期处理失败: sellerId={}, packageId={}", sellerId, packageId, e);
            throw e;
        }
    }
}