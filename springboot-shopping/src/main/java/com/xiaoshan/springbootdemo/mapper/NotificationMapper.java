package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface NotificationMapper {
    int insert(Notification notification);
    
    List<Notification> findByUserId(@Param("userId") Long userId,
        @Param("type") String type,
        @Param("offset") int offset,
        @Param("pageSize") int pageSize);
    
    int countByUserId(@Param("userId") Long userId, @Param("type") String type);
    
    int countUnread(@Param("userId") Long userId);
    
    int markAsRead(@Param("id") Long id);
    
    int markAllAsRead(@Param("userId") Long userId);
    
    @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND type = #{type} AND is_read = 0")
    int countUnreadByType(@Param("userId") Long userId, @Param("type") String type);
    
    @Select("SELECT content FROM notifications WHERE user_id = #{userId} AND type = #{type} " +
            "ORDER BY created_at DESC LIMIT 1")
    String getLatestContentByType(@Param("userId") Long userId, @Param("type") String type);
}