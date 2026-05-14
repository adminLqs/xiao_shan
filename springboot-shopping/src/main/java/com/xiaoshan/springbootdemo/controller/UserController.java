package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.User;
import com.xiaoshan.springbootdemo.entity.UserProfile;
import com.xiaoshan.springbootdemo.entity.dto.LoginDTO;
import com.xiaoshan.springbootdemo.entity.dto.RegisterDTO;
import com.xiaoshan.springbootdemo.entity.dto.UserProfileDTO;
import com.xiaoshan.springbootdemo.mapper.UserMapper;
import com.xiaoshan.springbootdemo.service.ProductService;
import com.xiaoshan.springbootdemo.service.SellerProfileService;
import com.xiaoshan.springbootdemo.service.UserService;
import com.xiaoshan.springbootdemo.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final JwtUtil jwtUtil;

    /** ===================== 公共权限 ======================= */

    // 登录用户
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestBody @Valid LoginDTO loginDTO,
            HttpServletResponse response) {
        try {
            // 登录逻辑返回用户信息
            User user = userService.loginUser(loginDTO);

            // 判断用户状态
            if (!user.getStatus()) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "账号已被禁用"
                ));
            }

            // 将用户信息封装为Json对象，用其密钥进行签名
            String token = jwtUtil.generateToken(user);

            // 生成JWT并设置到Cookie
            userService.setAuthCookie(response, token);

            // 返回用户信息和角色
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "登录成功");
            responseData.put("data", Map.of(
                    "id", user.getId(),
                    "role", user.getRole()
            ));
            return ResponseEntity.ok(responseData);

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of(
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

    // 退出账户
    @GetMapping("/auth/logout")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        try {
            // 清除Cookie
            userService.clearAuthCookie(response);

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
     * 首页商品列表（分页）
     * GET /api/v1/products?page=1&size=20&keyword=&level1CategoryId=
     */
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getHomeProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long level1CategoryId,  // 一级分类ID
            @RequestParam(required = false) Long level2CategoryId   // 二级分类ID
    ) {

        try {
            // 计算偏移量
            int offset = (page - 1) * pageSize;

            // 查询商品列表（只查询上架商品 status=1）
            List<Product> products = productService.getProductsForHome(
                    offset, pageSize, keyword, level1CategoryId, level2CategoryId
            );

            // 查询总数
            long total = productService.countProductsForHome(keyword, level1CategoryId, level2CategoryId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", products,
                    "total", total,
                    "page", page,
                    "size", pageSize,
                    "totalPages", (int) Math.ceil((double) total / pageSize)
            ));

        } catch (Exception e) {
            log.error("获取首页商品列表失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }

    }

    /**
     * 获取商品详情（公共方法）
     * GET /api/v1/products/{productId}
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<?> getProductDetail(@PathVariable Long productId) {
        try {

            // 查询商品详情（带分类名和所有图片）
            Product product = productService.getProductDetail(productId);

            // 验证商品是否存在
            if (product == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "商品不存在"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", product
            ));

        } catch (Exception e) {
            log.error("获取商品详情失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取当前登录用户的账号信息
     * 支持所有角色（USER、SELLER、ADMIN）调用
     * @param authentication Spring Security 认证信息
     * @return 用户账号信息，包含账号、角色、状态等
     */
    @GetMapping("/account/profile")
    public ResponseEntity<Map<String, Object>> getAccountProfile(Authentication authentication) {
        try {
            // 先检查认证对象
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "用户未登录，请先登录",
                        "code", 401
                ));
            }

            // 获取账号ID
            Long id = userService.getCurrentUserId(authentication);

            // 获取账号信息
            User accountProfile = userService.getCurrentUser(id);

            // 判断用户是否存在
            if (accountProfile == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "用户不存在",
                        "code", 404
                ));
            }

            // 返回用户信息（包含 role 字段）
            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "data", accountProfile,
                    "message", "获取成功",
                    "code", 200
            ));

        } catch (Exception e) {
            log.error("获取账号信息失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "获取账号信息失败: " + e.getMessage(),
                    "code", 500
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
                    "data", profile,
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
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "头像更新成功");
            response.put("data", newAvatarUrl);
            response.put("timestamp", System.currentTimeMillis());

            log.info("头像上传处理完成，返回响应");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("头像上传业务异常", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok().body(errorResponse);
        } catch (Exception e) {
            log.error("头像上传系统异常", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "系统错误，请稍后重试");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok().body(errorResponse);
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

            // 创建响应结构体
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户资料更新成功");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok().body(errorResponse);
        }
    }




}