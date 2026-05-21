package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {

    // 根据ID查询
    @Select("SELECT * FROM products WHERE id = #{id}")
    Product findById(@Param("id") Long id);

    // 查询所有商品
    @Select("SELECT * FROM products ORDER BY created_at DESC")
    List<Product> findAll();

    // 根据分类查询
    @Select("SELECT * FROM products WHERE category = #{category} ORDER BY created_at DESC")
    List<Product> findByCategory(@Param("category") String category);

    // 分页查询
    @Select("SELECT * FROM products ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<Product> findPage(@Param("offset") int offset, @Param("limit") int limit);

    // 插入商品
    @Insert("INSERT INTO products(name, description, price, original_price, image, " +
            "category, created_at , update_at) " +
            "VALUES(#{name}, #{description}, #{price}, #{originalPrice}, #{image}, " +
            "#{category}, #{createdAt}, #{updateAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    // 更新商品
    @Update("UPDATE products SET " +
            "name = #{name}, description = #{description}, price = #{price}, " +
            "original_price = #{originalPrice}, image = #{image}, category = #{category}, " +
            "updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    int update(Product product);

    // 删除商品
    @Delete("DELETE FROM products WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    // 统计商品数量
    @Select("SELECT COUNT(*) FROM products")
    int count();

    // 根据分类统计
    @Select("SELECT COUNT(*) FROM products WHERE category = #{category}")
    int countByCategory(@Param("category") String category);
}