package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Notification;
import com.xiaoshan.springbootdemo.mapper.NotificationMapper;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    
    public Notification create(Long userId, String type,
                               String title, String content, String extraData) {
        Notification notification = new Notification();
        notification.setId(snowflakeIdGenerator.nextId());
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setExtraData(extraData);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        return notification;
    }
    
    public Map<String, Object> getNotifications(Long userId, String type, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Notification> list = notificationMapper.findByUserId(userId, type, offset, pageSize);
        int total = notificationMapper.countByUserId(userId, type);
        int unread = notificationMapper.countUnread(userId);
        
        return Map.of("records", list, "total", total, "unread", unread, "page", page, "pageSize", pageSize);
    }
    
    public void markAsRead(Long id) { 
        notificationMapper.markAsRead(id); 
    } 
    
    public void markAllAsRead(Long userId) { 
        notificationMapper.markAllAsRead(userId); 
    } 
    
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnreadByType(userId, "ORDER") +
               notificationMapper.countUnreadByType(userId, "SYSTEM");
    }
    
    public Map<String, Object> getSummary(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        Map<String, Object> orderSummary = new HashMap<>();
        orderSummary.put("unread", notificationMapper.countUnreadByType(userId, "ORDER"));
        orderSummary.put("latest", getLatestOrDefault(notificationMapper.getLatestContentByType(userId, "ORDER"), "暂无订单消息"));
        result.put("ORDER", orderSummary);
        
        Map<String, Object> systemSummary = new HashMap<>();
        systemSummary.put("unread", notificationMapper.countUnreadByType(userId, "SYSTEM"));
        systemSummary.put("latest", getLatestOrDefault(notificationMapper.getLatestContentByType(userId, "SYSTEM"), "暂无系统消息"));
        result.put("SYSTEM", systemSummary);
        
        return result;
    }
    
    private String getLatestOrDefault(String latest, String defaultValue) {
        return latest != null && !latest.isEmpty() ? latest : defaultValue;
    }
}