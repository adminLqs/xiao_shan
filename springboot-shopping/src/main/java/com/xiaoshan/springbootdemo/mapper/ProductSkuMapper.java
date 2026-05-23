package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.ProductSku;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductSkuMapper {

    @Select("SELECT * FROM product_skus WHERE product_id = #{productId} ORDER BY sort_order, id")
    List<ProductSku> findByProductId(@Param("productId") Long productId);

    @Select("SELECT * FROM product_skus WHERE id = #{id}")
    Optional<ProductSku> findById(@Param("id") Long id);

    @Insert("INSERT INTO product_skus (product_id, sku_name, spec_info, price, original_price, stock, sku_image, sort_order) " +
            "VALUES (#{productId}, #{skuName}, #{specInfo}, #{price}, #{originalPrice}, #{stock}, #{skuImage}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductSku sku);

    @Update("UPDATE product_skus SET sku_name = #{skuName}, spec_info = #{specInfo}, price = #{price}, " +
            "original_price = #{originalPrice}, stock = #{stock}, sku_image = #{skuImage}, " +
            "sku_code = #{skuCode}, sort_order = #{sortOrder} WHERE id = #{id}")
    int update(ProductSku sku);

    @Delete("DELETE FROM product_skus WHERE product_id = #{productId}")
    int deleteByProductId(@Param("productId") Long productId);

    @Delete("DELETE FROM product_skus WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT SUM(stock) FROM product_skus WHERE product_id = #{productId}")
    Integer getTotalStockByProductId(@Param("productId") Long productId);

    @Select("SELECT MIN(price) FROM product_skus WHERE product_id = #{productId}")
    java.math.BigDecimal getMinPriceByProductId(@Param("productId") Long productId);

    /**
     * 批量插入 SKU
     */
    @Insert("<script>" +
            "INSERT INTO product_skus (product_id, sku_name, spec_info, price, original_price, stock, sku_image, sort_order) " +
            "VALUES " +
            "<foreach collection='list' item='sku' separator=','>" +
            "(#{sku.productId}, #{sku.skuName}, #{sku.specInfo}, #{sku.price}, #{sku.originalPrice}, #{sku.stock}, #{sku.skuImage}, #{sku.sortOrder})" +
            "</foreach>" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "list[].id")
    int batchInsert(@Param("productId") Long productId, @Param("list") List<ProductSku> skus);
}
