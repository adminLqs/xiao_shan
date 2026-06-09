package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.service.NotificationService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/seller/notifications")
@RequiredArgsConstructor
public class SellerNotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * 获取商家未读消息数量
     */
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        Long userId = userService.getCurrentUserId(authentication);
        int unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", unreadCount));
    }

    /**
     * 获取商家消息汇总（订单/系统/物流 + 客服列表）
     */
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(Authentication authentication) {
        Long userId = userService.getCurrentUserId(authentication);
        Map<String, Object> summary = notificationService.getSummary(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", summary));
    }

    /**
     * 获取商家消息列表（分页）
     */
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "ALL") String type) {
        Long userId = userService.getCurrentUserId(authentication);
        Map<String, Object> result = notificationService.getNotifications(userId, type, page, pageSize);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    /**
     * 标记单条消息为已读
     */
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 标记所有消息为已读
     */
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        Long userId = userService.getCurrentUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}