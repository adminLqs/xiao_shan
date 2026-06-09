package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.ChatMessage;
import com.xiaoshan.springbootdemo.entity.SellerProfile;
import com.xiaoshan.springbootdemo.entity.User;
import com.xiaoshan.springbootdemo.entity.UserProfile;
import com.xiaoshan.springbootdemo.mapper.SellerProfileMapper;
import com.xiaoshan.springbootdemo.mapper.UserMapper;
import com.xiaoshan.springbootdemo.mapper.UserProfileMapper;
import com.xiaoshan.springbootdemo.service.ChatService;
import com.xiaoshan.springbootdemo.service.UserService;
import com.xiaoshan.springbootdemo.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final UserMapper userMapper;
    private final SellerProfileMapper sellerProfileMapper;
    private final UserProfileMapper userProfileMapper;
    private final ChatService chatService;
    private final UserService userService;
    private final WebSocketService webSocketService;

    /**
     * 根据发送者角色获取发送者名称
     */
    private String getSenderName(Long senderId, Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_SELLER".equals(role)) {
                SellerProfile profile = sellerProfileMapper.findByUserId(senderId).orElse(null);
                return profile != null && profile.getStoreName() != null ? profile.getStoreName() : "商家";
            } else if ("ROLE_USER".equals(role)) {
                UserProfile profile = userProfileMapper.findByUserId(senderId).orElse(null);
                return profile != null && profile.getNickname() != null ? profile.getNickname() : "用户";
            }
        }
        return "用户";
    }

    /**
     * 获取聊天目标用户信息
     * GET /api/v1/chat/{targetId}/info
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @GetMapping("/{targetId}/info")
    public ResponseEntity<Map<String, Object>> getChatTargetInfo(
            @PathVariable Long targetId,
            Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);

            // 校验不能和自己聊天
            if (targetId.equals(currentUserId)) {
                return ResponseEntity.ok(Map.of("success", false, "message", "不能和自己聊天"));
            }

            User target = userMapper.findById(targetId).orElse(null);
            if (target == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
            }

            String name = "用户";
            String avatar = null;

            if ("ROLE_SELLER".equals(target.getRole())) {
                SellerProfile profile = sellerProfileMapper.findByUserId(targetId).orElse(null);
                if (profile != null) {
                    name = profile.getStoreName() != null && !profile.getStoreName().isEmpty()
                            ? profile.getStoreName()
                            : "商家";
                    avatar = profile.getStoreAvatar();
                } else {
                    name = "商家";
                }
            } else {
                UserProfile userProfile = userProfileMapper.findByUserId(targetId).orElse(null);
                if (userProfile != null) {
                    name = userProfile.getNickname() != null && !userProfile.getNickname().isEmpty()
                            ? userProfile.getNickname()
                            : target.getAccount();
                    avatar = userProfile.getAvatar();
                    if (avatar != null && avatar.contains("default-admin-avatar")) {
                        avatar = "";
                    }
                } else {
                    name = target.getAccount();
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "id", target.getId(),
                            "name", name,
                            "avatar", avatar != null ? avatar : "",
                            "online", true
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 获取聊天消息列表（分页）
     * GET /api/v1/chat/{targetId}/messages?page=1&pageSize=20
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @GetMapping("/{targetId}/messages")
    public ResponseEntity<Map<String, Object>> getChatMessages(
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);

            // 标记消息为已读
            chatService.markMessagesAsRead(currentUserId, targetId);

            // 获取消息列表
            List<ChatMessage> messages = chatService.getMessages(currentUserId, targetId, page, pageSize);
            int total = chatService.getMessageCount(currentUserId, targetId);

            // 转换为前端需要的格式
            List<Map<String, Object>> messageList = messages.stream().map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", msg.getId());
                map.put("senderId", msg.getSenderId());
                map.put("receiverId", msg.getReceiverId());
                map.put("text", msg.getContent());
                map.put("type", msg.getMessageType());
                if (msg.getMessageType() == ChatMessage.MessageType.VIDEO) {
                    map.put("videos", msg.getMediaUrlList() != null ? msg.getMediaUrlList() : new ArrayList<>());
                    map.put("images", new ArrayList<>());
                } else {
                    map.put("images", msg.getMediaUrlList() != null ? msg.getMediaUrlList() : new ArrayList<>());
                    map.put("videos", new ArrayList<>());
                }
                map.put("productId", msg.getProductId());
                map.put("orderId", msg.getOrderId());
                map.put("isRecalled", msg.getIsRecalled());
                map.put("sendTime", msg.getCreatedAt() != null ?
                        msg.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "");
                map.put("time", msg.getCreatedAt() != null ?
                        msg.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "messages", messageList,
                            "total", total,
                            "page", page,
                            "pageSize", pageSize
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 发送文本消息
     * POST /api/v1/chat/send/text
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @PostMapping("/send/text")
    public ResponseEntity<Map<String, Object>> sendTextMessage(
            @RequestParam Long targetId,
            @RequestParam String content,
            Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);

            // 校验不能和自己聊天
            if (targetId.equals(currentUserId)) {
                return ResponseEntity.ok(Map.of("success", false, "message", "不能和自己聊天"));
            }

            // 校验目标用户存在
            User target = userMapper.findById(targetId).orElse(null);
            if (target == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
            }

            // 发送消息
            ChatMessage message = chatService.sendMessage(
                    currentUserId, targetId, content,
                    ChatMessage.MessageType.TEXT, null, null, null, null
            );

            // 通过 WebSocket 推送给接收方
            String senderName = getSenderName(currentUserId, authentication);
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", message.getId());
            messageData.put("senderId", message.getSenderId());
            messageData.put("receiverId", message.getReceiverId());
            messageData.put("text", message.getContent());
            messageData.put("type", message.getMessageType());
            messageData.put("senderName", senderName);
            messageData.put("sendTime", message.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messageData.put("time", message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 根据接收者角色选择发送通道
            if ("ROLE_SELLER".equals(target.getRole())) {
                webSocketService.sendChatToSeller(targetId, message.getContent(), currentUserId, senderName);
            } else {
                webSocketService.sendChatMessage(targetId, messageData);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", messageData));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 发送图片消息
     * POST /api/v1/chat/send/image
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @PostMapping("/send/image")
    public ResponseEntity<Map<String, Object>> sendImageMessage(
            @RequestParam Long targetId,
            @RequestParam("images") List<MultipartFile> images,
            Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);

            // 校验目标用户存在
            User target = userMapper.findById(targetId).orElse(null);
            if (target == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
            }

            // 校验图片数量（最多9张）
            if (images == null || images.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "请选择图片"));
            }
            if (images.size() > 9) {
                return ResponseEntity.ok(Map.of("success", false, "message", "一次最多发送9张图片"));
            }

            // 校验单张图片大小（不超过5MB）
            for (MultipartFile image : images) {
                if (image.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.ok(Map.of("success", false, "message", "单张图片不能超过5MB"));
                }
            }

            // 上传图片并获取URL列表
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                String url = webSocketService.uploadFile(image);
                if (url != null) {
                    imageUrls.add(url);
                }
            }

            // 发送消息
            ChatMessage message = chatService.sendMessage(
                    currentUserId, targetId, "",
                    ChatMessage.MessageType.IMAGE, imageUrls, null, null, null
            );

            // 通过 WebSocket 推送给接收方
            String senderName = getSenderName(currentUserId, authentication);
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", message.getId());
            messageData.put("senderId", message.getSenderId());
            messageData.put("receiverId", message.getReceiverId());
            messageData.put("text", "");
            messageData.put("type", message.getMessageType());
            messageData.put("senderName", senderName);
            messageData.put("images", imageUrls);
            messageData.put("sendTime", message.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messageData.put("time", message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 根据接收者角色选择发送通道
            if ("ROLE_SELLER".equals(target.getRole())) {
                webSocketService.sendChatToSeller(targetId, "", currentUserId, "IMAGE", imageUrls, senderName);
            } else {
                webSocketService.sendChatToUser(targetId, currentUserId, "", "IMAGE", imageUrls, senderName);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", messageData));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 发送视频消息
     * POST /api/v1/chat/send/video
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @PostMapping("/send/video")
    public ResponseEntity<Map<String, Object>> sendVideoMessage(
            @RequestParam Long targetId,
            @RequestParam("video") MultipartFile video,
            Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);

            // 校验目标用户存在
            User target = userMapper.findById(targetId).orElse(null);
            if (target == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
            }

            // 校验视频大小（最大100MB）
            if (video.getSize() > 100 * 1024 * 1024) {
                return ResponseEntity.ok(Map.of("success", false, "message", "视频大小不能超过100MB"));
            }

            // 上传视频
            String videoUrl = webSocketService.uploadFile(video);
            if (videoUrl == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "视频上传失败"));
            }

            // 发送消息
            ChatMessage message = chatService.sendMessage(
                    currentUserId, targetId, "",
                    ChatMessage.MessageType.VIDEO, List.of(videoUrl), null, null, null
            );

            // 通过 WebSocket 推送给接收方
            String senderName = getSenderName(currentUserId, authentication);
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", message.getId());
            messageData.put("senderId", message.getSenderId());
            messageData.put("receiverId", message.getReceiverId());
            messageData.put("text", "");
            messageData.put("type", "VIDEO");
            messageData.put("senderName", senderName);
            messageData.put("videos", List.of(videoUrl));
            messageData.put("sendTime", message.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messageData.put("time", message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 根据接收者角色选择发送通道
            if ("ROLE_SELLER".equals(target.getRole())) {
                webSocketService.sendChatToSeller(targetId, "", currentUserId, "VIDEO", List.of(videoUrl), senderName);
            } else {
                webSocketService.sendChatToUser(targetId, currentUserId, "", "VIDEO", List.of(videoUrl), senderName);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", messageData));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 发送文字+媒体消息
     * POST /api/v1/chat/send/with-media
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @PostMapping("/send/with-media")
    public ResponseEntity<Map<String, Object>> sendMessageWithMedia(
            @RequestParam Long targetId,
            @RequestParam String content,
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);

            // 校验目标用户存在
            User target = userMapper.findById(targetId).orElse(null);
            if (target == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
            }

            // 上传所有文件（图片和视频统一用 IMAGE 类型）
            List<String> mediaUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = webSocketService.uploadFile(file);
                if (url != null) {
                    mediaUrls.add(url);
                }
            }

            // 发送消息
            ChatMessage message = chatService.sendMessage(
                    currentUserId, targetId, content,
                    ChatMessage.MessageType.IMAGE, mediaUrls, null, null, null
            );

            // 通过 WebSocket 推送给接收方
            String senderName = getSenderName(currentUserId, authentication);
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", message.getId());
            messageData.put("senderId", message.getSenderId());
            messageData.put("receiverId", message.getReceiverId());
            messageData.put("text", content);
            messageData.put("type", message.getMessageType());
            messageData.put("senderName", senderName);
            if (!mediaUrls.isEmpty()) messageData.put("images", mediaUrls);
            messageData.put("sendTime", message.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messageData.put("time", message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 根据接收者角色选择发送通道
            if ("ROLE_SELLER".equals(target.getRole())) {
                webSocketService.sendChatMessageToSeller(targetId, messageData);
            } else {
                webSocketService.sendChatMessage(targetId, messageData);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", messageData));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 发送商品卡片消息
     * POST /api/v1/chat/send/product
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @PostMapping("/send/product")
    public ResponseEntity<Map<String, Object>> sendProductCard(
            @RequestParam Long targetId,
            @RequestParam Long productId,
            Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);

            // 校验目标用户存在
            User target = userMapper.findById(targetId).orElse(null);
            if (target == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
            }

            // 发送商品卡片消息
            ChatMessage message = chatService.sendMessage(
                    currentUserId, targetId, "商品分享",
                    ChatMessage.MessageType.PRODUCT_CARD, null, null, productId, null
            );

            // 通过 WebSocket 推送给接收方
            String senderName = getSenderName(currentUserId, authentication);
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", message.getId());
            messageData.put("senderId", message.getSenderId());
            messageData.put("receiverId", message.getReceiverId());
            messageData.put("text", "商品分享");
            messageData.put("type", message.getMessageType());
            messageData.put("senderName", senderName);
            messageData.put("productId", productId);
            messageData.put("sendTime", message.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messageData.put("time", message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 根据接收者角色选择发送通道
            if ("ROLE_SELLER".equals(target.getRole())) {
                webSocketService.sendChatToSeller(targetId, "商品分享", currentUserId, "PRODUCT_CARD", senderName);
            } else {
                webSocketService.sendChatMessage(targetId, messageData);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", messageData));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 发送订单卡片消息
     * POST /api/v1/chat/send/order
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @PostMapping("/send/order")
    public ResponseEntity<Map<String, Object>> sendOrderCard(
            @RequestParam Long targetId,
            @RequestParam Long orderId,
            Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);

            // 校验目标用户存在
            User target = userMapper.findById(targetId).orElse(null);
            if (target == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
            }

            // 发送订单卡片消息
            ChatMessage message = chatService.sendMessage(
                    currentUserId, targetId, "订单分享",
                    ChatMessage.MessageType.ORDER_CARD, null, null, null, orderId
            );

            // 通过 WebSocket 推送给接收方
            String senderName = getSenderName(currentUserId, authentication);
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", message.getId());
            messageData.put("senderId", message.getSenderId());
            messageData.put("receiverId", message.getReceiverId());
            messageData.put("text", "订单分享");
            messageData.put("type", message.getMessageType());
            messageData.put("senderName", senderName);
            messageData.put("orderId", orderId);
            messageData.put("sendTime", message.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messageData.put("time", message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 根据接收者角色选择发送通道
            if ("ROLE_SELLER".equals(target.getRole())) {
                webSocketService.sendChatToSeller(targetId, "订单分享", currentUserId, "ORDER_CARD", senderName);
            } else {
                webSocketService.sendChatMessage(targetId, messageData);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", messageData));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 撤回消息
     * POST /api/v1/chat/{messageId}/recall
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @PostMapping("/{messageId}/recall")
    public ResponseEntity<Map<String, Object>> recallMessage(
            @PathVariable Long messageId,
            Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);
            boolean success = chatService.recallMessage(messageId, currentUserId);

            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "撤回成功"));
            } else {
                return ResponseEntity.ok(Map.of("success", false, "message", "撤回失败，只能撤回2分钟内自己发送的消息"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 获取会话列表
     * GET /api/v1/chat/sessions
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getChatSessions(Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);
            List<Map<String, Object>> sessions = chatService.getSessions(currentUserId);

            return ResponseEntity.ok(Map.of("success", true, "data", sessions));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 设置会话置顶
     * POST /api/v1/chat/session/{sessionId}/top
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @PostMapping("/session/{sessionId}/top")
    public ResponseEntity<Map<String, Object>> setSessionTop(
            @PathVariable Long sessionId,
            @RequestParam Boolean isTop) {
        try {
            chatService.setSessionTop(sessionId, isTop);
            return ResponseEntity.ok(Map.of("success", true, "message", isTop ? "已置顶" : "已取消置顶"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 设置会话免打扰
     * POST /api/v1/chat/session/{sessionId}/mute
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @PostMapping("/session/{sessionId}/mute")
    public ResponseEntity<Map<String, Object>> setSessionMute(
            @PathVariable Long sessionId,
            @RequestParam Boolean isMuted) {
        try {
            chatService.setSessionMuted(sessionId, isMuted);
            return ResponseEntity.ok(Map.of("success", true, "message", isMuted ? "已开启免打扰" : "已关闭免打扰"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 删除会话
     * DELETE /api/v1/chat/session/{targetId}
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @DeleteMapping("/session/{targetId}")
    public ResponseEntity<Map<String, Object>> deleteSession(
            @PathVariable Long targetId,
            Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);
            chatService.deleteSession(currentUserId, targetId);
            return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 获取未读消息总数
     * GET /api/v1/chat/unread-count
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        try {
            Long currentUserId = userService.getCurrentUserId(authentication);
            int count = chatService.getUnreadCount(currentUserId);
            return ResponseEntity.ok(Map.of("success", true, "data", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }
}