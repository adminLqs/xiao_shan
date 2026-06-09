package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Category;
import com.xiaoshan.springbootdemo.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/api/v1/admin")
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories/tree")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getCategoryTree() {
        try {
            Map<String, Object> result = categoryService.getCategoryTree();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取分类树失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllCategories() {
        try {
            List<Category> categories = categoryService.findAll();
            return ResponseEntity.ok(Map.of("success", true, "data", categories));
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @GetMapping("/categories/level1")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getLevel1Categories() {
        try {
            List<Category> categories = categoryService.findLevel1Categories();
            return ResponseEntity.ok(Map.of("success", true, "data", categories));
        } catch (Exception e) {
            log.error("获取一级分类失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @GetMapping("/categories/{parentId}/children")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getChildrenCategories(@PathVariable Long parentId) {
        try {
            List<Category> categories = categoryService.findAllChildrenByParentId(parentId);
            return ResponseEntity.ok(Map.of("success", true, "data", categories));
        } catch (Exception e) {
            log.error("获取子分类失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @GetMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            var category = categoryService.findById(id);
            if (category.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "分类不存在"));
            }
            return ResponseEntity.ok(Map.of("success", true, "data", category.get()));
        } catch (Exception e) {
            log.error("获取分类详情失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        try {
            Category created = categoryService.create(category);
            return ResponseEntity.ok(Map.of("success", true, "data", created, "message", "创建成功"));
        } catch (IllegalArgumentException e) {
            log.warn("创建分类失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("创建分类失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        try {
            Category updated = categoryService.update(id, category);
            return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "更新成功"));
        } catch (IllegalArgumentException e) {
            log.warn("更新分类失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新分类失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.delete(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
        } catch (IllegalArgumentException e) {
            log.warn("删除分类失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("删除分类失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PutMapping("/categories/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> toggleCategoryStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        try {
            Boolean isActive = body.get("isActive");
            if (isActive == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "状态参数不能为空"));
            }
            categoryService.updateStatus(id, isActive);
            String message = isActive ? "已启用" : "已禁用";
            return ResponseEntity.ok(Map.of("success", true, "message", message));
        } catch (IllegalArgumentException e) {
            log.warn("更新分类状态失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新分类状态失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PutMapping("/categories/{id}/sort")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateCategorySort(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer sortOrder = body.get("sortOrder");
            if (sortOrder == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "排序参数不能为空"));
            }
            categoryService.updateSortOrder(id, sortOrder);
            return ResponseEntity.ok(Map.of("success", true, "message", "排序更新成功"));
        } catch (IllegalArgumentException e) {
            log.warn("更新分类排序失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新分类排序失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }
}