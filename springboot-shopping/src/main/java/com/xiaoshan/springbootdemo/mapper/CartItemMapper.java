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
    @Insert("<script>" +
            "INSERT INTO cart_items (id, user_id, product_id, sku_id, quantity, added_at) " +
            "VALUES (#{id}, #{userId}, #{productId}, #{skuId}, #{quantity}, #{addedAt})" +
            "</script>")
    int insert(CartItem cartItem);

    // ========== 查 ==========

    /**
     * 根据ID查询购物车项
     */
    @Select("SELECT id, user_id, product_id, sku_id, quantity, added_at " +
            "FROM cart_items WHERE id = #{id}")
    Optional<CartItem> findById(Long id);

    /**
     * 根据用户ID查询购物车列表（关联商品和SKU信息）
     * 返回包含商品名称、价格、图片、SKU信息等完整信息
     */
    @Select("SELECT " +
            "ci.id, ci.user_id, ci.product_id, ci.sku_id, ci.quantity, ci.added_at, " +
            "p.name as product_name, p.brand, p.status as product_status, " +
            "COALESCE(s.sku_name, '') as sku_name, " +
            "COALESCE(s.price, (SELECT MIN(ps.price) FROM product_skus ps WHERE ps.product_id = p.id), 0) as price, " +
            "COALESCE(s.original_price, (SELECT MIN(ps.original_price) FROM product_skus ps WHERE ps.product_id = p.id), 0) as original_price, " +
            "COALESCE(s.stock, (SELECT COALESCE(SUM(ps.stock), 0) FROM product_skus ps WHERE ps.product_id = p.id), 0) as stock, " +
            "COALESCE(s.sku_image, (SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1)) as product_image, " +
            "COALESCE(sp.store_name, up.nickname) as seller_name, " +
            "COALESCE(sp.store_avatar, up.avatar) as seller_avatar " +
            "FROM cart_items ci " +
            "LEFT JOIN products p ON ci.product_id = p.id " +
            "LEFT JOIN seller_profiles sp ON p.seller_id = sp.user_id " +
            "LEFT JOIN user_profiles up ON p.seller_id = up.user_id " +
            "LEFT JOIN product_skus s ON ci.sku_id = s.id " +
            "WHERE ci.user_id = #{userId} " +
            "ORDER BY ci.added_at DESC")
    List<CartItemVO> findCartItemsWithProduct(Long userId);

    /**
     * 根据用户ID查询购物车列表（基础信息）
     */
    @Select("SELECT id, user_id, product_id, sku_id, quantity, added_at " +
            "FROM cart_items WHERE user_id = #{userId} ORDER BY added_at DESC")
    List<CartItem> findByUserId(Long userId);

    /**
     * 根据用户ID和商品ID查询购物车项（兼容旧版本）
     */
    @Select("SELECT id, user_id, product_id, sku_id, quantity, added_at " +
            "FROM cart_items WHERE user_id = #{userId} AND product_id = #{productId}")
    Optional<CartItem> findByUserIdAndProductId(@Param("userId") Long userId,
                                                @Param("productId") Long productId);

    /**
     * 根据用户ID、商品ID和SKU ID查询购物车项（新增）
     */
    @Select("<script>" +
            "SELECT id, user_id, product_id, sku_id, quantity, added_at " +
            "FROM cart_items WHERE user_id = #{userId} AND product_id = #{productId} " +
            "<if test='skuId != null'> AND sku_id = #{skuId} </if>" +
            "<if test='skuId == null'> AND sku_id IS NULL </if>" +
            "</script>")
    Optional<CartItem> findByUserIdAndProductIdAndSkuId(@Param("userId") Long userId,
                                                        @Param("productId") Long productId,
                                                        @Param("skuId") Long skuId);

    /**
     * 根据ID和用户ID查询购物车项（用于权限校验）
     */
    @Select("SELECT id, user_id, product_id, sku_id, quantity, added_at " +
            "FROM cart_items WHERE id = #{id} AND user_id = #{userId}")
    Optional<CartItem> findByIdAndUserId(@Param("id") Long id,
                                         @Param("userId") Long userId);



    /**
     * 统计用户购物车商品总数量
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE user_id = #{userId}")
    Integer countQuantityByUserId(Long userId);

    /**
     * 获取购物车结算项（联表查询，支持SKU）
     *
     * @param userId 用户ID，用于权限校验
     * @param cartItemIds 购物车项ID列表，使用 IN 查询
     * @return 结算商品信息列表
     */
    @Select("SELECT " +
            "ci.id as cartItemId, " +
            "ci.product_id as productId, " +
            "ci.sku_id as skuId, " +
            "ci.quantity, " +
            "p.name as productName, " +
            "p.status as productStatus, " +
            "p.seller_id as sellerId, " +
            "COALESCE(sp.store_name, up.nickname) as sellerName, " +
            "COALESCE(sp.store_avatar, up.avatar) as sellerAvatar, " +
            "s.sku_name as skuName, " +
            "p.brand, " +
            "COALESCE(s.price, (SELECT MIN(ps.price) FROM product_skus ps WHERE ps.product_id = p.id), 0) as price, " +
            "COALESCE(s.original_price, (SELECT MIN(ps.original_price) FROM product_skus ps WHERE ps.product_id = p.id), 0) as originalPrice, " +
            "COALESCE(s.stock, (SELECT COALESCE(SUM(ps.stock), 0) FROM product_skus ps WHERE ps.product_id = p.id), 0) as stock, " +
            "COALESCE(s.sku_image, (SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1)) as productImage, " +
            "p.is_free_shipping as isFreeShipping " +
            "FROM cart_items ci " +
            "LEFT JOIN products p ON ci.product_id = p.id " +
            "LEFT JOIN seller_profiles sp ON p.seller_id = sp.user_id " +
            "LEFT JOIN user_profiles up ON p.seller_id = up.user_id " +
            "LEFT JOIN product_skus s ON ci.sku_id = s.id " +
            "WHERE ci.id IN (${cartItemIds}) AND ci.user_id = #{userId}")
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

    /**
     * 根据商品ID删除购物车中的该商品（删除所有用户的购物车项）
     */
    @Delete("DELETE FROM cart_items WHERE product_id = #{productId}")
    int deleteByProductId(Long productId);
}