package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.mapper.FollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户关注服务
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;

    /**
     * 关注商家
     */
    public void follow(Long userId, Long sellerId) {
        followMapper.insert(userId, sellerId);
    }

    /**
     * 取消关注
     */
    public void unfollow(Long userId, Long sellerId) {
        followMapper.delete(userId, sellerId);
    }

    /**
     * 检查是否已关注
     */
    public boolean isFollowed(Long userId, Long sellerId) {
        return followMapper.count(userId, sellerId) > 0;
    }
}
