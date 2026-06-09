package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.UserCoupon;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserCouponMapper {

    @Select("SELECT * FROM user_coupons WHERE user_id = #{userId} AND status = #{status} ORDER BY received_at DESC")
    List<UserCoupon> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT * FROM user_coupons WHERE user_id = #{userId} ORDER BY received_at DESC")
    List<UserCoupon> findByUserId(Long userId);

    @Select("SELECT * FROM user_coupons WHERE id = #{id}")
    Optional<UserCoupon> findById(Long id);

    @Select("SELECT COUNT(*) FROM user_coupons WHERE user_id = #{userId} AND coupon_id = #{couponId}")
    int countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Insert("INSERT INTO user_coupons (id, user_id, coupon_id, status, expire_at) " +
            "VALUES (#{id}, #{userId}, #{couponId}, #{status}, #{expireAt})")
    int insert(UserCoupon userCoupon);

    @Update("UPDATE user_coupons SET status = 'USED', order_id = #{orderId}, used_at = NOW() WHERE id = #{id}")
    int markUsed(@Param("id") Long id, @Param("orderId") Long orderId);

    @Update("<script>UPDATE user_coupons SET status = 'EXPIRED' WHERE status = 'UNUSED' AND expire_at &lt; NOW()</script>")
    int expireOutdatedCoupons();

    @Select("<script>" +
            "SELECT uc.*, c.id as coupon_id, c.name as coupon_name, c.type as coupon_type, " +
            "c.min_amount as coupon_min_amount, c.discount_amount as coupon_discount_amount, " +
            "c.discount_rate as coupon_discount_rate, c.start_time as coupon_start_time, " +
            "c.end_time as coupon_end_time " +
            "FROM user_coupons uc " +
            "JOIN coupons c ON uc.coupon_id = c.id " +
            "WHERE uc.user_id = #{userId} AND uc.status = 'UNUSED' " +
            "AND (c.min_amount IS NULL OR c.min_amount &lt;= #{orderAmount}) " +
            "AND uc.expire_at &gt;= NOW() " +
            "ORDER BY c.discount_amount DESC" +
            "</script>")
    List<UserCoupon> findApplicableForOrder(@Param("userId") Long userId,
                                              @Param("orderAmount") java.math.BigDecimal orderAmount);

    @Select("SELECT * FROM user_coupons WHERE order_id = #{orderId}")
    List<UserCoupon> findByOrderId(Long orderId);
}
