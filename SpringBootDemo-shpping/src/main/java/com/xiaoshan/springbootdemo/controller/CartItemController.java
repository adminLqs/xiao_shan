package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.dto.CartAddDTO;
import com.xiaoshan.springbootdemo.entity.dto.CartUpdateDTO;
import com.xiaoshan.springbootdemo.service.CartService;
import com.xiaoshan.springbootdemo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xiaoshan.springbootdemo.entity.vo.CartItemVO;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class CartItemController {

    private final UserService userService;
    private final CartService cartService;

    /**
     * 添加商品到购物车
     * POST /api/v1/cart/items
     */
    @PostMapping("/cart/items")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> addCartItem(
            Authentication authentication,
            @Valid @RequestBody CartAddDTO dto
    ) {
        try {
            // 获取当前登录用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 调用服务添加购物车
            cartService.addCartItem(userId, dto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "添加成功"
            ));

        } catch (Exception e) {
            log.error("添加购物车失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除购物车项
     * DELETE /api/v1/cart/items/{cartItemId}
     */
    @DeleteMapping("/cart/items/{cartItemId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> deleteCartItem(
            Authentication authentication,
            @PathVariable Long cartItemId
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            cartService.deleteCartItem(userId, cartItemId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "删除成功"
            ));
        } catch (Exception e) {
            log.error("删除购物车项失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    /**
     * 批量删除购物车项
     * DELETE /api/v1/cart/items/batch
     */
    @DeleteMapping("/cart/items/batch")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> batchDeleteCartItems(
            Authentication authentication,
            @RequestBody Map<String, List<Long>> request
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            List<Long> ids = request.get("ids");

            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "请选择要删除的商品"
                ));
            }

            cartService.batchDeleteCartItems(userId, ids);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "删除成功"
            ));
        } catch (Exception e) {
            log.error("批量删除购物车失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 修改购物车商品数量
     * PUT /api/v1/cart/items
     */
    @PutMapping("/cart/items")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> updateCartItem(
            Authentication authentication,
            @Valid @RequestBody CartUpdateDTO dto
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            cartService.updateCartItemQuantity(userId, dto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "修改成功"
            ));

        } catch (Exception e) {
            log.error("修改购物车失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取购物车列表
     * GET /api/v1/cart/items
     */
    @GetMapping("/cart/items")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getCartList(Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            List<CartItemVO> cartList = cartService.getCartList(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", cartList
            ));
        } catch (Exception e) {
            log.error("获取购物车列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取购物车商品总数量
     * GET /api/v1/cart/count
     */
    @GetMapping("/cart/count")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getCartCount(Authentication authentication) {
        try {
            // 获取当前登录用户ID
            Long userId = userService.getCurrentUserId(authentication);
            // 获取购物车商品总数量
            int count = cartService.getCartCount(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", count
            ));
        } catch (Exception e) {
            log.error("获取购物车数量失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
