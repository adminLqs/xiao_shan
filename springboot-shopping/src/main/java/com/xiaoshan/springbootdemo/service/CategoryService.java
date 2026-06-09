package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Category;
import com.xiaoshan.springbootdemo.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<Category> findAllActive() {
        return categoryMapper.findAllActive();
    }

    public List<Category> findAll() {
        return categoryMapper.findAll();
    }

    public Optional<Category> findById(Long id) {
        return categoryMapper.findById(id);
    }

    public List<Category> findLevel1Categories() {
        return categoryMapper.findLevel1Categories();
    }

    public List<Category> findChildrenByParentId(Long parentId) {
        return categoryMapper.findChildrenByParentId(parentId);
    }

    public List<Category> findAllChildrenByParentId(Long parentId) {
        return categoryMapper.findAllChildrenByParentId(parentId);
    }

    @Transactional
    public Category create(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }

        if (categoryMapper.existsByNameAndParentId(category.getName().trim(), category.getParentId())) {
            throw new IllegalArgumentException("同一父分类下已存在同名分类");
        }

        category.setName(category.getName().trim());
        if (category.getSortOrder() == null) {
            Integer maxSort = categoryMapper.getMaxSortOrderByParentId(category.getParentId());
            category.setSortOrder(maxSort == null ? 0 : maxSort + 1);
        }
        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }
        category.setCreatedAt(LocalDateTime.now());

        categoryMapper.insert(category);
        log.info("创建分类: id={}, name={}, parentId={}", category.getId(), category.getName(), category.getParentId());
        return category;
    }

    @Transactional
    public Category update(Long id, Category category) {
        Optional<Category> existingOpt = categoryMapper.findById(id);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("分类不存在");
        }

        Category existing = existingOpt.get();

        if (category.getName() != null && !category.getName().trim().isEmpty()) {
            String newName = category.getName().trim();
            if (!newName.equals(existing.getName())) {
                if (categoryMapper.existsByNameAndParentId(newName, existing.getParentId())) {
                    throw new IllegalArgumentException("同一父分类下已存在同名分类");
                }
                existing.setName(newName);
            }
        }

        if (category.getParentId() != null) {
            if (!categoryMapper.existsById(category.getParentId())) {
                throw new IllegalArgumentException("父分类不存在");
            }
            existing.setParentId(category.getParentId());
        }

        if (category.getSortOrder() != null) {
            existing.setSortOrder(category.getSortOrder());
        }

        if (category.getIsActive() != null) {
            existing.setIsActive(category.getIsActive());
        }

        categoryMapper.updateById(existing);
        log.info("更新分类: id={}, name={}", id, existing.getName());
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        Optional<Category> categoryOpt = categoryMapper.findById(id);
        if (categoryOpt.isEmpty()) {
            throw new IllegalArgumentException("分类不存在");
        }

        List<Category> children = categoryMapper.findAllChildrenByParentId(id);
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("该分类下存在子分类，无法删除");
        }

        int productCount = categoryMapper.countProductsByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalArgumentException("该分类下存在商品，无法删除");
        }

        categoryMapper.deleteById(id);
        log.info("删除分类: id={}", id);
    }

    @Transactional
    public void updateStatus(Long id, Boolean isActive) {
        if (!categoryMapper.existsById(id)) {
            throw new IllegalArgumentException("分类不存在");
        }

        categoryMapper.updateStatus(id, isActive);
        log.info("更新分类状态: id={}, isActive={}", id, isActive);
    }

    @Transactional
    public void updateSortOrder(Long id, Integer sortOrder) {
        if (!categoryMapper.existsById(id)) {
            throw new IllegalArgumentException("分类不存在");
        }

        categoryMapper.updateSortOrder(id, sortOrder);
        log.info("更新分类排序: id={}, sortOrder={}", id, sortOrder);
    }

    public Map<String, Object> getCategoryTree() {
        List<Category> allCategories = categoryMapper.findAll();

        List<Map<String, Object>> level1List = new ArrayList<>();
        for (Category c : allCategories) {
            if (c.getParentId() == null) {
                List<Map<String, Object>> children = new ArrayList<>();
                for (Category child : allCategories) {
                    if (child.getParentId() != null && child.getParentId().equals(c.getId())) {
                        Map<String, Object> childMap = new HashMap<>();
                        childMap.put("id", child.getId());
                        childMap.put("name", child.getName());
                        childMap.put("sortOrder", child.getSortOrder());
                        childMap.put("isActive", child.getIsActive());
                        childMap.put("createdAt", child.getCreatedAt());
                        children.add(childMap);
                    }
                }

                Map<String, Object> parentMap = new HashMap<>();
                parentMap.put("id", c.getId());
                parentMap.put("name", c.getName());
                parentMap.put("sortOrder", c.getSortOrder());
                parentMap.put("isActive", c.getIsActive());
                parentMap.put("createdAt", c.getCreatedAt());
                parentMap.put("children", children);
                level1List.add(parentMap);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", level1List);
        return result;
    }
}