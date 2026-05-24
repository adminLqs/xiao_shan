package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.SellerProfile;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 商家资料数据访问接口
 * 负责商家资料表的 CRUD 操作和商家审核相关功能
 */
@Mapper
public interface SellerProfileMapper {

    // ========== 增 ==========

    /**
     * 插入商家资料
     *
     * @param sellerProfile 商家资料对象
     * @return 影响行数
     */
    @Insert("INSERT INTO seller_profiles (id, user_id, store_name, store_avatar, " +
            "store_detail, business_hours, contact_phone, address, created_at, updated_at) " +
            "VALUES (#{id}, #{userId}, #{storeName}, #{storeAvatar}, " +
            "#{storeDetail}, #{businessHours}, #{contactPhone}, #{address}, #{createdAt}, #{updatedAt})")
    int insert(SellerProfile sellerProfile);

    // ========== 查 ==========

    /**
     * 根据用户ID查询商家资料
     *
     * @param userId 用户ID
     * @return 商家资料
     */
    @Select("SELECT id, user_id, store_name, store_avatar, " +
            "store_detail, business_hours, contact_phone, address, created_at, updated_at " +
            "FROM seller_profiles WHERE user_id = #{userId}")
    Optional<SellerProfile> findByUserId(Long userId);

    /**
     * 根据主键ID查询商家资料
     *
     * @param id 主键ID
     * @return 商家资料
     */
    @Select("SELECT id, user_id, store_name, store_avatar, " +
            "store_detail, business_hours, contact_phone, address, created_at, updated_at " +
            "FROM seller_profiles WHERE id = #{id}")
    Optional<SellerProfile> findById(Long id);

    /**
     * 查询所有商家资料
     *
     * @return 商家资料列表
     */
    @Select("SELECT id, user_id, store_name, store_avatar, " +
            "store_detail, business_hours, contact_phone, address, created_at, updated_at " +
            "FROM seller_profiles ORDER BY created_at DESC")
    List<SellerProfile> findAll();

    // ========== 改 ==========

