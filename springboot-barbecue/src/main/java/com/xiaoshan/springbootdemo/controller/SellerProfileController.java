package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.SellerProfile;
import com.xiaoshan.springbootdemo.entity.dto.SellerProfileDTO;
import com.xiaoshan.springbootdemo.service.OrderService;
import com.xiaoshan.springbootdemo.service.SellerProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 商家信息管理
 *
 * @author xiaoshan
 * @date 2026-05-06
 */
@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;
    private final OrderService orderService;

    // ==================== 商家信息管理 ====================

    /**
     * 获取商家信息
     * <p>
     * 获取当前商家的基本资料，包括店铺名称、联系方式、营业状态等。
     *
     * @return 响应结果，包含商家信息实体
     */
    @GetMapping("/seller/profile")
    public ResponseEntity<Map<Object, Object>> getProfile() {
        try {
            SellerProfile profile = sellerProfileService.getSellerProfile();

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "data", Map.of(
                            "profile", profile
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "data", "获取信息失败请稍后"
            ));
        }
    }

    /**
     * 更新商家信息
     * <p>
     * 更新商家的基本资料，如店铺名称、联系电话、营业时间等。
     *
     * @param profile 商家信息实体，包含需要更新的字段
     * @return 响应结果，包含更新后的商家信息
     */
    @PutMapping("/seller/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody SellerProfileDTO profile) {
        try {
            SellerProfile savedProfile = sellerProfileService.saveSellerProfile(profile);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "profile", savedProfile
                            ),
                    "message", "保存成功"
            ));
        } catch (IllegalArgumentException e) {
            log.warn("参数校验失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("更新商家信息失败", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统繁忙，请稍后重试"
            ));
        }

    }

    /**
     * 更新商家头像
     * <p>
     * 上传并更新商家的店铺头像。支持常见图片格式，文件大小不超过5MB。
     *
     * @param avatarFile 头像文件（multipart/form-data格式）
     * @return 响应结果，包含新的头像URL
     */
    @PostMapping("/seller/avatar")
    public ResponseEntity<?> updateSellerAvatar(@RequestParam("avatar") MultipartFile avatarFile) {
        log.info("收到商家头像更新请求，文件大小: {} bytes", avatarFile.getSize());

        try {
            String avatarUrl = sellerProfileService.updateSellerAvatar(avatarFile);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "头像更新成功",
                    "data", Map.of(
                            "storeAvatar", avatarUrl)
            ));
        } catch (Exception e) {
            log.error("商家头像更新失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "商家头像更新失败"
            ));
        }
    }





}