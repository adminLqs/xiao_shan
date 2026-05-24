package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 评论Mapper
 */
@Mapper
public interface ReviewMapper {

    /**
     * 插入评论
     * @param review 评论实体
     * @return 影响行数
     */
    @Insert("INSERT INTO reviews (id, user_id, product_id, order_id, order_item_id, rating, comment, sku_spec, ip, location) " +
            "VALUES (#{id}, #{userId}, #{productId}, #{orderId}, #{orderItemId}, #{rating}, #{comment}, #{skuSpec}, #{ip}, #{location})")
    int insert(Review review);

    /**
     * 根据ID查询评论
     * @param id 评论ID
     * @return 评论实体
     */
    @Select("SELECT id, user_id, product_id, order_id, order_item_id, rating, comment, sku_spec, ip, location, created_at " +
            "FROM reviews WHERE id = #{id}")
    Review findById(Long id);

    /**
     * 根据订单项ID查询评论
     * @param orderItemId 订单项ID
     * @return 评论实体
     */
    @Select("SELECT id, user_id, product_id, order_id, order_item_id, rating, comment, sku_spec, ip, location, created_at " +
            "FROM reviews WHERE order_item_id = #{orderItemId}")
    Review findByOrderItemId(Long orderItemId);

    /**
     * 查询商品评价列表（分页）
     * @param productId 商品ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @param rating 评分筛选（精确匹配）
     * @param minRating 最小评分（范围筛选）
     * @param maxRating 最大评分（范围筛选）
     * @param hasImages 是否只查询有图片的评论
     * @return 评论列表
     */
    @Select("<script>" +
            "SELECT r.id, r.user_id, r.product_id, r.order_id, r.order_item_id, r.rating, r.comment, r.sku_spec, r.ip, r.location, r.created_at, " +
            "p.name as product_name, " +
            "(SELECT image FROM product_images WHERE product_id = r.product_id ORDER BY sort_order LIMIT 1) as product_image, " +
            "oi.sku_name " +
            "FROM reviews r " +
            "LEFT JOIN products p ON r.product_id = p.id " +
            "LEFT JOIN order_items oi ON r.order_item_id = oi.id " +
            "<if test='hasImages != null and hasImages'>" +
            "JOIN review_images ri ON r.id = ri.review_id " +
            "</if>" +
            "WHERE r.product_id = #{productId} " +
            "<if test='rating != null'>AND r.rating = #{rating}</if> " +
            "<if test='minRating != null'>AND r.rating <![CDATA[ >= ]]> #{minRating}</if> " +
            "<if test='maxRating != null'>AND r.rating <![CDATA[ <= ]]> #{maxRating}</if> " +
            "GROUP BY r.id " +
            "ORDER BY r.created_at DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Review> findByProductId(@Param("productId") Long productId,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit,
                                 @Param("rating") Integer rating,
                                 @Param("minRating") Integer minRating,
                                 @Param("maxRating") Integer maxRating,
                                 @Param("hasImages") Boolean hasImages);

    /**
     * 统计商品评价数量
     * @param productId 商品ID
     * @param rating 评分筛选（精确匹配）
     * @param minRating 最小评分（范围筛选）
     * @param maxRating 最大评分（范围筛选）
     * @return 评价数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM reviews r " +
            "WHERE r.product_id = #{productId} " +
            "<if test='rating != null'>AND r.rating = #{rating}</if> " +
            "<if test='minRating != null'>AND r.rating <![CDATA[ >= ]]> #{minRating}</if> " +
            "<if test='maxRating != null'>AND r.rating <![CDATA[ <= ]]> #{maxRating}</if>" +
            "</script>")
    long countByProductId(@Param("productId") Long productId,
                          @Param("rating") Integer rating,
                          @Param("minRating") Integer minRating,
                          @Param("maxRating") Integer maxRating);

    /**
     * 查询用户评价列表（分页）
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 评论列表
     */
    @Select("SELECT r.id, r.user_id, r.product_id, r.order_id, r.order_item_id, r.rating, r.comment, r.sku_spec, r.ip, r.location, r.created_at, " +
            "p.name as product_name, " +
            "(SELECT image FROM product_images WHERE product_id = r.product_id ORDER BY sort_order LIMIT 1) as product_image, " +
            "oi.sku_name " +
            "FROM reviews r " +
            "LEFT JOIN products p ON r.product_id = p.id " +
            "LEFT JOIN order_items oi ON r.order_item_id = oi.id " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Review> findByUserId(@Param("userId") Long userId,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    /**
     * 统计用户评价数量
     * @param userId 用户ID
     * @return 评价数量
     */
    @Select("SELECT COUNT(*) FROM reviews WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    /**
     * 查询商家评价列表（分页）
     * @param sellerId 商家ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 评论列表
     */
    @Select("SELECT r.id, r.user_id, r.product_id, r.order_item_id, r.rating, r.comment, r.ip, r.location, r.created_at, " +
            "p.name as product_name, " +
            "(SELECT image FROM product_images WHERE product_id = r.product_id ORDER BY sort_order LIMIT 1) as product_image, " +
            "oi.sku_name " +
            "FROM reviews r " +
            "LEFT JOIN products p ON r.product_id = p.id " +
            "LEFT JOIN order_items oi ON r.order_item_id = oi.id " +
            "WHERE p.seller_id = #{sellerId} " +
            "ORDER BY r.created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Review> findBySellerId(@Param("sellerId") Long sellerId,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    /**
     * 统计商家评价数量
     * @param sellerId 商家ID
     * @return 评价数量
     */
    @Select("SELECT COUNT(*) FROM reviews r " +
            "JOIN products p ON r.product_id = p.id " +
            "WHERE p.seller_id = #{sellerId}")
    long countBySellerId(Long sellerId);

    /**
     * 更新评论
     * @param review 评论实体
     * @return 影响行数
     */
    @Update("UPDATE reviews SET rating = #{rating}, comment = #{comment} WHERE id = #{id}")
    int update(Review review);

    /**
     * 删除评论
     * @param id 评论ID
     * @return 影响行数
     */
    @Delete("DELETE FROM reviews WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据商品ID删除所有评论
     * @param productId 商品ID
     * @return 影响行数
     */
    @Delete("DELETE FROM reviews WHERE product_id = #{productId}")
    int deleteByProductId(Long productId);
}