package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.ReviewVideo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 评论视频Mapper
 */
@Mapper
public interface ReviewVideoMapper {

    /**
     * 插入评论视频
     * @param reviewVideo 评论视频实体
     * @return 影响行数
     */
    @Insert("INSERT INTO review_videos (id, review_id, video_url, cover_url, duration, size, sort_order) " +
            "VALUES (#{id}, #{reviewId}, #{videoUrl}, #{coverUrl}, #{duration}, #{size}, #{sortOrder})")
    int insert(ReviewVideo reviewVideo);

    /**
     * 根据评论ID查询视频列表
     * @param reviewId 评论ID
     * @return 视频列表
     */
    @Select("SELECT id, review_id, video_url, cover_url, duration, size, sort_order, created_at " +
            "FROM review_videos WHERE review_id = #{reviewId} ORDER BY sort_order ASC")
    List<ReviewVideo> findByReviewId(Long reviewId);

    /**
     * 批量根据评论ID列表查询视频列表
     * @param reviewIds 评论ID列表
     * @return 视频列表
     */
    @Select("<script>" +
            "SELECT id, review_id, video_url, cover_url, duration, size, sort_order, created_at " +
            "FROM review_videos WHERE review_id IN " +
            "<foreach collection='reviewIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY review_id ASC, sort_order ASC" +
            "</script>")
    List<ReviewVideo> findByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    /**
     * 根据视频ID查询
     * @param id 视频ID
     * @return 视频实体
     */
    @Select("SELECT id, review_id, video_url, cover_url, duration, size, sort_order, created_at " +
            "FROM review_videos WHERE id = #{id}")
    ReviewVideo findById(Long id);

    /**
     * 删除视频
     * @param id 视频ID
     * @return 影响行数
     */
    @Delete("DELETE FROM review_videos WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据评论ID删除所有视频
     * @param reviewId 评论ID
     * @return 影响行数
     */
    @Delete("DELETE FROM review_videos WHERE review_id = #{reviewId}")
    int deleteByReviewId(Long reviewId);

    /**
     * 根据商品ID删除所有评论视频
     * @param productId 商品ID
     * @return 影响行数
     */
    @Delete("DELETE rv FROM review_videos rv " +
            "JOIN reviews r ON rv.review_id = r.id " +
            "WHERE r.product_id = #{productId}")
    int deleteByProductId(Long productId);

    /**
     * 统计商品有视频的评价数量
     * @param productId 商品ID
     * @return 有视频的评价数量
     */
    @Select("SELECT COUNT(DISTINCT r.id) FROM review_videos rv " +
            "JOIN reviews r ON rv.review_id = r.id " +
            "WHERE r.product_id = #{productId}")
    long countByProductId(Long productId);

}
