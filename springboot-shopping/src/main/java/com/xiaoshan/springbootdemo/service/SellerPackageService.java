package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.SellerPackage;
import com.xiaoshan.springbootdemo.entity.SellerPackageOrder;
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
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PackageProductService packageProductService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 获取所有启用的套餐列表
     */
    public List<SellerPackage> getActivePackages() {
        return sellerPackageMapper.findAllActive();
    }

    /**
     * 根据ID获取套餐详情
     */
    public SellerPackage getPackageById(Long id) {
        return sellerPackageMapper.findById(id)
                .orElseThrow(() -> new RuntimeException("套餐不存在"));
    }

    /**
     * 获取商家的当前套餐
     */
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

    /**
     * 获取商家的套餐使用情况
     */
    public Map<String, Object> getPackageUsage(Long sellerId) {
        SellerPackageOrder currentPackage = getCurrentPackage(sellerId);
        if (currentPackage == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("hasPackage", false);
            result.put("message", "暂未购买套餐");
            return result;
        }

        SellerPackage pkg = getPackageById(currentPackage.getPackageId());
        int currentProductCount = (int) productMapper.countBySellerId(sellerId);

        // 检查商品数量限制（-1表示无限制）
        int productLimit = pkg.getProductLimit();
        boolean isUnlimited = productLimit == -1;
        boolean isExceeded = !isUnlimited && currentProductCount >= productLimit;

        Map<String, Object> result = new HashMap<>();
        result.put("hasPackage", true);

        Map<String, Object> packageInfo = new HashMap<>();
        packageInfo.put("id", currentPackage.getId());
        packageInfo.put("name", currentPackage.getPackageName());
        packageInfo.put("price", currentPackage.getPrice());
        packageInfo.put("startDate", currentPackage.getStartDate());
        packageInfo.put("endDate", currentPackage.getEndDate());
        packageInfo.put("daysRemaining", calculateDaysRemaining(currentPackage.getEndDate()));
        result.put("currentPackage", packageInfo);

        result.put("productLimit", productLimit);
        result.put("isUnlimited", isUnlimited);
        result.put("currentProductCount", currentProductCount);
        result.put("remainingProducts", isUnlimited ? -1 : Math.max(0, productLimit - currentProductCount));
        result.put("isExceeded", isExceeded);

        return result;
    }

    /**
     * 创建套餐购买订单（待支付状态）
     */
    @Transactional
    public SellerPackageOrder createOrder(Long sellerId, Long packageId) {
        // 获取套餐信息
        SellerPackage pkg = getPackageById(packageId);

        // 创建待支付订单
        SellerPackageOrder order = new SellerPackageOrder();
        order.setId(snowflakeIdGenerator.nextId()); // 生成雪花ID
        order.setSellerId(sellerId);
        order.setPackageId(packageId);
        order.setPackageName(pkg.getName());
        order.setPrice(pkg.getPrice());
        order.setStatus("PENDING");
        order.setPaymentMethod("ALIPAY");
        order.setStartDate(null);  // 支付后才设置
        order.setEndDate(null);    // 支付后才设置

        sellerPackageOrderMapper.insert(order);

        log.info("商家 {} 创建套餐订单，套餐ID: {}, 订单ID: {}", sellerId, packageId, order.getId());
        return order;
    }

    /**
     * 处理套餐支付成功
     */
    @Transactional
    public void handlePaymentSuccess(Long orderId, String transactionId) {
        // 查询订单
        SellerPackageOrder order = sellerPackageOrderMapper.findById(orderId)
                .orElseThrow(() -> new RuntimeException("套餐订单不存在"));

        // 幂等性检查
        if (!"PENDING".equals(order.getStatus())) {
            log.info("套餐订单已处理，无需重复操作: orderId={}", orderId);
            return;
        }

        // 获取套餐信息
        SellerPackage pkg = getPackageById(order.getPackageId());

        // 设置生效时间
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(pkg.getDurationDays());

        // 将旧的套餐置为过期
        sellerPackageOrderMapper.expireOldPackages(order.getSellerId());

        // 更新订单状态
        order.setStatus("ACTIVE");
        order.setStartDate(startDate);
        order.setEndDate(endDate);
        order.setTransactionId(transactionId);
        order.setUpdatedAt(LocalDateTime.now());

        sellerPackageOrderMapper.update(order);

        log.info("套餐订单支付成功: orderId={}, sellerId={}, packageName={}", 
                orderId, order.getSellerId(), order.getPackageName());

        // 发送延迟消息
        sendPackageDelayMessages(order.getSellerId(), order.getId(), endDate);

        // 自动恢复被冻结的商品
        packageProductService.handlePackageRenew(order.getSellerId(), order.getPackageId());
    }

    /**
     * 根据订单号获取套餐订单
     */
    public SellerPackageOrder getOrderByOrderNumber(String orderNumber) {
        return sellerPackageOrderMapper.findByOrderNumber(orderNumber).orElse(null);
    }

    /**
     * 根据订单ID获取套餐订单
     */
    public SellerPackageOrder getOrderById(Long orderId) {
        return sellerPackageOrderMapper.findById(orderId)
                .orElseThrow(() -> new RuntimeException("套餐订单不存在"));
    }

    /**
     * 购买套餐（直接购买，跳过待支付状态）
     */
    @Transactional
    public SellerPackageOrder purchasePackage(Long sellerId, Long packageId, String paymentMethod, String transactionId) {
        // 查询套餐信息
        SellerPackage pkg = sellerPackageMapper.findById(packageId)
                .orElseThrow(() -> new RuntimeException("套餐不存在"));

        // 检查是否已有同套餐的 ACTIVE 订单（续费逻辑）
        Optional<SellerPackageOrder> existingActiveOrder = sellerPackageOrderMapper.findActiveSamePackage(sellerId, packageId);
        
        if (existingActiveOrder.isPresent()) {
            // 同套餐续费：延长到期时间
            SellerPackageOrder existingOrder = existingActiveOrder.get();
            int affected = sellerPackageOrderMapper.extendEndDate(existingOrder.getId(), pkg.getDurationDays());
            
            if (affected > 0) {
                // 更新交易记录信息
                existingOrder.setPaymentMethod(paymentMethod);
                existingOrder.setTransactionId(transactionId);
                existingOrder.setUpdatedAt(LocalDateTime.now());
                sellerPackageOrderMapper.update(existingOrder);
                
                log.info("商家 {} 续费套餐 {} 成功，订单ID: {}, 新到期时间: {}", 
                        sellerId, pkg.getName(), existingOrder.getId(), existingOrder.getEndDate());
                return existingOrder;
            } else {
                throw new RuntimeException("续费失败");
            }
        }

        // 购买新套餐（不同套餐或无现有套餐）
        SellerPackageOrder order = new SellerPackageOrder();
        order.setId(snowflakeIdGenerator.nextId()); // 生成雪花ID
        order.setSellerId(sellerId);
        order.setPackageId(packageId);
        order.setPackageName(pkg.getName());
        order.setPrice(pkg.getPrice());
        order.setStatus("ACTIVE");
        order.setPaymentMethod(paymentMethod);
        order.setTransactionId(transactionId);
        
        // 设置生效时间
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(pkg.getDurationDays());
        order.setStartDate(startDate);
        order.setEndDate(endDate);

        // 将旧套餐置为过期
        sellerPackageOrderMapper.expireOldPackages(sellerId);

        // 保存新套餐
        sellerPackageOrderMapper.insert(order);

        log.info("商家 {} 购买套餐 {} 成功，订单ID: {}", sellerId, pkg.getName(), order.getId());

        // 发送延迟消息
        sendPackageDelayMessages(sellerId, order.getId(), endDate);

        return order;
    }

    /**
     * 检查商家是否可以发布商品
     */
    public Map<String, Object> checkPublishPermission(Long sellerId) {
        SellerPackageOrder currentPackage = getCurrentPackage(sellerId);
        Map<String, Object> result = new HashMap<>();

        // 没有套餐
        if (currentPackage == null) {
            result.put("canPublish", false);
            result.put("reason", "NO_PACKAGE");
            result.put("message", "请先购买套餐");
            return result;
        }

        // 检查是否过期
        if (currentPackage.getEndDate().isBefore(LocalDateTime.now())) {
            result.put("canPublish", false);
            result.put("reason", "PACKAGE_EXPIRED");
            result.put("message", "套餐已到期，请续费");
            return result;
        }

        // 检查商品数量限制
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

    /**
     * 获取商家的套餐购买历史
     */
    public List<SellerPackageOrder> getPackageHistory(Long sellerId) {
        return sellerPackageOrderMapper.findAllBySellerId(sellerId);
    }

    /**
     * 定时任务：更新过期套餐状态
     */
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
     * 计算剩余天数
     */
    private long calculateDaysRemaining(LocalDateTime endDate) {
        long days = java.time.Duration.between(LocalDateTime.now(), endDate).toDays();
        return Math.max(0, days);
    }

    /**
     * 发送套餐延迟消息（7天提醒、1天提醒、到期处理）
     */
    private void sendPackageDelayMessages(Long sellerId, Long packageId, LocalDateTime expireTime) {
        // ===== 测试用（上线前改回天级） =====
        long delay7 = 30 * 1000L;      // 30秒后提醒"7天到期"
        long delay1 = 60 * 1000L;      // 60秒后提醒"1天到期"
        long expireMs = 90 * 1000L;    // 90秒后执行到期降级

        // ===== 正式用 =====
        // long expireMs = expireTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
        // long dayMs = 86400000L;
        // if (expireMs > 7 * dayMs) {
        //     delay7 = expireMs - 7 * dayMs;
        // }
        // if (expireMs > dayMs) {
        //     delay1 = expireMs - dayMs;
        // }

        Map<String, Object> remind7Data = Map.of(
            "sellerId", sellerId,
            "type", "remind",
            "days", 7,
            "packageId", packageId
        );
        rabbitTemplate.convertAndSend("package.expire.exchange", "package.expire.remind", remind7Data,
            msg -> {
                msg.getMessageProperties().setHeader("x-delay", (int) delay7);
                msg.getMessageProperties().setContentType("application/json");
                return msg;
            });
        log.info("发送7天到期提醒消息: sellerId={}, packageId={}, delay={}ms", sellerId, packageId, delay7);

        Map<String, Object> remind1Data = Map.of(
            "sellerId", sellerId,
            "type", "remind",
            "days", 1,
            "packageId", packageId
        );
        rabbitTemplate.convertAndSend("package.expire.exchange", "package.expire.remind", remind1Data,
            msg -> {
                msg.getMessageProperties().setHeader("x-delay", (int) delay1);
                msg.getMessageProperties().setContentType("application/json");
                return msg;
            });
        log.info("发送1天到期提醒消息: sellerId={}, packageId={}, delay={}ms", sellerId, packageId, delay1);

        Map<String, Object> expireData = Map.of(
            "sellerId", sellerId,
            "type", "expire",
            "packageId", packageId
        );
        rabbitTemplate.convertAndSend("package.expire.exchange", "package.expire.process", expireData,
            msg -> {
                msg.getMessageProperties().setHeader("x-delay", (int) expireMs);
                msg.getMessageProperties().setContentType("application/json");
                return msg;
            });
        log.info("发送到期处理消息: sellerId={}, packageId={}, delay={}ms", sellerId, packageId, expireMs);
    }
}
