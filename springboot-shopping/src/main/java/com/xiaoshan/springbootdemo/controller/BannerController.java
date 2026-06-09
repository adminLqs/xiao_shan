package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Banner;
import com.xiaoshan.springbootdemo.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/admin/banners")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllBanners() {
        try {
            List<Banner> banners = bannerService.findAll();
            return ResponseEntity.ok(Map.of("success", true, "data", banners));
        } catch (Exception e) {
            log.error("获取Banner列表失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @GetMapping("/admin/banners/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getBannerById(@PathVariable Long id) {
        try {
            Banner banner = bannerService.findById(id);
            if (banner == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Banner不存在"));
            }
            return ResponseEntity.ok(Map.of("success", true, "data", banner));
        } catch (Exception e) {
            log.error("获取Banner详情失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @GetMapping("/banners")
    public ResponseEntity<?> getActiveBanners(@RequestParam(defaultValue = "HOME") String position) {
        try {
            List<Banner> banners = bannerService.findActiveByPosition(position);
            return ResponseEntity.ok(Map.of("success", true, "data", banners));
        } catch (Exception e) {
            log.error("获取Banner列表失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PostMapping("/admin/banners")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createBanner(@RequestBody Banner banner) {
        try {
            Banner created = bannerService.create(banner);
            return ResponseEntity.ok(Map.of("success", true, "data", created, "message", "创建成功"));
        } catch (IllegalArgumentException e) {
            log.warn("创建Banner失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("创建Banner失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PutMapping("/admin/banners/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateBanner(@PathVariable Long id, @RequestBody Banner banner) {
        try {
            Banner updated = bannerService.update(id, banner);
            return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "更新成功"));
        } catch (IllegalArgumentException e) {
            log.warn("更新Banner失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新Banner失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @DeleteMapping("/admin/banners/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteBanner(@PathVariable Long id) {
        try {
            bannerService.delete(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
        } catch (IllegalArgumentException e) {
            log.warn("删除Banner失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("删除Banner失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PutMapping("/admin/banners/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> toggleBannerStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer status = body.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return ResponseEntity.ok(Map.of("success", false, "message", "状态参数错误"));
            }
            bannerService.updateStatus(id, status);
            String message = status == 1 ? "已启用" : "已禁用";
            return ResponseEntity.ok(Map.of("success", true, "message", message));
        } catch (IllegalArgumentException e) {
            log.warn("更新Banner状态失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新Banner状态失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PutMapping("/admin/banners/{id}/sort")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateBannerSort(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer sortOrder = body.get("sortOrder");
            if (sortOrder == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "排序参数不能为空"));
            }
            bannerService.updateSortOrder(id, sortOrder);
            return ResponseEntity.ok(Map.of("success", true, "message", "排序更新成功"));
        } catch (IllegalArgumentException e) {
            log.warn("更新Banner排序失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新Banner排序失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }
}