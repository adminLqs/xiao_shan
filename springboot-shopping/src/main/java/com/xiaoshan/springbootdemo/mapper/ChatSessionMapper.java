package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.ChatSession;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话Mapper
 */
@Mapper
public interface ChatSessionMapper {

    /**
     * 插入会话
     */
    @Insert("INSERT INTO chat_sessions (id, user_id, target_id, last_message, last_message_time, " +
            "unread_count, is_top, is_muted, created_at, updated_at) " +
            "VALUES (#{id}, #{userId}, #{targetId}, #{lastMessage}, #{lastMessageTime}, " +
            "#{unreadCount}, #{isTop}, #{isMuted}, #{createdAt}, #{updatedAt})")
    void insert(ChatSession session);

    /**
     * 根据ID查询会话
     */
    @Select("SELECT * FROM chat_sessions WHERE id = #{id}")
    ChatSession findById(Long id);

    /**
     * 根据用户ID和目标用户ID查询会话
     */
    @Select("SELECT * FROM chat_sessions WHERE user_id = #{userId} AND target_id = #{targetId}")
    ChatSession findByUserAndTarget(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 查询用户的所有会话（按置顶和更新时间排序）
     */
    @Select("SELECT * FROM chat_sessions WHERE user_id = #{userId} ORDER BY is_top DESC, updated_at DESC")
    List<ChatSession> findByUserId(Long userId);

    /**
     * 更新会话最后消息
     */
    @Update("UPDATE chat_sessions SET last_message = #{lastMessage}, last_message_time = #{lastMessageTime}, " +
            "updated_at = #{updatedAt} WHERE user_id = #{userId} AND target_id = #{targetId}")
    void updateLastMessage(@Param("userId") Long userId, @Param("targetId") Long targetId,
                           @Param("lastMessage") String lastMessage,
                           @Param("lastMessageTime") LocalDateTime lastMessageTime,
                           @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 增加未读消息数
     */
    @Update("UPDATE chat_sessions SET unread_count = unread_count + 1, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND target_id = #{targetId}")
    void incrementUnreadCount(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 清空未读消息数
     */
    @Update("UPDATE chat_sessions SET unread_count = 0, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND target_id = #{targetId}")
    void clearUnreadCount(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 设置会话置顶
     */
    @Update("UPDATE chat_sessions SET is_top = #{isTop}, updated_at = NOW() WHERE id = #{id}")
    void updateTopStatus(@Param("id") Long id, @Param("isTop") Boolean isTop);

    /**
     * 设置会话免打扰
     */
    @Update("UPDATE chat_sessions SET is_muted = #{isMuted}, updated_at = NOW() WHERE id = #{id}")
    void updateMuteStatus(@Param("id") Long id, @Param("isMuted") Boolean isMuted);

    /**
     * 删除会话
     */
    @Delete("DELETE FROM chat_sessions WHERE id = #{id}")
    void deleteById(Long id);

    /**
     * 根据用户ID和目标用户ID删除会话
     */
    @Delete("DELETE FROM chat_sessions WHERE user_id = #{userId} AND target_id = #{targetId}")
    void deleteByUserAndTarget(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 获取用户未读消息总数
     */
    @Select("SELECT COALESCE(SUM(unread_count), 0) FROM chat_sessions WHERE user_id = #{userId}")
    int sumUnreadCount(Long userId);
}