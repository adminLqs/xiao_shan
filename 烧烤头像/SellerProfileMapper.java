package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.SellerProfile;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SellerProfileMapper {

    // 获取唯一的商家信息
    @Select("SELECT * FROM seller_profiles LIMIT 1")
    SellerProfile findOne();

    // 插入商家信息（包含新增字段）
    @Insert("INSERT INTO seller_profiles(store_name, store_detail, store_avatar, " +
            "phone, is_open, business, address, slogan) " +
            "VALUES(#{storeName}, #{storeDetail}, #{storeAvatar}, " +
            "#{phone}, #{isOpen}, #{business}, #{address}, #{slogan})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SellerProfile sellerProfile);

    // 更新商家信息（包含新增字段）
    @Update("UPDATE seller_profiles SET " +
            "store_name = #{storeName}, " +
            "store_detail = #{storeDetail}, " +
            "store_avatar = #{storeAvatar}, " +
            "phone = #{phone}, " +
            "business = #{business}, " +
            "address = #{address}, " +
            "slogan = #{slogan}, " +
            "is_open = #{isOpen} " +
            "WHERE id = #{id}")
    int update(SellerProfile sellerProfile);

    // 更新营业状态
    @Update("UPDATE seller_profiles SET is_open = #{isOpen} WHERE id = #{id}")
    int updateOpenStatus(@Param("id") Long id, @Param("isOpen") Boolean isOpen);

    // 删除商家信息
    @Delete("DELETE FROM seller_profiles WHERE id = #{id}")
    int delete(@Param("id") Long id);

    // 专门更新头像的方法
    @Update("UPDATE seller_profiles SET store_avatar = #{avatarUrl} WHERE id = #{1}")
    int updateAvatar(@Param("avatarUrl") String avatarUrl);
}