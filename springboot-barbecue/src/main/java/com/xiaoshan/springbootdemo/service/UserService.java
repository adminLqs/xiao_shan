package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.User;
import com.xiaoshan.springbootdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务层
 * 负责用户登录、认证、手机号管理等业务逻辑
 *
 * @author xiaoshan
 * @date 2026-05-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * 统一获取用户ID：只保留deviceId
     *
     * @param deviceId 设备标识符
     * @return 用户ID
     */
    @Transactional
    public Long getUserId(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return null;
        }
        return authDeviceId(deviceId);
    }

    /**
     * 用deviceId验证（只返回ID）
     *
     * @param deviceId 设备标识符
     * @return 用户ID
     */
    private Long authDeviceId(String deviceId) {
        try {
            // 直接用二合一方法查询
            Long userId = userMapper.findIdByDeviceId(deviceId);

            if (userId == null) {
                // 创建新用户
                User user = new User(deviceId);
                userMapper.insert(user);
                userId = user.getId();
                log.info("创建新用户成功 - deviceId: {}, userId: {}", deviceId, userId);
            }
            return userId;
        } catch (Exception e) {
            log.error("deviceId登录失败", e);
            return null;
        }
    }

    // ==================== 手机号相关方法 ====================

    /**
     * 根据手机号查询用户ID
     *
     * @param phone 手机号
     * @return 用户ID，不存在返回 null
     */
    public Long getUserIdByPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }
        Long userId = userMapper.findIdByPhone(phone);
        if (userId == null) {
            return null;
        }
        return userId;
    }

    /**
     * 根据手机号查询用户信息
     *
     * @param phone 手机号
     * @return 用户实体，不存在返回 null
     */
    public User getUserByPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }
        return userMapper.findByPhone(phone);
    }

    /**
     * 根据ID查询用户
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    public User findById(Long userId) {
        if (userId == null) {
            log.warn("用户ID为空");
            return null;
        }
        return userMapper.findById(userId);
    }

    /**
     * 更改用户手机号
     *
     * @param userId 用户ID
     * @param phone  新手机号
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateUserPhone(Long userId, String phone) {
        if (userId == null) {
            log.warn("用户ID为空");
            return false;
        }
        if (phone == null || phone.isEmpty()) {
            log.warn("手机号为空");
            return false;
        }

        // 校验手机号格式
        if (!isValidPhone(phone)) {
            log.warn("手机号格式不正确: {}", phone);
            return false;
        }

        // 检查手机号是否已被其他用户绑定
        Long existingUserId = userMapper.findIdByPhone(phone);
        if (existingUserId != null && !existingUserId.equals(userId)) {
            log.warn("手机号已被其他用户绑定: {}", phone);
            return false;
        }

        int updated = userMapper.updatePhone(userId, phone);
        if (updated > 0) {
            log.info("用户手机号更新成功 - userId: {}, phone: {}", userId, phone);
            return true;
        }
        log.warn("用户手机号更新失败 - userId: {}", userId);
        return false;
    }

    /**
     * 验证手机号格式
     *
     * @param phone 手机号
     * @return 是否有效
     */
    private boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        // 简单手机号正则：1开头的11位数字
        return phone.matches("^1[3-9]\\d{9}$");
    }
}