package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Role;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {

    @Select("SELECT * FROM roles WHERE id = #{id}")
    Role findById(Long id);

    @Select("SELECT * FROM roles WHERE name = #{name}")
    Role findByName(String name);

    @Select("SELECT * FROM roles")
    List<Role> findAll();

    @Insert("INSERT INTO roles (name, description) VALUES (#{name}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Role role);

    @Update("UPDATE roles SET name = #{name}, description = #{description} WHERE id = #{id}")
    void update(Role role);

    @Delete("DELETE FROM roles WHERE id = #{id}")
    void deleteById(Long id);
}