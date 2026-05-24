package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.RefundVideo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RefundVideoMapper {

    @Insert("INSERT INTO refund_videos (id, refund_id, video_url, cover_url, duration, size, sort_order, created_at) " +
            "VALUES (#{id}, #{refundId}, #{videoUrl}, #{coverUrl}, #{duration}, #{size}, #{sortOrder}, #{createdAt})")
    int insert(RefundVideo refundVideo);

    @Insert("<script>" +
            "INSERT INTO refund_videos (id, refund_id, video_url, cover_url, duration, size, sort_order, created_at) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.refundId}, #{item.videoUrl}, #{item.coverUrl}, #{item.duration}, #{item.size}, #{item.sortOrder}, #{item.createdAt})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<RefundVideo> videos);

    @Select("SELECT id, refund_id, video_url, cover_url, duration, size, sort_order, created_at " +
            "FROM refund_videos WHERE refund_id = #{refundId} ORDER BY sort_order ASC")
    List<RefundVideo> findByRefundId(Long refundId);

    @Select("SELECT id, refund_id, video_url, cover_url, duration, size, sort_order, created_at " +
            "FROM refund_videos WHERE id = #{id}")
    RefundVideo findById(Long id);

    @Delete("DELETE FROM refund_videos WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM refund_videos WHERE refund_id = #{refundId}")
    int deleteByRefundId(Long refundId);

}