package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Address;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AddressMapper {

    // ========== 增 ==========

    /**
     * 新增收货地址
     */
    @Insert("INSERT INTO addresses (user_id, recipient_name, recipient_phone, " +
            "province, city, district, detail_address, label, is_default, created_at, updated_at) " +
            "VALUES (#{userId}, #{recipientName}, #{recipientPhone}, " +
            "#{province}, #{city}, #{district}, #{detailAddress}, #{label}, #{isDefault}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Address address);

    // ========== 查 ==========

    /**
     * 根据地址ID查询地址
     */
    @Select("SELECT id, user_id, recipient_name, recipient_phone, " +
            "province, city, district, detail_address, label, is_default, created_at, updated_at " +
            "FROM addresses WHERE id = #{id}")
    Optional<Address> findById(Long id);

    /**
     * 根据用户ID查询所有地址
     */
    @Select("SELECT id, user_id, recipient_name, recipient_phone, " +
            "province, city, district, detail_address, label, is_default, created_at, updated_at " +
            "FROM addresses WHERE user_id = #{userId} ORDER BY is_default DESC, created_at DESC")
    List<Address> findByUserId(Long userId);

    /**
     * 根据地址ID和用户ID查询地址（用于权限校验）
     */
    @Select("SELECT id, user_id, recipient_name, recipient_phone, " +
            "province, city, district, detail_address, label, is_default, created_at, updated_at " +
            "FROM addresses WHERE id = #{id} AND user_id = #{userId}")
    Optional<Address> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 查询用户的默认地址
     */
    @Select("SELECT id, user_id, recipient_name, recipient_phone, " +
            "province, city, district, detail_address, label, is_default, created_at, updated_at " +
            "FROM addresses WHERE user_id = #{userId} AND is_default = 1 LIMIT 1")
    Optional<Address> findDefaultByUserId(Long userId);

    // ========== 改 ==========

    /**
     * 更新收货地址（包含默认地址字段）
     * 注意：更新时如果修改了默认地址，需要配合 clearDefaultByUserId 使用
     */
    @Update("UPDATE addresses SET " +
            "recipient_name = #{recipientName}, " +
            "recipient_phone = #{recipientPhone}, " +
            "province = #{province}, " +
            "city = #{city}, " +
            "district = #{district}, " +
            "detail_address = #{detailAddress}, " +
            "label = #{label}, " +
            "is_default = #{isDefault}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId}")
    int update(Address address);

    /**
     * 清除用户所有默认地址标记
     */
    @Update("UPDATE addresses SET is_default = 0, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND is_default = 1")
    int clearDefaultByUserId(Long userId);

    /**
     * 将指定地址设为默认地址
     */
    @Update("UPDATE addresses SET is_default = 1, updated_at = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId}")
    int setDefault(@Param("id") Long id, @Param("userId") Long userId);

    // ========== 删 ==========

    /**
     * 删除收货地址
     */
    @Delete("DELETE FROM addresses WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}