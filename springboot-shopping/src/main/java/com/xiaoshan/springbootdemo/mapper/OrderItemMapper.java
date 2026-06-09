package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface OrderItemMapper {

    // ========== 增 ==========

    /**
     * 批量插入订单项
     *
     * @param items 订单项列表
     * @return 影响行数
     */
    @Insert("<script>" +
            "INSERT INTO order_items (id, order_id, product_id, seller_id, product_name, product_image, " +
            "quantity, price, total_price, is_reviewed, created_at, sku_id, sku_name) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.id}, #{item.orderId}, #{item.productId}, #{item.sellerId}, #{item.productName}, #{item.productImage}, " +
            "#{item.quantity}, #{item.price}, #{item.totalPrice}, #{item.isReviewed}, #{item.createdAt}, " +
            "#{item.skuId}, #{item.skuName})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("items") List<OrderItem> items);

    // ========== 查 ==========

    /**
     * 根据ID查询订单项
     *
     * @param id 订单项ID
     * @return 订单项
     */
    @Select("SELECT id, order_id, product_id, seller_id, product_name, product_image, " +
            "quantity, price, total_price, is_reviewed, reviewed_at, refund_status, created_at, " +
            "sku_name as skuName " +
            "FROM order_items WHERE id = #{id}")
    Optional<OrderItem> findById(Long id);

    /**
     * 根据订单ID查询订单项列表
     *
     * @param orderId 订单ID
     * @return 订单项列表
     */
    @Select("SELECT id, order_id, product_id, seller_id, product_name, product_image, " +
            "quantity, price, total_price, is_reviewed, reviewed_at, refund_status, created_at, " +
            "sku_name as skuName " +
            "FROM order_items WHERE order_id = #{orderId}")
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 根据订单ID列表批量查询订单项
     *
     * @param orderIds 订单ID列表
     * @return 订单项列表
     */
    @Select("<script>" +
            "SELECT id, order_id, product_id, seller_id, product_name, product_image, " +
            "quantity, price, total_price, is_reviewed, reviewed_at, refund_status, created_at, " +
            "sku_name as skuName " +
            "FROM order_items WHERE order_id IN " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<OrderItem> findByOrderIds(@Param("orderIds") List<Long> orderIds);

    /**
     * 根据订单ID列表批量查询订单项
     * 用于获取订单下的所有商品信息
     *
     * @param orderIds 订单ID列表
     * @return 订单项列表
     */
    @Select("<script>" +
            "SELECT id, order_id, product_id, seller_id, " +
            "product_name, product_image, quantity, price, total_price, " +
            "is_reviewed, reviewed_at, refund_status, created_at, " +
            "sku_name as skuName " +
            "FROM order_items " +
            "WHERE order_id IN " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "ORDER BY order_id ASC, id ASC" +
            "</script>")
    List<OrderItem> findOrderItemsByOrderIds(@Param("orderIds") List<Long> orderIds);

    /**
     * 查询订单中未评论的订单项
     *
     * @param orderId 订单ID
     * @return 未评论的订单项列表
     */
    @Select("SELECT id, order_id, product_id, seller_id, product_name, product_image, " +
            "quantity, price, total_price, is_reviewed, reviewed_at, refund_status, created_at, " +
            "sku_name as skuName " +
            "FROM order_items WHERE order_id = #{orderId} AND is_reviewed = 0")
    List<OrderItem> findUnreviewedByOrderId(Long orderId);

    /**
     * 查询用户所有未评论的订单项（用于评价中心）
     *
     * @param userId 用户ID
     * @return 未评论的订单项列表
     */
    @Select("SELECT oi.id, oi.order_id, oi.product_id, oi.seller_id, oi.product_name, " +
            "oi.product_image, oi.quantity, oi.price, oi.total_price, " +
            "oi.is_reviewed, oi.reviewed_at, oi.refund_status, oi.created_at, " +
            "oi.sku_name as skuName " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.user_id = #{userId} AND oi.is_reviewed = 0 " +
            "ORDER BY o.created_at DESC")
    List<OrderItem> findUnreviewedByUserId(Long userId);

    /**
     * 统计用户已完成订单中未评价的订单项数量
     *
     * @param userId 用户ID
     * @return 未评价的订单项数量
     */
    @Select("SELECT COUNT(*) FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.user_id = #{userId} AND o.status = 'COMPLETED' AND oi.is_reviewed = 0")
    Long countPendingReviewByUserId(Long userId);

    /**
     * 查询用户已完成订单中未评价的订单项（分页）
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 订单项列表
     */
    @Select("SELECT oi.id as orderItemId, oi.product_id as productId, " +
            "oi.product_name as productName, oi.product_image as productImage, " +
            "oi.sku_name as skuName, oi.order_id as orderId, oi.is_reviewed as isReviewed " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.user_id = #{userId} AND o.status = 'COMPLETED' AND oi.is_reviewed = 0 " +
            "ORDER BY o.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findPendingReviewItems(@Param("userId") Long userId,
                                                     @Param("offset") int offset,
                                                     @Param("limit") int limit);

    /**
     * 查询用户已评价的订单项（分页）
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 订单项列表
     */
    @Select("SELECT oi.id as orderItemId, oi.product_id as productId, " +
            "oi.product_name as productName, oi.product_image as productImage, " +
            "oi.sku_name as skuName, oi.order_id as orderId, oi.is_reviewed as isReviewed, " +
            "r.rating, r.comment, r.created_at as createdAt " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.id " +
            "LEFT JOIN reviews r ON oi.id = r.order_item_id " +
            "WHERE o.user_id = #{userId} AND oi.is_reviewed = 1 " +
            "ORDER BY r.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findReviewedItems(@Param("userId") Long userId, 
                                                @Param("offset") int offset, 
                                                @Param("limit") int limit);

    /**
     * 统计用户已评价的订单项数量
     *
     * @param userId 用户ID
     * @return 已评价的订单项数量
     */
    @Select("SELECT COUNT(*) FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.user_id = #{userId} AND oi.is_reviewed = 1")
    Long countReviewedByUserId(Long userId);

    /**
     * 查询订单下未退款完成的订单项数量
     *
     * @param orderId 订单ID
     * @return 未退款完成的订单项数量
     */
    @Select("SELECT COUNT(*) FROM order_items WHERE order_id = #{orderId} AND (refund_status IS NULL OR refund_status != 'SUCCESS')")
    int countUnrefundedByOrderId(Long orderId);

    // ========== 改 ==========

    /**
     * 更新订单项评论状态
     *
     * @param id 订单项ID
     * @param status 评论状态
     * @return 影响行数
     */
    @Update("UPDATE order_items SET is_reviewed = #{status}, reviewed_at = NOW() WHERE id = #{id}")
    int updateReviewedStatus(Long id, boolean status);

    /**
     * 批量更新订单项评论状态
     *
     * @param ids 订单项ID列表
     * @return 影响行数
     */
    @Update("<script>" +
            "UPDATE order_items SET is_reviewed = 1, reviewed_at = NOW() " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateReviewedStatus(@Param("ids") List<Long> ids);

    /**
     * 恢复订单项的库存
     * 根据订单项ID查找对应的SKU，将库存数量加回
     *
     * @param orderItemId 订单项ID
     * @return 影响行数
     */
    @Update("UPDATE product_skus SET stock = stock + (SELECT quantity FROM order_items WHERE id = #{orderItemId}) " +
            "WHERE id = (SELECT sku_id FROM order_items WHERE id = #{orderItemId})")
    int restoreStock(@Param("orderItemId") Long orderItemId);

    /**
     * 更新订单项售后状态
     *
     * @param id 订单项ID
     * @param refundStatus 售后状态
     * @return 影响行数
     */
    @Update("UPDATE order_items SET refund_status = #{refundStatus} WHERE id = #{id}")
    int updateRefundStatus(@Param("id") Long id, @Param("refundStatus") String refundStatus);

    /**
     * 根据ID更新订单项
     *
     * @param orderItem 订单项对象
     * @return 影响行数
     */
    @Update("UPDATE order_items SET " +
            "refund_status = #{refundStatus} " +
            "WHERE id = #{id}")
    int updateById(OrderItem orderItem);

    /**
     * 根据SKU ID计算预扣库存（订单状态为 PENDING, PAID, PROCESSING, SHIPPED 的商品数量之和）
     *
     * @param skuId SKU ID
     * @return 预扣库存数量
     */
    @Select("SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.id " +
            "WHERE oi.sku_id = #{skuId} AND o.status IN ('PENDING', 'PAID', 'PROCESSING', 'SHIPPED')")
    Long sumReservedBySkuId(Long skuId);
}