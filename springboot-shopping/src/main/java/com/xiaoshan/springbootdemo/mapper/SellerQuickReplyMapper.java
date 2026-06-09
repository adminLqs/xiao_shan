package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.SellerQuickReply;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商家快捷回复Mapper
 */
@Mapper
public interface SellerQuickReplyMapper {

    /**
     * 插入快捷回复
     */
    @Insert("INSERT INTO seller_quick_replies (seller_id, content, sort_order, created_at, updated_at) " +
            "VALUES (#{sellerId}, #{content}, #{sortOrder}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SellerQuickReply reply);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM seller_quick_replies WHERE id = #{id}")
    SellerQuickReply findById(Long id);

    /**
     * 查询商家的所有快捷回复（按排序）
     */
    @Select("SELECT * FROM seller_quick_replies WHERE seller_id = #{sellerId} ORDER BY sort_order ASC")
    List<SellerQuickReply> findBySellerId(Long sellerId);

    /**
     * 更新快捷回复
     */
    @Update("UPDATE seller_quick_replies SET content = #{content}, sort_order = #{sortOrder}, updated_at = #{updatedAt} WHERE id = #{id}")
    void update(SellerQuickReply reply);

    /**
     * 删除快捷回复
     */
    @Delete("DELETE FROM seller_quick_replies WHERE id = #{id}")
    void deleteById(Long id);

    /**
     * 批量删除
     */
    @Delete("<script>" +
            "DELETE FROM seller_quick_replies WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    void deleteByIds(@Param("ids") List<Long> ids);
}