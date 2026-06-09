package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.UserBrowseHistory;
import com.xiaoshan.springbootdemo.service.UserBrowseHistoryService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserBrowseHistoryController {

    private final UserService userService;
    private final UserBrowseHistoryService browseHistoryService;

    @PostMapping("/user/browse-history")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> recordBrowse(
            Authentication authentication,
            @RequestBody Map<String, Long> request
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            Long productId = request.get("productId");

            if (productId == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "商品ID不能为空"
                ));
            }

            browseHistoryService.recordBrowse(userId, productId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "记录成功"
            ));

        } catch (Exception e) {
            log.error("记录浏览历史失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/user/browse-history")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getBrowseHistory(Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            List<UserBrowseHistory> history = browseHistoryService.getBrowseHistory(userId, 50);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("records", history),
                    "total", history.size()
            ));

        } catch (Exception e) {
            log.error("获取浏览历史失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/user/browse-history")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> clearBrowseHistory(Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            browseHistoryService.clearBrowseHistory(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "清空成功"
            ));

        } catch (Exception e) {
            log.error("清空浏览历史失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}