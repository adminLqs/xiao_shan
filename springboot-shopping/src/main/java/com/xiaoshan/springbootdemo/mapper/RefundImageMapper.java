package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.RefundImage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 退款图片 Mapper
 */
@Mapper
public interface RefundImageMapper {

    /**
     * 插入单张图片
     */
    @Insert("INSERT INTO refund_images (id, refund_id, communication_id, image, image_type, sort_order, created_at) " +
            "VALUES (#{id}, #{refundId}, #{communicationId}, #{image}, #{imageType}, #{sortOrder}, #{createdAt})")
    int insert(RefundImage refundImage);

    /**
     * 批量插入图片
     */
    @Insert("<script>" +
            "INSERT INTO refund_images (id, refund_id, communication_id, image, image_type, sort_order, created_at) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.refundId}, #{item.communicationId}, #{item.image}, #{item.imageType}, #{item.sortOrder}, #{item.createdAt})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<RefundImage> images);

    /**
     * 根据退款ID和图片类型查询图片列表
     */
    @Select("SELECT * FROM refund_images WHERE refund_id = #{refundId} AND image_type = #{imageType} ORDER BY sort_order")
    List<RefundImage> findByRefundIdAndType(@Param("refundId") Long refundId, @Param("imageType") String imageType);

    /**
     * 根据退款ID删除所有图片
     */
    @Delete("DELETE FROM refund_images WHERE refund_id = #{refundId}")
    int deleteByRefundId(Long refundId);

    /**
     * 根据退款ID查询所有图片
     */
    @Select("SELECT * FROM refund_images WHERE refund_id = #{refundId} ORDER BY image_type, sort_order")
    List<RefundImage> findByRefundId(Long refundId);

    /**
     * 根据图片ID列表批量查询图片
     */
    @Select("<script>" +
            "SELECT * FROM refund_images WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY id" +
            "</script>")
    List<RefundImage> findByIds(@Param("ids") List<Long> ids);

    /**
     * 根据消息ID查询图片列表
     */
    @Select("SELECT * FROM refund_images WHERE communication_id = #{communicationId} ORDER BY sort_order")
    List<RefundImage> findByCommunicationId(@Param("communicationId") Long communicationId);

    /**
     * 根据消息ID删除图片
     */
    @Delete("DELETE FROM refund_images WHERE communication_id = #{communicationId}")
    int deleteByCommunicationId(Long communicationId);
}
