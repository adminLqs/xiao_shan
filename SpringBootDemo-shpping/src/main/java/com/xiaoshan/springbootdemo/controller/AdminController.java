package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.service.MerchantApplyService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final MerchantApplyService merchantApplyService;

    // 商家入驻申请管理 - 获取申请列表（分页+筛选）
    @GetMapping("/merchant/applications")
    public ResponseEntity<?> getMerchantApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String search) {

        try {
            // 调用服务层获取分页数据
            Map<String, Object> result = merchantApplyService.getMerchantApplicationsWithPagination(
                    page, size, status, businessType, dateFrom, dateTo, search);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", result.get("data"),
                        "currentPage", result.get("currentPage"),
                        "pageSize", result.get("pageSize"),
                        "total", result.get("total"),
                        "totalPages", result.get("totalPages")
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", result.get("message")
                ));
            }

        } catch (Exception e) {
            log.error("获取商家申请列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
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
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "获取统计数据失败")
            );
        }
    }

    // 获取申请详情
    @GetMapping("/merchant/applications/{applicationId}")
    public ResponseEntity<?> getApplicationDetail(@PathVariable Long applicationId) {
        try {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "获取申请详情成功");

            Map<String, Object> data = new HashMap<>();
            data.put("id", applicationId);
            data.put("storeName", "示例店铺");
            data.put("contactName", "张三");
            data.put("contactPhone", "13800138000");
            data.put("businessType", "COMPANY");
            data.put("mainCategory", "ELECTRONICS");
            data.put("storeDetail", "这是一个示例店铺描述");
            data.put("companyName", "示例有限公司");
            data.put("creditCode", "91310101MA1F123456");
            data.put("businessLicense", "/uploads/merchant_apply/123/business_license.jpg");
            data.put("idCardFront", "/uploads/merchant_apply/123/id_front.jpg");
            data.put("idCardBack", "/uploads/merchant_apply/123/id_back.jpg");

            responseData.put("data", data);

            return ResponseEntity.ok(responseData);

        } catch (Exception e) {
            log.error("获取申请详情失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }


}
