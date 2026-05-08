package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.service.OrderItemService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单项控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderItemController {

    private final UserService userService;
    private final OrderItemService orderItemService;

    /**
     * 根据ID查询订单项
     * GET /api/v1/orderItems/{orderItemId}
     *
     * @param authentication 认证信息
     * @param orderItemId 订单项ID
     * @return 订单项信息
     */
    @GetMapping("/orderItems/{orderItemId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getOrderItemById(
            Authentication authentication,
            @PathVariable Long orderItemId
    ) {
        try {
            // 获取当前登录用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 查询订单项
            OrderItem orderItem = orderItemService.getOrderItemById(userId, orderItemId);

            if (orderItem == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "订单项不存在"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", orderItem
            ));

        } catch (Exception e) {
            log.error("获取订单项详情失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}