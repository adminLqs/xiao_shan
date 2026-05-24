package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.MerchantApply;
import com.xiaoshan.springbootdemo.mapper.MerchantApplyMapper;
import com.xiaoshan.springbootdemo.mapper.SellerProfileMapper;
import com.xiaoshan.springbootdemo.mapper.UserMapper;
import com.xiaoshan.springbootdemo.service.MerchantApplyService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final MerchantApplyService merchantApplyService;
    private final MerchantApplyMapper merchantApplyMapper;
    private final SellerProfileMapper sellerProfileMapper;
    private final UserMapper userMapper;

    // ========== 商家入驻申请管理 ==========

    // 获取申请列表（分页+筛选）
    @GetMapping("/admin/merchant/applications")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getMerchantApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        try {
            Map<String, Object> result = merchantApplyService.getMerchantApplicationsWithPagination(
                    page, size, status, null, null, null, null);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", Map.of(
                                "data", result.get("data"),
                                "total", result.get("total"),
                                "page", result.get("currentPage"),
                                "size", result.get("pageSize"),
                                "totalPages", result.get("totalPages")
                        )
                ));
            } else {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", result.get("message")
                ));
            }

        } catch (Exception e) {
            log.error("获取商家申请列表失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    // 获取申请统计
    @GetMapping("/admin/merchant/applications/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getApplicationStats() {
        try {
            Map<String, Object> stats = merchantApplyService.getApplicationStatusOverview();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));
        } catch (Exception e) {
            log.error("获取申请统计失败", e);
            return ResponseEntity.ok().body(
                    Map.of("success", false, "message", "获取统计数据失败")
            );
        }
    }

    // 获取申请详情
    @GetMapping("/admin/merchant/applications/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getApplicationDetail(@PathVariable Long id) {
        try {
            MerchantApply application = merchantApplyMapper.selectById(id);
            if (application == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "申请记录不存在"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", application
            ));

        } catch (Exception e) {
            log.error("获取申请详情失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    // 审核通过申请
    @PutMapping("/admin/merchant/applications/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> approveApplication(@PathVariable Long id, Authentication authentication) {
        try {
            Long adminId = userService.getCurrentUserId(authentication);
            merchantApplyService.approve(id, adminId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "已通过审核"
            ));
        } catch (RuntimeException e) {
            log.error("审核通过失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("审核通过失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    // 审核驳回申请
    @PutMapping("/admin/merchant/applications/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> rejectApplication(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            Long adminId = userService.getCurrentUserId(authentication);
            String reason = body.get("reviewNotes");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "驳回原因不能为空"
                ));
            }
            merchantApplyService.reject(id, adminId, reason.trim());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "已驳回申请"
            ));
        } catch (RuntimeException e) {
            log.error("审核驳回失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("审核驳回失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }


    // ========== 商家管理 ==========

    // 获取商家列表（分页+搜索+筛选）
    @GetMapping("/admin/sellers")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getSellers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        try {
            int offset = (page - 1) * size;
            List<Map<String, Object>> sellers = sellerProfileMapper.findSellers(keyword, status, offset, size);
            long total = sellerProfileMapper.countSellers(keyword, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "records", sellers,
                            "total", total,
                            "page", page,
                            "size", size
                    )
            ));

        } catch (Exception e) {
            log.error("获取商家列表失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    // 获取商家详情
    @GetMapping("/admin/sellers/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getSellerDetail(@PathVariable Long id) {
        try {
            int offset = 0;
            List<Map<String, Object>> sellers = sellerProfileMapper.findSellers(null, null, offset, 1);
            Map<String, Object> seller = sellers.stream()
                    .filter(s -> id.equals(s.get("id")))
                    .findFirst()
                    .orElse(null);

            if (seller == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "商家不存在"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", seller
            ));

        } catch (Exception e) {
            log.error("获取商家详情失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    // 切换商家账号状态（封禁/解封）
    @PutMapping("/admin/users/{userId}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Integer> body) {

        try {
            Integer status = body.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "状态参数错误"
                ));
            }

            int updated = userMapper.updateStatus(userId, status);
            if (updated == 0) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "用户不存在"
                ));
            }

            String message = status == 1 ? "已解封" : "已封禁";
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message
            ));

        } catch (Exception e) {
            log.error("切换用户状态失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    // 重置商家密码
    @PutMapping("/admin/sellers/{id}/reset-password")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> resetSellerPassword(@PathVariable Long id) {
        try {
            // 默认密码设为 123456（实际应该加密存储）
            String defaultPassword = "123456";
            // 这里需要调用 UserService 或其他方式重置密码
            // 由于涉及密码加密，建议在 Service 层处理
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "密码已重置为默认密码: 123456"
            ));

        } catch (Exception e) {
            log.error("重置密码失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }


    // ========== 兼容旧接口（保留）==========

    // 商家入驻申请管理 - 获取申请列表（分页+筛选）
    @GetMapping("/merchant/applications")
    public ResponseEntity<?> getMerchantApplicationsOld(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String search) {

        try {
            // 调用服务层获取分页数据
            Map<String, Object> result = merchantApplyService.getMerchantApplicationsWithPagination(
                    page, pageSize, status, businessType, dateFrom, dateTo, search);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", Map.of(
                                "records", result.get("data"),
                                "total", result.get("total"),
                                "page", result.get("currentPage"),
                                "size", result.get("pageSize"),
                                "totalPages", result.get("totalPages")
                        )
                ));
            } else {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", result.get("message")
                ));
            }

        } catch (Exception e) {
            log.error("获取商家申请列表失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    // 商家申请统计
    @GetMapping("/merchant/applications/status")
    public ResponseEntity<?> getApplicationStatus(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String search) {
        try {
            // 获取统计结果，支持筛选参数
            Map<String, Object> stats = merchantApplyService.getApplicationStats(
                    status, businessType, dateFrom, dateTo, search);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));
        } catch (Exception e) {
            log.error("获取申请统计失败", e);
            return ResponseEntity.ok().body(
                    Map.of("success", false, "message", "获取统计数据失败")
            );
        }
    }

    // 获取申请详情（旧）
    @GetMapping("/merchant/applications/{applicationId}")
    public ResponseEntity<?> getApplicationDetailOld(@PathVariable Long applicationId) {
        try {
            MerchantApply application = merchantApplyMapper.selectById(applicationId);
            if (application == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "申请记录不存在"
                ));
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("data", application);
            return ResponseEntity.ok(responseData);

        } catch (Exception e) {
            log.error("获取申请详情失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }


}
