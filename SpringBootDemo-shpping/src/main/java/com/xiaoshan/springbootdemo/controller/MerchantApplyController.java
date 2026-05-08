package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.dto.MerchantApplyDTO;
import com.xiaoshan.springbootdemo.service.MerchantApplyService;
import com.xiaoshan.springbootdemo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MerchantApplyController {

    private UserService userService;
    private MerchantApplyService merchantApplyService;

    // 商家入驻申请提交
    @PostMapping("/merchant/applications")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> submitMerchantApplication(
            Authentication authentication,
            @ModelAttribute @Valid MerchantApplyDTO merchantApplyDTO,
            BindingResult bindingResult) {

        long startTime = System.currentTimeMillis();
        log.info("开始处理商家入驻申请，店铺名称：{}", merchantApplyDTO.getStoreName());

        try {
            // 参数验证
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.joining("; "));

                log.warn("参数验证失败: {}", errorMessage);

                // 直接构建错误响应
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", errorMessage);
                errorResponse.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 获取当前用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 处理商家申请
            Map<String, Object> result = merchantApplyService.processMerchantApply(userId, merchantApplyDTO);

            // 构建成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商家申请提交成功，请等待审核");
            response.put("data", result);
            response.put("timestamp", System.currentTimeMillis());

            long endTime = System.currentTimeMillis();
            log.info("商家申请处理完成，申请ID: {}, 耗时: {}ms",
                    result.get("applicationId"), (endTime - startTime));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // 业务异常（用户可理解的错误）
            log.warn("商家申请业务异常: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            // 系统异常
            log.error("商家申请系统异常", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "系统错误，请稍后重试");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 提交审核结果
    @PostMapping("/merchant/applications/{applicationId}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> submitApplicationReview(
            @PathVariable Long applicationId,
            @RequestBody Map<String, String> reviewData) {
        try {
            String status = reviewData.get("status");
            String reviewNotes = reviewData.get("reviewNotes");

            // 参数验证
            if (!Arrays.asList("APPROVED", "REJECTED").contains(status)) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "审核状态无效"
                ));
            }

            if ("REJECTED".equals(status) && (reviewNotes == null || reviewNotes.trim().isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "拒绝申请时必须填写审核备注"
                ));
            }

            // 这里实现审核逻辑
            log.info("审核申请 ID: {}, 状态: {}, 备注: {}", applicationId, status, reviewNotes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "审核提交成功"
            ));

        } catch (Exception e) {
            log.error("提交审核失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }
}
