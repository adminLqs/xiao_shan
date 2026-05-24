package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.SellerPackageOrder;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface SellerPackageOrderMapper {

    // 插入购买记录
    @Insert("INSERT INTO seller_package_orders (id, " +
            "seller_id, package_id, package_name, price, start_date, end_date, status, payment_method, transaction_id, created_at, updated_at) " +
            "VALUES (#{id}, #{sellerId}, #{packageId}, #{packageName}, #{price}, #{startDate}, #{endDate}, #{status}, #{paymentMethod}, #{transactionId}, #{createdAt}, #{updatedAt})")
    int insert(SellerPackageOrder order);

    // 根据ID查询
    @Select("SELECT * FROM seller_package_orders WHERE id = #{id}")
    Optional<SellerPackageOrder> findById(Long id);

    // 查询商家的当前生效套餐（仅订单表）
    @Select("SELECT spo.* FROM seller_package_orders spo " +
            "WHERE spo.seller_id = #{sellerId} AND spo.status = 'ACTIVE' " +
            "AND spo.start_date <= NOW() AND spo.end_date > NOW() " +
            "ORDER BY spo.id DESC LIMIT 1")
    Optional<SellerPackageOrder> findCurrentActiveBySellerId(Long sellerId);

    // 查询商家的当前生效套餐（关联套餐表，获取完整信息）
    @Select("SELECT sp.*, spo.id as order_id, spo.seller_id, spo.start_date, spo.end_date, spo.status, spo.created_at " +
            "FROM seller_package_orders spo " +
            "JOIN seller_packages sp ON spo.package_id = sp.id " +
            "WHERE spo.seller_id = #{sellerId} AND spo.status = 'ACTIVE' " +
            "AND spo.start_date <= NOW() AND spo.end_date > NOW() " +
            "ORDER BY sp.product_limit DESC LIMIT 1")
    Optional<Map<String, Object>> findCurrentActivePackageWithDetails(Long sellerId);

    // 查询商家的同套餐 ACTIVE 订单（用于续费判断）
    @Select("SELECT * FROM seller_package_orders " +
            "WHERE seller_id = #{sellerId} AND package_id = #{packageId} AND status = 'ACTIVE' " +
            "AND start_date <= NOW() AND end_date > NOW() LIMIT 1")
    Optional<SellerPackageOrder> findActiveSamePackage(@Param("sellerId") Long sellerId, @Param("packageId") Long packageId);

    // 续费：延长套餐到期时间
    @Update("UPDATE seller_package_orders SET end_date = end_date + INTERVAL #{days} DAY, updated_at = NOW() " +
            "WHERE id = #{id}")
    int extendEndDate(@Param("id") Long id, @Param("days") Integer days);

    // 查询商家的所有套餐订单
    @Select("SELECT * FROM seller_package_orders " +
            "WHERE seller_id = #{sellerId} ORDER BY created_at DESC")
    List<SellerPackageOrder> findBySellerId(Long sellerId);

    // 更新套餐订单状态
    @Update("UPDATE seller_package_orders SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // 查询所有生效中的套餐订单（用于定时任务检查过期）
    @Select("SELECT * FROM seller_package_orders " +
            "WHERE status = 'ACTIVE' AND end_date <= NOW()")
    List<SellerPackageOrder> findExpiredOrders();

    // 批量更新过期状态
    @Update("<script>" +
            "UPDATE seller_package_orders SET status = 'EXPIRED' " +
            "WHERE id IN " +
            "<foreach item='item' index='index' collection='list' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int batchUpdateExpired(@Param("list") List<Long> ids);

    // 查询商家的所有套餐购买记录
    @Select("SELECT * FROM seller_package_orders WHERE seller_id = #{sellerId} ORDER BY created_at DESC")
    List<SellerPackageOrder> findAllBySellerId(Long sellerId);

    // 根据订单号查询（用于支付回调）
    @Select("SELECT * FROM seller_package_orders WHERE id = #{orderId}")
    Optional<SellerPackageOrder> findByOrderNumber(String orderId);

    // 将商家的旧套餐置为过期
    @Update("UPDATE seller_package_orders SET status = 'EXPIRED' WHERE seller_id = #{sellerId} AND status = 'ACTIVE'")
    void expireOldPackages(Long sellerId);

    // 查询今日到期的商家ID列表（用于定时任务）
    @Select("SELECT DISTINCT seller_id FROM seller_package_orders " +
            "WHERE status = 'ACTIVE' AND end_date >= #{startOfDay} AND end_date <= #{endOfDay}")
    List<Long> findExpiredSellerIds(@Param("startOfDay") java.time.LocalDateTime startOfDay, 
                                     @Param("endOfDay") java.time.LocalDateTime endOfDay);

    // 根据商家ID更新套餐状态
    @Update("UPDATE seller_package_orders SET status = #{status}, updated_at = NOW() WHERE seller_id = #{sellerId} AND status = 'ACTIVE'")
    int updateStatusBySellerId(@Param("sellerId") Long sellerId, @Param("status") String status);

    // 更新套餐订单（包含支付成功后的完整更新）
    @Update("UPDATE seller_package_orders SET " +
            "status = #{status}, payment_method = #{paymentMethod}, transaction_id = #{transactionId}, " +
            "start_date = #{startDate}, end_date = #{endDate}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    int update(SellerPackageOrder order);

    // 将指定套餐订单置为过期
    @Update("UPDATE seller_package_orders SET status = 'EXPIRED', updated_at = NOW() WHERE id = #{packageId}")
    void expireOrder(Long packageId);
}
