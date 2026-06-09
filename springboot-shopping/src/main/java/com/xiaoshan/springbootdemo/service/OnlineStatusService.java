package com.xiaoshan.springbootdemo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineStatusService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String ONLINE_KEY_PREFIX = "online:user:";
    private static final String OFFLINE_KEY_PREFIX = "offline:user:";
    private static final long ONLINE_EXPIRE_SECONDS = 300; // 5分钟

    /**
     * 设置用户在线状态（WebSocket连接时调用）
     */
    public void setOnline(Long userId) {
        String key = ONLINE_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, "1",
            ONLINE_EXPIRE_SECONDS + new java.util.Random().nextInt(60), TimeUnit.SECONDS);
        log.debug("用户 {} 设置为在线", userId);
    }

    /**
     * 刷新用户在线状态（心跳时调用）
     */
    public void refreshOnline(Long userId) {
        String key = ONLINE_KEY_PREFIX + userId;
        stringRedisTemplate.expire(key, ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.debug("用户 {} 在线状态已刷新", userId);
    }

    /**
     * 设置用户离线状态（WebSocket断开时调用）
     */
    public void setOffline(Long userId) {
        String onlineKey = ONLINE_KEY_PREFIX + userId;
        String offlineKey = OFFLINE_KEY_PREFIX + userId;

        // 删除在线状态
        stringRedisTemplate.delete(onlineKey);

        // 记录离线时间
        long timestamp = Instant.now().toEpochMilli();
        stringRedisTemplate.opsForValue().set(offlineKey, String.valueOf(timestamp));
        log.debug("用户 {} 设置为离线，离线时间: {}", userId, timestamp);
    }

    /**
     * 查询用户是否在线
     */
    public boolean isOnline(Long userId) {
        String key = ONLINE_KEY_PREFIX + userId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 获取在线状态信息
     * @return 包含 online, lastOfflineTime, offlineMinutes
     */
    public java.util.Map<String, Object> getOnlineStatus(Long userId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        boolean online = isOnline(userId);
        result.put("online", online);

        if (!online) {
            String offlineKey = OFFLINE_KEY_PREFIX + userId;
            String offlineTimeStr = stringRedisTemplate.opsForValue().get(offlineKey);

            if (offlineTimeStr != null) {
                long offlineTimestamp = Long.parseLong(offlineTimeStr);
                Instant offlineInstant = Instant.ofEpochMilli(offlineTimestamp);
                ZoneId zoneId = ZoneId.systemDefault();
                String lastOfflineTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .withZone(zoneId)
                        .format(offlineInstant);

                long offlineMinutes = (Instant.now().toEpochMilli() - offlineTimestamp) / 60000;

                result.put("lastOfflineTime", lastOfflineTime);
                result.put("offlineMinutes", offlineMinutes);
            } else {
                result.put("lastOfflineTime", null);
                result.put("offlineMinutes", null);
            }
        } else {
            result.put("lastOfflineTime", null);
            result.put("offlineMinutes", null);
        }

        return result;
    }
}
