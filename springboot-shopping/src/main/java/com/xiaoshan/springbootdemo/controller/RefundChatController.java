package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.RefundChat;
import com.xiaoshan.springbootdemo.service.RefundChatService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 退款聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RefundChatController {

    private final RefundChatService refundChatService;
    private final UserService userService;

    /**
     * 获取退款聊天记录
     * GET /api/v1/refunds/{refundId}/chat
     */
    @GetMapping("/refunds/{refundId}/chat")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getChatHistory(
            Authentication authentication,
            @PathVariable Long refundId) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            List<RefundChat> chatHistory = refundChatService.getChatHistory(refundId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", chatHistory
            ));

        } catch (RuntimeException e) {
            log.warn("获取聊天记录失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("获取聊天记录系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 发送消息
     * POST /api/v1/refunds/{refundId}/chat/send
     */
    @PostMapping("/refunds/{refundId}/chat/send")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> sendMessage(
            Authentication authentication,
            @PathVariable Long refundId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            
            // 判断用户角色，确定发送者类型
            String senderType = userService.isSeller(authentication) ? "SELLER" : "BUYER";
            
            String message = (String) requestBody.get("message");
            String images = (String) requestBody.get("images");

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请输入消息内容"
                ));
            }

            RefundChat chat = refundChatService.sendMessage(refundId, senderType, userId, message, images);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "消息发送成功",
                    "data", chat
            ));

        } catch (RuntimeException e) {
            log.warn("发送消息失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("发送消息系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 发送消息（新路径）
     * POST /api/v1/refunds/{refundId}/messages
     */
    @PostMapping(value = "/refunds/{refundId}/messages", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> sendMessageNew(
            Authentication authentication,
            @PathVariable Long refundId,
            @RequestPart(value = "message", required = false) String message,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            
            // 判断用户角色，确定发送者类型
            String senderType = userService.isSeller(authentication) ? "SELLER" : "BUYER";

            // 消息内容和图片至少要有一个
            if ((message == null || message.trim().isEmpty()) && (images == null || images.isEmpty())) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请输入消息内容或选择图片"
                ));
            }

            refundChatService.sendMessageWithFiles(refundId, senderType, userId, message, images);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "消息发送成功");
            result.put("data", null);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.warn("发送消息失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("发送消息系统异常", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "系统错误，请稍后重试");
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 获取退款详情（含聊天记录）
     * GET /api/v1/refunds/{refundId}/detail-with-chat
     */
    @GetMapping("/refunds/{refundId}/detail-with-chat")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getRefundDetailWithChat(
            Authentication authentication,
            @PathVariable Long refundId) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            Map<String, Object> detail = refundChatService.getRefundDetailWithChat(refundId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", detail
            ));

        } catch (RuntimeException e) {
            log.warn("获取退款详情失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("获取退款详情系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 商家回复退款申请（带消息发送）
     * PUT /api/v1/seller/refunds/{refundId}/respond
     */
    @PutMapping("/seller/refunds/{refundId}/respond")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> sellerRespond(
            Authentication authentication,
            @PathVariable Long refundId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            String response = (String) requestBody.get("response");
            String images = (String) requestBody.get("images");

            if (response == null || response.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请输入回复内容"
                ));
            }

            RefundChat chat = refundChatService.sellerResponse(refundId, sellerId, response, images);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "回复成功",
                    "data", chat
            ));

        } catch (RuntimeException e) {
            log.warn("商家回复失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("商家回复系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }
}