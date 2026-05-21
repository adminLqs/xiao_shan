package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.CartItem;
import com.xiaoshan.springbootdemo.entity.vo.CheckoutItemVO;
import org.apache.ibatis.annotations.*;
import com.xiaoshan.springbootdemo.entity.vo.CartItemVO;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CartItemMapper {

    // ========== 增 ==========

    /**
     * 添加购物车项
     */
    @Insert("INSERT INTO cart_items (user_id, product_id, quantity, added_at) " +
            "VALUES (#{userId}, #{productId}, #{quantity}, #{addedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CartItem cartItem);

    // ========== 查 ==========

    /**
     * 根据ID查询购物车项
     */
    @Select("SELECT id, user_id, product_id, quantity, added_at " +
            "FROM cart_items WHERE id = #{id}")
    Optional<CartItem> findById(Long id);

    /**
     * 根据用户ID查询购物车列表（关联商品信息）
     * 返回包含商品名称、价格、图片等完整信息
     */
    @Select("SELECT " +
            "ci.id, ci.user_id, ci.product_id, ci.quantity, ci.added_at, " +
            "p.name as product_name, p.brand, p.price, p.original_price, p.stock, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as product_image " +
            "FROM cart_items ci " +
            "LEFT JOIN products p ON ci.product_id = p.id " +
            "WHERE ci.user_id = #{userId} " +
            "ORDER BY ci.added_at DESC")
    List<CartItemVO> findCartItemsWithProduct(Long userId);

    /**
     * 根据用户ID查询购物车列表（基础信息）
     */
    @Select("SELECT id, user_id, product_id, quantity, added_at " +
            "FROM cart_items WHERE user_id = #{userId} ORDER BY added_at DESC")
    List<CartItem> findByUserId(Long userId);

    /**
     * 根据用户ID和商品ID查询购物车项
     */
    @Select("SELECT id, user_id, product_id, quantity, added_at " +
            "FROM cart_items WHERE user_id = #{userId} AND product_id = #{productId}")
    Optional<CartItem> findByUserIdAndProductId(@Param("userId") Long userId,
                                                @Param("productId") Long productId);

    /**
     * 根据ID和用户ID查询购物车项（用于权限校验）
     */
    @Select("SELECT id, user_id, product_id, quantity, added_at " +
            "FROM cart_items WHERE id = #{id} AND user_id = #{userId}")
    Optional<CartItem> findByIdAndUserId(@Param("id") Long id,
                                         @Param("userId") Long userId);



    /**
     * 统计用户购物车商品总数量
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE user_id = #{userId}")
    Integer countQuantityByUserId(Long userId);

    /**
     * 获取购物车结算项（联表查询）
     *
     * SQL查询逻辑：
     * 1. 从购物车表（cart_items）获取用户选中的商品
     * 2. 左连接商品表（products）获取商品详细信息（名称、品牌、价格）
     * 3. 子查询商品图片表（product_images）获取第一张图片作为主图
     *
     * @param userId 用户ID，用于权限校验（确保只能查询自己的购物车）
     * @param cartItemIds 购物车项ID列表，使用 IN 查询
     * @return 结算商品信息列表
     */
    @Select("SELECT " +
            "ci.id as cartItemId, " +                    // 购物车项ID
            "ci.product_id as productId, " +             // 商品ID
            "ci.quantity, " +                            // 购买数量（来自购物车）
            "p.name as productName, " +                  // 商品名称
            "p.brand, " +                                // 商品品牌
            "p.price, " +                                // 商品价格
            "p.original_price as originalPrice, " +      // 商品原价
            "p.stock, " +                                // 商品库存
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as productImage " +  // 商品主图
            "FROM cart_items ci " +                      // 购物车表
            "LEFT JOIN products p ON ci.product_id = p.id " +  // 左连接商品表
            "WHERE ci.id IN (${cartItemIds}) AND ci.user_id = #{userId}")  // 条件：ID在列表中且属于当前用户
    List<CheckoutItemVO> getCheckoutItems(@Param("userId") Long userId,
                                          @Param("cartItemIds") String cartItemIds);

    // ========== 改 ==========

    /**
     * 更新购物车项数量
     */
    @Update("UPDATE cart_items SET quantity = #{quantity} WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    // ========== 删 ==========

    /**
     * 删除购物车项
     */
    @Delete("DELETE FROM cart_items WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 清空用户购物车
     */
    @Delete("DELETE FROM cart_items WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    /**
     * 批量删除购物车项
     */
    @Delete("<script>" +
            "DELETE FROM cart_items WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据用户ID和商品ID列表删除购物车商品
     */
    @Delete("<script>" +
            "DELETE FROM cart_items WHERE user_id = #{userId} AND product_id IN " +
            "<foreach collection='productIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteByUserIdAndProductIds(@Param("userId") Long userId,
                                    @Param("productIds") List<Long> productIds);
}