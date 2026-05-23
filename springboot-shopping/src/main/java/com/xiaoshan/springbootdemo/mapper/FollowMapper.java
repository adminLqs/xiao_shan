package com.xiaoshan.springbootdemo.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户关注商家 Mapper
 */
@Mapper
public interface FollowMapper {

    /**
     * 添加关注（忽略重复插入）
     */
    @Insert("INSERT IGNORE INTO follow_sellers (user_id, seller_id) VALUES (#{userId}, #{sellerId})")
    int insert(@Param("userId") Long userId, @Param("sellerId") Long sellerId);

    /**
     * 取消关注
     */
    @Delete("DELETE FROM follow_sellers WHERE user_id = #{userId} AND seller_id = #{sellerId}")
    int delete(@Param("userId") Long userId, @Param("sellerId") Long sellerId);

    /**
     * 检查是否已关注
     */
    @Select("SELECT COUNT(*) FROM follow_sellers WHERE user_id = #{userId} AND seller_id = #{sellerId}")
    int count(@Param("userId") Long userId, @Param("sellerId") Long sellerId);
}
