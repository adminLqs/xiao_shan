package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.ProductImage;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProductImageMapper {

    // 插入商品图片
    @Insert("INSERT INTO product_images (id, product_id, image, sort_order, created_at) " +
            "VALUES (#{id}, #{productId}, #{image}, #{sortOrder}, #{createdAt})")
    int insert(ProductImage productImage);

    // 根据商品ID查询图片列表
    @Select("SELECT id, product_id, image, sort_order, created_at " +
            "FROM product_images WHERE product_id = #{productId} " +
            "ORDER BY sort_order ASC")
    List<ProductImage> findByProductId(Long productId);

    // 根据商品ID查询主图（第一张图片）
    @Select("SELECT image FROM product_images WHERE product_id = #{productId} " +
            "ORDER BY sort_order ASC LIMIT 1")
    String findMainImageByProductId(Long productId);

    // 根据商品ID删除所有图片
    @Delete("DELETE FROM product_images WHERE product_id = #{productId}")
    int deleteByProductId(Long productId);

    // 根据图片ID删除
    @Delete("DELETE FROM product_images WHERE id = #{id}")
    int deleteById(Long id);

    // 更新图片排序
    @Update("UPDATE product_images SET sort_order = #{sortOrder} WHERE id = #{id}")
    int updateSortOrder(Long id, Integer sortOrder);
}