package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.ProductFreezeLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProductFreezeLogMapper {

    /**
     * 插入冻结记录
     */
    @Insert("INSERT INTO product_freeze_log (id, product_id, seller_id, freeze_time, unfreeze_time, reason) " +
            "VALUES (#{id}, #{productId}, #{sellerId}, #{freezeTime}, #{unfreezeTime}, #{reason})")
    int insert(ProductFreezeLog log);

    /**
     * 批量插入冻结记录
     */
    @Insert("<script>" +
            "INSERT INTO product_freeze_log (id, product_id, seller_id, freeze_time, unfreeze_time, reason) VALUES " +
            "<foreach collection='list' item='item' separator=','> " +
            "(#{item.id}, #{item.productId}, #{item.sellerId}, #{item.freezeTime}, #{item.unfreezeTime}, #{item.reason})" +
            "</foreach> " +
            "</script>")
    int batchInsert(@Param("list") List<ProductFreezeLog> logs);

    /**
     * 根据商品ID查询冻结记录
     */
    @Select("SELECT * FROM product_freeze_log WHERE product_id = #{productId} AND unfreeze_time IS NULL")
    List<ProductFreezeLog> findByProductId(Long productId);

    /**
     * 根据商家ID查询未解冻的冻结记录
     */
    @Select("SELECT * FROM product_freeze_log WHERE seller_id = #{sellerId} AND unfreeze_time IS NULL ORDER BY freeze_time DESC")
    List<ProductFreezeLog> findUnfrozenBySellerId(Long sellerId);

    /**
     * 更新解冻时间
     */
    @Update("UPDATE product_freeze_log SET unfreeze_time = #{unfreezeTime} WHERE product_id = #{productId} AND unfreeze_time IS NULL")
    int updateUnfreezeTime(Long productId, LocalDateTime unfreezeTime);

    /**
     * 批量更新解冻时间
     */
    @Update("<script>" +
            "UPDATE product_freeze_log SET unfreeze_time = #{unfreezeTime} WHERE product_id IN " +
            "<foreach collection='productIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND unfreeze_time IS NULL" +
            "</script>")
    int batchUpdateUnfreezeTime(@Param("productIds") List<Long> productIds, @Param("unfreezeTime") LocalDateTime unfreezeTime);

    /**
     * 删除解冻记录
     */
    @Delete("DELETE FROM product_freeze_log WHERE product_id = #{productId}")
    int deleteByProductId(Long productId);

    /**
     * 统计商家未解冻的冻结记录数量
     */
    @Select("SELECT COUNT(*) FROM product_freeze_log WHERE seller_id = #{sellerId} AND unfreeze_time IS NULL")
    long countUnfrozenBySellerId(Long sellerId);

    /**
     * 更新冻结记录
     */
    @Update("UPDATE product_freeze_log SET unfreeze_time = #{unfreezeTime}, reason = #{reason} WHERE id = #{id}")
    int updateById(ProductFreezeLog log);
}