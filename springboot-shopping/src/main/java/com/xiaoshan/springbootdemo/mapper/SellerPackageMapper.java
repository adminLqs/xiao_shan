package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.SellerPackage;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface SellerPackageMapper {

    // 查询所有启用的套餐
    @Select("SELECT * FROM seller_packages WHERE is_active = 1 ORDER BY sort_order ASC")
    List<SellerPackage> findAllActive();

    // 根据ID查询
    @Select("SELECT * FROM seller_packages WHERE id = #{id}")
    Optional<SellerPackage> findById(Long id);

    // 查询所有套餐（管理员用）
    @Select("SELECT * FROM seller_packages ORDER BY sort_order ASC")
    List<SellerPackage> findAll();

    // 插入套餐
    @Insert("INSERT INTO seller_packages (id, name, description, price, duration_days, product_limit, features, is_active, sort_order) " +
            "VALUES (#{id}, #{name}, #{description}, #{price}, #{durationDays}, #{productLimit}, #{features}, #{isActive}, #{sortOrder})")
    int insert(SellerPackage pkg);

    // 更新套餐
    @Update("UPDATE seller_packages SET " +
            "name = #{name}, description = #{description}, price = #{price}, " +
            "duration_days = #{durationDays}, product_limit = #{productLimit}, " +
            "features = #{features}, is_active = #{isActive}, sort_order = #{sortOrder} " +
            "WHERE id = #{id}")
    int update(SellerPackage pkg);

    // 删除套餐（软删除，设置is_active=0）
    @Update("UPDATE seller_packages SET is_active = 0 WHERE id = #{id}")
    int deleteById(Long id);
}
