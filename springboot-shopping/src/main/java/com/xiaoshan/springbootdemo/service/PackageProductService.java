package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.SellerPackage;
import com.xiaoshan.springbootdemo.entity.SellerPackageOrder;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.SellerPackageMapper;
import com.xiaoshan.springbootdemo.mapper.SellerPackageOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                }
            }

        } catch (Exception e) {
            log.error("处理商家 {} 套餐到期失败", sellerId, e);
            throw e;
        }
    }

    /**
     * 续费后自动恢复被下架的商品
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

            int productLimit = newPackage.getProductLimit();

            // 2. 查询当前上架商品数量
            long activeCount = productMapper.countActiveBySellerId(sellerId);

            // 3. 查询已下架的商品（status=0，按创建时间降序，最新发布的优先上架）
            List<Long> inactiveProductIds = productMapper.findInactiveProductIdsBySellerIdOrderByCreatedAtDesc(sellerId);
            log.info("商家 {} 有 {} 个已下架的商品", sellerId, inactiveProductIds.size());

            if (inactiveProductIds.isEmpty()) {
                log.info("商家 {} 没有已下架的商品，无需恢复", sellerId);
                return;
            }

            // 4. 计算可解冻数量
            int canActivate = productLimit == -1 ? inactiveProductIds.size() : (productLimit - (int) activeCount);

            if (canActivate <= 0) {
                log.info("商家 {} 当前上架商品已达新套餐限制，无法恢复更多商品", sellerId);
                return;
            }

            // 5. 确定需要恢复的商品（最新发布的优先恢复）
            int recoverCount = Math.min(canActivate, inactiveProductIds.size());
            List<Long> recoverProductIds = inactiveProductIds.subList(0, recoverCount);

            log.info("商家 {} 将恢复 {} 个商品", sellerId, recoverProductIds.size());

            // 6. 恢复商品（status = 1）
            productMapper.batchUpdateStatus(recoverProductIds, 1);

            log.info("商家 {} 已恢复 {} 个商品", sellerId, recoverCount);
            log.info("续费解冻: sellerId={}, 新配额={}, 当前上架={}, 可解冻={}, 实际解冻={}",
                    sellerId, productLimit, activeCount, canActivate, recoverCount);

        } catch (Exception e) {
            log.error("处理商家 {} 套餐续费失败", sellerId, e);
            throw e;
        }
    }

    /**
     * 续费/升级后恢复所有已下架的商品上架
     * @param sellerId 商家ID
     */
    @Transactional
    public void restoreProducts(Long sellerId) {
        log.info("恢复商家 {} 的已下架商品", sellerId);

        try {
            // 查询该商家已下架的商品（status=0，按创建时间降序，最新发布的优先上架）
            List<Long> inactiveProductIds = productMapper.findInactiveProductIdsBySellerIdOrderByCreatedAtDesc(sellerId);
            
            if (inactiveProductIds.isEmpty()) {
                log.info("商家 {} 没有已下架的商品，无需恢复", sellerId);
                return;
            }

            // 批量恢复商品上架（status = 1）
            productMapper.batchUpdateStatus(inactiveProductIds, 1);

            // 通知商家
            webSocketService.sendPackageNotification(sellerId, "renew", 0,
                "续费成功，已恢复" + inactiveProductIds.size() + "件商品上架");

            log.info("恢复商品完成: sellerId={}, 恢复数量={}", sellerId, inactiveProductIds.size());

        } catch (Exception e) {
            log.error("恢复商家 {} 已下架商品失败", sellerId, e);
            throw e;
        }
    }
}