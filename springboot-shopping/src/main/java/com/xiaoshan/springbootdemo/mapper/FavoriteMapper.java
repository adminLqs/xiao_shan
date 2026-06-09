package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Favorite;
import com.xiaoshan.springbootdemo.entity.vo.FavoriteVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FavoriteMapper {

    // ========== 增 ==========

    /**
     * 添加收藏
     */
    @Insert("INSERT INTO favorites (id, user_id, product_id, created_at) VALUES (#{id}, #{userId}, #{productId}, #{createdAt})")
    int insert(Favorite favorite);

    // ========== 删 ==========

    /**
     * 取消收藏（根据用户ID和收藏ID）
     */
    @Delete("DELETE FROM favorites WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 取消收藏（根据用户ID和商品ID）
     */
    @Delete("DELETE FROM favorites WHERE user_id = #{userId} AND product_id = #{productId}")
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 批量删除收藏
     */
    @Delete("<script>" +
            "DELETE FROM favorites WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据商品ID删除收藏中的该商品（删除所有用户的收藏）
     */
    @Delete("DELETE FROM favorites WHERE product_id = #{productId}")
    int deleteByProductId(Long productId);

    // ========== 查 ==========

    /**
     * 检查是否已收藏
     */
    @Select("SELECT COUNT(*) FROM favorites WHERE user_id = #{userId} AND product_id = #{productId}")
    int countByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 检查收藏是否存在且属于当前用户
     */
    @Select("SELECT COUNT(*) > 0 FROM favorites WHERE id = #{id} AND user_id = #{userId}")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 分页查询用户收藏列表（关联商品信息）
     */
    @Select("SELECT f.id, f.user_id, f.product_id, f.created_at, " +
            "p.name as product_name, p.brand, p.status as product_status, " +
            "(SELECT MIN(ps.price) FROM product_skus ps WHERE ps.product_id = p.id) as price, " +
            "(SELECT MIN(ps.original_price) FROM product_skus ps WHERE ps.product_id = p.id) as original_price, " +
            "(SELECT COALESCE(SUM(ps.stock), 0) FROM product_skus ps WHERE ps.product_id = p.id) as stock, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as product_image, " +
            "(SELECT ps.sku_name FROM product_skus ps WHERE ps.product_id = p.id ORDER BY ps.price ASC LIMIT 1) as sku_name, " +
            "COALESCE(sp.store_name, up.nickname) as seller_name, " +
            "COALESCE(sp.store_avatar, up.avatar) as seller_avatar " +
            "FROM favorites f " +
            "LEFT JOIN products p ON f.product_id = p.id " +
            "LEFT JOIN seller_profiles sp ON p.seller_id = sp.user_id " +
            "LEFT JOIN user_profiles up ON p.seller_id = up.user_id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<FavoriteVO> findFavoritesWithProduct(@Param("userId") Long userId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    /**
     * 查询用户收藏商品ID列表
     */
    @Select("SELECT product_id FROM favorites WHERE user_id = #{userId}")
    List<Long> findProductIdsByUserId(Long userId);

    /**
     * 查询用户所有收藏（不分页）- 管理后台使用
     */
    @Select("SELECT f.id, f.user_id, f.product_id, f.created_at, " +
            "p.name as product_name, p.brand, " +
            "(SELECT MIN(ps.price) FROM product_skus ps WHERE ps.product_id = p.id) as price, " +
            "(SELECT MIN(ps.original_price) FROM product_skus ps WHERE ps.product_id = p.id) as original_price, " +
            "(SELECT COALESCE(SUM(ps.stock), 0) FROM product_skus ps WHERE ps.product_id = p.id) as stock, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as product_image, " +
            "(SELECT ps.sku_name FROM product_skus ps WHERE ps.product_id = p.id ORDER BY ps.price ASC LIMIT 1) as sku_name, " +
            "COALESCE(sp.store_name, up.nickname) as seller_name, " +
            "COALESCE(sp.store_avatar, up.avatar) as seller_avatar " +
            "FROM favorites f " +
            "LEFT JOIN products p ON f.product_id = p.id " +
            "LEFT JOIN seller_profiles sp ON p.seller_id = sp.user_id " +
            "LEFT JOIN user_profiles up ON p.seller_id = up.user_id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.created_at DESC")
    List<FavoriteVO> findAllFavoritesWithProduct(Long userId);

    /**
     * 统计用户收藏数量
     */
    @Select("SELECT COUNT(*) FROM favorites WHERE user_id = #{userId}")
    int countByUserId(Long userId);
}