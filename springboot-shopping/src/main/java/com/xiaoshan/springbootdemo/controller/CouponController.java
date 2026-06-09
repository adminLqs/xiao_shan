package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Coupon;
import com.xiaoshan.springbootdemo.entity.UserCoupon;
import com.xiaoshan.springbootdemo.service.CouponService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final UserService userService;

    @GetMapping("/api/v1/seller/coupons")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getSellerCoupons(Authentication authentication,
                                              @RequestParam(required = false) Integer status) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            List<Coupon> coupons = couponService.getSellerCoupons(sellerId, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", coupons
            ));

        } catch (Exception e) {
            log.error("获取商家优惠券列表失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }

    @GetMapping("/api/v1/seller/coupons/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getCouponDetail(@PathVariable Long id) {
        try {
            Coupon coupon = couponService.getCouponById(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", coupon
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("获取优惠券详情失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }

    @PostMapping("/api/v1/seller/coupons")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> createCoupon(Authentication authentication, @RequestBody Coupon coupon) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            Coupon created = couponService.createCoupon(sellerId, coupon);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "创建成功",
                    "data", created
            ));

        } catch (RuntimeException e) {
            log.warn("创建优惠券失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("创建优惠券系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    @PutMapping("/api/v1/seller/coupons/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> updateCoupon(@PathVariable Long id, @RequestBody Coupon coupon) {
        try {
            Coupon updated = couponService.updateCoupon(id, coupon);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "更新成功",
                    "data", updated
            ));

        } catch (RuntimeException e) {
            log.warn("更新优惠券失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("更新优惠券系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    @PutMapping("/api/v1/seller/coupons/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> updateCouponStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer status = body.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "无效的状态值"
                ));
            }
            Coupon updated = couponService.updateCouponStatus(id, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", status == 1 ? "启用成功" : "禁用成功",
                    "data", updated
            ));

        } catch (RuntimeException e) {
            log.warn("更新优惠券状态失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("更新优惠券状态系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    @DeleteMapping("/api/v1/seller/coupons/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> deleteCoupon(@PathVariable Long id) {
        try {
            couponService.deleteCoupon(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "删除成功"
            ));

        } catch (RuntimeException e) {
            log.warn("删除优惠券失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("删除优惠券系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    @GetMapping("/api/v1/coupons/available")
    public ResponseEntity<?> getAvailableCoupons() {
        try {
            List<Coupon> coupons = couponService.getAvailableCoupons();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", coupons
            ));

        } catch (Exception e) {
            log.error("获取可领取优惠券列表失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }

    @GetMapping("/api/v1/seller/{sellerId}/coupons/available")
    public ResponseEntity<?> getShopAvailableCoupons(@PathVariable Long sellerId) {
        try {
            List<Coupon> coupons = couponService.getShopAvailableCoupons(sellerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("coupons", coupons)
            ));

        } catch (Exception e) {
            log.error("获取店铺优惠券列表失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取数据失败"
            ));
        }
    }

    @PostMapping("/api/v1/coupons/{id}/receive")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> receiveCoupon(Authentication authentication, @PathVariable Long id) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            UserCoupon userCoupon = couponService.receiveCoupon(userId, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "领取成功",
                    "data", userCoupon
            ));

        } catch (RuntimeException e) {
            log.warn("领取优惠券失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("领取优惠券系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    @GetMapping("/api/v1/user/coupons")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getUserCoupons(Authentication authentication,
                                             @RequestParam(required = false) String status) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            List<UserCoupon> userCoupons = couponService.getUserCoupons(userId, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", userCoupons
            ));

        } catch (RuntimeException e) {
            log.warn("获取用户优惠券失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("获取用户优惠券系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }
}
