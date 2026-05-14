package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.SellerProfile;
import com.xiaoshan.springbootdemo.entity.dto.SellerProfileDTO;
import com.xiaoshan.springbootdemo.service.SellerProfileService;
import com.xiaoshan.springbootdemo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class SellerProfileController {

    private final UserService userService;
    private final SellerProfileService sellerProfileService;

    // 获取商家信息
    @GetMapping("/seller/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getSellerProfile(Authentication authentication) {
        try {
            // 获取用户id
            Long id = userService.getCurrentUserId(authentication);

            // 获取所有个人信息
            SellerProfile profile = sellerProfileService.getUserProfile(id);

            // 判断个人信息是否存在
            if (profile == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "未查询到商家信息"
                ));
            }

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "data", profile,
                    "message", "获取成功"
            ));
        }catch (Exception e){
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 更新商家信息（包含基本信息、头像、横幅）
     * PUT /api/v1/seller/profile
     *
     * @param authentication 认证信息
     * @param avatar 店铺头像文件
     * @param banner 店铺横幅文件
     * @return 更新结果
     */
    @PutMapping("/seller/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> updateSellerProfile(
            Authentication authentication,
            @RequestPart("storeInfo") @Valid SellerProfileDTO storeInfoDTO,  // 基本信息JSON
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,  // 头像文件
            @RequestPart(value = "banner", required = false) MultipartFile banner   // 横幅文件
    ) {
        try {
            // 获取当前登录用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 更新商家信息
            sellerProfileService.updateSellerProfile(userId, storeInfoDTO, avatar, banner);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "更新成功"
            ));
        } catch (Exception e) {
            log.error("更新商家信息失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

}
