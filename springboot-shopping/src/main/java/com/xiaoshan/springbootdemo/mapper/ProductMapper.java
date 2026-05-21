package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Product;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

/**
 * 商品数据访问层
 *
 * @author xiaoshan
 * @date 2026-03-30
 */
@Mapper
public interface ProductMapper {

    // ========== 增（Create） ==========

    /**
     * 插入商品
     */
    @Insert("INSERT INTO products (name, brand, description, price, original_price, stock, sales_count, " +
            "category_id, seller_id, status, created_at, updated_at) " +
            "VALUES (#{name}, #{brand}, #{description}, #{price}, #{originalPrice}, #{stock}, #{salesCount}, " +
            "#{categoryId}, #{sellerId}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    // ========== 删（Delete） ==========

    /**
     * 根据ID删除商品
     */
    @Delete("DELETE FROM products WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 批量删除商品
     */
    @Delete("<script>" +
            "DELETE FROM products WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteByIds(@Param("ids") List<Long> ids);

    // ========== 改（Update） ==========

    /**
     * 更新商品信息
     */
    @Update("UPDATE products SET " +
            "name = #{name}, " +
            "brand = #{brand}, " +
            "description = #{description}, " +
            "price = #{price}, " +
            "original_price = #{originalPrice}, " +
            "stock = #{stock}, " +
            "category_id = #{categoryId}, " +
            "status = #{status}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(Product product);

    /**
     * 更新商品状态（上架/下架）
     */
    @Update("UPDATE products SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 扣减库存
     */
    @Update("UPDATE products SET stock = stock - #{quantity}, updated_at = NOW() " +
            "WHERE id = #{id} AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 增加销量（支付成功后调用）
     */
    @Update("UPDATE products SET sales_count = sales_count + #{quantity} WHERE id = #{productId}")
    int incrementSalesCount(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    // ========== 查（Select）- 基础查询 ==========

    /**
     * 根据ID查询商品
     */
    @Select("SELECT id, name, brand, description, price, original_price, stock, sales_count, " +
            "category_id, seller_id, status, created_at, updated_at " +
            "FROM products WHERE id = #{id}")
    Optional<Product> findById(Long id);

    // 查询所有商品的信息
    @Select("SELECT id, name, brand, description, price, original_price, stock, sales_count," +
            "category_id, seller_id, status, created_at, updated_at FROM products")
    List<Product> findAll();

    // ========== 查（Select）- 商家管理 ==========

    /**
     * 分页查询商家商品列表
     */
    @Select("<script>" +
            "SELECT p.id, p.name, p.brand, p.description, p.price, p.original_price, " +
            "p.stock, p.sales_count, p.category_id, p.seller_id, p.status, p.created_at, p.updated_at, " +
            "c.name as category_name, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as images " +
            "FROM products p " +
            "LEFT JOIN categories c ON p.category_id = c.id " +
            "WHERE p.seller_id = #{sellerId} " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (p.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.brand LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null'>" +
            "AND p.status = #{status} " +
            "</if>" +
            "ORDER BY p.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Product> findSellerProducts(@Param("sellerId") Long sellerId,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit,
                                     @Param("keyword") String keyword,
                                     @Param("status") Integer status);

    /**
     * 统计商家商品数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM products p " +
            "WHERE p.seller_id = #{sellerId} " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (p.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.brand LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null'>" +
            "AND p.status = #{status} " +
            "</if>" +
            "</script>")
    long countSellerProducts(@Param("sellerId") Long sellerId,
                             @Param("keyword") String keyword,
                             @Param("status") Integer status);

    // ========== 查（Select）- 用户首页 ==========

    /**
     * 首页商品列表（分页 + 筛选）
     */
    @Select("<script>" +
            "SELECT p.id, p.name, p.brand, p.description, p.price, p.original_price, " +
            "p.stock, p.sales_count, p.category_id, p.seller_id, p.status, " +
            "c.name as category_name, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as images " +
            "FROM products p " +
            "LEFT JOIN categories c ON p.category_id = c.id " +
            "WHERE p.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR p.brand LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='level1CategoryId != null'>" +
            "AND p.category_id IN (SELECT id FROM categories WHERE parent_id = #{level1CategoryId}) " +
            "</if>" +
            "<if test='level2CategoryId != null'>" +
            "AND p.category_id = #{level2CategoryId} " +
            "</if>" +
            "ORDER BY p.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Product> findUserProducts(@Param("offset") int offset,
                                   @Param("limit") int limit,
                                   @Param("keyword") String keyword,
                                   @Param("level1CategoryId") Long level1CategoryId,
                                   @Param("level2CategoryId") Long level2CategoryId);

    /**
     * 统计首页商品总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM products p " +
            "WHERE p.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR p.brand LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='level1CategoryId != null'>" +
            "AND p.category_id IN (SELECT id FROM categories WHERE parent_id = #{level1CategoryId}) " +
            "</if>" +
            "<if test='level2CategoryId != null'>" +
            "AND p.category_id = #{level2CategoryId} " +
            "</if>" +
            "</script>")
    long countUserProducts(@Param("keyword") String keyword,
                           @Param("level1CategoryId") Long level1CategoryId,
                           @Param("level2CategoryId") Long level2CategoryId);

    // ========== 查（Select）- 商品详情 ==========

    /**
     * 根据商品ID查询详情（带分类名和所有图片）
     */
    @Select("SELECT p.id, p.name, p.brand, p.description, p.price, p.original_price, " +
            "p.stock, p.sales_count, p.category_id, p.seller_id, p.status, p.created_at, p.updated_at, " +
            "c.name as category_name, " +
            "GROUP_CONCAT(pi.image ORDER BY pi.sort_order ASC SEPARATOR ',') as images " +
            "FROM products p " +
            "LEFT JOIN categories c ON p.category_id = c.id " +
            "LEFT JOIN product_images pi ON p.id = pi.product_id " +
            "WHERE p.id = #{productId} " +
            "GROUP BY p.id")
    Optional<Product> findByIdWithDetails(Long productId);

}