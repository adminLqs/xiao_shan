package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.UserBrowseHistory;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.UserBrowseHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBrowseHistoryService {

    private final UserBrowseHistoryMapper browseHistoryMapper;
    private final ProductMapper productMapper;

    @Transactional
    public void recordBrowse(Long userId, Long productId) {
        try {
            Product product = productMapper.findBasicInfoForBrowse(productId).orElse(null);
            if (product == null || product.getStatus() != 1) {
                return;
            }

            browseHistoryMapper.deleteByUserIdAndProductId(userId, productId);

            UserBrowseHistory history = new UserBrowseHistory();
            history.setUserId(userId);
            history.setProductId(productId);
            history.setProductName(product.getName());
            history.setProductImage(product.getImages());
            history.setProductPrice(product.getPrice());
            browseHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("记录浏览历史失败: {}", e.getMessage());
        }
    }

    public List<UserBrowseHistory> getBrowseHistory(Long userId, int limit) {
        return browseHistoryMapper.findByUserId(userId, limit);
    }

    public int getBrowseHistoryCount(Long userId) {
        return browseHistoryMapper.countByUserId(userId);
    }

    @Transactional
    public void clearBrowseHistory(Long userId) {
        browseHistoryMapper.deleteByUserId(userId);
    }
}