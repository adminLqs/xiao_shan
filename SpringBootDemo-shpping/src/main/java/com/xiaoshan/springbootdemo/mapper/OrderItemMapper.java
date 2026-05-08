package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;
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
            "INSERT INTO order_items (order_id, product_id, seller_id, product_name, product_image, " +
            "quantity, price, total_price, is_reviewed, created_at) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.orderId}, #{item.productId}, #{item.sellerId}, #{item.productName}, #{item.productImage}, " +
            "#{item.quantity}, #{item.price}, #{item.totalPrice}, #{item.isReviewed}, #{item.createdAt})" +
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
            "quantity, price, total_price, is_reviewed, reviewed_at, created_at " +
            "FROM order_items WHERE id = #{id}")
    Optional<OrderItem> findById(Long id);

    /**
     * 根据订单ID查询订单项列表
     *
     * @param orderId 订单ID
     * @return 订单项列表
     */
    @Select("SELECT id, order_id, product_id, seller_id, product_name, product_image, " +
            "quantity, price, total_price, is_reviewed, reviewed_at, created_at " +
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
            "quantity, price, total_price, is_reviewed, reviewed_at, created_at " +
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
            "is_reviewed, reviewed_at, created_at " +
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
            "quantity, price, total_price, is_reviewed, reviewed_at, created_at " +
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
            "oi.is_reviewed, oi.reviewed_at, oi.created_at " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.user_id = #{userId} AND oi.is_reviewed = 0 " +
            "ORDER BY o.created_at DESC")
    List<OrderItem> findUnreviewedByUserId(Long userId);

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
}