    /**
     * 更新商家资料（根据主键ID）
     *
     * @param sellerProfile 商家资料对象
     * @return 影响行数
     */
    @Update("UPDATE seller_profiles SET " +
            "store_name = #{storeName}, " +
            "store_avatar = #{storeAvatar}, " +
            "store_detail = #{storeDetail}, " +
            "business_hours = #{businessHours}, " +
            "contact_phone = #{contactPhone}, " +
            "address = #{address}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateById(SellerProfile sellerProfile);

    /**
     * 根据用户ID更新商家资料
     *
     * @param sellerProfile 商家资料对象
     * @return 影响行数
     */
    @Update("UPDATE seller_profiles SET " +
            "store_name = #{storeName}, " +
            "store_avatar = #{storeAvatar}, " +
            "store_detail = #{storeDetail}, " +
            "business_hours = #{businessHours}, " +
            "contact_phone = #{contactPhone}, " +
            "address = #{address}, " +
            "updated_at = NOW() " +
            "WHERE user_id = #{userId}")
    int updateByUserId(SellerProfile sellerProfile);

    // ========== 删 ==========

    /**
     * 根据用户ID删除商家资料
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM seller_profiles WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    /**
     * 根据主键ID删除商家资料
     *
     * @param id 主键ID
     * @return 影响行数
     */
    @Delete("DELETE FROM seller_profiles WHERE id = #{id}")
    int deleteById(Long id);

    // ========== 商家管理（管理员用）==========

    /**
     * 分页查询商家列表（联表查询）
     *
     * @param keyword 搜索关键词（店铺名称/联系人）
     * @param status 用户状态（0封禁 1正常）
     * @param offset 偏移量
     * @param size 每页数量
     * @return 商家列表
     */
    @Select("<script>" +
            "SELECT sp.id, sp.user_id as userId, sp.store_name as storeName, sp.store_avatar as logo, " +
            "ma.contact_name as contactName, ma.contact_phone as contactPhone, " +
            "ma.business_type as businessType, ma.main_category as mainCategory, ma.description, " +
            "ma.address, ma.application_id as applicationId, ma.reviewed_at as reviewedAt, ma.review_notes as reviewNotes, " +
            "u.account, u.status, sp.created_at as createdAt " +
            "FROM seller_profiles sp " +
            "LEFT JOIN merchant_apply ma ON sp.user_id = ma.user_id AND ma.status = 'APPROVED' " +
            "LEFT JOIN users u ON sp.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (sp.store_name LIKE CONCAT('%', #{keyword}, '%') OR ma.contact_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND u.status = #{status} " +
            "</if>" +
            "ORDER BY sp.created_at DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<Map<String, Object>> findSellers(@Param("keyword") String keyword,
                                           @Param("status") String status,
                                           @Param("offset") int offset,
                                           @Param("size") int size);

    /**
     * 统计商家总数
     *
     * @param keyword 搜索关键词
     * @param status 用户状态
     * @return 商家总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM seller_profiles sp " +
            "LEFT JOIN merchant_apply ma ON sp.user_id = ma.user_id AND ma.status = 'APPROVED' " +
            "LEFT JOIN users u ON sp.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (sp.store_name LIKE CONCAT('%', #{keyword}, '%') OR ma.contact_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND u.status = #{status} " +
            "</if>" +
            "</script>")
    long countSellers(@Param("keyword") String keyword, @Param("status") String status);

    /**
     * 统计商家粉丝数
     *
     * @param sellerId 商家ID（店铺资料ID）
     * @return 粉丝数量
     */
    @Select("SELECT COUNT(*) FROM follow_sellers WHERE seller_id = #{sellerId}")
    long countFollowers(Long sellerId);

    /**
     * 统计商家的平均评分（根据该商家所有商品的评论）
     *
     * @param sellerId 商家ID
     * @return 平均评分
     */
    @Select("SELECT COALESCE(AVG(r.rating), 0) FROM reviews r " +
            "INNER JOIN products p ON r.product_id = p.id " +
            "WHERE p.seller_id = #{sellerId}")
    double getAverageRating(Long sellerId);

    /**
     * 统计商家的好评率（4-5星评论占比）
     *
     * @param sellerId 商家ID
     * @return 好评率（百分比，0-100）
     */
    @Select("SELECT COALESCE(" +
            "(SELECT COUNT(*) FROM reviews r " +
            "INNER JOIN products p ON r.product_id = p.id " +
            "WHERE p.seller_id = #{sellerId} AND r.rating >= 4) * 100.0 / " +
            "NULLIF((SELECT COUNT(*) FROM reviews r " +
            "INNER JOIN products p ON r.product_id = p.id " +
            "WHERE p.seller_id = #{sellerId}), 0), 0) AS positive_rate")
    double getPositiveRate(Long sellerId);

    /**
     * 计算商家的平均发货时间（小时）
     *
     * @param sellerId 商家ID
     * @return 平均发货时间（小时）
     */
    @Select("SELECT COALESCE(AVG(TIMESTAMPDIFF(HOUR, o.created_at, o.shipped_at)), 0) " +
            "FROM orders o " +
            "INNER JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.seller_id = #{sellerId} AND o.shipped_at IS NOT NULL")
    double getAverageDeliveryHours(Long sellerId);

    /**
     * 计算商家的发货准时率（假设24小时内发货为准时）
     *
     * @param sellerId 商家ID
     * @return 准时率（百分比，0-100）
     */
    @Select("SELECT COALESCE(" +
            "(SELECT COUNT(*) FROM orders o " +
            "INNER JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.seller_id = #{sellerId} " +
            "AND o.shipped_at IS NOT NULL " +
            "AND TIMESTAMPDIFF(HOUR, o.created_at, o.shipped_at) <= 24) * 100.0 / " +
            "NULLIF((SELECT COUNT(*) FROM orders o " +
            "INNER JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.seller_id = #{sellerId} AND o.shipped_at IS NOT NULL), 0), 99) AS on_time_rate")
    double getOnTimeRate(Long sellerId);
}