package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper {

    /**
     * 保存消息
     */
    @Insert("INSERT INTO chat_messages(sender_id, receiver_id, content, is_read, created_at) " +
            "VALUES(#{senderId}, #{receiverId}, #{content}, #{isRead}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);

    /**
     * 获取与指定用户的聊天记录（用户视角）
     * @param userId 用户ID
     */
    @Select("SELECT id, sender_id, receiver_id, content, is_read, created_at " +
            "FROM chat_messages " +
            "WHERE sender_id = #{userId} OR receiver_id = #{userId} " +
            "ORDER BY created_at ASC")
    List<ChatMessage> findConversationByUser(@Param("userId") Long userId);

    /**
     * 获取商家会话列表（按用户分组）
     */
    @Select("SELECT " +
            "  sender_id as userId, " +
            "  COUNT(*) as totalMessages, " +
            "  SUM(CASE WHEN is_read = 0 THEN 1 ELSE 0 END) as unreadCount, " +
            "  (SELECT content FROM chat_messages cm2 " +
            "   WHERE cm2.sender_id = cm1.sender_id " +
            "   ORDER BY cm2.created_at DESC LIMIT 1) as lastContent, " +
            "  (SELECT created_at FROM chat_messages cm2 " +
            "   WHERE cm2.sender_id = cm1.sender_id " +
            "   ORDER BY cm2.created_at DESC LIMIT 1) as lastTime " +
            "FROM chat_messages cm1 " +
            "WHERE sender_id IS NOT NULL " +
            "GROUP BY sender_id " +
            "ORDER BY lastTime DESC")
    List<Map<String, Object>> findSellerConversations();

    /**
     * 标记为已读（商家查看后）
     */
    @Update("UPDATE chat_messages SET is_read = 1 " +
            "WHERE is_read = 0 AND (sender_id = #{userId} OR receiver_id = #{userId})")
    int markAsRead(@Param("userId") Long userId);

    /**
     * 标记所有消息为已读（商家全部已读）
     */
    @Update("UPDATE chat_messages SET is_read = 1 WHERE is_read = 0")
    int markAllAsRead();

    /**
     * 获取未读消息总数（商家侧）
     */
    @Select("SELECT COUNT(*) FROM chat_messages WHERE is_read = 0")
    int countUnread();
}