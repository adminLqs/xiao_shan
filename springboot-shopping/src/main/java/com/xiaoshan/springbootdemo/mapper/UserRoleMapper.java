package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.UserRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserRoleMapper {

    @Select("SELECT * FROM user_roles WHERE id = #{id}")
    UserRole findById(Long id);

    @Select("SELECT ur.* FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<UserRole> findByUserId(Long userId);

    @Select("SELECT ur.* FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = #{userId} AND r.name = #{roleName}")
    UserRole findByUserIdAndRoleName(@Param("userId") Long userId, @Param("roleName") String roleName);

    @Insert("INSERT INTO user_roles (user_id, role_id) VALUES (#{userId}, #{roleId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserRole userRole);

    @Delete("DELETE FROM user_roles WHERE user_id = #{userId} AND role_id = #{roleId}")
    void deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM user_roles WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);
}