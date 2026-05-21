package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.OrderItem;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface OrderItemMapper {

    // ==================== 插入方法 ====================

    /**
     * 插入单个订单项
     */
    @Insert("INSERT INTO order_items(order_id, product_id, product_name, quantity, " +
            "image, price, created_at) " +
            "VALUES(#{orderId}, #{productId}, #{productName}, #{quantity}, " +
            "#{image}, #{price}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderItem orderItem);

    /**
     * 批量插入订单项
     */
    @Insert("<script>" +
            "INSERT INTO order_items(order_id, product_id, product_name, quantity, " +
            "image, price, created_at) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.orderId}, #{item.productId}, #{item.productName}, #{item.quantity}, " +
            "#{item.image}, #{item.price}, #{item.createdAt})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<OrderItem> orderItems);

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询订单项
     */
    @Select("SELECT id, order_id, product_id, product_name, quantity, " +
            "image, price, created_at FROM order_items WHERE id = #{id}")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "order_id", property = "orderId"),
            @Result(column = "product_id", property = "productId"),
            @Result(column = "product_name", property = "productName"),
            @Result(column = "quantity", property = "quantity"),
            @Result(column = "image", property = "image"),
            @Result(column = "price", property = "price"),
            @Result(column = "created_at", property = "createdAt")
    })
    OrderItem findById(@Param("id") Long id);

    /**
     * 根据订单ID查询所有订单项
     */
    @Select("SELECT id, order_id, product_id, product_name, quantity, " +
            "image, price, created_at FROM order_items " +
            "WHERE order_id = #{orderId} ORDER BY id ASC")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "order_id", property = "orderId"),
            @Result(column = "product_id", property = "productId"),
            @Result(column = "product_name", property = "productName"),
            @Result(column = "quantity", property = "quantity"),
            @Result(column = "image", property = "image"),
            @Result(column = "price", property = "price"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据商品ID查询所有订单项
     */
    @Select("SELECT id, order_id, product_id, product_name, quantity, " +
            "image, price, created_at FROM order_items " +
            "WHERE product_id = #{productId} ORDER BY created_at DESC")
    List<OrderItem> findByProductId(@Param("productId") Long productId);

    /**
     * 查询订单的所有商品总数
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM order_items WHERE order_id = #{orderId}")
    Integer countTotalItemsByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询订单的商品种类数
     */
    @Select("SELECT COUNT(*) FROM order_items WHERE order_id = #{orderId}")
    Integer countProductTypesByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询订单的总金额
     */
    @Select("SELECT COALESCE(SUM(quantity * price), 0) FROM order_items WHERE order_id = #{orderId}")
    BigDecimal calculateOrderTotal(@Param("orderId") Long orderId);

    // ==================== 更新方法 ====================

    /**
     * 更新订单项数量
     */
    @Update("UPDATE order_items SET quantity = #{quantity} WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 更新订单项价格
     */
    @Update("UPDATE order_items SET price = #{price} WHERE id = #{id}")
    int updatePrice(@Param("id") Long id, @Param("price") BigDecimal price);

    /**
     * 更新订单项（全字段）
     */
    @Update("UPDATE order_items SET " +
            "product_id = #{productId}, " +
            "product_name = #{productName}, " +
            "quantity = #{quantity}, " +
            "image = #{image}, " +
            "price = #{price} " +
            "WHERE id = #{id}")
    int update(OrderItem orderItem);

    // ==================== 删除方法 ====================

    /**
     * 根据ID删除订单项
     */
    @Delete("DELETE FROM order_items WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /**
     * 根据订单ID删除所有订单项
     */
    @Delete("DELETE FROM order_items WHERE order_id = #{orderId}")
    int deleteByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据订单ID和商品ID删除指定订单项
     */
    @Delete("DELETE FROM order_items WHERE order_id = #{orderId} AND product_id = #{productId}")
    int deleteByOrderIdAndProductId(@Param("orderId") Long orderId, @Param("productId") Long productId);
}