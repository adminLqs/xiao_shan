package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.ProductParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductParamMapper {

    /**
     * 根据商品ID查询所有参数
     */
    @Select("SELECT id, product_id, param_name, param_value " +
            "FROM product_params WHERE product_id = #{productId} ORDER BY sort_order")
    List<ProductParam> findByProductId(@Param("productId") Long productId);

    /**
     * 插入商品参数
     */
    @Insert("INSERT INTO product_params (id, product_id, param_name, param_value, sort_order) " +
            "VALUES (#{id}, #{productId}, #{paramName}, #{paramValue}, #{sortOrder})")
    int insert(ProductParam productParam);

    /**
     * 删除商品的所有参数
     */
    @Delete("DELETE FROM product_params WHERE product_id = #{productId}")
    int deleteByProductId(@Param("productId") Long productId);
}