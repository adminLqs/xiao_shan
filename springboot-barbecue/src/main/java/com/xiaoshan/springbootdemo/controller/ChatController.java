package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.ChatMessage;
import com.xiaoshan.springbootdemo.mapper.ChatMessageMapper;
import com.xiaoshan.springbootdemo.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageMapper chatMessageMapper;
    private final WebSocketService webSocketService;

    /**
     * 用户发送消息
     */
    @PostMapping("/chat/user/send")
    public ResponseEntity<?> userSendMessage(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String content = request.get("content").toString();

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "消息内容不能为空"));
            }

            // 使用 @Builder 链式调用
            ChatMessage message = ChatMessage.builder()
                    .senderId(userId)
                    .receiverId(null)
                    .content(content)
                    .isRead(false)
                    .build();
            chatMessageMapper.insert(message);

            webSocketService.sendChatToSeller(userId, content);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "发送成功",
                    "data", Map.of("id", message.getId())
            ));
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "发送失败"));
        }
    }

    /**
     * 商家回复消息
     */
    @PostMapping("/chat/seller/reply")
    public ResponseEntity<?> sellerReply(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String content = request.get("content").toString();

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "消息内容不能为空"));
            }

            // 使用 @Builder 链式调用
            ChatMessage message = ChatMessage.builder()
                    .senderId(null)
                    .receiverId(userId)
                    .content(content)
                    .isRead(true)
                    .build();
            chatMessageMapper.insert(message);

            webSocketService.sendChatToUser(userId, content);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "回复成功",
                    "data", Map.of("id", message.getId())
            ));
        } catch (Exception e) {
            log.error("回复消息失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "回复失败"));
        }
    }

    /**
     * 获取与指定用户的聊天记录（用户/商家共用）
     * @param userId 对方用户ID
     * @param isSeller 是否为商家调用（true:商家会标记已读，false:用户不标记）
     */
    @GetMapping("/chat/conversation/{userId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean isSeller) {
        try {
            List<ChatMessage> messages = chatMessageMapper.findConversationByUser(userId);

            // 商家调用时标记该用户的消息为已读
            if (isSeller) {
                chatMessageMapper.markAsRead(userId);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "messages", messages,
                            "total", messages.size(),
                            "userId", userId
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "获取失败")
            );
        }
    }

    /**
     * 获取所有会话列表（商家视角）
     */
    @GetMapping("/chat/conversations")
    public ResponseEntity<?> getConversations() {
        try {
            List<Map<String, Object>> conversations = chatMessageMapper.findSellerConversations();
            int totalUnread = chatMessageMapper.countUnread();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "conversations", conversations,
                            "totalUnread", totalUnread
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "获取失败"));
        }
    }

    /**
     * 标记消息为已读（商家查看后）
     */
    @PutMapping("/chat/read/{userId}")
    public ResponseEntity<?> markAsRead(@PathVariable Long userId) {
        try {
            chatMessageMapper.markAsRead(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "已标记为已读"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "操作失败"));
        }
    }

    /**
     * 标记所有消息为已读（商家全部已读）
     */
    @PutMapping("/chat/read/all")
    public ResponseEntity<?> markAllAsRead() {
        try {
            int count = chatMessageMapper.markAllAsRead();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "已标记所有消息为已读",
                    "data", Map.of("count", count)
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "操作失败"));
        }
    }

    /**
     * 获取未读消息总数（商家侧）
     */
    @GetMapping("/chat/unread/count")
    public ResponseEntity<?> getUnreadCount() {
        try {
            int count = chatMessageMapper.countUnread();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("count", count)
            ));
        } catch (Exception e) {
            log.error("获取未读数失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "获取失败"));
        }
    }
}