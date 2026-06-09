package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.service.OnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 心跳控制器
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final OnlineStatusService onlineStatusService;

    /**
     * 接收客户端心跳，刷新在线状态 TTL
     * 前端每 60 秒调用一次
     */
    @MessageMapping("/heartbeat")
    public void heartbeat(Principal principal) {
        if (principal != null) {
            try {
                Long userId = Long.parseLong(principal.getName());
                onlineStatusService.refreshOnline(userId);
                log.debug("心跳收到: userId={}", userId);
            } catch (NumberFormatException e) {
                log.warn("心跳解析用户ID失败: {}", principal.getName());
            }
        }
    }
}
