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
    @Insert("INSERT INTO reviews (user_id, product_id, order_item_id, rating, comment, created_at) " +
            "VALUES (#{userId}, #{productId}, #{orderItemId}, #{rating}, #{comment}, NOW() )")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Review review);

    /**
     * 根据ID查询评论
     * @param id 评论ID
     * @return 评论实体
     */
    @Select("SELECT id, user_id, product_id, order_item_id, rating, comment, created_at " +
            "FROM reviews WHERE id = #{id}")
    Review findById(Long id);

    /**
     * 根据订单项ID查询评论
     * @param orderItemId 订单项ID
     * @return 评论实体
     */
    @Select("SELECT id, user_id, product_id, order_item_id, rating, comment, created_at " +
            "FROM reviews WHERE order_item_id = #{orderItemId}")
    Review findByOrderItemId(Long orderItemId);

    /**
     * 查询商品评价列表（分页）
     * @param productId 商品ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @param rating 评分筛选
     * @return 评论列表
     */
    @Select("<script>" +
            "SELECT id, user_id, product_id, order_item_id, rating, comment, created_at " +
            "FROM reviews WHERE product_id = #{productId} " +
            "<if test='rating != null'>AND rating = #{rating}</if> " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Review> findByProductId(@Param("productId") Long productId,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit,
                                 @Param("rating") Integer rating);

    /**
     * 统计商品评价数量
     * @param productId 商品ID
     * @param rating 评分筛选
     * @return 评价数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM reviews WHERE product_id = #{productId} " +
            "<if test='rating != null'>AND rating = #{rating}</if>" +
            "</script>")
    long countByProductId(@Param("productId") Long productId,
                          @Param("rating") Integer rating);

    /**
     * 查询用户评价列表（分页）
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 评论列表
     */
    @Select("SELECT id, product_id, order_item_id, rating, comment, created_at " +
            "FROM reviews WHERE user_id = #{userId} ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
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
}