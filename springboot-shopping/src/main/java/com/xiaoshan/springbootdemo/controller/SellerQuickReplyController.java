package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.SellerQuickReply;
import com.xiaoshan.springbootdemo.service.ChatService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商家快捷回复控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/seller/quick-replies")
@RequiredArgsConstructor
public class SellerQuickReplyController {

    private final ChatService chatService;
    private final UserService userService;

    /**
     * 获取商家的快捷回复列表
     * GET /api/v1/seller/quick-replies
     */
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getQuickReplies(Authentication authentication) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            List<SellerQuickReply> replies = chatService.getQuickReplies(sellerId);
            return ResponseEntity.ok(Map.of("success", true, "data", replies));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 添加快捷回复
     * POST /api/v1/seller/quick-replies
     */
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> addQuickReply(
            @RequestParam String content,
            Authentication authentication) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            SellerQuickReply reply = chatService.addQuickReply(sellerId, content);
            return ResponseEntity.ok(Map.of("success", true, "data", reply));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新快捷回复
     * PUT /api/v1/seller/quick-replies/{id}
     */
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateQuickReply(
            @PathVariable Long id,
            @RequestParam String content,
            Authentication authentication) {
        try {
            chatService.updateQuickReply(id, content);
            return ResponseEntity.ok(Map.of("success", true, "message", "更新成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 删除快捷回复
     * DELETE /api/v1/seller/quick-replies/{id}
     */
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteQuickReply(@PathVariable Long id) {
        try {
            chatService.deleteQuickReply(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 初始化默认快捷回复
     * POST /api/v1/seller/quick-replies/init
     */
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initDefaultReplies(Authentication authentication) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            chatService.initDefaultQuickReplies(sellerId);
            return ResponseEntity.ok(Map.of("success", true, "message", "初始化成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }
}