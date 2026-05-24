package com.xiaoshan.springbootdemo.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            // 挤掉旧连接
            WebSocketSession oldSession = onlineUsers.get(userId);
            if (oldSession != null && oldSession.isOpen()) {
                try {
                    oldSession.sendMessage(new TextMessage("您的账号在其他设备登录"));
                    oldSession.close();
                    log.info("用户 {} 的旧连接已被挤掉", userId);
                } catch (IOException e) {
                    log.error("关闭用户 {} 的旧连接失败: {}", userId, e.getMessage());
                }
            }
            
            onlineUsers.put(userId, session);
            log.info("用户 {} 连接成功，当前在线人数: {}", userId, onlineUsers.size());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            onlineUsers.remove(userId);
            log.info("用户 {} 断开连接，当前在线人数: {}", userId, onlineUsers.size());
        }
    }

    public static void sendToUser(Long userId, String message) {
        WebSocketSession session = onlineUsers.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.debug("向用户 {} 发送消息: {}", userId, message);
            } catch (IOException e) {
                log.error("向用户 {} 发送消息失败: {}", userId, e.getMessage());
            }
        }
    }

    public static void broadcast(String message) {
        for (Map.Entry<Long, WebSocketSession> entry : onlineUsers.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("广播消息给用户 {} 失败: {}", entry.getKey(), e.getMessage());
                }
            }
        }
        log.debug("广播消息，在线人数: {}", onlineUsers.size());
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("userId=")) {
                        return Long.parseLong(param.substring(7));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析用户ID失败: {}", e.getMessage());
        }
        return null;
    }
}