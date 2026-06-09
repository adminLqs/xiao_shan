package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.ProductFreezeLog;
import com.xiaoshan.springbootdemo.entity.SellerPackage;
import com.xiaoshan.springbootdemo.entity.SellerPackageOrder;
import com.xiaoshan.springbootdemo.mapper.ProductFreezeLogMapper;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.SellerPackageMapper;
import com.xiaoshan.springbootdemo.mapper.SellerPackageOrderMapper;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerPackageService {

    private final SellerPackageMapper sellerPackageMapper;
    private final SellerPackageOrderMapper sellerPackageOrderMapper;
    private final ProductMapper productMapper;
    private final ProductFreezeLogMapper productFreezeLogMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PackageProductService packageProductService;
    private final RabbitTemplate rabbitTemplate;
    private final WebSocketService webSocketService;

    public List<SellerPackage> getActivePackages() {
        return sellerPackageMapper.findAllActive();
    }

    public SellerPackage getPackageById(Long id) {
        return sellerPackageMapper.findById(id)
                .orElseThrow(() -> new RuntimeException("套餐不存在"));
    }

    private int getPackageLevel(Integer productLimit) {
        if (productLimit == null || productLimit == -1) {
            return Integer.MAX_VALUE;
        }
        return productLimit;
    }

    public SellerPackageOrder getCurrentPackage(Long sellerId) {
        log.info("查询商家 {} 的当前套餐", sellerId);
        try {
            Optional<SellerPackageOrder> result = sellerPackageOrderMapper.findCurrentActiveBySellerId(sellerId);
            if (result.isPresent()) {
                SellerPackageOrder order = result.get();
                log.info("商家 {} 找到有效套餐: id={}, name={}, startDate={}, endDate={}", 
                        sellerId, order.getId(), order.getPackageName(), order.getStartDate(), order.getEndDate());
                return order;
            } else {
                log.info("商家 {} 没有找到有效套餐", sellerId);
                return null;
            }
        } catch (Exception e) {
            log.error("查询商家 {} 套餐失败", sellerId, e);
            throw e;
        }
    }

    public Map<String, Object> getPackageUsage(Long sellerId) {
        SellerPackageOrder currentPackage = getCurrentPackage(sellerId);
        if (currentPackage == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("hasPackage", false);
            result.put("message", "暂未购买套餐");
            return result;
        }

        SellerPackage pkg = getPackageById(currentPackage.getPackageId());
        int currentProductCount = (int) productMapper.countActiveBySellerId(sellerId);

        int productLimit = pkg.getProductLimit();
        boolean isUnlimited = productLimit == -1;
        boolean isExceeded = !isUnlimited && currentProductCount >= productLimit;

        Map<String, Object> result = new HashMap<>();
        result.put("hasPackage", true);

        Map<String, Object> packageInfo = new HashMap<>();
        packageInfo.put("id", currentPackage.getId());         // 订单ID
        packageInfo.put("packageId", currentPackage.getPackageId());  // 套餐ID
        packageInfo.put("orderId", currentPackage.getId());    // 保留订单ID供其他用途
        packageInfo.put("name", currentPackage.getPackageName());
        packageInfo.put("price", currentPackage.getPrice());
        packageInfo.put("startDate", currentPackage.getStartDate());
        packageInfo.put("endDate", currentPackage.getEndDate());
        
        // 计算剩余时间
        long remainingSeconds;
        if ("PAUSED".equals(currentPackage.getStatus())) {
            remainingSeconds = currentPackage.getRemainingSeconds() != null ? currentPackage.getRemainingSeconds() : 0;
        } else {
            remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), currentPackage.getEndDate());
        }
        remainingSeconds = Math.max(0, remainingSeconds);
        
        packageInfo.put("remainingSeconds", remainingSeconds);
        packageInfo.put("remainingTimeText", formatRemainingTime(remainingSeconds));
        packageInfo.put("daysRemaining", calculateDaysRemaining(currentPackage.getEndDate()));
        packageInfo.put("status", currentPackage.getStatus());
        result.put("currentPackage", packageInfo);

        result.put("productLimit", productLimit);
        result.put("isUnlimited", isUnlimited);
        result.put("currentProductCount", currentProductCount);
        result.put("remainingProducts", isUnlimited ? -1 : Math.max(0, productLimit - currentProductCount));
        result.put("isExceeded", isExceeded);

        return result;
    }

    public SellerPackageOrder findActiveOrPausedSamePackage(Long sellerId, Long packageId) {
        return sellerPackageOrderMapper.findActiveOrPausedSamePackage(sellerId, packageId).orElse(null);
    }

    @Transactional
    public SellerPackageOrder createOrder(Long sellerId, Long packageId) {
        SellerPackage pkg = getPackageById(packageId);

        SellerPackageOrder order = new SellerPackageOrder();
        order.setId(snowflakeIdGenerator.nextId());
        order.setSellerId(sellerId);
        order.setPackageId(packageId);
        order.setPackageName(pkg.getName());
        order.setPrice(pkg.getPrice());
        order.setStatus("PENDING");
        order.setPaymentMethod("ALIPAY");
        order.setStartDate(null);
        order.setEndDate(null);

        sellerPackageOrderMapper.insert(order);

        log.info("商家 {} 创建套餐订单，套餐ID: {}, 订单ID: {}", sellerId, packageId, order.getId());
        return order;
    }

    private void handleSamePackageRenew(SellerPackageOrder targetOrder, Long sellerId, Long packageId, 
            long packageSeconds, SellerPackage newPkg, String transactionId, 
            Optional<SellerPackageOrder> currentActiveOpt) {
        
        LocalDateTime now = LocalDateTime.now();
        
        if ("PAUSED".equals(targetOrder.getStatus())) {
            // PAUSED 套餐续费：只叠加时间，不激活，不改变当前ACTIVE，不发消息
            targetOrder.setRemainingSeconds(targetOrder.getRemainingSeconds() + (int) packageSeconds);
            targetOrder.setTransactionId(transactionId);
            targetOrder.setPaymentMethod("ALIPAY");
            targetOrder.setUpdatedAt(now);
            sellerPackageOrderMapper.update(targetOrder);

            log.info("商家 {} 同套餐PAUSED续费：订单 {} 叠加时间，remainingSeconds={}, transactionId={}", 
                    sellerId, targetOrder.getId(), targetOrder.getRemainingSeconds(), transactionId);
        } else if ("ACTIVE".equals(targetOrder.getStatus())) {
            // ACTIVE 套餐续费：延长有效期
            LocalDateTime newEndDate = targetOrder.getEndDate().plusSeconds(packageSeconds);
            targetOrder.setEndDate(newEndDate);
            targetOrder.setTransactionId(transactionId);
            targetOrder.setPaymentMethod("ALIPAY");
            targetOrder.setUpdatedAt(now);
            sellerPackageOrderMapper.update(targetOrder);

            log.info("商家 {} 同套餐ACTIVE续费：订单 {} 延长至 {}, transactionId={}", 
                    sellerId, targetOrder.getId(), newEndDate, transactionId);

            sendPackageDelayMessages(sellerId, targetOrder.getId(), newEndDate);
            webSocketService.sendPackageNotification(sellerId, "renew", newPkg.getDurationDays(),
                    "套餐续费成功，有效期至 " + newEndDate.toString());
        }
    }

    @Transactional
    public void handlePaymentSuccess(Long orderId, String transactionId) {
        SellerPackageOrder order = sellerPackageOrderMapper.findById(orderId)
                .orElseThrow(() -> new RuntimeException("套餐订单不存在"));

        Long sellerId = order.getSellerId();
        Long packageId = order.getPackageId();
        SellerPackage newPkg = getPackageById(packageId);
        
        long packageSeconds = newPkg.getDurationDays() * 24L * 3600L;

        Optional<SellerPackageOrder> currentActiveOpt = sellerPackageOrderMapper
                .findCurrentActiveBySellerId(sellerId);

        // 同套餐续费场景：订单本身是 ACTIVE 或 PAUSED（不创建新订单，直接使用原订单）
        if ("ACTIVE".equals(order.getStatus()) || "PAUSED".equals(order.getStatus())) {
            handleSamePackageRenew(order, sellerId, packageId, packageSeconds, newPkg, transactionId, currentActiveOpt);
            return;
        }

        // 新订单购买场景：订单是 PENDING
        if (!"PENDING".equals(order.getStatus())) {
            log.info("套餐订单已处理，无需重复操作: orderId={}", orderId);
            return;
        }

        // 同套餐续费场景：新 PENDING 订单，查到同套餐 ACTIVE/PAUSED → 延长旧订单 + 取消新订单
        Optional<SellerPackageOrder> samePackageOrderOpt = sellerPackageOrderMapper
                .findActiveSamePackage(sellerId, packageId);

        if (samePackageOrderOpt.isPresent()) {
            SellerPackageOrder samePackageOrder = samePackageOrderOpt.get();
            handleSamePackageRenew(samePackageOrder, sellerId, packageId, packageSeconds, newPkg, transactionId, currentActiveOpt);
            
            // 取消当前新创建的 PENDING 订单
            order.setStatus("CANCELLED");
            order.setUpdatedAt(LocalDateTime.now());
            sellerPackageOrderMapper.update(order);
            log.info("商家 {} 同套餐续费，旧订单 {} 延长，新订单 {} 已取消", 
                    sellerId, samePackageOrder.getId(), orderId);
            return;
        }

        if (currentActiveOpt.isPresent()) {
            SellerPackageOrder currentActive = currentActiveOpt.get();
            SellerPackage currentPkg = getPackageById(currentActive.getPackageId());
            int currentLimit = currentPkg.getProductLimit() != null ? currentPkg.getProductLimit() : -1;
            int newLimit = newPkg.getProductLimit() != null ? newPkg.getProductLimit() : -1;

            // 优先级比较：product_limit 越大优先级越高，-1 = 最高优先级（Integer.MAX_VALUE）
            int currentPriority = currentLimit == -1 ? Integer.MAX_VALUE : currentLimit;
            int newPriorityVal = newLimit == -1 ? Integer.MAX_VALUE : newLimit;

            LocalDateTime now = LocalDateTime.now();

            if (newPriorityVal > currentPriority) {
                // 新 > 旧：升级，旧套餐 PAUSED，新套餐 ACTIVE
                long remainingSeconds = ChronoUnit.SECONDS.between(now, currentActive.getEndDate());
                if (remainingSeconds > 0) {
                    currentActive.setRemainingSeconds((int) remainingSeconds);
                }
                currentActive.setStatus("PAUSED");
                currentActive.setUpdatedAt(now);
                sellerPackageOrderMapper.update(currentActive);
                log.info("商家 {} 升级：旧套餐 {} PAUSED，剩余秒数={}", sellerId, currentActive.getPackageName(), currentActive.getRemainingSeconds());

                LocalDateTime endDate = now.plusSeconds(packageSeconds);
                
                SellerPackageOrder newOrder = new SellerPackageOrder();
                newOrder.setId(snowflakeIdGenerator.nextId());
                newOrder.setSellerId(sellerId);
                newOrder.setPackageId(packageId);
                newOrder.setPackageName(newPkg.getName());
                newOrder.setPrice(newPkg.getPrice());
                newOrder.setPaymentMethod("ALIPAY");
                newOrder.setTransactionId(transactionId);
                newOrder.setStatus("ACTIVE");
                newOrder.setStartDate(now);
                newOrder.setEndDate(endDate);
                newOrder.setCreatedAt(now);
                newOrder.setUpdatedAt(now);
                sellerPackageOrderMapper.insert(newOrder);

                log.info("商家 {} 升级套餐：新套餐 {} ACTIVE，endDate={}", sellerId, newPkg.getName(), endDate);

                sendPackageDelayMessages(sellerId, newOrder.getId(), endDate);
                webSocketService.sendPackageNotification(sellerId, "renew", newPkg.getDurationDays(),
                        "套餐升级成功，有效期至 " + endDate.toString());
                
                packageProductService.handlePackageRenew(sellerId, packageId);
            } else if (newPriorityVal < currentPriority) {
                // 新 < 旧：降级，新套餐 PAUSED，旧套餐保持 ACTIVE
                LocalDateTime endDate = now.plusSeconds(packageSeconds);
                
                SellerPackageOrder newOrder = new SellerPackageOrder();
                newOrder.setId(snowflakeIdGenerator.nextId());
                newOrder.setSellerId(sellerId);
                newOrder.setPackageId(packageId);
                newOrder.setPackageName(newPkg.getName());
                newOrder.setPrice(newPkg.getPrice());
                newOrder.setPaymentMethod("ALIPAY");
                newOrder.setTransactionId(transactionId);
                newOrder.setStatus("PAUSED");
                newOrder.setStartDate(now);
                newOrder.setEndDate(endDate);
                newOrder.setRemainingSeconds((int) packageSeconds);
                newOrder.setCreatedAt(now);
                newOrder.setUpdatedAt(now);
                sellerPackageOrderMapper.insert(newOrder);

                log.info("商家 {} 购买降级套餐 {}，PAUSED状态，remainingSeconds={}", 
                        sellerId, newPkg.getName(), newOrder.getRemainingSeconds());

                freezeExceedProducts(sellerId, newPkg.getProductLimit());
            } else {
                // 同级：旧套餐 EXPIRE，新套餐 ACTIVE（继承 endDate）
                currentActive.setStatus("EXPIRED");
                currentActive.setUpdatedAt(now);
                sellerPackageOrderMapper.update(currentActive);
                log.info("商家 {} 同级套餐更换：旧套餐 {} EXPIRED", sellerId, currentActive.getPackageName());

                // 继承旧套餐的 endDate
                LocalDateTime endDate = currentActive.getEndDate().plusSeconds(packageSeconds);
                
                SellerPackageOrder newOrder = new SellerPackageOrder();
                newOrder.setId(snowflakeIdGenerator.nextId());
                newOrder.setSellerId(sellerId);
                newOrder.setPackageId(packageId);
                newOrder.setPackageName(newPkg.getName());
                newOrder.setPrice(newPkg.getPrice());
                newOrder.setPaymentMethod("ALIPAY");
                newOrder.setTransactionId(transactionId);
                newOrder.setStatus("ACTIVE");
                newOrder.setStartDate(now);
                newOrder.setEndDate(endDate);
                newOrder.setCreatedAt(now);
                newOrder.setUpdatedAt(now);
                sellerPackageOrderMapper.insert(newOrder);

                log.info("商家 {} 同级套餐更换：新套餐 {} ACTIVE，继承endDate并延长至 {}", sellerId, newPkg.getName(), endDate);

                sendPackageDelayMessages(sellerId, newOrder.getId(), endDate);
                webSocketService.sendPackageNotification(sellerId, "renew", newPkg.getDurationDays(),
                        "套餐更换成功，有效期至 " + endDate.toString());
                
                packageProductService.handlePackageRenew(sellerId, packageId);
            }

            order.setStatus("CANCELLED");
            order.setUpdatedAt(LocalDateTime.now());
            sellerPackageOrderMapper.update(order);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusSeconds(packageSeconds);
        
        SellerPackageOrder newOrder = new SellerPackageOrder();
        newOrder.setId(snowflakeIdGenerator.nextId());
        newOrder.setSellerId(sellerId);
        newOrder.setPackageId(packageId);
        newOrder.setPackageName(newPkg.getName());
        newOrder.setPrice(newPkg.getPrice());
        newOrder.setPaymentMethod("ALIPAY");
        newOrder.setTransactionId(transactionId);
        newOrder.setStatus("ACTIVE");
        newOrder.setStartDate(now);
        newOrder.setEndDate(endDate);
        newOrder.setCreatedAt(now);
        newOrder.setUpdatedAt(now);
        sellerPackageOrderMapper.insert(newOrder);

        log.info("商家 {} 首次购买套餐 {}，订单 {} ACTIVE，endDate={}", 
                sellerId, newPkg.getName(), newOrder.getId(), endDate);

        sendPackageDelayMessages(sellerId, newOrder.getId(), endDate);
        webSocketService.sendPackageNotification(sellerId, "renew", newPkg.getDurationDays(),
                "套餐购买成功，有效期至 " + endDate.toString());

        packageProductService.handlePackageRenew(sellerId, packageId);
        freezeExceedProducts(sellerId, newPkg.getProductLimit());

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        sellerPackageOrderMapper.update(order);
    }

    @Transactional
    public SellerPackageOrder purchasePackage(Long sellerId, Long packageId, String paymentMethod, String transactionId) {
        SellerPackage newPkg = getPackageById(packageId);
        
        long packageSeconds = newPkg.getDurationDays() * 24L * 3600L;

        Optional<SellerPackageOrder> samePackageOrderOpt = sellerPackageOrderMapper
                .findActiveSamePackage(sellerId, packageId);

        Optional<SellerPackageOrder> currentActiveOpt = sellerPackageOrderMapper
                .findCurrentActiveBySellerId(sellerId);

        if (samePackageOrderOpt.isPresent()) {
            SellerPackageOrder samePackageOrder = samePackageOrderOpt.get();
            
            LocalDateTime now = LocalDateTime.now();
            
            if ("PAUSED".equals(samePackageOrder.getStatus())) {
                // PAUSED 套餐续费：只叠加时间，不激活，不改变当前ACTIVE，不发消息
                samePackageOrder.setRemainingSeconds(samePackageOrder.getRemainingSeconds() + (int) packageSeconds);
                samePackageOrder.setTransactionId(transactionId);
                samePackageOrder.setPaymentMethod(paymentMethod);
                samePackageOrder.setUpdatedAt(now);
                sellerPackageOrderMapper.update(samePackageOrder);

                log.info("商家 {} 同套餐PAUSED续费：订单 {} 叠加时间，remainingSeconds={}, transactionId={}", 
                        sellerId, samePackageOrder.getId(), samePackageOrder.getRemainingSeconds(), transactionId);
                
                return samePackageOrder;
            } else if ("ACTIVE".equals(samePackageOrder.getStatus())) {
                LocalDateTime newEndDate = samePackageOrder.getEndDate().plusSeconds(packageSeconds);
                samePackageOrder.setEndDate(newEndDate);
                samePackageOrder.setTransactionId(transactionId);
                samePackageOrder.setPaymentMethod(paymentMethod);
                samePackageOrder.setUpdatedAt(now);
                sellerPackageOrderMapper.update(samePackageOrder);

                log.info("商家 {} 同套餐ACTIVE续费：订单 {} 延长至 {}, transactionId={}", 
                        sellerId, samePackageOrder.getId(), newEndDate, transactionId);

                sendPackageDelayMessages(sellerId, samePackageOrder.getId(), newEndDate);
                webSocketService.sendPackageNotification(sellerId, "renew", newPkg.getDurationDays(),
                        "套餐续费成功，有效期至 " + newEndDate.toString());
                return samePackageOrder;
            }
        }

        if (currentActiveOpt.isPresent()) {
            SellerPackageOrder currentActive = currentActiveOpt.get();
            SellerPackage currentPkg = getPackageById(currentActive.getPackageId());
            int currentLimit = currentPkg.getProductLimit() != null ? currentPkg.getProductLimit() : -1;
            int newLimit = newPkg.getProductLimit() != null ? newPkg.getProductLimit() : -1;

            // 优先级比较：product_limit 越大优先级越高，-1 = 最高优先级（Integer.MAX_VALUE）
            int currentPriority = currentLimit == -1 ? Integer.MAX_VALUE : currentLimit;
            int newPriorityVal = newLimit == -1 ? Integer.MAX_VALUE : newLimit;

            LocalDateTime now = LocalDateTime.now();

            if (newPriorityVal > currentPriority) {
                // 新 > 旧：升级，旧套餐 PAUSED，新套餐 ACTIVE
                long remainingSeconds = ChronoUnit.SECONDS.between(now, currentActive.getEndDate());
                if (remainingSeconds > 0) {
                    currentActive.setRemainingSeconds((int) remainingSeconds);
                }
                currentActive.setStatus("PAUSED");
                currentActive.setUpdatedAt(now);
                sellerPackageOrderMapper.update(currentActive);
                log.info("商家 {} 升级：旧套餐 {} PAUSED，剩余秒数={}", sellerId, currentActive.getPackageName(), currentActive.getRemainingSeconds());

                LocalDateTime endDate = now.plusSeconds(packageSeconds);
                
                SellerPackageOrder newOrder = new SellerPackageOrder();
                newOrder.setId(snowflakeIdGenerator.nextId());
                newOrder.setSellerId(sellerId);
                newOrder.setPackageId(packageId);
                newOrder.setPackageName(newPkg.getName());
                newOrder.setPrice(newPkg.getPrice());
                newOrder.setPaymentMethod(paymentMethod);
                newOrder.setTransactionId(transactionId);
                newOrder.setStatus("ACTIVE");
                newOrder.setStartDate(now);
                newOrder.setEndDate(endDate);
                newOrder.setCreatedAt(now);
                newOrder.setUpdatedAt(now);
                sellerPackageOrderMapper.insert(newOrder);

                log.info("商家 {} 升级套餐：新套餐 {} ACTIVE，endDate={}", sellerId, newPkg.getName(), endDate);

                sendPackageDelayMessages(sellerId, newOrder.getId(), endDate);
                webSocketService.sendPackageNotification(sellerId, "renew", newPkg.getDurationDays(),
                        "套餐升级成功，有效期至 " + endDate.toString());
                
                packageProductService.handlePackageRenew(sellerId, packageId);
                return newOrder;
            } else if (newPriorityVal < currentPriority) {
                // 新 < 旧：降级，新套餐 PAUSED，旧套餐保持 ACTIVE
                LocalDateTime endDate = now.plusSeconds(packageSeconds);
                
                SellerPackageOrder newOrder = new SellerPackageOrder();
                newOrder.setId(snowflakeIdGenerator.nextId());
                newOrder.setSellerId(sellerId);
                newOrder.setPackageId(packageId);
                newOrder.setPackageName(newPkg.getName());
                newOrder.setPrice(newPkg.getPrice());
                newOrder.setPaymentMethod(paymentMethod);
                newOrder.setTransactionId(transactionId);
                newOrder.setStatus("PAUSED");
                newOrder.setStartDate(now);
                newOrder.setEndDate(endDate);
                newOrder.setRemainingSeconds((int) packageSeconds);
                newOrder.setCreatedAt(now);
                newOrder.setUpdatedAt(now);
                sellerPackageOrderMapper.insert(newOrder);

                log.info("商家 {} 购买降级套餐 {}，PAUSED状态，remainingSeconds={}", 
                        sellerId, newPkg.getName(), newOrder.getRemainingSeconds());

                freezeExceedProducts(sellerId, newPkg.getProductLimit());
                return newOrder;
            } else {
                // 同级：旧套餐 EXPIRE，新套餐 ACTIVE（继承 endDate）
                currentActive.setStatus("EXPIRED");
                currentActive.setUpdatedAt(now);
                sellerPackageOrderMapper.update(currentActive);
                log.info("商家 {} 同级套餐更换：旧套餐 {} EXPIRED", sellerId, currentActive.getPackageName());

                // 继承旧套餐的 endDate
                LocalDateTime endDate = currentActive.getEndDate().plusSeconds(packageSeconds);
                
                SellerPackageOrder newOrder = new SellerPackageOrder();
                newOrder.setId(snowflakeIdGenerator.nextId());
                newOrder.setSellerId(sellerId);
                newOrder.setPackageId(packageId);
                newOrder.setPackageName(newPkg.getName());
                newOrder.setPrice(newPkg.getPrice());
                newOrder.setPaymentMethod(paymentMethod);
                newOrder.setTransactionId(transactionId);
                newOrder.setStatus("ACTIVE");
                newOrder.setStartDate(now);
                newOrder.setEndDate(endDate);
                newOrder.setCreatedAt(now);
                newOrder.setUpdatedAt(now);
                sellerPackageOrderMapper.insert(newOrder);

                log.info("商家 {} 同级套餐更换：新套餐 {} ACTIVE，继承endDate并延长至 {}", sellerId, newPkg.getName(), endDate);

                sendPackageDelayMessages(sellerId, newOrder.getId(), endDate);
                webSocketService.sendPackageNotification(sellerId, "renew", newPkg.getDurationDays(),
                        "套餐更换成功，有效期至 " + endDate.toString());
                
                packageProductService.handlePackageRenew(sellerId, packageId);
                return newOrder;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusSeconds(packageSeconds);
        
        SellerPackageOrder newOrder = new SellerPackageOrder();
        newOrder.setId(snowflakeIdGenerator.nextId());
        newOrder.setSellerId(sellerId);
        newOrder.setPackageId(packageId);
        newOrder.setPackageName(newPkg.getName());
        newOrder.setPrice(newPkg.getPrice());
        newOrder.setPaymentMethod(paymentMethod);
        newOrder.setTransactionId(transactionId);
        newOrder.setStatus("ACTIVE");
        newOrder.setStartDate(now);
        newOrder.setEndDate(endDate);
        newOrder.setCreatedAt(now);
        newOrder.setUpdatedAt(now);
        sellerPackageOrderMapper.insert(newOrder);

        log.info("商家 {} 首次购买套餐 {}，订单 {} ACTIVE，endDate={}", 
                sellerId, newPkg.getName(), newOrder.getId(), endDate);

        sendPackageDelayMessages(sellerId, newOrder.getId(), endDate);
        webSocketService.sendPackageNotification(sellerId, "renew", newPkg.getDurationDays(),
                "套餐购买成功，有效期至 " + endDate.toString());
        
        packageProductService.handlePackageRenew(sellerId, packageId);
        freezeExceedProducts(sellerId, newPkg.getProductLimit());

        return newOrder;
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, int durationDays) {
        return startDate.plusDays(durationDays);
    }

    public SellerPackageOrder getOrderByOrderNumber(String orderNumber) {
        return sellerPackageOrderMapper.findByOrderNumber(orderNumber).orElse(null);
    }

    public SellerPackageOrder getOrderById(Long orderId) {
        return sellerPackageOrderMapper.findById(orderId)
                .orElseThrow(() -> new RuntimeException("套餐订单不存在"));
    }

    public Map<String, Object> checkPublishPermission(Long sellerId) {
        SellerPackageOrder currentPackage = getCurrentPackage(sellerId);
        Map<String, Object> result = new HashMap<>();

        if (currentPackage == null) {
            result.put("canPublish", false);
            result.put("reason", "NO_PACKAGE");
            result.put("message", "请先购买套餐");
            return result;
        }

        if (currentPackage.getEndDate().isBefore(LocalDateTime.now())) {
            result.put("canPublish", false);
            result.put("reason", "PACKAGE_EXPIRED");
            result.put("message", "套餐已到期，请续费");
            return result;
        }

        SellerPackage pkg = getPackageById(currentPackage.getPackageId());
        int productLimit = pkg.getProductLimit();

        if (productLimit != -1) {
            long currentCount = productMapper.countBySellerId(sellerId);
            if (currentCount >= productLimit) {
                result.put("canPublish", false);
                result.put("reason", "PRODUCT_LIMIT_EXCEEDED");
                result.put("message", "商品数量已达到上限（" + productLimit + "），请升级套餐");
                result.put("productLimit", productLimit);
                result.put("currentCount", currentCount);
                return result;
            }
        }

        result.put("canPublish", true);
        result.put("reason", "OK");
        result.put("message", "可以发布商品");
        return result;
    }

    public List<SellerPackageOrder> getPackageHistory(Long sellerId) {
        return sellerPackageOrderMapper.findAllBySellerId(sellerId);
    }

    @Transactional
    public void updateExpiredPackages() {
        List<SellerPackageOrder> expiredOrders = sellerPackageOrderMapper.findExpiredOrders();
        if (!expiredOrders.isEmpty()) {
            List<Long> ids = expiredOrders.stream().map(SellerPackageOrder::getId).toList();
            sellerPackageOrderMapper.batchUpdateExpired(ids);
            log.info("更新了 {} 个过期套餐", ids.size());
        }
    }

    /**
     * 套餐到期时冻结超出配额的商品（从 currentPackage 获取配额）
     * 场景：套餐到期时，currentPackage 还是 ACTIVE 状态
     */
    @Transactional
    public int freezeExceedProducts(Long sellerId) {
        SellerPackageOrder currentPackage = getCurrentPackage(sellerId);
        
        int productLimit = -1;
        if (currentPackage != null) {
            SellerPackage pkg = sellerPackageMapper.findById(currentPackage.getPackageId()).orElse(null);
            if (pkg != null) {
                productLimit = pkg.getProductLimit() != null ? pkg.getProductLimit() : -1;
            }
        }
        
        // 查询所有上架商品ID（按创建时间升序，最早发布的在前）
        List<Long> activeProductIds = productMapper.findAllActiveProductIdsBySellerIdOrderByCreatedAtAsc(sellerId);
        
        // 无上架商品，直接返回
        if (activeProductIds.isEmpty()) {
            return 0;
        }
        
        List<Long> freezeProductIds = new java.util.ArrayList<>();
        
        // 无套餐（currentPackage == null）→ 全部下架
        if (currentPackage == null) {
            freezeProductIds = activeProductIds;
        }
        // productLimit < 0 表示旗舰版套餐（无限制）→ 不下架
        else if (productLimit < 0) {
            return 0;
        } else if (activeProductIds.size() > productLimit) {
            // 超出配额：只下架超出部分（最早发布的先下架）
            int excessCount = activeProductIds.size() - productLimit;
            freezeProductIds = activeProductIds.subList(0, excessCount);
        }
        
        if (freezeProductIds.isEmpty()) {
            return 0;
        }
        
        // 批量下架商品
        productMapper.batchUpdateStatus(freezeProductIds, 0);
        
        log.info("商家 {} 套餐到期，已下架 {} 个商品（配额：{}，原上架：{}）", 
                sellerId, freezeProductIds.size(), productLimit, activeProductIds.size());
        return freezeProductIds.size();
    }

    /**
     * 降级时冻结超出配额的商品（直接传入新套餐配额）
     * 场景：购买降级套餐时，旧套餐已 EXPIRED，需传入新套餐配额
     */
    @Transactional
    public int freezeExceedProducts(Long sellerId, int productLimit) {
        // 查询所有上架商品ID（按创建时间升序，最早发布的在前）
        List<Long> activeProductIds = productMapper.findAllActiveProductIdsBySellerIdOrderByCreatedAtAsc(sellerId);
        
        // 无上架商品，直接返回
        if (activeProductIds.isEmpty()) {
            return 0;
        }
        
        List<Long> freezeProductIds = new java.util.ArrayList<>();
        
        // productLimit = 0 → 无套餐，全部下架
        if (productLimit == 0) {
            freezeProductIds = activeProductIds;
        }
        // productLimit < 0 表示旗舰版套餐（无限制）
        else if (productLimit < 0) {
            // 无限制，不下架任何商品
            return 0;
        } else if (activeProductIds.size() > productLimit) {
            // 超出配额：只下架超出部分（最早发布的先下架）
            int excessCount = activeProductIds.size() - productLimit;
            freezeProductIds = activeProductIds.subList(0, excessCount);
        }
        
        if (freezeProductIds.isEmpty()) {
            return 0;
        }
        
        // 批量下架商品
        productMapper.batchUpdateStatus(freezeProductIds, 0);
        
        log.info("商家 {} 套餐降级，已下架 {} 个商品（配额：{}，原上架：{}）", 
                sellerId, freezeProductIds.size(), productLimit, activeProductIds.size());
        return freezeProductIds.size();
    }

    /**
     * 套餐到期时冻结超出配额的商品（从 currentPackage 获取配额，返回详情）
     * 场景：套餐到期时，currentPackage 还是 ACTIVE 状态
     */
    @Transactional
    public Map<String, Object> freezeExceedProductsWithDetails(Long sellerId) {
        SellerPackageOrder currentPackage = getCurrentPackage(sellerId);
        
        int productLimit = -1;
        if (currentPackage != null) {
            SellerPackage pkg = sellerPackageMapper.findById(currentPackage.getPackageId()).orElse(null);
            if (pkg != null) {
                productLimit = pkg.getProductLimit() != null ? pkg.getProductLimit() : -1;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        List<String> frozenProductNames = new java.util.ArrayList<>();
        
        // 查询所有上架商品ID（按创建时间升序，最早发布的在前）
        List<Long> activeProductIds = productMapper.findAllActiveProductIdsBySellerIdOrderByCreatedAtAsc(sellerId);
        
        // 无上架商品，直接返回
        if (activeProductIds.isEmpty()) {
            result.put("frozenCount", 0);
            result.put("frozenProductNames", frozenProductNames);
            return result;
        }
        
        List<Long> freezeProductIds = new java.util.ArrayList<>();
        
        // 无套餐（currentPackage == null）→ 全部下架
        if (currentPackage == null) {
            freezeProductIds = activeProductIds;
        }
        // productLimit < 0 表示旗舰版套餐（无限制）→ 不下架
        else if (productLimit < 0) {
            result.put("frozenCount", 0);
            result.put("frozenProductNames", frozenProductNames);
            return result;
        } else if (activeProductIds.size() > productLimit) {
            // 超出配额：只下架超出部分（最早发布的先下架）
            int excessCount = activeProductIds.size() - productLimit;
            freezeProductIds = activeProductIds.subList(0, excessCount);
        }
        
        if (freezeProductIds.isEmpty()) {
            result.put("frozenCount", 0);
            result.put("frozenProductNames", frozenProductNames);
            return result;
        }
        
        // 根据ID查询商品详情（用于返回商品名）
        List<Product> freezeProducts = productMapper.findByIds(freezeProductIds);
        for (Product product : freezeProducts) {
            frozenProductNames.add(product.getName());
        }
        
        // 批量下架商品
        productMapper.batchUpdateStatus(freezeProductIds, 0);
        
        result.put("frozenCount", freezeProductIds.size());
        result.put("frozenProductNames", frozenProductNames);
        return result;
    }

    /**
     * 降级时冻结超出配额的商品（直接传入新套餐配额，返回详情）
     * 场景：购买降级套餐时，旧套餐已 EXPIRED，需传入新套餐配额
     */
    @Transactional
    public Map<String, Object> freezeExceedProductsWithDetails(Long sellerId, int productLimit) {
        Map<String, Object> result = new HashMap<>();
        List<String> frozenProductNames = new java.util.ArrayList<>();
        
        // 查询所有上架商品ID（按创建时间升序，最早发布的在前）
        List<Long> activeProductIds = productMapper.findAllActiveProductIdsBySellerIdOrderByCreatedAtAsc(sellerId);
        
        // 无上架商品，直接返回
        if (activeProductIds.isEmpty()) {
            result.put("frozenCount", 0);
            result.put("frozenProductNames", frozenProductNames);
            return result;
        }
        
        List<Long> freezeProductIds = new java.util.ArrayList<>();
        
        // productLimit = 0 → 无套餐，全部下架
        if (productLimit == 0) {
            freezeProductIds = activeProductIds;
        }
        // productLimit < 0 表示旗舰版套餐（无限制）→ 不下架
        else if (productLimit < 0) {
            result.put("frozenCount", 0);
            result.put("frozenProductNames", frozenProductNames);
            return result;
        } else if (activeProductIds.size() > productLimit) {
            // 超出配额：只下架超出部分（最早发布的先下架）
            int excessCount = activeProductIds.size() - productLimit;
            freezeProductIds = activeProductIds.subList(0, excessCount);
        }
        
        if (freezeProductIds.isEmpty()) {
            result.put("frozenCount", 0);
            result.put("frozenProductNames", frozenProductNames);
            return result;
        }
        
        // 根据ID查询商品详情（用于返回商品名）
        List<Product> freezeProducts = productMapper.findByIds(freezeProductIds);
        for (Product product : freezeProducts) {
            frozenProductNames.add(product.getName());
        }
        
        // 批量下架商品
        productMapper.batchUpdateStatus(freezeProductIds, 0);
        
        result.put("frozenCount", freezeProductIds.size());
        result.put("frozenProductNames", frozenProductNames);
        return result;
    }

    private long calculateDaysRemaining(LocalDateTime endDate) {
        long days = java.time.Duration.between(LocalDateTime.now(), endDate).toDays();
        return Math.max(0, days);
    }

    /**
     * 格式化剩余时间显示
     * @param totalSeconds 总秒数
     * @return 格式化的时间文本，如 "3天5小时"、"2小时30分钟"、"5分钟"
     */
    private String formatRemainingTime(long totalSeconds) {
        if (totalSeconds <= 0) return "已到期";
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (days > 0) return days + "天" + hours + "小时";
        if (hours > 0) return hours + "小时" + minutes + "分钟";
        return minutes + "分钟";
    }

    public void sendPackageDelayMessages(Long sellerId, Long packageId, LocalDateTime expireTime) {
        long now = System.currentTimeMillis();
        long expireMs = expireTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long totalMs = expireMs - now;
        
        if (totalMs <= 0) {
            log.warn("套餐已过期，不发送延迟消息: sellerId={}, packageId={}", sellerId, packageId);
            return;
        }
        
        log.info("发送延迟消息: sellerId={}, packageId={}, expireTime={}, totalMs={}", sellerId, packageId, expireTime, totalMs);
        
        // 生产环境时间常量
        long sevenDaysRemaining = 7 * 86400000L;
        long oneDayRemaining = 86400000L;
        
        // 到期处理（始终发送）- 添加 expireTimeMs 用于幂等性检查
        sendDelayedMessage("package.expire.process", Map.of(
            "sellerId", sellerId, "type", "expire", "packageId", packageId, "expireTimeMs", expireMs
        ), (int) totalMs);
        log.info("发送到期处理: sellerId={}, packageId={}, delay={}ms, expireTimeMs={}", sellerId, packageId, totalMs, expireMs);
        
        // 7天提醒
        long delay7 = totalMs - sevenDaysRemaining;
        if (delay7 > 0) {
            sendDelayedMessage("package.expire.remind", Map.of(
                "sellerId", sellerId, "type", "remind", "days", 7, "packageId", packageId, "expireTimeMs", expireMs
            ), (int) delay7);
            log.info("发送7天提醒: sellerId={}, packageId={}, delay={}ms", sellerId, packageId, delay7);
        } else {
            log.info("总时长不足，跳过7天提醒: sellerId={}, totalMs={}, sevenDaysRemaining={}", sellerId, totalMs, sevenDaysRemaining);
        }
        
        // 1天提醒
        long delay1 = totalMs - oneDayRemaining;
        if (delay1 > 0) {
            sendDelayedMessage("package.expire.remind", Map.of(
                "sellerId", sellerId, "type", "remind", "days", 1, "packageId", packageId, "expireTimeMs", expireMs
            ), (int) delay1);
            log.info("发送1天提醒: sellerId={}, packageId={}, delay={}ms", sellerId, packageId, delay1);
        } else {
            log.info("总时长不足，跳过1天提醒: sellerId={}, totalMs={}, oneDayRemaining={}", sellerId, totalMs, oneDayRemaining);
        }
    }

    private void sendDelayedMessage(String routingKey, Map<String, Object> data, int delayMs) {
        String exchange = "package.expire.exchange";
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, data,
                msg -> {
                    msg.getMessageProperties().setHeader("x-delay", delayMs);
                    msg.getMessageProperties().setContentType("application/json");
                    return msg;
                });
        } catch (Exception e) {
            log.error("消息发送失败: exchange={}, routingKey={}", exchange, routingKey, e);
        }
    }

    /**
     * 恢复暂停的套餐（使用 remainingSeconds 重建 endDate）
     */
    @Transactional
    public SellerPackageOrder resumePausedPackage(Long sellerId, Long pausedOrderId) {
        SellerPackageOrder pausedOrder = sellerPackageOrderMapper.findById(pausedOrderId)
                .orElseThrow(() -> new RuntimeException("暂停的套餐订单不存在"));

        if (!"PAUSED".equals(pausedOrder.getStatus())) {
            throw new RuntimeException("套餐订单状态不是暂停状态");
        }

        int remainingSeconds = pausedOrder.getRemainingSeconds() != null ? pausedOrder.getRemainingSeconds() : 0;
        if (remainingSeconds <= 0) {
            pausedOrder.setStatus("EXPIRED");
            pausedOrder.setUpdatedAt(LocalDateTime.now());
            sellerPackageOrderMapper.update(pausedOrder);
            log.info("暂停套餐已过期，置为 EXPIRED: sellerId={}, orderId={}", sellerId, pausedOrderId);
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        pausedOrder.setStatus("ACTIVE");
        pausedOrder.setStartDate(now);
        pausedOrder.setEndDate(now.plusSeconds(remainingSeconds));
        pausedOrder.setUpdatedAt(now);
        sellerPackageOrderMapper.update(pausedOrder);

        log.info("恢复暂停套餐: sellerId={}, packageName={}, remainingSeconds={}, endDate={}", 
                sellerId, pausedOrder.getPackageName(), remainingSeconds, pausedOrder.getEndDate());

        sendPackageDelayMessages(sellerId, pausedOrder.getId(), pausedOrder.getEndDate());
        packageProductService.handlePackageRenew(sellerId, pausedOrder.getPackageId());
        
        // 恢复暂停套餐时，获取恢复套餐的配额
        SellerPackage pkg = sellerPackageMapper.findById(pausedOrder.getPackageId()).orElse(null);
        if (pkg != null) {
            freezeExceedProducts(sellerId, pkg.getProductLimit());
        }

        return pausedOrder;
    }
}