package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.Category;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

/**
 * 商品分类数据访问接口
 * 负责分类表的 CRUD 操作和分类查询功能
 *
 * @author System
 * @date 2025
 */
@Mapper
public interface CategoryMapper {

    /**
     * 插入分类
     *
     * @param category 分类对象
     * @return 影响的行数（1表示插入成功，0表示失败）
     */
    @Insert("INSERT INTO categories (name, parent_id, sort_order, is_active, created_at) " +
            "VALUES (#{name}, #{parentId}, #{sortOrder}, #{isActive}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    /**
     * 根据主键ID更新分类信息
     *
     * @param category 分类对象
     * @return 影响的行数
     */
    @Update("UPDATE categories SET " +
            "name = #{name}, " +
            "parent_id = #{parentId}, " +
            "sort_order = #{sortOrder}, " +
            "is_active = #{isActive} " +
            "WHERE id = #{id}")
    int updateById(Category category);

    /**
     * 根据主键ID删除分类
     *
     * @param id 分类ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM categories WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据主键ID查询分类
     *
     * @param id 分类ID
     * @return 分类信息（可能为空）
     */
    @Select("SELECT id, name, parent_id, sort_order, is_active, created_at, updated_at " +
            "FROM categories WHERE id = #{id}")
    Optional<Category> findById(Long id);

    /**
     * 查询所有分类（按排序顺序）
     *
     * @return 分类列表
     */
    @Select("SELECT id, name, parent_id, sort_order, is_active, created_at, updated_at " +
            "FROM categories ORDER BY " +
            "CASE WHEN parent_id IS NULL THEN 0 ELSE parent_id END, sort_order, id")
    List<Category> findAll();

    /**
     * 查询所有启用的分类
     *
     * @return 启用的分类列表
     */
    @Select("SELECT id, name, parent_id, sort_order, is_active, created_at, updated_at " +
            "FROM categories WHERE is_active = 1 " +
            "ORDER BY CASE WHEN parent_id IS NULL THEN 0 ELSE parent_id END, sort_order, id")
    List<Category> findAllActive();

    /**
     * 查询所有一级分类（parent_id 为 NULL）
     *
     * @return 一级分类列表
     */
    @Select("SELECT id, name, parent_id, sort_order, is_active, created_at, updated_at " +
            "FROM categories WHERE parent_id IS NULL AND is_active = 1 " +
            "ORDER BY sort_order, id")
    List<Category> findLevel1Categories();

    /**
     * 根据父分类ID查询子分类
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @Select("SELECT id, name, parent_id, sort_order, is_active, created_at, updated_at " +
            "FROM categories WHERE parent_id = #{parentId} AND is_active = 1 " +
            "ORDER BY sort_order, id")
    List<Category> findChildrenByParentId(Long parentId);

    /**
     * 根据父分类ID查询所有子分类（包括禁用的）
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @Select("SELECT id, name, parent_id, sort_order, is_active, created_at, updated_at " +
            "FROM categories WHERE parent_id = #{parentId} " +
            "ORDER BY sort_order, id")
    List<Category> findAllChildrenByParentId(Long parentId);

    /**
     * 获取同级分类的最大排序序号
     *
     * @param parentId 父分类ID，null表示查询一级分类
     * @return 最大排序序号，如果没有数据则返回null
     */
    @Select("SELECT MAX(sort_order) FROM categories WHERE " +
            "<if test='parentId == null'>parent_id IS NULL</if>" +
            "<if test='parentId != null'>parent_id = #{parentId}</if>")
    Integer getMaxSortOrderByParentId(@Param("parentId") Long parentId);

    /**
     * 检查分类名称是否已存在（同一父级下）
     *
     * @param name 分类名称
     * @param parentId 父分类ID
     * @return true-存在，false-不存在
     */
    @Select("SELECT COUNT(*) > 0 FROM categories WHERE name = #{name} AND " +
            "<if test='parentId == null'>parent_id IS NULL</if>" +
            "<if test='parentId != null'>parent_id = #{parentId}</if>")
    boolean existsByNameAndParentId(@Param("name") String name, @Param("parentId") Long parentId);

    /**
     * 检查分类是否存在（根据主键ID）
     *
     * @param id 分类ID
     * @return true-存在，false-不存在
     */
    @Select("SELECT COUNT(*) > 0 FROM categories WHERE id = #{id}")
    boolean existsById(Long id);

    /**
     * 更新分类状态
     *
     * @param id 分类ID
     * @param isActive 状态（true-启用，false-禁用）
     * @return 影响的行数
     */
    @Update("UPDATE categories SET is_active = #{isActive} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("isActive") Boolean isActive);

    /**
     * 更新分类排序序号
     *
     * @param id 分类ID
     * @param sortOrder 排序序号
     * @return 影响的行数
     */
    @Update("UPDATE categories SET sort_order = #{sortOrder} WHERE id = #{id}")
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    /**
     * 获取分类下的商品数量
     *
     * @param categoryId 分类ID
     * @return 商品数量
     */
    @Select("SELECT COUNT(*) FROM products WHERE category_id = #{categoryId}")
    int countProductsByCategoryId(Long categoryId);

    /**
     * 批量删除分类
     *
     * @param ids 分类ID列表
     * @return 影响的行数
     */
    @Delete("<script>" +
            "DELETE FROM categories WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    /**
     * 批量更新分类状态
     *
     * @param ids 分类ID列表
     * @param isActive 状态
     * @return 影响的行数
     */
    @Update("<script>" +
            "UPDATE categories SET is_active = #{isActive} WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("isActive") Boolean isActive);
}