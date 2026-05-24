package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.ProductSku;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductSkuMapper {

    @Select("SELECT * FROM product_skus WHERE product_id = #{productId} ORDER BY sort_order ASC, id ASC")
    List<ProductSku> findByProductId(@Param("productId") Long productId);

    @Select("SELECT * FROM product_skus WHERE id = #{id}")
    Optional<ProductSku> findById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT * FROM product_skus WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<ProductSku> findByIds(@Param("ids") List<Long> ids);

    @Insert("INSERT INTO product_skus (id, product_id, sku_name, spec_info, price, original_price, stock, sku_image, sort_order) " +
            "VALUES (#{id}, #{productId}, #{skuName}, #{specInfo}, #{price}, #{originalPrice}, #{stock}, #{skuImage}, #{sortOrder})")
    int insert(ProductSku sku);

    @Update("UPDATE product_skus SET sku_name = #{skuName}, spec_info = #{specInfo}, price = #{price}, " +
            "original_price = #{originalPrice}, stock = #{stock}, sku_image = #{skuImage}, " +
            "sort_order = #{sortOrder} WHERE id = #{id}")
    int update(ProductSku sku);

    @Delete("DELETE FROM product_skus WHERE product_id = #{productId}")
    int deleteByProductId(@Param("productId") Long productId);

    @Delete("DELETE FROM product_skus WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("<script>" +
            "DELETE FROM product_skus WHERE product_id = #{productId} " +
            "<if test='keepIds != null and keepIds.size() > 0'>" +
            "AND id NOT IN " +
            "<foreach collection='keepIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</if>" +
            "</script>")
    int deleteByProductIdAndNotInIds(@Param("productId") Long productId, @Param("keepIds") List<Long> keepIds);

    @Select("SELECT SUM(stock) FROM product_skus WHERE product_id = #{productId}")
    Integer getTotalStockByProductId(@Param("productId") Long productId);

    @Select("SELECT MIN(price) FROM product_skus WHERE product_id = #{productId}")
    java.math.BigDecimal getMinPriceByProductId(@Param("productId") Long productId);

    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM product_skus WHERE product_id = #{productId}")
    int getMaxSortOrder(@Param("productId") Long productId);

    /**
     * 批量插入 SKU
     */
    @Insert("<script>" +
            "INSERT INTO product_skus (id, product_id, sku_name, spec_info, price, original_price, stock, sku_image, sort_order) " +
            "VALUES " +
            "<foreach collection='list' item='sku' separator=','>" +
            "(#{sku.id}, #{sku.productId}, #{sku.skuName}, #{sku.specInfo}, #{sku.price}, #{sku.originalPrice}, #{sku.stock}, #{sku.skuImage}, #{sku.sortOrder})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("productId") Long productId, @Param("list") List<ProductSku> skus);
}
