package com.xiaoshan.springbootdemo.handler;

import com.xiaoshan.springbootdemo.service.OnlineStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static OnlineStatusService onlineStatusService;

    public static void setOnlineStatusService(OnlineStatusService service) {
        onlineStatusService = service;
        log.info("OnlineStatusService 已初始化");
    }

    // 存储每个用户的定时任务，以便断开时取消
    private static final Map<Long, Runnable> heartbeatTasks = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        
        if (userId != null) {
            log.info("用户上线: userId={}", userId);
            
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
                // 取消旧连接的心跳任务
                heartbeatTasks.remove(userId);
            }

            onlineUsers.put(userId, session);

            // 设置在线状态
            if (onlineStatusService != null) {
                onlineStatusService.setOnline(userId);
                log.info("用户 {} 在线状态已设置", userId);
            } else {
                log.warn("onlineStatusService 未初始化，无法设置在线状态");
            }

            // 启动心跳任务：每分钟刷新一次在线状态
            Runnable heartbeatTask = () -> {
                try {
                    if (session.isOpen() && onlineStatusService != null) {
                        // 发送 ping 保持连接活跃
                        session.sendMessage(new PingMessage());
                        // 刷新在线状态过期时间
                        onlineStatusService.refreshOnline(userId);
                        log.debug("用户 {} 心跳刷新成功", userId);
                    }
                } catch (IOException e) {
                    log.warn("用户 {} 心跳发送失败: {}", userId, e.getMessage());
                    // 发送失败说明连接可能已断开，尝试关闭
                    try {
                        if (session.isOpen()) {
                            session.close();
                        }
                    } catch (IOException closeEx) {
                        log.warn("关闭失败连接: {}", closeEx.getMessage());
                    }
                }
            };
            heartbeatTasks.put(userId, heartbeatTask);
            scheduler.scheduleAtFixedRate(heartbeatTask, 60, 60, TimeUnit.SECONDS);

            log.info("用户 {} 连接成功，当前在线人数: {}", userId, onlineUsers.size());
        } else {
            log.warn("无法获取用户ID，WebSocket连接可能未携带用户信息");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            onlineUsers.remove(userId);

            // 取消心跳任务
            heartbeatTasks.remove(userId);

            // 设置离线状态
            if (onlineStatusService != null) {
                onlineStatusService.setOffline(userId);
                log.info("用户 {} 离线状态已设置", userId);
            }

            log.info("用户 {} 断开连接，当前在线人数: {}", userId, onlineUsers.size());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 收到消息时的处理，如果需要可以根据消息类型处理
        super.handleTextMessage(session, message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            log.error("用户 {} WebSocket传输错误: {}", userId, exception.getMessage());
            onlineUsers.remove(userId);

            // 取消心跳任务
            heartbeatTasks.remove(userId);

            // 设置离线状态
            if (onlineStatusService != null) {
                onlineStatusService.setOffline(userId);
            }
        }
        super.handleTransportError(session, exception);
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
            Object attr = session.getAttributes().get("userId");
            if (attr != null) {
                return Long.parseLong(attr.toString());
            }
        } catch (Exception e) {
            log.warn("从 session 属性获取用户ID失败: {}", e.getMessage());
        }
        return null;
    }
}