package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.SellerProfile;
import com.xiaoshan.springbootdemo.entity.dto.SellerProfileDTO;
import org.apache.ibatis.annotations.*;

import java.util.Optional;

/**
 * 商家信息数据访问接口
 * 面向独立商家，ID固定为1
 *
 * @author xiaoshan
 * @date 2026-05-08
 */
@Mapper
public interface SellerProfileMapper {

    // ==================== 新增方法 ====================

    /**
     * 初始化商家信息（首次创建）
     *
     * @param profile 商家信息实体
     * @return 受影响的行数
     */
    @Insert("INSERT INTO seller_profiles(id, store_name, slogan, store_detail, " +
            "store_avatar, phone, address, is_open, business) " +
            "VALUES(1, #{storeName}, #{slogan}, #{storeDetail}, " +
            "#{storeAvatar}, #{phone}, #{address}, #{isOpen}, #{business})")
    int insert(SellerProfile profile);

    // ==================== 查询方法 ====================

    /**
     * 查询商家信息（独立商家，ID固定为1）
     *
     * @return 商家信息实体
     */
    @Select("SELECT id, store_name, slogan, store_detail, store_avatar, " +
            "phone, address, is_open, business " +
            "FROM seller_profiles WHERE id = 1")
    Optional<SellerProfile> findById();

    /**
     * 查询商家营业状态
     *
     * @return true-营业中, false-休息中
     */
    @Select("SELECT is_open FROM seller_profiles WHERE id = 1")
    Boolean findOpenStatus();

    /**
     * 查询商家头像
     *
     * @return 头像URL
     */
    @Select("SELECT store_avatar FROM seller_profiles WHERE id = 1")
    String findAvatar();



    // ==================== 更新方法 ====================

    /**
     * 更新商家信息
     *
     * @param profile 商家信息实体
     * @return 受影响的行数
     */
    @Update("UPDATE seller_profiles SET " +
            "store_name = #{storeName}, " +
            "slogan = #{slogan}, " +
            "store_detail = #{storeDetail}, " +
            "phone = #{phone}, " +
            "address = #{address}, " +
            "business = #{business}, " +
            "is_open = #{isOpen} " +
            "WHERE id = 1")
    int update(SellerProfileDTO profile);

    /**
     * 更新营业状态
     *
     * @param isOpen 营业状态
     * @return 受影响的行数
     */
    @Update("UPDATE seller_profiles SET is_open = #{isOpen} WHERE id = 1")
    int updateOpenStatus(@Param("isOpen") Boolean isOpen);

    /**
     * 更新商家头像
     *
     * @param avatarUrl 头像URL
     * @return 受影响的行数
     */
    @Update("UPDATE seller_profiles SET store_avatar = #{avatarUrl} WHERE id = 1")
    int updateAvatar(@Param("avatarUrl") String avatarUrl);

    /**
     * 更新商家电话
     *
     * @param phone 联系电话
     * @return 受影响的行数
     */
    @Update("UPDATE seller_profiles SET phone = #{phone} WHERE id = 1")
    int updatePhone(@Param("phone") String phone);

    /**
     * 更新营业时间
     *
     * @param business 营业时间
     * @return 受影响的行数
     */
    @Update("UPDATE seller_profiles SET business = #{business} WHERE id = 1")
    int updateBusiness(@Param("business") String business);

    /**
     * 更新店铺标语
     *
     * @param slogan 店铺标语
     * @return 受影响的行数
     */
    @Update("UPDATE seller_profiles SET slogan = #{slogan} WHERE id = 1")
    int updateSlogan(@Param("slogan") String slogan);

    /**
     * 更新店铺详情
     *
     * @param storeDetail 店铺详情
     * @return 受影响的行数
     */
    @Update("UPDATE seller_profiles SET store_detail = #{storeDetail} WHERE id = 1")
    int updateStoreDetail(@Param("storeDetail") String storeDetail);

}