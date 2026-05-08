package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Favorite;
import com.xiaoshan.springbootdemo.entity.vo.FavoriteVO;
import com.xiaoshan.springbootdemo.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;

    /**
     * 添加收藏
     */
    @Transactional
    public void addFavorite(Long userId, Long productId) {
        // 检查是否已收藏
        int count = favoriteMapper.countByUserIdAndProductId(userId, productId);
        if (count > 0) {
            throw new RuntimeException("商品已收藏");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);

        log.info("添加收藏成功: userId={}, productId={}", userId, productId);
    }

    /**
     * 取消收藏
     */
    @Transactional
    public void removeFavorite(Long userId, Long favoriteId) {
        int deleted = favoriteMapper.deleteByIdAndUserId(favoriteId, userId);
        if (deleted == 0) {
            throw new RuntimeException("收藏不存在");
        }
    }

    /**
     * 批量取消收藏
     */
    @Transactional
    public void batchRemoveFavorites(Long userId, List<Long> favoriteIds) {
        // 验证所有收藏是否属于当前用户
        for (Long id : favoriteIds) {
            boolean exists = favoriteMapper.existsByIdAndUserId(id, userId);
            if (!exists) {
                throw new RuntimeException("收藏不存在或无权操作: " + id);
            }
        }

        int deleted = favoriteMapper.batchDeleteByIds(favoriteIds);
        log.info("批量取消收藏成功: userId={}, 删除数量={}", userId, deleted);
    }

    /**
     * 检查是否已收藏
     */
    public boolean isFavorited(Long userId, Long productId) {
        return favoriteMapper.countByUserIdAndProductId(userId, productId) > 0;
    }

    /**
     * 获取用户收藏列表（分页）- 用户端使用
     */
    public List<FavoriteVO> getUserFavorites(Long userId, int offset, int limit) {
        return favoriteMapper.findFavoritesWithProduct(userId, offset, limit);
    }

    /**
     * 获取用户所有收藏（不分页）- 管理后台使用
     */
    public List<FavoriteVO> getAllFavorites(Long userId) {
        return favoriteMapper.findAllFavoritesWithProduct(userId);
    }

    /**
     * 获取用户收藏商品ID列表
     */
    public List<Long> getUserFavoriteProductIds(Long userId) {
        return favoriteMapper.findProductIdsByUserId(userId);
    }

    /**
     * 统计用户收藏数量
     */
    public int getFavoriteCount(Long userId) {
        return favoriteMapper.countByUserId(userId);
    }
}