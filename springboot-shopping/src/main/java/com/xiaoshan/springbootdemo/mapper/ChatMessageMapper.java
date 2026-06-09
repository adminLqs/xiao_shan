package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息Mapper
 */
@Mapper
public interface ChatMessageMapper {

    /**
     * 插入消息
     */
    @Insert("INSERT INTO chat_messages (id, sender_id, receiver_id, content, message_type, " +
            "media_urls, media_duration, product_id, order_id, is_read, is_recalled, created_at) " +
            "VALUES (#{id}, #{senderId}, #{receiverId}, #{content}, #{messageType}, " +
            "#{mediaUrls}, #{mediaDuration}, #{productId}, #{orderId}, #{isRead}, #{isRecalled}, #{createdAt})")
    void insert(ChatMessage message);

    /**
     * 根据ID查询消息
     */
    @Select("SELECT * FROM chat_messages WHERE id = #{id}")
    ChatMessage findById(Long id);

    /**
     * 查询用户之间的消息（分页）
     */
    @Select("<script>" +
            "SELECT * FROM chat_messages WHERE " +
            "(sender_id = #{userId} AND receiver_id = #{targetId}) OR " +
            "(sender_id = #{targetId} AND receiver_id = #{userId}) " +
            "AND is_recalled = 0 " +
            "ORDER BY created_at DESC " +
            "<if test='pageSize != null'>LIMIT #{offset}, #{pageSize}</if>" +
            "</script>")
    List<ChatMessage> findMessagesBetweenUsers(
            @Param("userId") Long userId,
            @Param("targetId") Long targetId,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 查询用户之间消息总数
     */
    @Select("SELECT COUNT(*) FROM chat_messages WHERE " +
            "(sender_id = #{userId} AND receiver_id = #{targetId}) OR " +
            "(sender_id = #{targetId} AND receiver_id = #{userId}) " +
            "AND is_recalled = 0")
    int countMessagesBetweenUsers(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 查询用户之间的消息（按时间倒序，用于获取最新消息）
     */
    @Select("SELECT * FROM chat_messages WHERE " +
            "(sender_id = #{userId} AND receiver_id = #{targetId}) OR " +
            "(sender_id = #{targetId} AND receiver_id = #{userId}) " +
            "ORDER BY created_at DESC LIMIT 1")
    ChatMessage findLatestMessage(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 更新消息已读状态
     */
    @Update("UPDATE chat_messages SET is_read = 1 WHERE receiver_id = #{userId} AND sender_id = #{targetId} AND is_read = 0")
    void markMessagesAsRead(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 撤回消息
     */
    @Update("UPDATE chat_messages SET is_recalled = 1, recalled_at = #{recalledAt} WHERE id = #{messageId}")
    void recallMessage(@Param("messageId") Long messageId, @Param("recalledAt") LocalDateTime recalledAt);

    /**
     * 查询未读消息数量
     */
    @Select("SELECT COUNT(*) FROM chat_messages WHERE receiver_id = #{userId} AND is_read = 0 AND is_recalled = 0")
    int countUnreadMessages(@Param("userId") Long userId);

    /**
     * 查询与特定用户之间的未读消息数量
     */
    @Select("SELECT COUNT(*) FROM chat_messages WHERE receiver_id = #{userId} AND sender_id = #{targetId} AND is_read = 0 AND is_recalled = 0")
    int countUnreadMessagesWithTarget(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 删除消息（物理删除，仅管理员可用）
     */
    @Delete("DELETE FROM chat_messages WHERE id = #{id}")
    void deleteById(Long id);

    /**
     * 批量删除消息
     */
    @Delete("<script>" +
            "DELETE FROM chat_messages WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    void deleteByIds(@Param("ids") List<Long> ids);
}