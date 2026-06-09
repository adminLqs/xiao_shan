package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.ProductParam;
import com.xiaoshan.springbootdemo.entity.User;
import com.xiaoshan.springbootdemo.entity.UserProfile;
import com.xiaoshan.springbootdemo.entity.vo.ProductVO;
import com.xiaoshan.springbootdemo.entity.dto.LoginDTO;
import com.xiaoshan.springbootdemo.entity.dto.RegisterDTO;
import com.xiaoshan.springbootdemo.entity.dto.UserProfileDTO;
import com.xiaoshan.springbootdemo.mapper.ProductParamMapper;
import com.xiaoshan.springbootdemo.mapper.UserMapper;
import com.xiaoshan.springbootdemo.service.OnlineStatusService;
import com.xiaoshan.springbootdemo.service.ProductService;
import com.xiaoshan.springbootdemo.service.SellerProfileService;
import com.xiaoshan.springbootdemo.service.UserService;
import com.xiaoshan.springbootdemo.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final ProductService productService;
    private final SellerProfileService sellerProfileService;

    private final OnlineStatusService onlineStatusService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;


    /** ===================== 公共权限 ======================= */

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestBody @Valid LoginDTO loginDTO,
            HttpServletResponse response) {
        try {
            User user = userService.loginUser(loginDTO, response);

            List<String> roles = new ArrayList<>();
            if (user.getRoles() != null) {
                for (com.xiaoshan.springbootdemo.entity.Role role : user.getRoles()) {
                    roles.add(role.getName());
                }
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "登录成功");
            responseData.put("data", Map.of(
                    "id", user.getId(),
                    "role", user.getRole(),
                    "roles", roles
            ));
            return ResponseEntity.ok(responseData);

        } catch (RuntimeException e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "用户名或密码错误"
            ));
        }
    }

    // 注册用户
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterDTO registerDTO) {
        try {
            // 注册用户
            userService.registerUser(registerDTO);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "注册成功"
            ));

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.ok().body(errorResponse);
        }
    }

    /**
     * 获取当前登录用户的账号信息
     * 支持所有角色（USER、SELLER、ADMIN）调用
     * @param authentication Spring Security 认证信息
     * @return 用户账号信息，包含账号、角色、状态、头像、昵称等
     */
    @GetMapping("/account/profile")
    public ResponseEntity<Map<String, Object>> getAccountProfile(Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "用户未登录，请先登录"
                ));
            }

            Long id = userService.getCurrentUserId(authentication);

            Map<String, Object> accountProfile = userService.getAccountProfile(id);

            if (accountProfile == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "用户不存在"
                ));
            }

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "获取成功",
                    "data", Map.of("accountProfile", accountProfile)
            ));

        } catch (Exception e) {
            log.error("获取账号信息失败: {}", e.getMessage(), e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取账号信息失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/auth/logout")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> logout(HttpServletResponse response, Authentication authentication) {
        try {
            Long userId = null;
            if (authentication != null && authentication.getPrincipal() instanceof Long) {
                userId = (Long) authentication.getPrincipal();
            }
            userService.clearAuthCookie(response, userId);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "已退出"
            ));
        } catch(Exception e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "退出账户失败"
            ));
        }
    }

    /**
     * 切换活跃角色
     * POST /api/v1/user/switch-role
     */
    @PostMapping("/user/switch-role")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> switchRole(
            @RequestBody Map<String, String> requestBody,
            HttpServletResponse response,
            Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            String newRole = requestBody.get("role");

            if (newRole == null || newRole.isEmpty()) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "角色不能为空"
                ));
            }

            userService.switchActiveRole(userId, newRole);

            User user = userService.getUserWithRoles(userId);
            String newToken = jwtUtil.generateToken(user);
            stringRedisTemplate.opsForValue().set("jwt:user:" + userId, newToken);
            userService.setAuthCookie(response, newToken);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "角色切换成功",
                    "data", Map.of("role", newRole, "token", newToken)
            ));
        } catch (Exception e) {
            log.error("切换角色失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取用户角色列表
     * GET /api/v1/user/roles
     */
    @GetMapping("/user/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getUserRoles(Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            List<com.xiaoshan.springbootdemo.entity.Role> roles = userService.getUserRoles(userId);

            List<Map<String, Object>> roleList = roles.stream().map(role -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", role.getId());
                map.put("name", role.getName());
                map.put("description", role.getDescription());
                return map;
            }).toList();

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "data", Map.of("roles", roleList)
            ));
        } catch (Exception e) {
            log.error("获取角色列表失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取用户在线状态
     * GET /api/v1/user/{userId}/online-status
     */
    @GetMapping("/user/{userId}/online-status")
    public ResponseEntity<?> getOnlineStatus(@PathVariable Long userId) {
        try {
            Map<String, Object> status = onlineStatusService.getOnlineStatus(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", status
            ));
        } catch (Exception e) {
            log.error("获取在线状态失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    /** ================== 用户权限 ===================== */

    // 获取所有个人信息
    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        try {
            // 获取账号id
            Long id = userService.getCurrentUserId(authentication);

            // 获取所有个人信息
            UserProfile profile = userService.getUserProfile(id);

            // 判断个人信息是否存在
            if (profile == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "未查询到个人信息"
                ));
            }

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "data", Map.of("profile", profile),
                    "message", "获取成功"
            ));
        } catch (Exception e){
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }

    }

    // 头像上传
    @PostMapping("/user/avatar")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> updateAvatar(
            Authentication authentication,
            @RequestParam("avatar") MultipartFile avatarFile) {
        try {
            log.info("开始处理头像上传请求");

            Long userId = userService.getCurrentUserId(authentication);
            User user = userService.getCurrentUser(userId);

            // 更新用户头像
            String newAvatarUrl = userService.updateUserAvatar(user.getId(), avatarFile);

            // 创建响应体结构
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "头像更新成功",
                    "data", Map.of("avatarUrl", newAvatarUrl)
            ));

        } catch (RuntimeException e) {
            log.error("头像上传业务异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("头像上传系统异常", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    // 用户资料更新
    @PutMapping("/user/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> updateUserProfile(
            Authentication authentication,
            @RequestBody @Valid UserProfileDTO userProfileDTO) {
        try {
            // 获取userId
            Long userId = userService.getCurrentUserId(authentication);

            // 更新用户资料
            userService.updateUserProfile(userId, userProfileDTO);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "用户资料更新成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }




}