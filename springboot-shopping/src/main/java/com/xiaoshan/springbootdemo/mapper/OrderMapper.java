package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Address;
import com.xiaoshan.springbootdemo.entity.Order;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface OrderMapper {

    // ========== 增 ==========

    /**
     * 创建订单
     */
    @Insert("INSERT INTO orders (id, order_number, user_id, address_id, total_amount, discount_amount, status, " +
            "source, payment_method, is_deleted, created_at, updated_at) " +
            "VALUES (#{id}, #{orderNumber}, #{userId}, #{addressId}, #{totalAmount}, #{discountAmount}, #{status}, " +
            "#{source}, #{paymentMethod}, 0, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = false, keyProperty = "id")
    int insert(Order order);

    // ========== 删 ==========

    /**
     * 软删除订单（标记删除）
     */
    @Update("UPDATE orders SET is_deleted = 1, updated_at = NOW() WHERE id = #{id} AND user_id = #{userId}")
    int softDeleteById(@Param("id") Long id, @Param("userId") Long userId);

    // ========== 查 ==========
    /**
     * 根据订单ID查询订单（内部使用，不校验用户权限）
     * 使用场景：支付回调、定时任务、系统内部调用
     *
     * @param id 订单ID
     * @return 订单信息
     */
    @Select("SELECT id, order_number, user_id, address_id, total_amount, discount_amount, status, source, " +
            "payment_method, transaction_id, paid_at, " +
            "tracking_number, logistics_code, logistics_name, " +
            "shipped_at, completed_at, processing_at, cancelled_at, created_at, updated_at " +
            "FROM orders WHERE id = #{id} AND is_deleted = 0")
    Optional<Order> findById(@Param("id") Long id);

    /**
     * 批量查询订单（根据订单ID列表）
     *
     * @param orderIds 订单ID列表
     * @return 订单列表
     */
    @Select("<script>" +
            "SELECT id, order_number, user_id, address_id, total_amount, discount_amount, status, source, " +
            "payment_method, transaction_id, paid_at, " +
            "tracking_number, logistics_code, logistics_name, " +
            "shipped_at, completed_at, processing_at, cancelled_at, created_at, updated_at " +
            "FROM orders WHERE id IN " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "ORDER BY created_at DESC" +
            "</script>")
    List<Order> findByIds(@Param("orderIds") List<Long> orderIds);

    // ---------------- 用户 ---------------
    /**
     * 根据用户ID查询订单列表（只查未删除）
     */
    @Select("SELECT id, order_number, user_id, address_id, total_amount, discount_amount, status, source, " +
            "payment_method, transaction_id, paid_at, " +
            "tracking_number, logistics_code, logistics_name, " +
            "shipped_at, completed_at, processing_at, cancelled_at, created_at, updated_at " +
            "FROM orders WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<Order> findByUserId(Long userId);



    /**
     * 根据订单ID和用户ID查询订单（用户端使用，校验权限）
     * 使用场景：用户查看订单详情、用户取消订单等
     *
     * @param id 订单ID
     * @param userId 用户ID（用于权限校验）
     * @return 订单信息
     */
    @Select("SELECT id, order_number, user_id, address_id, total_amount, discount_amount, status, source," +
            "payment_method, transaction_id, paid_at, " +
            "tracking_number, logistics_code, logistics_name, " +
            "shipped_at, completed_at, processing_at, cancelled_at, created_at, updated_at " +
            "FROM orders WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    Optional<Order> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 根据订单号查询订单（只查未删除）
     */
    @Select("SELECT id, order_number, user_id, address_id, total_amount, discount_amount, status, source," +
            "payment_method, transaction_id, paid_at, " +
            "tracking_number, logistics_code, logistics_name, " +
            "shipped_at, completed_at, processing_at, cancelled_at, created_at, updated_at " +
            "FROM orders WHERE order_number = #{orderNumber} AND is_deleted = 0")
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 查询超时未支付的订单
     * @param expireTime 超时时间点
     * @return 超时订单列表
     */
    @Select("<script>" +
            "SELECT id, order_number, user_id, address_id, total_amount, discount_amount, status, source, " +
            "payment_method, transaction_id, paid_at, " +
            "tracking_number, logistics_code, logistics_name, " +
            "shipped_at, completed_at, processing_at, cancelled_at, created_at, updated_at, is_deleted " +
            "FROM orders " +
            "WHERE status = 'PENDING' AND is_deleted = 0 AND created_at &lt; #{expireTime}" +
            "</script>")
    List<Order> findExpiredOrders(LocalDateTime expireTime);

    /**
     * 统计用户订单数量（只统计未删除）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM orders WHERE user_id = #{userId} AND is_deleted = 0 " +
            "<if test='status != null'>" +
            "<if test='status instanceof java.util.List'>" +
            "AND status IN <foreach collection='status' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</if>" +
            "<if test='status.getClass().isArray()'>" +
            "AND status IN <foreach collection='status' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</if>" +
            "</if>" +
            "</script>")
    long countByUserId(@Param("userId") Long userId, @Param("status") String[] status);

    /**
     * 查询用户订单ID列表（分页）
     * 根据用户ID查询订单ID列表，支持状态筛选
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @param status 订单状态
     * @return 订单ID列表
     */
    @Select("<script>" +
            "SELECT id " +
            "FROM orders " +
            "WHERE user_id = #{userId} " +
            "AND is_deleted = 0 " +
            "<if test='status != null'>" +
            "<if test='status instanceof java.util.List'>" +
            "AND status IN <foreach collection='status' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</if>" +
            "<if test='status.getClass().isArray()'>" +
            "AND status IN <foreach collection='status' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</if>" +
            "</if>" +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Long> findUserOrderIds(@Param("userId") Long userId,
                                @Param("offset") int offset,
                                @Param("limit") int limit,
                                @Param("status") String[] status);


    // ============= 商家 ===============

    @Select("<script>" +
            "<choose>" +
            "<when test='status != null'>" +
            "<choose>" +
            "<when test='(status instanceof java.util.List and status.contains(\"PROCESSING\")) or (status.getClass().isArray() and status.length > 0 and \"PROCESSING\" == status[0])'>" +
            "SELECT DISTINCT o.id " +
            "FROM orders o " +
            "INNER JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.seller_id = #{sellerId} " +
            "AND oi.refund_status IN ('PROCESSING', 'WAITING_RETURN', 'RETURNING') " +
            "ORDER BY o.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</when>" +
            "<otherwise>" +
            "SELECT o.id " +
            "FROM orders o " +
            "WHERE o.id IN (" +
            "  SELECT DISTINCT oi.order_id " +
            "  FROM order_items oi " +
            "  WHERE oi.seller_id = #{sellerId}" +
            ") " +
            "<if test='status instanceof java.util.List'>" +
            "AND o.status IN " +
            "<foreach collection='status' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</if>" +
            "<if test='status.getClass().isArray()'>" +
            "AND o.status IN " +
            "<foreach collection='status' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</if>" +
            "ORDER BY o.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</otherwise>" +
            "</choose>" +
            "</when>" +
            "<otherwise>" +
            "SELECT o.id " +
            "FROM orders o " +
            "WHERE o.id IN (" +
            "  SELECT DISTINCT oi.order_id " +
            "  FROM order_items oi " +
            "  WHERE oi.seller_id = #{sellerId}" +
            ") " +
            "ORDER BY o.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</otherwise>" +
            "</choose>" +
            "</script>")
    List<Long> findSellerOrderIds(@Param("sellerId") Long sellerId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit,
                                  @Param("status") String[] status);

    @Select("<script>" +
            "SELECT DISTINCT o.id " +
            "FROM orders o " +
            "JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.seller_id = #{sellerId} " +
            "AND o.is_deleted = 0 " +
            "AND oi.refund_status IN " +
            "<foreach collection='refundStatus' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "ORDER BY oi.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Long> findSellerOrderIdsByRefundStatus(@Param("sellerId") Long sellerId,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit,
                                                 @Param("refundStatus") String[] refundStatus);

    @Select("<script>" +
            "SELECT COUNT(DISTINCT o.id) " +
            "FROM orders o " +
            "JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.seller_id = #{sellerId} " +
            "AND o.is_deleted = 0 " +
            "AND oi.refund_status IN " +
            "<foreach collection='refundStatus' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</script>")
    long countSellerOrdersByRefundStatus(@Param("sellerId") Long sellerId,
                                         @Param("refundStatus") String[] refundStatus);


    /**
     * 统计商家订单数量
     *
     * @param sellerId 商家ID
     * @param status 订单状态
     * @return 订单总数
     */
    @Select("<script>" +
            "<choose>" +
            "<when test='status != null'>" +
            "<choose>" +
            "<when test='(status instanceof java.util.List and status.contains(\"PROCESSING\")) or (status.getClass().isArray() and status.length > 0 and \"PROCESSING\" == status[0])'>" +
            "SELECT COUNT(DISTINCT o.id) " +
            "FROM orders o " +
            "INNER JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.seller_id = #{sellerId} " +
            "AND oi.refund_status IN ('PROCESSING', 'WAITING_RETURN', 'RETURNING') " +
            "</when>" +
            "<otherwise>" +
            "SELECT COUNT(DISTINCT o.id) " +
            "FROM orders o " +
            "INNER JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.seller_id = #{sellerId} " +
            "<if test='status instanceof java.util.List'>" +
            "AND o.status IN " +
            "<foreach collection='status' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</if>" +
            "<if test='status.getClass().isArray()'>" +
            "AND o.status IN " +
            "<foreach collection='status' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</if>" +
            "</otherwise>" +
            "</choose>" +
            "</when>" +
            "<otherwise>" +
            "SELECT COUNT(DISTINCT o.id) " +
            "FROM orders o " +
            "INNER JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.seller_id = #{sellerId} " +
            "</otherwise>" +
            "</choose>" +
            "</script>")
    long countSellerOrders(@Param("sellerId") Long sellerId,
                           @Param("status") String[] status);


    /**
     * 统计商家各状态订单数量
     *
     * @param sellerId 商家ID
     * @return 各状态订单数量统计（PENDING、PAID、SHIPPED、COMPLETED、CANCELLED等）
     */
    @Select("SELECT o.status, COUNT(DISTINCT o.id) as count " +
            "FROM orders o " +
            "INNER JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.seller_id = #{sellerId} " +
            "GROUP BY o.status")
    List<Map<String, Object>> getSellerOrderCounts(Long sellerId);

    /**
     * 统计商家退款中的订单项数量
     *
     * @param sellerId 商家ID
     * @return 退款中的订单项数量
     */
    @Select("SELECT COUNT(*) FROM order_items oi " +
            "WHERE oi.seller_id = #{sellerId} " +
            "AND oi.refund_status IN ('PROCESSING', 'WAITING_RETURN', 'RETURNING')")
    Long countRefundingItems(Long sellerId);

    /**
     * 根据订单ID和商家ID查询订单（商家端使用，校验权限）
     * 使用场景：商家查看订单详情、处理订单、发货、取消订单等
     *
     * @param id 订单ID
     * @param sellerId 商家ID（用于权限校验）
     * @return 订单信息
     */
    @Select("SELECT o.id, o.order_number, o.user_id, o.address_id, o.total_amount, o.discount_amount, o.status, o.source, " +
            "o.payment_method, o.transaction_id, o.paid_at, " +
            "o.tracking_number, o.logistics_code, o.logistics_name, " +
            "o.shipped_at, o.completed_at, o.processing_at, o.cancelled_at, o.created_at, o.updated_at " +
            "FROM orders o " +
            "INNER JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE o.id = #{id} " +
            "AND oi.seller_id = #{sellerId} " +
            "GROUP BY o.id")
    Optional<Order> findByIdAndSellerId(@Param("id") Long id, @Param("sellerId") Long sellerId);

    /**
     * 根据订单ID查询关联的地址信息
     * 使用场景：商家端查询订单详情时获取收货地址
     *
     * @param orderId 订单ID
     * @return 地址信息
     */
    @Select("SELECT a.id, a.user_id, a.recipient_name, a.recipient_phone, " +
            "a.province, a.city, a.district, a.detail_address, " +
            "a.label, a.is_default, a.created_at, a.updated_at " +
            "FROM orders o " +
            "INNER JOIN addresses a ON o.address_id = a.id " +
            "WHERE o.id = #{orderId}")
    Optional<Address> findAddressByOrderId(Long orderId);

    // ============== 改 ============

    /**
     * 更新支付信息（支付成功后调用）
     * @param order 订单实体对象（包含 id, status, paymentMethod, transactionId, paidAt）
     */
    @Update("UPDATE orders SET status = #{status}, " +
            "payment_method = #{paymentMethod}, " +
            "transaction_id = #{transactionId}, " +
            "paid_at = #{paidAt}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int updatePaymentInfo(Order order);

    /**
     * 更新订单状态
     * @param id 订单ID
     * @param status 订单状态（CANCELLED/PAID/PENDING等）
     */
    @Update("UPDATE orders SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新订单处理时间
     * @param id 订单ID
     * @param processingAt 处理时间
     */
    @Update("UPDATE orders SET processing_at = #{processingAt}, updated_at = NOW() WHERE id = #{id}")
    int updateProcessingTime(@Param("id") Long id, @Param("processingAt") LocalDateTime processingAt);

    /**
     * 更新订单取消时间
     * @param id 订单ID
     * @param cancelledAt 取消时间
     */
    @Update("UPDATE orders SET cancelled_at = #{cancelledAt}, updated_at = NOW() WHERE id = #{id}")
    int updateCancelledTime(@Param("id") Long id, @Param("cancelledAt") LocalDateTime cancelledAt);

    /**
     * 确认收货 - 更新订单状态为COMPLETED并设置完成时间
     * @param id 订单ID
     */
    @Update("UPDATE orders SET status = 'COMPLETED', completed_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int confirmReceive(@Param("id") Long id);

    /**
     * 更新发货信息（商家发货时调用）
     *
     * @param order 订单实体对象（包含 id, trackingNumber, logisticsCode, logisticsName, status, shippedAt, updatedAt）
     * @return 影响行数
     */
    @Update("UPDATE orders SET " +
            "tracking_number = #{trackingNumber}, " +
            "logistics_code = #{logisticsCode}, " +
            "logistics_name = #{logisticsName}, " +
            "status = #{status}, " +
            "shipped_at = #{shippedAt}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateShipInfo(Order order);

    /**
     * 根据物流单号查询订单
     *
     * @param trackingNumber 物流单号
     * @return 订单信息
     */
    @Select("SELECT id, order_number, user_id, address_id, total_amount, discount_amount, status, source, " +
            "payment_method, transaction_id, paid_at, " +
            "tracking_number, logistics_code, logistics_name, " +
            "shipped_at, completed_at, processing_at, cancelled_at, created_at, updated_at " +
            "FROM orders WHERE tracking_number = #{trackingNumber} AND is_deleted = 0")
    Optional<Order> findByTrackingNumber(String trackingNumber);

    /**
     * 更新订单优惠金额和总金额
     *
     * @param id 订单ID
     * @param discountAmount 优惠金额
     * @param totalAmount 订单总金额
     * @return 影响行数
     */
    @Update("UPDATE orders SET discount_amount = #{discountAmount}, total_amount = #{totalAmount}, " +
            "updated_at = NOW() WHERE id = #{id}")
    int updateDiscountAmount(@Param("id") Long id,
                             @Param("discountAmount") BigDecimal discountAmount,
                             @Param("totalAmount") BigDecimal totalAmount);
}