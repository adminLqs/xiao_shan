package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.OrderRefund;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 退款记录数据访问层
 *
 * @author xiaoshan
 * @date 2026-04-14
 */
@Mapper
public interface OrderRefundMapper {

    // ========== 增（Create） ==========

    /**
     * 新增退款申请
     *
     * @param refund 退款记录
     * @return 影响行数
     */
    @Insert("INSERT INTO order_refunds (order_id, order_number, user_id, refund_amount, " +
            "refund_status, refund_reason, refund_transaction_id, apply_time, review_time, " +
            "complete_time, review_notes, reviewed_by) " +
            "VALUES (#{orderId}, #{orderNumber}, #{userId}, #{refundAmount}, " +
            "#{refundStatus}, #{refundReason}, #{refundTransactionId}, #{applyTime}, " +
            "#{reviewTime}, #{completeTime}, #{reviewNotes}, #{reviewedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderRefund refund);

    // ========== 删（Delete） ==========

    /**
     * 根据退款ID删除记录（物理删除，慎用）
     *
     * @param id 退款ID
     * @return 影响行数
     */
    @Delete("DELETE FROM order_refunds WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据订单ID删除退款记录
     *
     * @param orderId 订单ID
     * @return 影响行数
     */
    @Delete("DELETE FROM order_refunds WHERE order_id = #{orderId}")
    int deleteByOrderId(Long orderId);

    // ========== 改（Update） ==========

    /**
     * 更新退款记录
     *
     * @param refund 退款记录
     * @return 影响行数
     */
    @Update("UPDATE order_refunds SET " +
            "refund_amount = #{refundAmount}, " +
            "refund_status = #{refundStatus}, " +
            "refund_reason = #{refundReason}, " +
            "refund_transaction_id = #{refundTransactionId}, " +
            "review_time = #{reviewTime}, " +
            "complete_time = #{completeTime}, " +
            "review_notes = #{reviewNotes}, " +
            "reviewed_by = #{reviewedBy} " +
            "WHERE id = #{id}")
    int update(OrderRefund refund);

    /**
     * 更新退款状态
     *
     * @param id           退款ID
     * @param refundStatus 退款状态
     * @return 影响行数
     */
    @Update("UPDATE order_refunds SET refund_status = #{refundStatus}, " +
            "complete_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("refundStatus") String refundStatus);

    /**
     * 审核通过（退款成功）
     *
     * @param id           退款ID
     * @param reviewedBy   审核人ID
     * @param reviewNotes  审核备注
     * @return 影响行数
     */
    @Update("UPDATE order_refunds SET " +
            "refund_status = 'SUCCESS', " +
            "review_time = NOW(), " +
            "complete_time = NOW(), " +
            "review_notes = #{reviewNotes}, " +
            "reviewed_by = #{reviewedBy} " +
            "WHERE id = #{id}")
    int approve(@Param("id") Long id,
                @Param("reviewedBy") Long reviewedBy,
                @Param("reviewNotes") String reviewNotes);

    /**
     * 审核拒绝（退款失败）
     *
     * @param id           退款ID
     * @param reviewedBy   审核人ID
     * @param reviewNotes  审核备注
     * @return 影响行数
     */
    @Update("UPDATE order_refunds SET " +
            "refund_status = 'FAILED', " +
            "review_time = NOW(), " +
            "review_notes = #{reviewNotes}, " +
            "reviewed_by = #{reviewedBy} " +
            "WHERE id = #{id}")
    int reject(@Param("id") Long id,
               @Param("reviewedBy") Long reviewedBy,
               @Param("reviewNotes") String reviewNotes);

    // ========== 查（Select）- 基础查询 ==========

    /**
     * 根据退款ID查询
     *
     * @param id 退款ID
     * @return 退款记录
     */
    @Select("SELECT id, order_id, order_number, user_id, refund_amount, refund_status, " +
            "refund_reason, refund_transaction_id, apply_time, review_time, " +
            "complete_time, review_notes, reviewed_by " +
            "FROM order_refunds WHERE id = #{id}")
    Optional<OrderRefund> findById(Long id);

    /**
     * 根据订单ID查询退款记录
     *
     * @param orderId 订单ID
     * @return 退款记录
     */
    @Select("SELECT id, order_id, order_number, user_id, refund_amount, refund_status, " +
            "refund_reason, refund_transaction_id, apply_time, review_time, " +
            "complete_time, review_notes, reviewed_by " +
            "FROM order_refunds WHERE order_id = #{orderId}")
    Optional<OrderRefund> findByOrderId(Long orderId);

    /**
     * 根据订单号查询退款记录
     *
     * @param orderNumber 订单号
     * @return 退款记录
     */
    @Select("SELECT id, order_id, order_number, user_id, refund_amount, refund_status, " +
            "refund_reason, refund_transaction_id, apply_time, review_time, " +
            "complete_time, review_notes, reviewed_by " +
            "FROM order_refunds WHERE order_number = #{orderNumber}")
    Optional<OrderRefund> findByOrderNumber(String orderNumber);

    /**
     * 查询所有退款记录
     *
     * @return 退款记录列表
     */
    @Select("SELECT id, order_id, order_number, user_id, refund_amount, refund_status, " +
            "refund_reason, refund_transaction_id, apply_time, review_time, " +
            "complete_time, review_notes, reviewed_by " +
            "FROM order_refunds ORDER BY apply_time DESC")
    List<OrderRefund> findAll();

    // ========== 查（Select）- 按条件查询 ==========

    /**
     * 根据用户ID查询退款记录列表
     *
     * @param userId 用户ID
     * @return 退款记录列表
     */
    @Select("SELECT id, order_id, order_number, user_id, refund_amount, refund_status, " +
            "refund_reason, refund_transaction_id, apply_time, review_time, " +
            "complete_time, review_notes, reviewed_by " +
            "FROM order_refunds WHERE user_id = #{userId} ORDER BY apply_time DESC")
    List<OrderRefund> findByUserId(Long userId);

    /**
     * 根据退款状态查询退款记录列表
     *
     * @param refundStatus 退款状态
     * @return 退款记录列表
     */
    @Select("SELECT id, order_id, order_number, user_id, refund_amount, refund_status, " +
            "refund_reason, refund_transaction_id, apply_time, review_time, " +
            "complete_time, review_notes, reviewed_by " +
            "FROM order_refunds WHERE refund_status = #{refundStatus} ORDER BY apply_time DESC")
    List<OrderRefund> findByStatus(String refundStatus);

    /**
     * 分页查询退款记录（后台管理用）
     *
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 退款记录列表
     */
    @Select("SELECT id, order_id, order_number, user_id, refund_amount, refund_status, " +
            "refund_reason, refund_transaction_id, apply_time, review_time, " +
            "complete_time, review_notes, reviewed_by " +
            "FROM order_refunds ORDER BY apply_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<OrderRefund> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计退款记录总数
     *
     * @return 总数
     */
    @Select("SELECT COUNT(*) FROM order_refunds")
    long count();

    /**
     * 统计用户退款记录数量
     *
     * @param userId 用户ID
     * @return 总数
     */
    @Select("SELECT COUNT(*) FROM order_refunds WHERE user_id = #{userId}")
    long countByUserId(Long userId);
}