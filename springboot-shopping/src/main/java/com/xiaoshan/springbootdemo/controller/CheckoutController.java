package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.UserCoupon;
import com.xiaoshan.springbootdemo.entity.vo.CheckoutItemVO;
import com.xiaoshan.springbootdemo.service.CartService;
import com.xiaoshan.springbootdemo.service.CouponService;
import com.xiaoshan.springbootdemo.service.ProductService;
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
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class CheckoutController {

    private final UserService userService;
    private final CartService cartService;
    private final ProductService productService;
    private final CouponService couponService;

    /**
     * 购物车结算：根据购物车项ID查询商品信息
     */
    @PostMapping("/checkout/cart")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getCheckoutItemsFromCart(
            Authentication authentication,
            @RequestBody Map<String, List<Long>> request
    ) {
        Long userId = userService.getCurrentUserId(authentication);
        List<Long> cartItemIds = request.get("ids");

        // 联表查询：购物车表 + 商品表 + 图片表
        List<CheckoutItemVO> items = cartService.getCheckoutItems(userId, cartItemIds);

        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("items", items)));
    }

    /**
     * 立即购买：根据商品ID和数量查询商品信息
     */
    @PostMapping("/checkout/product")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getCheckoutItemsFromProduct(
            Authentication authentication,
            @RequestBody Map<String, Object> request
    ) {
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = (Integer) request.getOrDefault("quantity", 1);
        Long skuId = request.containsKey("skuId") && request.get("skuId") != null 
                     ? Long.valueOf(request.get("skuId").toString()) : null;

        CheckoutItemVO item = productService.getCheckoutItem(productId, quantity, skuId);

        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("items", List.of(item))));
    }

    /**
     * 获取用户可用的优惠券列表
     */
    @GetMapping("/checkout/coupons")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getApplicableCoupons(
            Authentication authentication,
            @RequestParam BigDecimal orderAmount
    ) {
        Long userId = userService.getCurrentUserId(authentication);
        List<UserCoupon> coupons = couponService.getApplicableCoupons(userId, orderAmount);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("coupons", coupons)));
    }

    /**
     * 订单应用优惠券
     */
    @PostMapping("/orders/{orderId}/apply-coupon")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> applyCouponToOrder(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody Map<String, Long> request
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            Long userCouponId = request.get("userCouponId");

            if (userCouponId == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "优惠券ID不能为空"));
            }

            couponService.applyCouponToOrder(userId, orderId, userCouponId);

            return ResponseEntity.ok(Map.of("success", true, "message", "优惠券应用成功"));

        } catch (RuntimeException e) {
            log.warn("应用优惠券失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("应用优惠券系统异常", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

}
