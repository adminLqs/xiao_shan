package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.vo.CheckoutItemVO;
import com.xiaoshan.springbootdemo.service.CartService;
import com.xiaoshan.springbootdemo.service.ProductService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        return ResponseEntity.ok(Map.of("success", true, "data", items));
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

        // 直接查询：商品表 + 图片表
        CheckoutItemVO item = productService.getCheckoutItem(productId, quantity);

        return ResponseEntity.ok(Map.of("success", true, "data", List.of(item)));
    }



}
