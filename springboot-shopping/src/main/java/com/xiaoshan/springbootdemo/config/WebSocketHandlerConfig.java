package com.xiaoshan.springbootdemo.config;

import com.xiaoshan.springbootdemo.handler.NotificationWebSocketHandler;
import com.xiaoshan.springbootdemo.service.OnlineStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class WebSocketHandlerConfig {

    private final OnlineStatusService onlineStatusService;

    @Autowired
    public WebSocketHandlerConfig(OnlineStatusService onlineStatusService) {
        this.onlineStatusService = onlineStatusService;
    }

    @PostConstruct
    public void init() {
        NotificationWebSocketHandler.setOnlineStatusService(onlineStatusService);
        log.info("WebSocketHandlerConfig 已初始化，OnlineStatusService 已注入");
    }
}
