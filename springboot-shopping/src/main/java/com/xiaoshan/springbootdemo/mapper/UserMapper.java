package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 负责用户表的 CRUD 操作和用户相关查询功能
 *
 * @author System
 * @date 2025
 */
@Mapper
public interface UserMapper {

    /**
     * 插入用户
     *
     * @param user 用户对象
     * @return 影响的行数（1表示插入成功，0表示失败）
     */
    @Insert("INSERT INTO users (account, password, status, created_at, role) " +
            "VALUES (#{account}, #{password}, #{status}, #{createdAt}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 根据主键ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息（可能为空）
     */
    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);

    /**
     * 根据账号查询用户
     *
     * @param account 用户账号
     * @return 用户信息（可能为空）
     */
    @Select("SELECT * FROM users WHERE account = #{account}")
    Optional<User> findByAccount(String account);

    /**
     * 根据主键ID更新用户信息
     *
     * @param user 用户对象
     * @return 影响的行数
     */
    @Update("UPDATE users SET account = #{account}, password = #{password}, " +
            "status = #{status}, role = #{role} WHERE id = #{id}")
    int updateById(User user);

    /**
     * 根据用户ID更新用户状态
     *
     * @param userId 用户ID
     * @param status 用户状态（0-禁用，1-启用）
     * @return 影响的行数
     */
    @Update("UPDATE users SET status = #{status} WHERE id = #{userId}")
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 根据用户ID更新用户角色
     *
     * @param userId 用户ID
     * @param role 用户角色（ADMIN, SELLER, USER）
     * @return 影响的行数
     */
    @Update("UPDATE users SET role = #{role} WHERE id = #{userId}")
    int updateRole(@Param("userId") Long userId, @Param("role") String role);

    /**
     * 根据用户ID更新用户密码
     *
     * @param userId 用户ID
     * @param password 加密后的密码
     * @return 影响的行数
     */
    @Update("UPDATE users SET password = #{password} WHERE id = #{userId}")
    int updatePassword(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 根据主键ID删除用户
     *
     * @param id 用户ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据用户ID删除用户
     *
     * @param userId 用户ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM users WHERE id = #{userId}")
    int deleteByUserId(Long userId);

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    @Select("SELECT * FROM users ORDER BY created_at DESC")
    List<User> findAll();

    /**
     * 分页查询用户
     *
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 用户列表
     */
    @Select("SELECT * FROM users ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<User> findPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据角色查询用户列表
     *
     * @param role 用户角色（ADMIN, SELLER, USER）
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE role = #{role} ORDER BY created_at DESC")
    List<User> findByRole(String role);

    /**
     * 根据状态查询用户列表
     *
     * @param status 用户状态（0-禁用，1-启用）
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE status = #{status} ORDER BY created_at DESC")
    List<User> findByStatus(Integer status);

    /**
     * 检查账号是否存在
     *
     * @param account 用户账号
     * @return true-存在，false-不存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE account = #{account}")
    boolean existsByAccount(String account);

    /**
     * 检查用户ID是否存在
     *
     * @param userId 用户ID
     * @return true-存在，false-不存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE id = #{userId}")
    boolean existsById(Long userId);

    /**
     * 统计用户总数
     *
     * @return 用户总数
     */
    @Select("SELECT COUNT(*) FROM users")
    int countTotal();

    /**
     * 统计指定角色的用户数量
     *
     * @param role 用户角色
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM users WHERE role = #{role}")
    int countByRole(String role);

    /**
     * 统计活跃用户数量（启用状态）
     *
     * @return 活跃用户数量
     */
    @Select("SELECT COUNT(*) FROM users WHERE status = 1")
    int countActiveUsers();

    /**
     * 根据账号模糊查询用户
     *
     * @param account 用户账号（支持模糊查询）
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE account LIKE CONCAT('%', #{account}, '%') ORDER BY created_at DESC")
    List<User> findByAccountLike(@Param("account") String account);

    /**
     * 批量更新用户状态
     *
     * @param userIds 用户ID列表
     * @param status 用户状态
     * @return 影响的行数
     */
    @Update("<script>" +
            "UPDATE users SET status = #{status} WHERE id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateStatus(@Param("userIds") List<Long> userIds, @Param("status") Integer status);

    /**
     * 批量删除用户
     *
     * @param userIds 用户ID列表
     * @return 影响的行数
     */
    @Delete("<script>" +
            "DELETE FROM users WHERE id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchDeleteByIds(@Param("userIds") List<Long> userIds);
}