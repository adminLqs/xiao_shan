package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Coupon;
import com.xiaoshan.springbootdemo.entity.UserCoupon;
import com.xiaoshan.springbootdemo.mapper.CouponMapper;
import com.xiaoshan.springbootdemo.mapper.UserCouponMapper;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public List<Coupon> getSellerCoupons(Long sellerId, Integer status) {
        return couponMapper.findBySellerIdAndStatus(sellerId, status);
    }

    public Coupon getCouponById(Long id) {
        return couponMapper.findById(id)
                .orElseThrow(() -> new RuntimeException("优惠券不存在"));
    }

    @Transactional
    public Coupon createCoupon(Long sellerId, Coupon coupon) {
        coupon.setId(snowflakeIdGenerator.nextId());
        coupon.setSellerId(sellerId);
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        coupon.setStatus(1);
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());

        couponMapper.insert(coupon);
        log.info("商家 {} 创建优惠券: id={}, name={}", sellerId, coupon.getId(), coupon.getName());
        return coupon;
    }

    @Transactional
    public Coupon updateCoupon(Long id, Coupon coupon) {
        Coupon existing = getCouponById(id);
        coupon.setId(id);
        coupon.setSellerId(existing.getSellerId());
        coupon.setReceivedCount(existing.getReceivedCount());
        coupon.setUsedCount(existing.getUsedCount());
        coupon.setUpdatedAt(LocalDateTime.now());

        couponMapper.update(coupon);
        log.info("更新优惠券: id={}, name={}", id, coupon.getName());
        return coupon;
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon existing = getCouponById(id);
        if (existing.getStatus() == 2) {
            throw new RuntimeException("优惠券已删除");
        }
        couponMapper.deleteById(id);
        log.info("软删除优惠券: id={}", id);
    }

    @Transactional
    public Coupon updateCouponStatus(Long id, Integer status) {
        Coupon existing = getCouponById(id);
        if (existing.getStatus() == 2) {
            throw new RuntimeException("优惠券已删除，无法修改状态");
        }
        couponMapper.updateStatus(id, status);
        existing.setStatus(status);
        log.info("更新优惠券状态: id={}, status={}", id, status);
        return existing;
    }

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        if (coupon == null || orderAmount == null) {
            return BigDecimal.ZERO;
        }

        String type = coupon.getType();
        BigDecimal discount = BigDecimal.ZERO;

        switch (type) {
            case "FULL_REDUCTION":
                if (orderAmount.compareTo(coupon.getMinAmount()) >= 0) {
                    discount = coupon.getDiscountAmount();
                }
                break;
            case "DISCOUNT":
                BigDecimal rate = coupon.getDiscountRate();
                if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                    discount = orderAmount.multiply(BigDecimal.ONE.subtract(rate))
                            .setScale(2, RoundingMode.HALF_UP);
                }
                break;
            case "NO_THRESHOLD":
                if (orderAmount.compareTo(coupon.getDiscountAmount()) > 0) {
                    discount = coupon.getDiscountAmount();
                }
                break;
            default:
                log.warn("未知的优惠券类型: {}", type);
                return BigDecimal.ZERO;
        }

        BigDecimal minPayAmount = new BigDecimal("0.01");
        BigDecimal maxDiscount = orderAmount.subtract(minPayAmount);
        if (discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }

        return discount.max(BigDecimal.ZERO);
    }

    public List<Coupon> getAvailableCoupons() {
        return couponMapper.findAvailableCoupons();
    }

    public List<Coupon> getShopAvailableCoupons(Long sellerId) {
        return couponMapper.findAvailableBySellerId(sellerId);
    }

    @Transactional
    public UserCoupon receiveCoupon(Long userId, Long couponId) {
        Coupon coupon = getCouponById(couponId);

        if (coupon.getStatus() != 1) {
            throw new RuntimeException("优惠券不可领取");
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            throw new RuntimeException("优惠券尚未开始领取");
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            throw new RuntimeException("优惠券已过期");
        }

        if (coupon.getTotalCount() != null && coupon.getTotalCount() != -1
                && coupon.getReceivedCount() >= coupon.getTotalCount()) {
            throw new RuntimeException("优惠券已领完");
        }

        int received = userCouponMapper.countByUserIdAndCouponId(userId, couponId);
        if (coupon.getPerUserLimit() != null && received >= coupon.getPerUserLimit()) {
            throw new RuntimeException("已达领取上限");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(snowflakeIdGenerator.nextId());
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus("UNUSED");
        userCoupon.setReceivedAt(now);
        userCoupon.setExpireAt(coupon.getEndTime());

        userCouponMapper.insert(userCoupon);
        couponMapper.incrementReceivedCount(couponId);

        log.info("用户 {} 领取优惠券: userId={}, couponId={}, userCouponId={}",
                userId, userId, couponId, userCoupon.getId());
        return userCoupon;
    }

    public List<UserCoupon> getUserCoupons(Long userId, String status) {
        if (status == null || status.isEmpty()) {
            return userCouponMapper.findByUserId(userId);
        }
        return userCouponMapper.findByUserIdAndStatus(userId, status);
    }

    public List<UserCoupon> getApplicableCoupons(Long userId, BigDecimal orderAmount) {
        List<UserCoupon> userCoupons = userCouponMapper.findApplicableForOrder(userId, orderAmount);
        for (UserCoupon uc : userCoupons) {
            Coupon coupon = couponMapper.findById(uc.getCouponId()).orElse(null);
            uc.setCoupon(coupon);
        }
        return userCoupons;
    }

    @Transactional
    public void applyCouponToOrder(Long userId, Long orderId, Long userCouponId) {
        UserCoupon userCoupon = userCouponMapper.findById(userCouponId)
                .orElseThrow(() -> new RuntimeException("优惠券不存在"));

        if (!"UNUSED".equals(userCoupon.getStatus())) {
            throw new RuntimeException("优惠券已使用或已过期");
        }

        if (userCoupon.getUserId() == null || !userCoupon.getUserId().equals(userId)) {
            throw new RuntimeException("无权使用该优惠券");
        }

        userCouponMapper.markUsed(userCouponId, orderId);
        couponMapper.incrementUsedCount(userCoupon.getCouponId());

        log.info("订单 {} 使用优惠券: userCouponId={}", orderId, userCouponId);
    }
}
