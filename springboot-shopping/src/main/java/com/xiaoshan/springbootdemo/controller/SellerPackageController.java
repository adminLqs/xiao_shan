package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.SellerPackage;
import com.xiaoshan.springbootdemo.entity.SellerPackageOrder;
import com.xiaoshan.springbootdemo.service.AlipayService;
import com.xiaoshan.springbootdemo.service.SellerPackageService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/seller/packages")
@RequiredArgsConstructor
public class SellerPackageController {

    private final SellerPackageService sellerPackageService;
    private final UserService userService;
    private final AlipayService alipayService;

    /**
     * 获取所有可用套餐
     * GET /api/v1/seller/packages
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getActivePackages() {
        try {
            List<SellerPackage> packages = sellerPackageService.getActivePackages();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", packages
            ));

        } catch (Exception e) {
            log.error("获取套餐列表失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }

    /**
     * 获取套餐详情
     * GET /api/v1/seller/packages/{packageId}
     */
    @GetMapping("/{packageId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getPackageDetail(@PathVariable Long packageId) {
        try {
            SellerPackage pkg = sellerPackageService.getPackageById(packageId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", pkg
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("获取套餐详情失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }

    /**
     * 购买套餐（创建订单并返回支付宝支付HTML）
     * POST /api/v1/seller/packages/{packageId}/buy
     */
    @PostMapping("/{packageId}/buy")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> buyPackage(Authentication authentication, @PathVariable Long packageId) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            
            // 创建套餐订单（待支付状态）
            SellerPackageOrder order = sellerPackageService.createOrder(sellerId, packageId);
            
            // 使用订单ID作为订单号创建支付宝支付页面
            String pageHtml = alipayService.createPackagePayPage(order);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "orderId", order.getId(),
                            "paymentHtml", pageHtml
                    )
            ));

        } catch (RuntimeException e) {
            log.warn("购买套餐失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("购买套餐系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 获取套餐订单状态
     * GET /api/v1/seller/packages/orders/{orderId}
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getPackageOrderStatus(Authentication authentication, @PathVariable Long orderId) {
        try {
            SellerPackageOrder order = sellerPackageService.getOrderById(orderId);
            Long sellerId = userService.getCurrentUserId(authentication);

            // 权限检查：只能查询自己的订单
            if (!order.getSellerId().equals(sellerId)) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "无权查询此订单"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", order
            ));

        } catch (RuntimeException e) {
            log.warn("查询订单失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("查询订单系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 获取当前套餐信息和使用情况
     * GET /api/v1/seller/packages/current
     */
    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getCurrentPackage(Authentication authentication) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            var usage = sellerPackageService.getPackageUsage(sellerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", usage
            ));

        } catch (Exception e) {
            log.error("获取当前套餐失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }

    /**
     * 检查套餐状态（用于数据分析页权限校验）
     * GET /api/v1/seller/packages/status
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> checkPackageStatus(Authentication authentication) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            log.info("检查商家 {} 的套餐状态", sellerId);
            
            SellerPackageOrder currentPackage = sellerPackageService.getCurrentPackage(sellerId);
            
            Map<String, Object> result = new HashMap<>();
            if (currentPackage != null) {
                result.put("active", true);
                result.put("packageName", currentPackage.getPackageName());
                result.put("endDate", currentPackage.getEndDate());
                result.put("startDate", currentPackage.getStartDate());
            } else {
                result.put("active", false);
                result.put("packageName", null);
                result.put("endDate", null);
                result.put("startDate", null);
            }
            
            log.info("商家 {} 套餐状态: {}", sellerId, result);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result
            ));

        } catch (Exception e) {
            log.error("检查套餐状态失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统繁忙，请稍后重试: " + e.getMessage()
            ));
        }
    }

    /**
     * 检查发布商品权限
     * GET /api/v1/seller/packages/check-publish
     */
    @GetMapping("/check-publish")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> checkPublishPermission(Authentication authentication) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            var result = sellerPackageService.checkPublishPermission(sellerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result
            ));

        } catch (Exception e) {
            log.error("检查发布权限失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "检查失败"
            ));
        }
    }

    /**
     * 获取套餐购买历史
     * GET /api/v1/seller/packages/history
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getPackageHistory(Authentication authentication) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            var history = sellerPackageService.getPackageHistory(sellerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", history
            ));

        } catch (Exception e) {
            log.error("获取购买历史失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }
}
