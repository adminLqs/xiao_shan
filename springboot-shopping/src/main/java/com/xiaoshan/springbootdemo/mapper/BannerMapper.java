package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Banner;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BannerMapper {

    @Insert("INSERT INTO banners (id, title, image_url, link_url, sort_order, status, start_time, end_time, position, created_at, updated_at) " +
            "VALUES (#{id}, #{title}, #{imageUrl}, #{linkUrl}, #{sortOrder}, #{status}, #{startTime}, #{endTime}, #{position}, #{createdAt}, #{updatedAt})")
    int insert(Banner banner);

    @Update("UPDATE banners SET title = #{title}, image_url = #{imageUrl}, link_url = #{linkUrl}, " +
            "sort_order = #{sortOrder}, status = #{status}, start_time = #{startTime}, " +
            "end_time = #{endTime}, position = #{position}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateById(Banner banner);

    @Delete("DELETE FROM banners WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM banners WHERE id = #{id}")
    Banner findById(Long id);

    @Select("SELECT * FROM banners ORDER BY sort_order, created_at DESC")
    List<Banner> findAll();

    @Select("SELECT * FROM banners WHERE status = 1 AND position = #{position} " +
            "AND (start_time IS NULL OR start_time <= NOW()) " +
            "AND (end_time IS NULL OR end_time >= NOW()) " +
            "ORDER BY sort_order, created_at DESC")
    List<Banner> findActiveByPosition(String position);

    @Select("SELECT * FROM banners WHERE position = #{position} ORDER BY sort_order, created_at DESC")
    List<Banner> findByPosition(String position);

    @Select("SELECT MAX(sort_order) FROM banners WHERE position = #{position}")
    Integer getMaxSortOrderByPosition(String position);

    @Update("UPDATE banners SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE banners SET sort_order = #{sortOrder} WHERE id = #{id}")
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);
}