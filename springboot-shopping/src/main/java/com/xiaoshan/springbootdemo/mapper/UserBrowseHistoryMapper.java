package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.UserBrowseHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserBrowseHistoryMapper {

    @Insert("INSERT INTO user_browse_history (user_id, product_id, product_name, product_image, product_price, browse_time) " +
            "VALUES (#{userId}, #{productId}, #{productName}, #{productImage}, #{productPrice}, NOW())")
    int insert(UserBrowseHistory history);

    @Delete("DELETE FROM user_browse_history WHERE user_id = #{userId} AND product_id = #{productId}")
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Delete("DELETE FROM user_browse_history WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Select("SELECT * FROM user_browse_history WHERE user_id = #{userId} ORDER BY browse_time DESC LIMIT #{limit}")
    List<UserBrowseHistory> findByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_browse_history WHERE user_id = #{userId}")
    int countByUserId(Long userId);
}