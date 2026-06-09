package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.OrderCoupon;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OrderCouponMapper {

    @Select("SELECT * FROM order_coupons WHERE order_id = #{orderId}")
    List<OrderCoupon> findByOrderId(Long orderId);

    @Select("SELECT * FROM order_coupons WHERE id = #{id}")
    Optional<OrderCoupon> findById(Long id);

    @Insert("INSERT INTO order_coupons (id, order_id, user_coupon_id, coupon_id, coupon_name, coupon_type, discount_amount) " +
            "VALUES (#{id}, #{orderId}, #{userCouponId}, #{couponId}, #{couponName}, #{couponType}, #{discountAmount})")
    int insert(OrderCoupon orderCoupon);
}
