package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.RefundRecord;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款记录数据访问接口
 *
 * @author xiaoshan
 * @date 2026-05-07
 */
@Mapper
public interface RefundRecordMapper {

    /**
     * 创建退款记录
     */
    @Insert("INSERT INTO refund_records(order_number, refund_amount, refund_reason, " +
            "refund_transaction_id, status, fail_reason, created_at, completed_at) " +
            "VALUES(#{orderNumber}, #{refundAmount}, #{refundReason}, " +
            "#{refundTransactionId}, #{status}, #{failReason}, #{createdAt}, #{completedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RefundRecord refundRecord);

    /**
     * 根据ID查询退款记录
     */
    @Select("SELECT id, order_number, refund_amount, refund_reason, " +
            "refund_transaction_id, status, fail_reason, created_at, completed_at " +
            "FROM refund_records WHERE id = #{id}")
    RefundRecord findById(Long id);

    /**
     * 根据订单号查询所有退款记录
     */
    @Select("SELECT id, order_number, refund_amount, refund_reason, " +
            "refund_transaction_id, status, fail_reason, created_at, completed_at " +
            "FROM refund_records WHERE order_number = #{orderNumber} " +
            "ORDER BY created_at DESC")
    List<RefundRecord> findByOrderNumber(String orderNumber);

    /**
     * 根据订单号列表批量查询退款记录
     */
    @Select("<script>" +
            "SELECT id, order_number, refund_amount, refund_reason, " +
            "refund_transaction_id, status, fail_reason, created_at, completed_at " +
            "FROM refund_records " +
            "WHERE order_number IN " +
            "<foreach item='item' collection='list' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            " ORDER BY created_at DESC" +
            "</script>")
    List<RefundRecord> findByOrderNumbers(List<String> orderNumbers);

    /**
     * 更新退款状态为成功
     */
    @Update("UPDATE refund_records SET status = 'SUCCESS', " +
            "refund_transaction_id = #{refundTransactionId}, " +
            "completed_at = #{completedAt} " +
            "WHERE id = #{id}")
    int updateToSuccess(@Param("id") Long id,
                        @Param("refundTransactionId") String refundTransactionId,
                        @Param("completedAt") LocalDateTime completedAt);

    /**
     * 更新退款状态为失败（带失败原因）
     */
    @Update("UPDATE refund_records SET status = 'FAILED', " +
            "fail_reason = #{failReason}, " +
            "completed_at = #{completedAt} " +
            "WHERE id = #{id}")
    int updateToFailed(@Param("id") Long id,
                       @Param("failReason") String failReason,
                       @Param("completedAt") LocalDateTime completedAt);

    /**
     * 查询处理中的退款记录（用于定时任务重试）
     */
    @Select("SELECT id, order_number, refund_amount, refund_reason, " +
            "refund_transaction_id, status, fail_reason, created_at, completed_at " +
            "FROM refund_records WHERE status = 'PROCESSING' " +
            "AND created_at <= DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    List<RefundRecord> findProcessingTimeout(@Param("timeoutMinutes") int timeoutMinutes);
}