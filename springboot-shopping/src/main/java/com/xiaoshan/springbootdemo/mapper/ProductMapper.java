package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.vo.ProductVO;
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
    @Insert("INSERT INTO products (id, name, brand, description, sales_count, " +
            "category_id, seller_id, status, " +
            "weight, is_free_shipping, service_guarantee, delivery_city, detail_html) " +
            "VALUES (#{id}, #{name}, #{brand}, #{description}, #{salesCount}, " +
            "#{categoryId}, #{sellerId}, #{status}, " +
            "#{weight}, #{isFreeShipping}, #{serviceGuarantee}, #{deliveryCity}, #{detailHtml})")
    int insert(Product product);

    // ========== 删（Delete） ==========

    /**
     * 根据ID删除商品（硬删除，谨慎使用）
     */
    @Delete("DELETE FROM products WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 批量删除商品（硬删除，谨慎使用）
     */
    @Delete("<script>" +
            "DELETE FROM products WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 软删除商品（设置 status = 2）
     */
    @Update("UPDATE products SET status = 2, updated_at = NOW() WHERE id = #{id}")
    int softDelete(Long id);

    /**
     * 恢复已删除商品（设置 status = 0，恢复到下架状态）
     */
    @Update("UPDATE products SET status = 0, updated_at = NOW() WHERE id = #{id} AND status = 2")
    int restoreProduct(Long id);

    // ========== 改（Update） ==========

    /**
     * 更新商品信息
     */
    @Update("UPDATE products SET " +
            "name = #{name}, " +
            "brand = #{brand}, " +
            "description = #{description}, " +
            "category_id = #{categoryId}, " +
            "status = #{status}, " +
            "weight = #{weight}, " +
            "is_free_shipping = #{isFreeShipping}, " +
            "service_guarantee = #{serviceGuarantee}, " +
            "delivery_city = #{deliveryCity}, " +
            "detail_html = #{detailHtml}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(Product product);

    /**
     * 根据ID更新商品（仅更新非空字段）
     */
    @Update("UPDATE products SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateById(Product product);

    /**
     * 更新商品状态（上架/下架）
     */
    @Update("UPDATE products SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 批量更新商品状态
     */
    @Update("<script>" +
            "UPDATE products SET status = #{status}, updated_at = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    /**
     * 统计商家上架商品数量
     */
    @Select("SELECT COUNT(*) FROM products WHERE seller_id = #{sellerId} AND status = 1")
    long countActiveBySellerId(Long sellerId);

    /**
     * 查询商家上架商品ID（按创建时间升序，用于下架）
     */
    @Select("SELECT id FROM products WHERE seller_id = #{sellerId} AND status = 1 ORDER BY created_at ASC LIMIT #{limit}")
    List<Long> findActiveProductIdsBySellerIdOrderByCreatedAtAsc(@Param("sellerId") Long sellerId, @Param("limit") Integer limit);

    /**
     * 查询商家所有上架商品ID（按创建时间升序，不限数量）
     */
    @Select("SELECT id FROM products WHERE seller_id = #{sellerId} AND status = 1 ORDER BY created_at ASC")
    List<Long> findAllActiveProductIdsBySellerIdOrderByCreatedAtAsc(Long sellerId);

    /**
     * 查询商家已下架商品ID（status=0，按创建时间升序，先下架的先恢复）
     */
    @Select("SELECT id FROM products WHERE seller_id = #{sellerId} AND status = 0 ORDER BY created_at ASC")
    List<Long> findInactiveProductIdsBySellerIdOrderByCreatedAtAsc(Long sellerId);

    /**
     * 查询商家已下架商品ID（status=0，按创建时间降序，最新发布的优先上架）
     */
    @Select("SELECT id FROM products WHERE seller_id = #{sellerId} AND status = 0 ORDER BY created_at DESC")
    List<Long> findInactiveProductIdsBySellerIdOrderByCreatedAtDesc(Long sellerId);

    /**
     * 根据ID列表查询商品
     */
    @Select("<script>" +
            "SELECT * FROM products WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Product> findByIds(@Param("ids") List<Long> ids);

    /**
     * 增加销量（支付成功后调用）
     */
    @Update("UPDATE products SET sales_count = sales_count + #{quantity} WHERE id = #{productId}")
    int incrementSalesCount(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 增加浏览量（访问商品详情时调用）
     */
    @Update("UPDATE products SET view_count = view_count + 1 WHERE id = #{productId}")
    int incrementViewCount(@Param("productId") Long productId);

    // ========== SKU 库存操作 ==========

    /**
     * 查询商品下库存充足的 SKU（用于扣减库存）
     */
    @Select("SELECT id FROM product_skus WHERE product_id = #{productId} AND stock >= #{quantity} ORDER BY stock DESC LIMIT 1")
    Long findAvailableSkuId(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 扣减指定 SKU 的库存
     */
    @Update("UPDATE product_skus SET stock = stock - #{quantity} WHERE id = #{skuId}")
    int deductSkuStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    // ========== 查（Select）- 基础查询 ==========

    /**
     * 根据ID查询商品
     */
    @Select("SELECT p.id, p.name, p.brand, p.description, p.sales_count, p.view_count, " +
            "p.category_id, p.seller_id, p.status, p.created_at, p.updated_at, " +
            "p.weight, p.is_free_shipping, p.service_guarantee, p.delivery_city, " +
            "(SELECT SUM(stock) FROM product_skus WHERE product_id = p.id) as stock, " +
            "(SELECT MIN(price) FROM product_skus WHERE product_id = p.id) as price, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as images " +
            "FROM products p WHERE p.id = #{id}")
    Optional<Product> findById(Long id);

    /**
     * 查询商品基本信息（用于浏览历史记录，不增加浏览量）
     */
    @Select("SELECT p.id, p.name, " +
            "(SELECT MIN(price) FROM product_skus WHERE product_id = p.id) as price, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as image, " +
            "p.status " +
            "FROM products p WHERE p.id = #{id}")
    Optional<Product> findBasicInfoForBrowse(Long id);

    // 查询所有商品的信息
    @Select("SELECT p.id, p.name, p.brand, p.description, p.sales_count, p.view_count, " +
            "p.category_id, p.seller_id, p.status, p.created_at, p.updated_at, " +
            "p.weight, p.is_free_shipping, p.service_guarantee, p.delivery_city, " +
            "(SELECT SUM(stock) FROM product_skus WHERE product_id = p.id) as stock, " +
            "(SELECT MIN(price) FROM product_skus WHERE product_id = p.id) as price, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as images " +
            "FROM products p")
    List<Product> findAll();

    // ========== 查（Select）- 商家管理 ==========

    /**
     * 分页查询商家商品列表（不含已删除商品，除非明确指定 status=2）
     */
    @Select("<script>" +
            "SELECT p.id, p.name, p.brand, p.description, " +
            "p.sales_count, p.category_id, p.seller_id, p.status, p.created_at, p.updated_at, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as images, " +
            "(SELECT MIN(price) FROM product_skus WHERE product_id = p.id) as price, " +
            "(SELECT MIN(original_price) FROM product_skus WHERE product_id = p.id AND original_price IS NOT NULL) as original_price, " +
            "(SELECT SUM(stock) FROM product_skus WHERE product_id = p.id) as stock " +
            "FROM products p " +
            "WHERE p.seller_id = #{sellerId} " +
            "<if test='status != null'>" +
            "AND p.status = #{status} " +
            "</if>" +
            "<if test='status == null'>" +
            "AND p.status != 2 " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (p.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.brand LIKE CONCAT('%', #{keyword}, '%')) " +
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
     * 统计商家商品数量（不含已删除商品，除非明确指定 status=2）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM products p " +
            "WHERE p.seller_id = #{sellerId} " +
            "<if test='status != null'>" +
            "AND p.status = #{status} " +
            "</if>" +
            "<if test='status == null'>" +
            "AND p.status != 2 " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (p.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.brand LIKE CONCAT('%', #{keyword}, '%')) " +
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
            "SELECT p.id, p.name, p.brand, p.description, " +
            "p.sales_count, p.category_id, p.seller_id, p.status, " +
            "c.name as category_name, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as images, " +
            "(SELECT MIN(price) FROM product_skus WHERE product_id = p.id) as price, " +
            "(SELECT MIN(original_price) FROM product_skus WHERE product_id = p.id AND original_price IS NOT NULL) as original_price, " +
            "(SELECT SUM(stock) FROM product_skus WHERE product_id = p.id) as stock " +
            "FROM products p " +
            "LEFT JOIN categories c ON p.category_id = c.id " +
            "WHERE p.status = 1 " +
            "<if test='sellerId != null'>" +
            "AND p.seller_id = #{sellerId} " +
            "</if>" +
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
                                   @Param("sellerId") Long sellerId,
                                   @Param("level1CategoryId") Long level1CategoryId,
                                   @Param("level2CategoryId") Long level2CategoryId);

    /**
     * 统计首页商品总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM products p " +
            "WHERE p.status = 1 " +
            "<if test='sellerId != null'>" +
            "AND p.seller_id = #{sellerId} " +
            "</if>" +
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
                           @Param("sellerId") Long sellerId,
                           @Param("level1CategoryId") Long level1CategoryId,
                           @Param("level2CategoryId") Long level2CategoryId);

    // ========== 查（Select）- 商品详情 ==========

    /**
     * 根据商品ID查询详情（带分类名、所有图片和评论总数）
     */
    @Select("SELECT p.id, p.name, p.brand, p.description, p.detail_html, " +
            "p.sales_count, p.view_count, " +
            "p.category_id, p.seller_id, p.status, p.created_at, p.updated_at, " +
            "p.weight, p.is_free_shipping, p.service_guarantee, p.delivery_city, " +
            "(SELECT SUM(stock) FROM product_skus WHERE product_id = p.id) as stock, " +
            "c.name as category_name, " +
            "(SELECT COUNT(*) FROM reviews r WHERE r.product_id = p.id) as comment_count " +
            "FROM products p " +
            "LEFT JOIN categories c ON p.category_id = c.id " +
            "WHERE p.id = #{productId}")
    Optional<Product> findByIdWithDetails(Long productId);

    /**
     * 查询商品详情（返回 ProductVO）
     */
    Optional<ProductVO> findProductVOById(Long productId);

    /**
     * 统计商家商品总数（用于套餐数量限制检查，不含已删除商品）
     */
    @Select("SELECT COUNT(*) FROM products WHERE seller_id = #{sellerId} AND status != 2")
    long countBySellerId(@Param("sellerId") Long sellerId);

    /**
     * 根据商家ID和状态查询商品列表
     */
    @Select("SELECT p.id, p.name, p.brand, p.description, p.sales_count, p.view_count, " +
            "p.category_id, p.seller_id, p.status, p.created_at, p.updated_at, " +
            "p.weight, p.is_free_shipping, p.service_guarantee, p.delivery_city, " +
            "(SELECT SUM(stock) FROM product_skus WHERE product_id = p.id) as stock, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as images " +
            "FROM products p WHERE p.seller_id = #{sellerId} AND p.status = #{status}")
    List<Product> findBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") Integer status);

    // ========== 查（Select）- 搜索筛选 ==========

    /**
     * 搜索商品列表（支持排序、价格区间、分类筛选）
     */
    @Select("<script>" +
            "SELECT p.id, p.name, p.brand, p.description, " +
            "p.sales_count, p.category_id, p.seller_id, p.status, p.created_at, " +
            "(SELECT image FROM product_images WHERE product_id = p.id ORDER BY sort_order ASC LIMIT 1) as images, " +
            "(SELECT MIN(price) FROM product_skus WHERE product_id = p.id) as price, " +
            "(SELECT MIN(original_price) FROM product_skus WHERE product_id = p.id AND original_price IS NOT NULL) as original_price, " +
            "(SELECT SUM(stock) FROM product_skus WHERE product_id = p.id) as stock " +
            "FROM products p " +
            "WHERE p.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR p.brand LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='categoryId != null'>" +
            "AND p.category_id = #{categoryId} " +
            "</if>" +
            "<if test='minPrice != null'>" +
            "AND EXISTS (SELECT 1 FROM product_skus ps WHERE ps.product_id = p.id AND ps.price &gt;= #{minPrice}) " +
            "</if>" +
            "<if test='maxPrice != null'>" +
            "AND EXISTS (SELECT 1 FROM product_skus ps WHERE ps.product_id = p.id AND ps.price &lt;= #{maxPrice}) " +
            "</if>" +
            "<if test='sort != null and sort != \"\"'>" +
            "<choose>" +
            "<when test='sort == \"sales_desc\"'>ORDER BY p.sales_count DESC</when>" +
            "<when test='sort == \"price_asc\"'>ORDER BY (SELECT MIN(price) FROM product_skus WHERE product_id = p.id) ASC</when>" +
            "<when test='sort == \"price_desc\"'>ORDER BY (SELECT MIN(price) FROM product_skus WHERE product_id = p.id) DESC</when>" +
            "<when test='sort == \"newest\"'>ORDER BY p.created_at DESC</when>" +
            "<otherwise>ORDER BY p.created_at DESC</otherwise>" +
            "</choose>" +
            "</if>" +
            "<if test='sort == null or sort == \"\"'>ORDER BY p.created_at DESC</if>" +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Product> searchProducts(@Param("offset") int offset,
                                 @Param("limit") int limit,
                                 @Param("keyword") String keyword,
                                 @Param("categoryId") Long categoryId,
                                 @Param("minPrice") java.math.BigDecimal minPrice,
                                 @Param("maxPrice") java.math.BigDecimal maxPrice,
                                 @Param("sort") String sort);

    /**
     * 统计搜索商品总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM products p " +
            "WHERE p.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR p.brand LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='categoryId != null'>" +
            "AND p.category_id = #{categoryId} " +
            "</if>" +
            "<if test='minPrice != null'>" +
            "AND EXISTS (SELECT 1 FROM product_skus ps WHERE ps.product_id = p.id AND ps.price &gt;= #{minPrice}) " +
            "</if>" +
            "<if test='maxPrice != null'>" +
            "AND EXISTS (SELECT 1 FROM product_skus ps WHERE ps.product_id = p.id AND ps.price &lt;= #{maxPrice}) " +
            "</if>" +
            "</script>")
    long countSearchProducts(@Param("keyword") String keyword,
                             @Param("categoryId") Long categoryId,
                             @Param("minPrice") java.math.BigDecimal minPrice,
                             @Param("maxPrice") java.math.BigDecimal maxPrice);

    /**
     * 查询热销商品（按销量排序）
     */
    @Select("SELECT p.id, p.name, p.brand, p.description, p.sales_count, p.view_count, " +
            "p.category_id, p.seller_id, p.status, p.created_at, p.updated_at, " +
            "(SELECT MIN(price) FROM product_skus WHERE product_id = p.id) as price " +
            "FROM products p WHERE p.status = 1 ORDER BY p.sales_count DESC LIMIT #{limit}")
    List<Product> findHotProducts(@Param("limit") int limit);

    /**
     * 查询指定品类的热销商品
     */
    @Select("<script>" +
            "SELECT p.id, p.name, p.brand, p.description, p.sales_count, p.view_count, " +
            "p.category_id, p.seller_id, p.status, p.created_at, p.updated_at, " +
            "(SELECT MIN(price) FROM product_skus WHERE product_id = p.id) as price " +
            "FROM products p WHERE p.status = 1 AND p.category_id IN " +
            "<foreach collection='categoryIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY p.sales_count DESC LIMIT #{limit}" +
            "</script>")
    List<Product> findHotProductsByCategories(@Param("categoryIds") List<Long> categoryIds, @Param("limit") int limit);

    /**
     * 查询用户浏览过的商品品类ID列表
     */
    @Select("SELECT DISTINCT p.category_id FROM user_browse_history bh " +
            "JOIN products p ON bh.product_id = p.id " +
            "WHERE bh.user_id = #{userId} AND p.category_id IS NOT NULL " +
            "ORDER BY bh.browse_time DESC LIMIT 5")
    List<Long> findBrowsedCategoryIds(@Param("userId") Long userId);

    @Select("SELECT name FROM products \n" +
            "WHERE status != 2 AND name LIKE CONCAT('%', #{keyword}, '%') \n" +
            "GROUP BY name \n" +
            "ORDER BY MAX(sales_count) DESC \n" +
            "LIMIT #{limit}")
    List<String> suggest(@Param("keyword") String keyword, @Param("limit") int limit);

}