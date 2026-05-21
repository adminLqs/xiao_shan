package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    // ==================== 插入方法 ====================

    /**
     * 创建用户
     *
     * @param user 用户实体
     * @return 受影响行数
     */
    @Insert("INSERT INTO users(id, device_id, created_at) " +
            "VALUES(#{id}, #{deviceId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    // ==================== 查询方法 ====================

    /**
     * 根据设备ID查询用户ID
     *
     * @param deviceId 设备唯一标识
     * @return 用户ID
     */
    @Select("SELECT id FROM users WHERE device_id = #{deviceId}")
    Long findIdByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据设备ID查询用户信息
     *
     * @param deviceId 设备唯一标识
     * @return 用户实体
     */
    @Select("SELECT id, device_id, phone, created_at FROM users WHERE device_id = #{deviceId}")
    User findByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户实体
     */
    @Select("SELECT id, device_id, phone, created_at FROM users WHERE id = #{id}")
    User findById(@Param("id") Long id);

    /**
     * 根据手机号查询用户ID（用于找回账号）
     *
     * @param phone 手机号
     * @return 用户ID
     */
    @Select("SELECT id FROM users WHERE phone = #{phone}")
    Long findIdByPhone(@Param("phone") String phone);

    /**
     * 根据手机号查询用户信息
     *
     * @param phone 手机号
     * @return 用户实体
     */
    @Select("SELECT id, device_id, phone, created_at FROM users WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);



    // ==================== 更新方法 ====================

    /**
     * 更新用户手机号
     *
     * @param id    用户ID
     * @param phone 手机号
     * @return 受影响行数
     */
    @Update("UPDATE users SET phone = #{phone} WHERE id = #{id}")
    int updatePhone(@Param("id") Long id, @Param("phone") String phone);

    /**
     * 更新设备ID（重新绑定设备）
     *
     * @param id       用户ID
     * @param deviceId 新设备ID
     * @return 受影响行数
     */
    @Update("UPDATE users SET device_id = #{deviceId} WHERE id = #{id}")
    int updateDeviceId(@Param("id") Long id, @Param("deviceId") String deviceId);

    // ==================== 验证方法 ====================

    /**
     * 验证设备ID是否存在
     *
     * @param deviceId 设备ID
     * @return 是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM users WHERE device_id = #{deviceId}")
    boolean existsByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 验证手机号是否存在
     *
     * @param phone 手机号
     * @return 是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM users WHERE phone = #{phone}")
    boolean existsByPhone(@Param("phone") String phone);

    // ==================== 删除方法 ====================

    /**
     * 删除用户（慎用）
     *
     * @param id 用户ID
     * @return 受影响行数
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    int delete(@Param("id") Long id);

    // ==================== 统计方法 ====================

    /**
     * 统计总用户数
     *
     * @return 用户总数
     */
    @Select("SELECT COUNT(1) FROM users")
    long count();
}