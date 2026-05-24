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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐商品管理服务
 * 处理套餐降级下架和续费恢复逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageProductService {

    private final ProductMapper productMapper;
    private final SellerPackageOrderMapper sellerPackageOrderMapper;
    private final SellerPackageMapper sellerPackageMapper;
    private final ProductFreezeLogMapper productFreezeLogMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final WebSocketService webSocketService;

    /**
     * 套餐降级时自动下架多余商品
     * @param sellerId 商家ID
     */
    @Transactional
    public void handlePackageExpire(Long sellerId) {
        log.info("处理商家 {} 的套餐到期", sellerId);

        try {
            // 1. 获取当前生效套餐（如果有）
            var currentPackageOpt = sellerPackageOrderMapper.findCurrentActiveBySellerId(sellerId);
            if (currentPackageOpt.isEmpty()) {
                log.info("商家 {} 没有生效套餐，使用基础限制", sellerId);
                return;
            }

            SellerPackageOrder order = currentPackageOpt.get();
            SellerPackage pkg = sellerPackageMapper.findById(order.getPackageId())
                    .orElse(null);

            if (pkg == null) {
                log.warn("商家 {} 的套餐 {} 不存在", sellerId, order.getPackageId());
                return;
            }

            int productLimit = pkg.getProductLimit();
            if (productLimit == -1) {
                log.info("商家 {} 的套餐 {} 无商品数量限制", sellerId, pkg.getName());
                return;
            }

            // 2. 查询当前上架商品数量
            long activeProductCount = productMapper.countActiveBySellerId(sellerId);
            log.info("商家 {} 当前上架商品数: {}, 套餐限制: {}", sellerId, activeProductCount, productLimit);

            // 3. 如果超出限制，下架多余商品
            if (activeProductCount > productLimit) {
                int excessCount = (int) (activeProductCount - productLimit);
                log.info("商家 {} 需要下架 {} 个商品", sellerId, excessCount);

                // 查询需要下架的商品（按创建时间升序，最早创建的先下架）
                List<Long> productIds = productMapper.findActiveProductIdsBySellerIdOrderByCreatedAtAsc(sellerId, excessCount);

                if (!productIds.isEmpty()) {
                    // 下架商品
                    productMapper.batchUpdateStatus(productIds, 0);
                    log.info("商家 {} 已下架 {} 个商品", sellerId, productIds.size());

                    // 记录冻结日志
                    List<ProductFreezeLog> logs = new ArrayList<>();
                    for (Long productId : productIds) {
                        logs.add(new ProductFreezeLog(
                                snowflakeIdGenerator.nextId(),
                                productId,
                                sellerId,
                                "套餐到期自动下架"
                        ));
                    }
                    productFreezeLogMapper.batchInsert(logs);
                    log.info("商家 {} 已记录 {} 条冻结日志", sellerId, logs.size());
                }
            }

        } catch (Exception e) {
            log.error("处理商家 {} 套餐到期失败", sellerId, e);
            throw e;
        }
    }

    /**
     * 续费后自动恢复被冻结的商品
     * @param sellerId 商家ID
     * @param newPackageId 新套餐ID
     */
    @Transactional
    public void handlePackageRenew(Long sellerId, Long newPackageId) {
        log.info("处理商家 {} 的套餐续费，新套餐ID: {}", sellerId, newPackageId);

        try {
            // 1. 获取新套餐信息
            SellerPackage newPackage = sellerPackageMapper.findById(newPackageId)
                    .orElseThrow(() -> new RuntimeException("套餐不存在"));

            int newLimit = newPackage.getProductLimit();
            if (newLimit == -1) {
                log.info("商家 {} 的新套餐 {} 无商品数量限制，恢复所有冻结商品", sellerId, newPackage.getName());
                newLimit = Integer.MAX_VALUE;
            }

            // 2. 查询当前上架商品数量
            long activeProductCount = productMapper.countActiveBySellerId(sellerId);
            log.info("商家 {} 当前上架商品数: {}, 新套餐限制: {}", sellerId, activeProductCount, newLimit);

            // 3. 查询被冻结的商品（按冻结时间倒序，最新冻结的先恢复）
            List<ProductFreezeLog> frozenLogs = productFreezeLogMapper.findUnfrozenBySellerId(sellerId);
            log.info("商家 {} 有 {} 个被冻结的商品", sellerId, frozenLogs.size());

            if (frozenLogs.isEmpty()) {
                log.info("商家 {} 没有被冻结的商品，无需恢复", sellerId);
                return;
            }

            // 4. 计算可恢复数量
            int availableSlots = newLimit - (int) activeProductCount;
            if (availableSlots <= 0) {
                log.info("商家 {} 当前上架商品已达新套餐限制，无法恢复更多商品", sellerId);
                return;
            }

            // 5. 确定需要恢复的商品
            int recoverCount = Math.min(availableSlots, frozenLogs.size());
            List<Long> recoverProductIds = frozenLogs.stream()
                    .limit(recoverCount)
                    .map(ProductFreezeLog::getProductId)
                    .collect(Collectors.toList());

            log.info("商家 {} 将恢复 {} 个商品", sellerId, recoverProductIds.size());

            // 6. 恢复商品
            productMapper.batchUpdateStatus(recoverProductIds, 1);
            log.info("商家 {} 已恢复 {} 个商品", sellerId, recoverProductIds.size());

            // 7. 更新冻结日志（标记解冻时间）
            productFreezeLogMapper.batchUpdateUnfreezeTime(recoverProductIds, LocalDateTime.now());
            log.info("商家 {} 已更新 {} 条解冻记录", sellerId, recoverProductIds.size());

        } catch (Exception e) {
            log.error("处理商家 {} 套餐续费失败", sellerId, e);
            throw e;
        }
    }

    /**
     * 续费/升级后恢复所有被冻结的商品上架
     * @param sellerId 商家ID
     */
    @Transactional
    public void restoreProducts(Long sellerId) {
        log.info("恢复商家 {} 的冻结商品", sellerId);

        try {
            // 查询该商家因套餐到期被冻结的商品
            List<ProductFreezeLog> freezeLogs = productFreezeLogMapper.findUnfrozenBySellerId(sellerId);
            
            if (freezeLogs.isEmpty()) {
                log.info("商家 {} 没有被冻结的商品，无需恢复", sellerId);
                return;
            }

            for (ProductFreezeLog freezeLog : freezeLogs) {
                // 恢复商品上架
                Product product = productMapper.findById(freezeLog.getProductId()).orElse(null);
                if (product != null && product.getStatus() == 0) {
                    product.setStatus(1);
                    productMapper.updateById(product);
                }

                // 更新冻结日志
                freezeLog.setUnfreezeTime(LocalDateTime.now());
                freezeLog.setReason("套餐续费恢复");
                productFreezeLogMapper.updateById(freezeLog);
            }

            // 通知商家
            webSocketService.sendPackageNotification(sellerId, "renew", 0,
                "续费成功，已恢复" + freezeLogs.size() + "件商品上架");

            log.info("续费恢复商品完成: sellerId={}, 恢复数量={}", sellerId, freezeLogs.size());

        } catch (Exception e) {
            log.error("恢复商家 {} 冻结商品失败", sellerId, e);
            throw e;
        }
    }
}