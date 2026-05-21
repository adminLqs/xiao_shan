package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Order;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单数据访问接口
 * 提供订单的增删改查操作
 *
 * @author xiaoshan
 * @date 2026-05-08
 */
@Mapper
public interface OrderMapper {

    // ==================== 插入方法 ====================

    /**
     * 创建订单
     * 插入一条新的订单记录，支持所有配送类型（到店用餐/打包自取/外卖配送）。
     *
     * @param order 订单实体对象
     * @return 受影响的行数
     */
    @Insert("INSERT INTO orders(user_id, order_number, total_amount, delivery_type, " +
            "recipient_name, recipient_phone, detail_address, people_count, " +
            "table_preference, remark, status, refunded_amount, created_at, updated_at) " +
            "VALUES(#{userId}, #{orderNumber}, #{totalAmount}, #{deliveryType}, " +
            "#{recipientName}, #{recipientPhone}, #{detailAddress}, #{peopleCount}, " +
            "#{tablePreference}, #{remark}, #{status}, #{refundedAmount}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(Order order);

    // ==================== 查询方法 ====================

    /**
     * 根据订单号查询订单详情
     *
     * @param orderNumber 订单号
     * @return 订单实体，不存在时返回 null
     */
    @Select("SELECT id, user_id, order_number, total_amount, delivery_type, " +
            "recipient_name, recipient_phone, detail_address, people_count, " +
            "table_preference, remark, payment_method, transaction_id, paid_at, " +
            "status, refunded_amount, created_at, updated_at " +
            "FROM orders WHERE order_number = #{orderNumber}")
    Order findByOrderNumber(String orderNumber);

    /**
     * 根据ID查询订单
     *
     * @param id 订单ID
     * @return 订单实体
     */
    @Select("SELECT id, user_id, order_number, total_amount, delivery_type, " +
            "recipient_name, recipient_phone, detail_address, people_count, " +
            "table_preference, remark, payment_method, transaction_id, paid_at, " +
            "status, refunded_amount, created_at, updated_at " +
            "FROM orders WHERE id = #{id}")
    Order findById(Long id);

    /**
     * 分页查询用户订单列表
     *
     * @param userId   用户ID
     * @param offset   偏移量
     * @param pageSize 每页记录数
     * @return 订单列表
     */
    @Select("<script>" +
            "SELECT id, user_id, order_number, total_amount, delivery_type, " +
            "recipient_name, recipient_phone, detail_address, people_count, " +
            "table_preference, remark, payment_method, transaction_id, paid_at, " +
            "status, refunded_amount, created_at, updated_at " +
            "FROM orders WHERE user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Order> findByUserIdWithPage(@Param("userId") Long userId,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize,
                                     @Param("status") String status);

    /**
     * 统计用户订单总数
     *
     * @param userId 用户ID
     * @return 订单总数
     */
    @Select("SELECT COUNT(*) FROM orders WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 分页查询所有订单（商家用）
     *
     * @param offset   偏移量
     * @param pageSize 每页记录数
     * @param status   订单状态（可选）
     * @return 订单列表
     */
    @Select("<script>" +
            "SELECT id, user_id, order_number, total_amount, delivery_type, " +
            "recipient_name, recipient_phone, detail_address, people_count, " +
            "table_preference, remark, payment_method, transaction_id, paid_at, " +
            "status, refunded_amount, created_at, updated_at " +
            "FROM orders " +
            "<where>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "</where>" +
            "ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Order> findAllWithPage(@Param("offset") int offset,
                                @Param("pageSize") int pageSize,
                                @Param("status") String status);

    /**
     * 统计所有订单总数（商家用）
     *
     * @param status 订单状态（可选）
     * @return 订单总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM orders " +
            "<where>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "</where>" +
            "</script>")
    int countAll(@Param("status") String status);

    /**
     * 查询超时的待支付订单
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 超时订单列表
     */
    @Select("SELECT id, user_id, order_number, total_amount, delivery_type, " +
            "recipient_name, recipient_phone, detail_address, people_count, " +
            "table_preference, remark, payment_method, transaction_id, paid_at, " +
            "status, refunded_amount, created_at, updated_at " +
            "FROM orders WHERE status = 'PENDING' " +
            "AND created_at <= DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    List<Order> findTimeoutPendingOrders(@Param("timeoutMinutes") int timeoutMinutes);

    /**
     * 查询待支付的订单数量
     *
     * @return 待支付订单数量
     */
    @Select("SELECT COUNT(*) FROM orders WHERE status = 'PENDING'")
    int countPendingOrders();

    /**
     * 查询已支付的订单数量
     *
     * @return 已支付订单数量
     */
    @Select("SELECT COUNT(*) FROM orders WHERE status = 'PAID'")
    int countPaidOrders();

    // ==================== 更新方法 ====================

    /**
     * 支付成功更新
     * <p>
     * 支付回调时调用，更新订单状态为已支付，记录支付方式和交易号。
     * 同时更新 updated_at 时间戳。
     *
     * @param orderNumber   订单号
     * @param paymentMethod 支付方式（WECHAT/ALIPAY）
     * @param transactionId 支付平台交易号
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET status = 'PAID', payment_method = #{paymentMethod}, " +
            "transaction_id = #{transactionId}, paid_at = NOW(), updated_at = NOW() " +
            "WHERE order_number = #{orderNumber} AND status = 'PENDING'")
    int updateOrderPaySuccess(@Param("orderNumber") String orderNumber,
                              @Param("paymentMethod") String paymentMethod,
                              @Param("transactionId") String transactionId);

    /**
     * 确认收货
     * <p>
     * 用户确认收货，将订单状态从 SHIPPED 更新为 COMPLETED。
     *
     * @param orderNumber 订单号
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET status = 'COMPLETED', updated_at = NOW() " +
            "WHERE order_number = #{orderNumber} AND status = 'SHIPPED'")
    int confirmReceipt(String orderNumber);


    /**
     * 用户取消订单
     * <p>
     * 用户在订单详情页取消未支付的订单。
     * 仅当订单状态为待支付（PENDING）时才能取消。
     *
     * @param orderNumber 订单号
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET status = 'CANCELLED', updated_at = NOW() " +
            "WHERE order_number = #{orderNumber} AND status = 'PENDING'")
    int cancelOrder(String orderNumber);

    /**
     * 更新订单为已完成（核销）
     * <p>
     * 用户到店用餐或取餐时，商家扫码核销。
     *
     * @param orderNumber 订单号
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET status = 'COMPLETED', updated_at = NOW() " +
            "WHERE order_number = #{orderNumber} AND status = 'PAID'")
    int completeOrder(String orderNumber);

    /**
     * 更新订单为已发货（外卖场景）
     *
     * @param orderNumber 订单号
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET status = 'SHIPPED', updated_at = NOW() " +
            "WHERE order_number = #{orderNumber} AND status = 'PAID'")
    int shipOrder(String orderNumber);

    /**
     * 更新订单为退款中
     *
     * @param orderNumber 订单号
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET status = 'REFUNDING', updated_at = NOW() " +
            "WHERE order_number = #{orderNumber} AND status = 'PAID'")
    int updateToRefunding(String orderNumber);

    /**
     * 更新订单为已退款
     *
     * @param orderNumber 订单号
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET status = 'REFUNDED', updated_at = NOW() " +
            "WHERE order_number = #{orderNumber}")
    int updateToRefunded(String orderNumber);

    /**
     * 更新订单为退款失败
     * <p>
     * 退款处理失败时调用，将订单状态从 REFUNDING 更新为 REFUNDFAILED。
     * 使用乐观锁条件，确保只有退款中的订单才能更新。
     *
     * @param orderNumber 订单号
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET status = 'REFUNDFAILED', updated_at = NOW() " +
            "WHERE order_number = #{orderNumber} AND status = 'REFUNDING'")
    int updateToRefundFailed(String orderNumber);

    /**
     * 批量取消超时订单
     * <p>
     * 定时任务调用，批量取消超过指定时间未支付的订单。
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 取消的订单数量
     */
    @Update("UPDATE orders SET status = 'CANCELLED', updated_at = NOW() " +
            "WHERE status = 'PENDING' " +
            "AND created_at <= DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    int cancelTimeoutOrders(@Param("timeoutMinutes") int timeoutMinutes);

    /**
     * 更新订单状态（商家用）
     * <p>
     * 商家将已支付的订单标记为已完成，或将已完成的订单标记为其他状态。
     * 注意：此方法不校验状态流转合法性，调用前需自行校验。
     *
     * @param orderNumber 订单号
     * @param status      新状态
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET status = #{status}, updated_at = NOW() " +
            "WHERE order_number = #{orderNumber}")
    int updateOrderStatus(@Param("orderNumber") String orderNumber,
                          @Param("status") String status);



    /**
     * 更新订单退款汇总信息
     * <p>
     * 退款成功时调用，更新已退款总额。
     *
     * @param orderNumber    订单号
     * @param refundedAmount 已退款总额
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET refunded_amount = #{refundedAmount}, updated_at = NOW() " +
            "WHERE order_number = #{orderNumber}")
    int updateRefundedAmount(@Param("orderNumber") String orderNumber,
                             @Param("refundedAmount") BigDecimal refundedAmount);

    /**
     * 根据退款总额自动更新退款状态
     * <p>
     * 当 refunded_amount 等于 total_amount 时自动变为全额退款。
     *
     * @param orderNumber 订单号
     * @return 受影响的行数
     */
    @Update("UPDATE orders SET " +
            "status = CASE WHEN refunded_amount >= total_amount THEN 'REFUNDED' ELSE status END, " +
            "updated_at = NOW() " +
            "WHERE order_number = #{orderNumber}")
    int autoUpdateRefundStatus(String orderNumber);

    // ==================== 删除方法（慎用） ====================

    /**
     * 根据ID物理删除订单
     * <p>
     * 直接从数据库中删除订单记录，不可恢复。
     * 建议使用状态变更代替物理删除。
     *
     * @param id 订单ID
     * @return 受影响的行数
     */
    @Delete("DELETE FROM orders WHERE id = #{id}")
    int deleteById(Long id);
}