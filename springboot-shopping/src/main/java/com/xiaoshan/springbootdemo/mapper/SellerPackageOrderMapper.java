package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.SellerPackageOrder;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface SellerPackageOrderMapper {

    @Insert("INSERT INTO seller_package_orders (id, " +
            "seller_id, package_id, package_name, price, start_date, end_date, status, payment_method, transaction_id, " +
            "remaining_seconds) " +
            "VALUES (#{id}, #{sellerId}, #{packageId}, #{packageName}, #{price}, #{startDate}, #{endDate}, #{status}, #{paymentMethod}, #{transactionId}, " +
            "#{remainingSeconds})")
    int insert(SellerPackageOrder order);

    @Select("SELECT * FROM seller_package_orders WHERE id = #{id}")
    Optional<SellerPackageOrder> findById(Long id);

    @Select("SELECT spo.* FROM seller_package_orders spo " +
            "WHERE spo.seller_id = #{sellerId} AND spo.status = 'ACTIVE' " +
            "AND spo.start_date <= NOW() AND spo.end_date > NOW() " +
            "ORDER BY spo.id DESC LIMIT 1")
    Optional<SellerPackageOrder> findCurrentActiveBySellerId(Long sellerId);

    @Select("SELECT sp.*, spo.id as order_id, spo.seller_id, spo.start_date, spo.end_date, spo.status, spo.created_at " +
            "FROM seller_package_orders spo " +
            "JOIN seller_packages sp ON spo.package_id = sp.id " +
            "WHERE spo.seller_id = #{sellerId} AND spo.status = 'ACTIVE' " +
            "AND spo.start_date <= NOW() AND spo.end_date > NOW() " +
            "ORDER BY sp.product_limit DESC LIMIT 1")
    Optional<Map<String, Object>> findCurrentActivePackageWithDetails(Long sellerId);

    @Select("SELECT * FROM seller_package_orders " +
            "WHERE seller_id = #{sellerId} AND package_id = #{packageId} AND status IN ('ACTIVE', 'PAUSED') " +
            "ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, end_date DESC LIMIT 1")
    Optional<SellerPackageOrder> findActiveSamePackage(@Param("sellerId") Long sellerId, @Param("packageId") Long packageId);

    @Select("SELECT * FROM seller_package_orders " +
            "WHERE seller_id = #{sellerId} AND package_id = #{packageId} AND status IN ('ACTIVE', 'PAUSED') " +
            "ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, end_date DESC LIMIT 1")
    Optional<SellerPackageOrder> findActiveOrPausedSamePackage(@Param("sellerId") Long sellerId, @Param("packageId") Long packageId);

    @Update("UPDATE seller_package_orders SET end_date = end_date + INTERVAL #{days} DAY, updated_at = NOW() " +
            "WHERE id = #{id}")
    int extendEndDate(@Param("id") Long id, @Param("days") Integer days);

    @Select("SELECT * FROM seller_package_orders " +
            "WHERE seller_id = #{sellerId} ORDER BY created_at DESC")
    List<SellerPackageOrder> findBySellerId(Long sellerId);

    @Update("UPDATE seller_package_orders SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT * FROM seller_package_orders " +
            "WHERE status = 'ACTIVE' AND end_date <= NOW()")
    List<SellerPackageOrder> findExpiredOrders();

    @Update("<script>" +
            "UPDATE seller_package_orders SET status = 'EXPIRED' " +
            "WHERE id IN " +
            "<foreach item='item' index='index' collection='list' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int batchUpdateExpired(@Param("list") List<Long> ids);

    @Select("SELECT * FROM seller_package_orders WHERE seller_id = #{sellerId} AND status NOT IN ('PENDING', 'CANCELLED') ORDER BY created_at DESC")
    List<SellerPackageOrder> findAllBySellerId(Long sellerId);

    @Select("SELECT * FROM seller_package_orders WHERE id = #{orderId}")
    Optional<SellerPackageOrder> findByOrderNumber(String orderId);

    @Select("SELECT * FROM seller_package_orders WHERE seller_id = #{sellerId} AND status = 'PAUSED' ORDER BY end_date DESC")
    List<SellerPackageOrder> findPausedBySellerId(Long sellerId);

    @Select("SELECT spo.* FROM seller_package_orders spo " +
            "JOIN seller_packages sp ON spo.package_id = sp.id " +
            "WHERE spo.seller_id = #{sellerId} AND spo.status = 'PAUSED' AND spo.remaining_seconds > 0 " +
            "ORDER BY CASE WHEN sp.product_limit = -1 THEN 999999 ELSE sp.product_limit END DESC, spo.created_at ASC")
    List<SellerPackageOrder> findPausedBySellerIdOrderByLevel(Long sellerId);

    @Update("UPDATE seller_package_orders SET status = 'EXPIRED', updated_at = NOW() WHERE seller_id = #{sellerId} AND status = 'ACTIVE'")
    void expireOldPackages(Long sellerId);

    @Select("SELECT DISTINCT seller_id FROM seller_package_orders " +
            "WHERE status = 'ACTIVE' AND end_date >= #{startOfDay} AND end_date <= #{endOfDay}")
    List<Long> findExpiredSellerIds(@Param("startOfDay") java.time.LocalDateTime startOfDay, 
                                     @Param("endOfDay") java.time.LocalDateTime endOfDay);

    @Update("UPDATE seller_package_orders SET status = #{status}, updated_at = NOW() WHERE seller_id = #{sellerId} AND status = 'ACTIVE'")
    int updateStatusBySellerId(@Param("sellerId") Long sellerId, @Param("status") String status);

    @Update("UPDATE seller_package_orders SET " +
            "status = #{status}, payment_method = #{paymentMethod}, transaction_id = #{transactionId}, " +
            "start_date = #{startDate}, end_date = #{endDate}, remaining_seconds = #{remainingSeconds}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    int update(SellerPackageOrder order);

    @Update("UPDATE seller_package_orders SET status = 'EXPIRED', updated_at = NOW() WHERE id = #{packageId}")
    void expireOrder(Long packageId);
}