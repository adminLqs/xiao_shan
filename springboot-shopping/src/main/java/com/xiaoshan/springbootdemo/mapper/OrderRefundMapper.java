package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.OrderRefund;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface OrderRefundMapper {

    // 插入退款记录
    @Insert("INSERT INTO order_refunds (id, order_id, order_item_id, order_number, user_id, " +
            "refund_amount, refund_status, refund_reason, refund_type, " +
            "description, apply_time) " +
            "VALUES (#{id}, #{orderId}, #{orderItemId}, #{orderNumber}, #{userId}, " +
            "#{refundAmount}, #{refundStatus}, #{refundReason}, #{refundType}, " +
            "#{description}, #{applyTime})")
    int insert(OrderRefund refund);

    // 根据ID查询
    @Select("SELECT * FROM order_refunds WHERE id = #{id}")
    Optional<OrderRefund> findById(Long id);

    // 根据订单ID查询
    @Select("SELECT * FROM order_refunds WHERE order_id = #{orderId}")
    List<OrderRefund> findByOrderId(Long orderId);

    // 根据订单项ID查询退款记录
    @Select("SELECT * FROM order_refunds WHERE order_item_id = #{orderItemId} ORDER BY apply_time DESC")
    List<OrderRefund> findByOrderItemId(Long orderItemId);

    // 鏍规嵁鐢ㄦ埛ID鏌ヨ閫€娆捐褰?
    @Select("SELECT * FROM order_refunds WHERE user_id = #{userId} ORDER BY apply_time DESC")
    List<OrderRefund> findByUserId(Long userId);

    // 鍒嗛〉鏌ヨ鐢ㄦ埛閫€娆捐褰?
    @Select("SELECT * FROM order_refunds WHERE user_id = #{userId} ORDER BY apply_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<OrderRefund> findByUserIdWithPage(@Param("userId") Long userId, 
                                           @Param("offset") int offset, 
                                           @Param("limit") int limit);

    // 缁熻鐢ㄦ埛閫€娆捐褰曟€绘暟
    @Select("SELECT COUNT(*) FROM order_refunds WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    // 缁熻鐢ㄦ埛杩涜涓殑閫€娆捐褰曟€绘暟
    @Select("SELECT COUNT(*) FROM order_refunds WHERE user_id = #{userId} AND refund_status IN ('PROCESSING', 'APPROVED') OR (refund_status = 'PROCESSING' AND return_status = 'RETURNING')")
    int countPendingByUserId(Long userId);

    // 更新退款状态
    @Update("UPDATE order_refunds SET " +
            "refund_status = #{refundStatus}, " +
            "review_time = #{reviewTime}, " +
            "reviewed_by = #{reviewedBy}, " +
            "review_notes = #{reviewNotes}, " +
            "refund_transaction_id = #{refundTransactionId}, " +
            "communication_round = #{communicationRound} " +
            "WHERE id = #{id}")
    int updateStatus(OrderRefund refund);

    // 更新退货信息（不修改 refund_status）
    @Update("UPDATE order_refunds SET " +
            "return_method = #{returnMethod}, " +
            "return_logistics_name = #{returnLogisticsName}, " +
            "return_tracking_number = #{returnTrackingNumber}, " +
            "return_status = #{returnStatus}, " +
            "return_apply_time = NOW() " +
            "WHERE id = #{id}")
    int updateReturnInfo(OrderRefund refund);

    // 确认收货并完成退款
    @Update("UPDATE order_refunds SET " +
            "return_status = #{returnStatus}, " +
            "return_receive_time = NOW(), " +
            "refund_status = #{refundStatus}, " +
            "complete_time = NOW() " +
            "WHERE id = #{id}")
    int confirmReceive(OrderRefund refund);

    // 更新退款状态为成功
    @Update("UPDATE order_refunds SET " +
            "refund_status = 'SUCCESS', " +
            "complete_time = #{completeTime}, " +
            "refund_transaction_id = #{refundTransactionId} " +
            "WHERE id = #{id}")
    int markAsSuccess(@Param("id") Long id,
                     @Param("completeTime") String completeTime,
                     @Param("refundTransactionId") String refundTransactionId);

    // 检查订单是否有退款记录
    @Select("SELECT COUNT(*) > 0 FROM order_refunds WHERE order_id = #{orderId}")
    boolean existsByOrderId(Long orderId);

    // 查询所有退款记录（管理员用）
    @Select("SELECT * FROM order_refunds ORDER BY apply_time DESC")
    List<OrderRefund> findAll();

    // 分页查询退款记录（管理员用）
    @Select("<script>" +
            "SELECT * FROM order_refunds " +
            "<where>" +
            "   <if test='status != null and status != \"\"'>AND refund_status = #{status}</if>" +
            "   <if test='keyword != null and keyword != \"\"'>" +
            "       AND (order_number LIKE CONCAT('%', #{keyword}, '%'))" +
            "   </if>" +
            "</where>" +
            "ORDER BY apply_time DESC LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<OrderRefund> findWithPagination(
            @Param("pageSize") int pageSize,
            @Param("offset") int offset,
            @Param("status") String status,
            @Param("keyword") String keyword);

    // 统计退款记录总数
    @Select("<script>" +
            "SELECT COUNT(*) FROM order_refunds " +
            "<where>" +
            "   <if test='status != null and status != \"\"'>AND refund_status = #{status}</if>" +
            "   <if test='keyword != null and keyword != \"\"'>" +
            "       AND (order_number LIKE CONCAT('%', #{keyword}, '%'))" +
            "   </if>" +
            "</where>" +
            "</script>")
    int countWithFilters(
            @Param("status") String status,
            @Param("keyword") String keyword);
}
