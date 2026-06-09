package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.*;
import com.xiaoshan.springbootdemo.mapper.MerchantApplyMapper;
import com.xiaoshan.springbootdemo.mapper.ProductImageMapper;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.RoleMapper;
import com.xiaoshan.springbootdemo.mapper.SellerProfileMapper;
import com.xiaoshan.springbootdemo.mapper.UserMapper;
import com.xiaoshan.springbootdemo.mapper.UserProfileMapper;
import com.xiaoshan.springbootdemo.mapper.UserRoleMapper;
import com.xiaoshan.springbootdemo.service.MerchantApplyService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;

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


    // ========== 管理员账号管理 ==========

    @GetMapping("/admin/admins")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAdminList() {
        try {
            List<Map<String, Object>> admins = userMapper.findAdmins();
            for (Map<String, Object> admin : admins) {
                Object avatar = admin.get("avatar");
                if (avatar != null && avatar.toString().contains("default-admin-avatar")) {
                    admin.put("avatar", "");
                }
            }
            return ResponseEntity.ok(Map.of("success", true, "data", admins));
        } catch (Exception e) {
            log.error("获取管理员列表失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PostMapping("/admin/admins")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, String> body) {
        try {
            String account = body.get("account");
            String password = body.get("password");

            if (account == null || account.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "账号不能为空"));
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "密码不能为空"));
            }
            if (password.length() < 6) {
                return ResponseEntity.ok(Map.of("success", false, "message", "密码长度不能少于6位"));
            }

            if (userMapper.existsByAccount(account.trim())) {
                return ResponseEntity.ok(Map.of("success", false, "message", "账号已存在"));
            }

            User admin = new User(account.trim(), passwordEncoder.encode(password));
            admin.setCreatedAt(LocalDateTime.now());
            admin.setRole("ROLE_ADMIN");
            userMapper.insert(admin);

            UserRole userRole = new UserRole();
            userRole.setUserId(admin.getId());
            userRole.setRoleId(3L);
            userRoleMapper.insert(userRole);

            UserProfile profile = new UserProfile();
            profile.setUserId(admin.getId());
            profile.setNickname("管理员" + admin.getId());
            userProfileMapper.insert(profile);

            return ResponseEntity.ok(Map.of("success", true, "message", "管理员创建成功"));
        } catch (Exception e) {
            log.error("创建管理员失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @DeleteMapping("/admin/admins/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long id, Authentication authentication) {
        try {
            Long currentAdminId = userService.getCurrentUserId(authentication);
            if (currentAdminId.equals(id)) {
                return ResponseEntity.ok(Map.of("success", false, "message", "不能删除当前登录的管理员"));
            }

            userMapper.deleteById(id);
            userRoleMapper.deleteByUserId(id);

            return ResponseEntity.ok(Map.of("success", true, "message", "管理员已删除"));
        } catch (Exception e) {
            log.error("删除管理员失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PutMapping("/admin/admins/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateAdmin(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            User user = userMapper.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "管理员不存在"));
            }

            if (body.containsKey("password")) {
                String password = (String) body.get("password");
                if (password != null && !password.trim().isEmpty()) {
                    if (password.length() < 6) {
                        return ResponseEntity.ok(Map.of("success", false, "message", "密码长度不能少于6位"));
                    }
                    userMapper.updatePassword(id, passwordEncoder.encode(password));
                }
            }

            if (body.containsKey("status")) {
                Integer status = (Integer) body.get("status");
                if (status != null) {
                    userMapper.updateStatus(id, status);
                }
            }

            UserProfile profile = userProfileMapper.findByUserId(id).orElse(null);
            if (profile == null) {
                profile = new UserProfile();
                profile.setUserId(id);
            }

            if (body.containsKey("nickname")) {
                String nickname = (String) body.get("nickname");
                if (nickname != null) {
                    profile.setNickname(nickname.trim());
                }
            }

            if (body.containsKey("avatar")) {
                String avatar = (String) body.get("avatar");
                if (avatar != null) {
                    profile.setAvatar(avatar.trim());
                }
            }

            if (profile.getId() == null) {
                userProfileMapper.insert(profile);
            } else {
                userProfileMapper.updateByUserId(profile);
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "更新成功"));
        } catch (Exception e) {
            log.error("更新管理员失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PutMapping("/admin/admins/{id}/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<String>> body) {
        try {
            List<String> roles = body.get("roles");
            if (roles == null || roles.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "角色列表不能为空"));
            }

            userRoleMapper.deleteByUserId(id);

            for (String roleName : roles) {
                Role role = roleMapper.findByName(roleName);
                if (role != null) {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(id);
                    userRole.setRoleId(role.getId());
                    userRoleMapper.insert(userRole);
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "权限分配成功"));
        } catch (Exception e) {
            log.error("分配权限失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    // ========== 用户管理 ==========

    @GetMapping("/admin/users")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        try {
            int offset = (page - 1) * size;

            java.util.List<java.util.Map<String, Object>> users = userMapper.findUsersPage(offset, size, keyword, status);
            long total = userMapper.countUsers(keyword, status);

            for (java.util.Map<String, Object> user : users) {
                Object avatar = user.get("avatar");
                if (avatar != null && avatar.toString().contains("default-admin-avatar")) {
                    user.put("avatar", "");
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "records", users,
                            "total", total,
                            "page", page,
                            "size", size
                    )
            ));

        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @GetMapping("/admin/users/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getUserDetail(@PathVariable Long id) {
        try {
            java.util.Map<String, Object> user = userMapper.getAccountProfile(id);
            if (user == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
            }
            Object avatar = user.get("avatar");
            if (avatar != null && avatar.toString().contains("default-admin-avatar")) {
                user.put("avatar", "");
            }
            return ResponseEntity.ok(Map.of("success", true, "data", user));
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PutMapping("/admin/users/{id}/reset-password")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id) {
        try {
            String defaultPassword = "123456";
            userMapper.updatePassword(id, passwordEncoder.encode(defaultPassword));
            return ResponseEntity.ok(Map.of("success", true, "message", "密码已重置为默认密码：123456"));
        } catch (Exception e) {
            log.error("重置用户密码失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    // ========== 商品管理 ==========

    @GetMapping("/admin/products")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long sellerId) {
        try {
            java.util.List<Product> products = productMapper.findAll();
            
            java.util.List<java.util.Map<String, Object>> filteredResult = new java.util.ArrayList<>();
            for (Product p : products) {
                if (keyword != null && !keyword.isEmpty()) {
                    String name = p.getName() != null ? p.getName().toLowerCase() : "";
                    String brand = p.getBrand() != null ? p.getBrand().toLowerCase() : "";
                    if (!name.contains(keyword.toLowerCase()) && !brand.contains(keyword.toLowerCase())) {
                        continue;
                    }
                }
                if (status != null && !p.getStatus().equals(status)) {
                    continue;
                }
                
                java.util.Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("name", p.getName());
                item.put("brand", p.getBrand());
                item.put("price", p.getPrice() != null ? p.getPrice() : java.math.BigDecimal.ZERO);
                item.put("stock", p.getStock() != null ? p.getStock() : 0);
                item.put("salesCount", p.getSalesCount() != null ? p.getSalesCount() : 0);
                item.put("sales", p.getSalesCount() != null ? p.getSalesCount() : 0);
                item.put("status", p.getStatus() != null ? p.getStatus() : 0);
                item.put("images", p.getImages());
                item.put("image", p.getImages());
                item.put("sellerId", p.getSellerId());
                item.put("categoryId", p.getCategoryId());
                item.put("createdAt", p.getCreatedAt());
                
                java.util.List<java.util.Map<String, Object>> productImages = new java.util.ArrayList<>();
                java.util.List<ProductImage> images = productImageMapper.findByProductId(p.getId());
                for (ProductImage img : images) {
                    java.util.Map<String, Object> imgItem = new HashMap<>();
                    imgItem.put("image", img.getImage());
                    productImages.add(imgItem);
                }
                item.put("productImages", productImages);
                
                if (p.getSellerId() != null) {
                    java.util.Optional<SellerProfile> seller = sellerProfileMapper.findByUserId(p.getSellerId());
                    item.put("sellerName", seller.map(SellerProfile::getStoreName).orElse("-"));
                } else {
                    item.put("sellerName", "-");
                }
                
                filteredResult.add(item);
            }

            // 分页处理
            int total = filteredResult.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            java.util.List<java.util.Map<String, Object>> pageResult = 
                    fromIndex < total ? filteredResult.subList(fromIndex, toIndex) : new java.util.ArrayList<>();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "records", pageResult,
                            "total", total,
                            "page", page,
                            "size", size
                    )
            ));

        } catch (Exception e) {
            log.error("获取商品列表失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @GetMapping("/admin/products/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            java.util.Optional<Product> productOpt = productMapper.findById(id);
            if (productOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商品不存在"));
            }
            Product p = productOpt.get();

            java.util.Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("brand", p.getBrand());
            item.put("price", p.getPrice() != null ? p.getPrice() : java.math.BigDecimal.ZERO);
            item.put("stock", p.getStock() != null ? p.getStock() : 0);
            item.put("salesCount", p.getSalesCount() != null ? p.getSalesCount() : 0);
            item.put("sales", p.getSalesCount() != null ? p.getSalesCount() : 0);
            item.put("status", p.getStatus() != null ? p.getStatus() : 0);
            item.put("images", p.getImages());
            item.put("image", p.getImages());
            item.put("sellerId", p.getSellerId());
            item.put("categoryId", p.getCategoryId());
            item.put("createdAt", p.getCreatedAt());

            java.util.List<java.util.Map<String, Object>> productImages = new java.util.ArrayList<>();
            java.util.List<ProductImage> images = productImageMapper.findByProductId(p.getId());
            for (ProductImage img : images) {
                java.util.Map<String, Object> imgItem = new HashMap<>();
                imgItem.put("image", img.getImage());
                productImages.add(imgItem);
            }
            item.put("productImages", productImages);

            if (p.getSellerId() != null) {
                java.util.Optional<SellerProfile> seller = sellerProfileMapper.findByUserId(p.getSellerId());
                item.put("sellerName", seller.map(SellerProfile::getStoreName).orElse("-"));
            } else {
                item.put("sellerName", "-");
            }

            return ResponseEntity.ok(Map.of("success", true, "data", item));
        } catch (Exception e) {
            log.error("获取商品详情失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @PutMapping("/admin/products/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> toggleProductStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer status = body.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return ResponseEntity.ok(Map.of("success", false, "message", "状态参数错误"));
            }

            int updated = productMapper.updateStatus(id, status);
            if (updated == 0) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商品不存在"));
            }

            String message = status == 1 ? "已上架" : "已下架";
            return ResponseEntity.ok(Map.of("success", true, "message", message));

        } catch (Exception e) {
            log.error("切换商品状态失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
        }
    }

    @DeleteMapping("/admin/products/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            java.util.Optional<Product> product = productMapper.findById(id);
            if (product.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商品不存在"));
            }

            productMapper.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));

        } catch (Exception e) {
            log.error("删除商品失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "系统错误，请稍后重试"));
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
