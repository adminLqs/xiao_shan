package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.service.FollowService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 关注控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FollowController {

    private final UserService userService;
    private final FollowService followService;

    /**
     * 关注商家
     */
    @PostMapping("/seller/{sellerId}/follow")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> followSeller(Authentication authentication, @PathVariable Long sellerId) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            followService.follow(userId, sellerId);
            return ResponseEntity.ok(Map.of("success", true, "message", "关注成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 取消关注
     */
    @DeleteMapping("/seller/{sellerId}/follow")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> unfollowSeller(Authentication authentication, @PathVariable Long sellerId) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            followService.unfollow(userId, sellerId);
            return ResponseEntity.ok(Map.of("success", true, "message", "已取消关注"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 检查是否已关注
     */
    @GetMapping("/seller/{sellerId}/follow/check")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> checkFollow(Authentication authentication, @PathVariable Long sellerId) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.ok(Map.of("success", true, "data", Map.of("isFollowed", false)));
            }
            Long userId = userService.getCurrentUserId(authentication);
            boolean isFollowed = followService.isFollowed(userId, sellerId);
            return ResponseEntity.ok(Map.of("success", true, "data", Map.of("isFollowed", isFollowed)));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage(), "data", Map.of("isFollowed", false)));
        }
    }
}
