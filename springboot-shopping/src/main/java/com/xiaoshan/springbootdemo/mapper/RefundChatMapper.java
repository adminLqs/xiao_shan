package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.RefundChat;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 退款聊天记录Mapper
 */
@Mapper
public interface RefundChatMapper {

    /**
     * 插入聊天记录
     */
    @Insert("INSERT INTO refund_communications (id, refund_id, sender_type, sender_id, content, round, created_at) " +
            "VALUES (#{id}, #{refundId}, #{senderType}, #{senderId}, #{message}, #{round}, #{sendTime})")
    void insert(RefundChat chat);

    /**
     * 根据退款ID查询聊天记录（按时间正序），包含发送者头像和名称
     */
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "refundId", column = "refund_id"),
        @Result(property = "senderType", column = "sender_type"),
        @Result(property = "senderId", column = "sender_id"),
        @Result(property = "senderName", column = "sender_name"),
        @Result(property = "senderAvatar", column = "sender_avatar"),
        @Result(property = "message", column = "message"),
        @Result(property = "round", column = "round"),
        @Result(property = "sendTime", column = "send_time")
    })
    @Select("SELECT " +
            "  c.id, c.refund_id, c.sender_type, c.sender_id, " +
            "  c.content as message, c.round, c.created_at as send_time, " +
            "  CASE " +
            "    WHEN c.sender_type = 'SYSTEM' THEN '📢' " +
            "    WHEN c.sender_type = 'BUYER' THEN up.avatar " +
            "    WHEN c.sender_type = 'SELLER' THEN sp.store_avatar " +
            "  END as sender_avatar, " +
            "  CASE " +
            "    WHEN c.sender_type = 'SYSTEM' THEN '系统' " +
            "    WHEN c.sender_type = 'BUYER' THEN up.nickname " +
            "    WHEN c.sender_type = 'SELLER' THEN sp.store_name " +
            "  END as sender_name " +
            "FROM refund_communications c " +
            "LEFT JOIN user_profiles up ON c.sender_type = 'BUYER' AND c.sender_id = up.user_id " +
            "LEFT JOIN seller_profiles sp ON c.sender_type = 'SELLER' AND c.sender_id = sp.user_id " +
            "WHERE c.refund_id = #{refundId} " +
            "ORDER BY c.created_at ASC")
    List<RefundChat> findByRefundId(Long refundId);

    /**
     * 根据退款ID统计消息数量
     */
    @Select("SELECT COUNT(*) FROM refund_communications WHERE refund_id = #{refundId}")
    Long countByRefundId(Long refundId);

    /**
     * 根据ID查询聊天记录
     */
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "refundId", column = "refund_id"),
        @Result(property = "senderType", column = "sender_type"),
        @Result(property = "senderId", column = "sender_id"),
        @Result(property = "senderName", column = "sender_name"),
        @Result(property = "senderAvatar", column = "sender_avatar"),
        @Result(property = "message", column = "message"),
        @Result(property = "round", column = "round"),
        @Result(property = "sendTime", column = "send_time")
    })
    @Select("SELECT " +
            "  c.id, c.refund_id, c.sender_type, c.sender_id, " +
            "  c.content as message, c.round, c.created_at as send_time, " +
            "  CASE " +
            "    WHEN c.sender_type = 'SYSTEM' THEN '📢' " +
            "    WHEN c.sender_type = 'BUYER' THEN up.avatar " +
            "    WHEN c.sender_type = 'SELLER' THEN sp.store_avatar " +
            "  END as sender_avatar, " +
            "  CASE " +
            "    WHEN c.sender_type = 'SYSTEM' THEN '系统' " +
            "    WHEN c.sender_type = 'BUYER' THEN up.nickname " +
            "    WHEN c.sender_type = 'SELLER' THEN sp.store_name " +
            "  END as sender_name " +
            "FROM refund_communications c " +
            "LEFT JOIN user_profiles up ON c.sender_type = 'BUYER' AND c.sender_id = up.user_id " +
            "LEFT JOIN seller_profiles sp ON c.sender_type = 'SELLER' AND c.sender_id = sp.user_id " +
            "WHERE c.id = #{id}")
    RefundChat findById(Long id);

    /**
     * 删除聊天记录
     */
    @Delete("DELETE FROM refund_communications WHERE id = #{id}")
    void delete(Long id);

    /**
     * 根据退款ID删除所有聊天记录
     */
    @Delete("DELETE FROM refund_communications WHERE refund_id = #{refundId}")
    void deleteByRefundId(Long refundId);
}