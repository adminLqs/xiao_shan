package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Coupon;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CouponMapper {

    @Select("<script>" +
            "SELECT * FROM coupons WHERE seller_id = #{sellerId} AND status != 2 " +
            "<if test='status != null'>AND status = #{status}</if> " +
            "ORDER BY created_at DESC" +
            "</script>")
    List<Coupon> findBySellerIdAndStatus(Long sellerId, Integer status);

    @Select("SELECT * FROM coupons WHERE id = #{id}")
    Optional<Coupon> findById(Long id);

    @Insert("INSERT INTO coupons (id, seller_id, name, type, min_amount, discount_amount, discount_rate, " +
            "total_count, received_count, used_count, per_user_limit, start_time, end_time, status) " +
            "VALUES (#{id}, #{sellerId}, #{name}, #{type}, #{minAmount}, #{discountAmount}, #{discountRate}, " +
            "#{totalCount}, #{receivedCount}, #{usedCount}, #{perUserLimit}, #{startTime}, #{endTime}, #{status})")
    int insert(Coupon coupon);

    @Update("UPDATE coupons SET name = #{name}, type = #{type}, min_amount = #{minAmount}, " +
            "discount_amount = #{discountAmount}, discount_rate = #{discountRate}, total_count = #{totalCount}, " +
            "per_user_limit = #{perUserLimit}, start_time = #{startTime}, end_time = #{endTime}, " +
            "status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int update(Coupon coupon);

    @Update("UPDATE coupons SET status = 2, updated_at = NOW() WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE coupons SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(Long id, Integer status);

    @Update("UPDATE coupons SET received_count = received_count + 1 WHERE id = #{id}")
    int incrementReceivedCount(Long id);

    @Update("UPDATE coupons SET used_count = used_count + 1 WHERE id = #{id}")
    int incrementUsedCount(Long id);

    @Select("<script>" +
            "SELECT * FROM coupons WHERE status = 1 AND start_time &lt;= NOW() AND end_time &gt;= NOW() " +
            "AND (total_count = -1 OR received_count &lt; total_count) " +
            "ORDER BY created_at DESC" +
            "</script>")
    List<Coupon> findAvailableCoupons();

    @Select("<script>" +
            "SELECT * FROM coupons WHERE status = 1 AND seller_id = #{sellerId} " +
            "AND start_time &lt;= NOW() AND end_time &gt;= NOW() " +
            "AND (total_count = -1 OR received_count &lt; total_count) " +
            "ORDER BY created_at DESC" +
            "</script>")
    List<Coupon> findAvailableBySellerId(Long sellerId);

    @Select("<script>" +
            "SELECT * FROM coupons WHERE status = 1 AND start_time &lt;= NOW() AND end_time &gt;= NOW() " +
            "AND (total_count = -1 OR received_count &lt; total_count) " +
            "AND (min_amount IS NULL OR min_amount &lt;= #{orderAmount}) " +
            "<if test='sellerIds != null and sellerIds.size() &gt; 0'>" +
            "  AND seller_id IN " +
            "  <foreach item='sellerId' collection='sellerIds' open='(' separator=',' close=')'>" +
            "    #{sellerId}" +
            "  </foreach>" +
            "</if>" +
            " ORDER BY created_at DESC" +
            "</script>")
    List<Coupon> findApplicableCoupons(@Param("orderAmount") BigDecimal orderAmount,
                                        @Param("sellerIds") List<Long> sellerIds);
}
