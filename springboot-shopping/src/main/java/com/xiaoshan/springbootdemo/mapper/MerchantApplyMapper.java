package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.MerchantApply;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface MerchantApplyMapper {

    // 插入数据
    @Insert("INSERT INTO merchant_apply (" +
            "user_id, contact_name, contact_phone, contact_email, " +
            "store_name, store_detail, business_type, main_category, " +
            "business_license, id_card_front, id_card_back, " +
            "status, created_at, updated_at" +
            ") VALUES (" +
            "#{userId}, #{contactName}, #{contactPhone}, #{contactEmail}, " +
            "#{storeName}, #{storeDetail}, #{businessType}, #{mainCategory}, " +
            "#{businessLicense}, #{idCardFront}, #{idCardBack}, " +
            "#{status}, #{createdAt}, #{updatedAt}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MerchantApply apply);

    // 查询所有用户申请记录
    @Select("SELECT * FROM merchant_apply ORDER BY created_at DESC")
    List<MerchantApply> selectAllApplications();

    // 查找用户当前唯一的待处理申请
    @Select("SELECT * FROM merchant_apply WHERE user_id = #{userId} AND status = 'PENDING'")
    Optional<MerchantApply> selectPendingByUserId(Long userId);

    // 判断商家入驻申请是否为待定
    @Select("SELECT COUNT(*) FROM merchant_apply WHERE user_id = #{userId} AND status = 'PENDING'")
    boolean existsPendingByUserId(Long userId);

    // 商家申请状态统计
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending, " +
            "COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved, " +
            "COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected " +
            "FROM merchant_apply")
    Map<String, Object> countApplicationStatus();

    // 分页查询数据（带筛选条件）
    @Select("<script>" +
            "SELECT * FROM merchant_apply " +
            "<where>" +
            "   <if test=\"status != null and status != ''\">AND status = #{status}</if>" +
            "   <if test=\"businessType != null and businessType != ''\">AND business_type = #{businessType}</if>" +
            "   <if test=\"dateFrom != null and dateFrom != ''\">AND created_at &gt;= #{dateFrom}</if>" +
            "   <if test=\"dateTo != null and dateTo != ''\">AND created_at &lt;= #{dateTo}</if>" +
            "   <if test=\"search != null and search != ''\">" +
            "       AND (store_name LIKE CONCAT('%', #{search}, '%') OR " +
            "            contact_name LIKE CONCAT('%', #{search}, '%') OR " +
            "            contact_phone LIKE CONCAT('%', #{search}, '%'))" +
            "   </if>" +
            "</where>" +
            "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<MerchantApply> selectApplicationsWithPagination(
            @Param("pageSize") int pageSize,
            @Param("offset") int offset,
            @Param("status") String status,
            @Param("businessType") String businessType,
            @Param("dateFrom") String dateFrom,
            @Param("dateTo") String dateTo,
            @Param("search") String search);

    // 统计筛选条件下的总记录数
    @Select("<script>" +
            "SELECT COUNT(*) FROM merchant_apply " +
            "<where>" +
            "   <if test=\"status != null and status != ''\">AND status = #{status}</if>" +
            "   <if test=\"businessType != null and businessType != ''\">AND business_type = #{businessType}</if>" +
            "   <if test=\"dateFrom != null and dateFrom != ''\">AND created_at &gt;= #{dateFrom}</if>" +
            "   <if test=\"dateTo != null and dateTo != ''\">AND created_at &lt;= #{dateTo}</if>" +
            "   <if test=\"search != null and search != ''\">" +
            "       AND (store_name LIKE CONCAT('%', #{search}, '%') OR " +
            "            contact_name LIKE CONCAT('%', #{search}, '%') OR " +
            "            contact_phone LIKE CONCAT('%', #{search}, '%'))" +
            "   </if>" +
            "</where>" +
            "</script>")
    int countWithFilters(
            @Param("status") String status,
            @Param("businessType") String businessType,
            @Param("dateFrom") String dateFrom,
            @Param("dateTo") String dateTo,
            @Param("search") String search);

    // 统计各状态数量（带筛选条件）
    @Select("<script>" +
            "SELECT " +
            "COUNT(*) as total, " +
            "COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending, " +
            "COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved, " +
            "COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected " +
            "FROM merchant_apply " +
            "<where>" +
            "   <if test=\"status != null and status != ''\">AND status = #{status}</if>" +
            "   <if test=\"businessType != null and businessType != ''\">AND business_type = #{businessType}</if>" +
            "   <if test=\"dateFrom != null and dateFrom != ''\">AND created_at &gt;= #{dateFrom}</if>" +
            "   <if test=\"dateTo != null and dateTo != ''\">AND created_at &lt;= #{dateTo}</if>" +
            "   <if test=\"search != null and search != ''\">" +
            "       AND (store_name LIKE CONCAT('%', #{search}, '%') OR " +
            "            contact_name LIKE CONCAT('%', #{search}, '%') OR " +
            "            contact_phone LIKE CONCAT('%', #{search}, '%'))" +
            "   </if>" +
            "</where>" +
            "</script>")
    Map<String, Object> countStatusWithFilters(
            @Param("status") String status,
            @Param("businessType") String businessType,
            @Param("dateFrom") String dateFrom,
            @Param("dateTo") String dateTo,
            @Param("search") String search);
}