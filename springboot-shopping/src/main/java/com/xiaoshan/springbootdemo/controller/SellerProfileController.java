package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.MerchantApply;
import com.xiaoshan.springbootdemo.entity.SellerProfile;
import com.xiaoshan.springbootdemo.entity.dto.SellerProfileDTO;
import com.xiaoshan.springbootdemo.mapper.MerchantApplyMapper;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class SellerProfileController {

    private final UserService userService;
    private final SellerProfileService sellerProfileService;
    private final MerchantApplyMapper merchantApplyMapper;

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
                    "data", Map.of("profile", profile),
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
     * 获取商家收货地址
     * GET /api/v1/seller/{sellerId}/address
     */
    @GetMapping("/seller/{sellerId}/address")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getSellerAddress(@PathVariable Long sellerId) {
        try {
            SellerProfile profile = sellerProfileService.getUserProfile(sellerId);
            if (profile == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商家不存在"));
            }
            
            Map<String, String> result = new HashMap<>();
            result.put("address", profile.getAddress() != null ? profile.getAddress() : "");
            result.put("contactPhone", profile.getContactPhone() != null ? profile.getContactPhone() : "");
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            log.error("获取商家地址失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 根据商家ID获取商家信息（公开访问）
     * GET /api/v1/seller/info/{sellerId}
     */
    @GetMapping("/seller/info/{sellerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getSellerInfoById(@PathVariable Long sellerId) {
        try {
            // 获取商家资料
            SellerProfile profile = sellerProfileService.getUserProfile(sellerId);
            if (profile == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商家不存在"));
            }
            
            // 重要：获取真正的 userId（users.id），用于统计查询
            Long userId = profile.getUserId();

            
            // 使用 userId 获取商品数量
            long productCount = sellerProfileService.getSellerProductCount(userId);
            
            // 使用 userId 获取统计信息（粉丝数、评分、好评率等）
            Map<String, Object> statistics = sellerProfileService.getSellerStatistics(userId);
            
            // 使用 HashMap 避免 Map.of 的 null 值限制
            Map<String, Object> data = new HashMap<>();
            data.put("id", profile.getId());
            data.put("userId", userId);
            data.put("storeName", profile.getStoreName() != null ? profile.getStoreName() : "");
            data.put("storeAvatar", profile.getStoreAvatar() != null ? profile.getStoreAvatar() : "");
            data.put("storeDetail", profile.getStoreDetail() != null ? profile.getStoreDetail() : "");
            data.put("businessHours", profile.getBusinessHours() != null ? profile.getBusinessHours() : "");
            data.put("contactPhone", profile.getContactPhone() != null ? profile.getContactPhone() : "");
            data.put("address", profile.getAddress() != null ? profile.getAddress() : "");
            data.put("productCount", productCount);
            
            // 添加统计信息
            data.putAll(statistics);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", data);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取商家信息失败: {}", e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage() != null ? e.getMessage() : "获取商家信息失败");
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * 更新商家信息（包含基本信息、头像）
     * PUT /api/v1/seller/profile
     *
     * @param authentication 认证信息
     * @param avatar 店铺头像文件
     * @return 更新结果
     */
    @PutMapping("/seller/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> updateSellerProfile(
            Authentication authentication,
            @RequestPart("storeInfo") @Valid SellerProfileDTO storeInfoDTO,  // 基本信息JSON
            @RequestPart(value = "avatar", required = false) MultipartFile avatar   // 头像文件
    ) {
        try {
            // 获取当前登录用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 更新商家信息
            sellerProfileService.updateSellerProfile(userId, storeInfoDTO, avatar);

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

    /**
     * 获取商家资质信息
     * GET /api/v1/seller/{userId}/qualification
     */
    @GetMapping("/seller/{userId}/qualification")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getSellerQualification(@PathVariable Long userId) {
        try {
            Optional<MerchantApply> qualification = merchantApplyMapper.findLatestApprovedByUserId(userId);
            Map<String, Object> result = new HashMap<>();
            if (qualification.isPresent()) {
                result.put("success", true);
                result.put("data", qualification.get());
            } else {
                result.put("success", false);
                result.put("message", "暂无资质信息");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取商家资质信息失败: {}", e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage() != null ? e.getMessage() : "获取资质信息失败");
            return ResponseEntity.ok(errorResult);
        }
    }

}
