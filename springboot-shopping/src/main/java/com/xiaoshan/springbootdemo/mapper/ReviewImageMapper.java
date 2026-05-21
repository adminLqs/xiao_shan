package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.ReviewImage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 评论图片Mapper
 */
@Mapper
public interface ReviewImageMapper {

    /**
     * 插入评论图片
     * @param reviewImage 评论图片实体
     * @return 影响行数
     */
    @Insert("INSERT INTO review_images (review_id, image, sort_order, created_at) " +
            "VALUES (#{reviewId}, #{image}, #{sortOrder}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReviewImage reviewImage);

    /**
     * 根据评论ID查询图片列表
     * @param reviewId 评论ID
     * @return 图片列表
     */
    @Select("SELECT id, review_id, image, sort_order, created_at " +
            "FROM review_images WHERE review_id = #{reviewId} ORDER BY sort_order ASC")
    List<ReviewImage> findByReviewId(Long reviewId);

    /**
     * 批量根据评论ID列表查询图片列表
     * @param reviewIds 评论ID列表
     * @return 图片列表
     */
    @Select("<script>" +
            "SELECT id, review_id, image, sort_order, created_at " +
            "FROM review_images WHERE review_id IN " +
            "<foreach collection='reviewIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY review_id ASC, sort_order ASC" +
            "</script>")
    List<ReviewImage> findByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    /**
     * 根据图片ID查询
     * @param id 图片ID
     * @return 图片实体
     */
    @Select("SELECT id, review_id, image, sort_order, created_at FROM review_images WHERE id = #{id}")
    ReviewImage findById(Long id);

    /**
     * 删除图片
     * @param id 图片ID
     * @return 影响行数
     */
    @Delete("DELETE FROM review_images WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据评论ID删除所有图片
     * @param reviewId 评论ID
     * @return 影响行数
     */
    @Delete("DELETE FROM review_images WHERE review_id = #{reviewId}")
    int deleteByReviewId(Long reviewId);
}