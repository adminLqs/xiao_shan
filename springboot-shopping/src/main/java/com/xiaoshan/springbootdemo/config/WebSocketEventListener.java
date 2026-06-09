package com.xiaoshan.springbootdemo.config;

import com.xiaoshan.springbootdemo.service.OnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final OnlineStatusService onlineStatusService;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        
        String userIdStr = accessor.getFirstNativeHeader("userId");
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdStr);
                log.info("WebSocket连接: userId={}", userId);
                
                accessor.getSessionAttributes().put("userId", userId);
                
                onlineStatusService.setOnline(userId);
                log.info("用户 {} 在线状态已设置", userId);
                
            } catch (NumberFormatException e) {
                log.error("解析 userId 失败: {}", userIdStr);
            }
        } else {
            log.warn("WebSocket连接未携带 userId");
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        
        Object userIdObj = accessor.getSessionAttributes().get("userId");
        if (userIdObj != null) {
            try {
                Long userId = Long.parseLong(userIdObj.toString());
                log.info("WebSocket断开: userId={}", userId);
                
                onlineStatusService.setOffline(userId);
                log.info("用户 {} 离线状态已设置", userId);
                
            } catch (NumberFormatException e) {
                log.error("解析 userId 失败: {}", userIdObj);
            }
        } else {
            log.warn("WebSocket断开未携带 userId");
        }
    }
}
