package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.SellerProfile;
import org.apache.ibatis.annotations.*;

import java.util.List;
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
    @Insert("INSERT INTO seller_profiles (user_id, store_name, store_avatar, store_banner, " +
            "store_detail, business_hours, contact_phone, created_at, updated_at) " +
            "VALUES (#{userId}, #{storeName}, #{storeAvatar}, #{storeBanner}, " +
            "#{storeDetail}, #{businessHours}, #{contactPhone}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SellerProfile sellerProfile);

    // ========== 查 ==========

    /**
     * 根据用户ID查询商家资料
     *
     * @param userId 用户ID
     * @return 商家资料
     */
    @Select("SELECT id, user_id, store_name, store_avatar, store_banner, " +
            "store_detail, business_hours, contact_phone, created_at, updated_at " +
            "FROM seller_profiles WHERE user_id = #{userId}")
    Optional<SellerProfile> findByUserId(Long userId);

    /**
     * 根据主键ID查询商家资料
     *
     * @param id 主键ID
     * @return 商家资料
     */
    @Select("SELECT id, user_id, store_name, store_avatar, store_banner, " +
            "store_detail, business_hours, contact_phone, created_at, updated_at " +
            "FROM seller_profiles WHERE id = #{id}")
    Optional<SellerProfile> findById(Long id);

    /**
     * 查询所有商家资料
     *
     * @return 商家资料列表
     */
    @Select("SELECT id, user_id, store_name, store_avatar, store_banner, " +
            "store_detail, business_hours, contact_phone, created_at, updated_at " +
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
            "store_banner = #{storeBanner}, " +
            "store_detail = #{storeDetail}, " +
            "business_hours = #{businessHours}, " +
            "contact_phone = #{contactPhone}, " +
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
            "store_banner = #{storeBanner}, " +
            "store_detail = #{storeDetail}, " +
            "business_hours = #{businessHours}, " +
            "contact_phone = #{contactPhone}, " +
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
}