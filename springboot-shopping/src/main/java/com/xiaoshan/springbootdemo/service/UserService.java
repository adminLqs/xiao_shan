package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.User;
import com.xiaoshan.springbootdemo.entity.UserProfile;
import com.xiaoshan.springbootdemo.entity.dto.LoginDTO;
import com.xiaoshan.springbootdemo.entity.dto.RegisterDTO;
import com.xiaoshan.springbootdemo.entity.dto.UserProfileDTO;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.SellerProfileMapper;
import com.xiaoshan.springbootdemo.mapper.UserMapper;
import com.xiaoshan.springbootdemo.mapper.UserProfileMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileMapper userProfileMapper;
    private final SellerProfileMapper sellerProfileMapper;
    private final ProductMapper productMapper;

    // 配置文件获取上传目录
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    // 登录逻辑
    public User loginUser(LoginDTO loginDTO){
        User user = userMapper.findByAccount(loginDTO.getAccount())
                .orElseThrow(() -> new RuntimeException("用户不存在")); // 否则抛出错误

        if(!user.getStatus()){
            throw new RuntimeException("用户状态错误！");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误！");
        }

        return user;
    }

    // 注册逻辑
    @Transactional
    public User registerUser(RegisterDTO registerDTO){

        // 检验账号是否存在
        if (userMapper.existsByAccount(registerDTO.getAccount())) {
            throw new RuntimeException("用户已存在！");
        }

        // 检查密码与第二次输入是否相同
        if(!registerDTO.getConfirmPassword().equals(registerDTO.getPassword())){
            throw new RuntimeException("两次输入的密码不一致！");
        }

        // 加密密码
        String encodedPassword = passwordEncoder.encode(registerDTO.getPassword());

        // 创建User对象
        User user = new User(registerDTO.getAccount(), encodedPassword);
        user.setCreatedAt(LocalDateTime.now());

        // 保存User对象
        userMapper.insert(user);

        // 创建用户资料
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setAvatar("/images/user-avatar.jpg");
        profile.setNickname(registerDTO.getAccount());
        profile.setGender(UserProfile.Gender.UNKNOWN);

        // 保存UserProfile对象
        userProfileMapper.insert(profile);

        log.info("原始密码: {}", registerDTO.getPassword()); // 日志输出
        log.info("加密后密码: {}", encodedPassword);

        return user; // 返回用户对象
    }

    /**
     * 设置Cookie认证信息
     * 将JWT token存储到HttpOnly Cookie中，提高安全性
     *
     * @param response HTTP响应对象，用于添加Cookie
     * @param token JWT认证令牌
     */
    public void setAuthCookie(HttpServletResponse response, String token) {
        // 获取当前时间
        ZonedDateTime now = ZonedDateTime.now();
        // 计算Cookie有效期（1个月后的时间戳）
        long seconds = now.until(now.plusMonths(1), ChronoUnit.SECONDS);

        // 构建安全Cookie
        ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", token)
                .httpOnly(true)      // 禁止JavaScript访问，防止XSS攻击
              //  .secure(true)        // 仅通过HTTPS传输 开发环境false
                .sameSite("Lax")     // 防止CSRF攻击，允许同站点请求携带Cookie
                .path("/")           // Cookie生效路径，整个应用可用
                .maxAge(seconds)     // Cookie有效期
                .build();

        // 将Cookie添加到响应头
        response.addHeader("Set-Cookie", cookie.toString());
    }

    // 清除Cookie信息
    public void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", "")
                .path("/")
                .maxAge(0)
                .build();
        response.setHeader("Set-Cookie", cookie.toString());
    }

    // ------------------------ 更新用户头像 -----------------------------------
    /*
        1. 建目录 ✅
        2. 生名字 ✅
        3. 拼路径 ✅
        4. 存文件 ✅
        5. 返路径 ✅
    */

    // 修改用户头像
    public String updateUserAvatar(Long userId, MultipartFile avatarFile) {
        try {
            // 验证头像数据
            validateAvatarFile(avatarFile);

            // 获取用户信息
            UserProfile profile = getUserProfile(userId);

            // 获取旧头像路径
            String oldAvatarUrl = profile.getAvatar();

            // 生成唯一文件名
            String fileName = generateUniqueFileName(avatarFile);

            // 保存新头像到服务器
            String newAvatarUrl = saveAvatarFile(avatarFile, fileName, userId);

            // 数据库更新头像路径
            profile.setAvatar(newAvatarUrl);

            // 更新个人资料
            userProfileMapper.updateByUserId(profile);

            // 删除原始头像
            cleanupOldAvatarFile(oldAvatarUrl);

            log.info("用户头像更新成功 - 用户ID: {}, 文件名: {}", userId, fileName);
            return newAvatarUrl;

        } catch (Exception e) {
            log.error("头像更新失败 - 用户ID: {}, 错误: {}", userId, e.getMessage());
            throw new RuntimeException("头像更新失败: " + e.getMessage());
        }
    }

    // 验证用户头像文件
    private void validateAvatarFile(MultipartFile file) {
        // 检查文件空值
        if (file.isEmpty()) {
            throw new RuntimeException("头像文件不能为空");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp").contains(contentType)) {
            throw new RuntimeException("不支持的文件格式，仅支持JPEG、PNG、GIF、WebP");
        }

        // 检查文件大小
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过2MB");
        }

        // 检查文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
    }

    // 生成唯一文件名
    private String generateUniqueFileName(MultipartFile file) {
        // 返回唯一文件名: 时间戳_UUID.扩展名
        return System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8) +
                getFileExtension(file.getOriginalFilename());
    }

    // 获取文件扩展名
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    // 保存头像文件到服务器
    private String saveAvatarFile(MultipartFile file, String fileName, Long userId) throws IOException {
        // 头像目录路径: 工作目录/uploads/user/{userId}/avatar
        Path avatarDir = Paths.get(uploadDir, "user", String.valueOf(userId), "avatar").toAbsolutePath();

        // 创建目录结构（如果目录不存在）
        Files.createDirectories(avatarDir);

        // 拼接完整的文件路径
        Path targetPath = avatarDir.resolve(fileName);

        // 将上传的文件内容写入到目标路径
        file.transferTo(targetPath);

        log.info("文件已保存到: {}", targetPath.toAbsolutePath());

        return String.format("/uploads/user/%d/avatar/%s",userId, fileName);
    }

    // 清理旧头像文件
    private void cleanupOldAvatarFile(String oldAvatarUrl) {
        // 路径为空，直接返回
        if (oldAvatarUrl == null || oldAvatarUrl.isEmpty()) {
            return;
        }

        // 默认头像不删除
        if (oldAvatarUrl.contains("user-avatar.jpg")) {
            return;
        }

        try {
            // 提取文件名
            String oldFileName = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);

            // 构建物理路径（去掉前导斜杠）
            Path oldFilePath = Paths.get(oldAvatarUrl.substring(1)).toAbsolutePath();

            // 文件不存在，直接返回
            if (!Files.exists(oldFilePath)) {
                log.debug("旧头像文件不存在: {}", oldFileName);
                return;
            }

            // 删除文件
            Files.delete(oldFilePath);
            log.debug("旧头像文件已删除: {}", oldFileName);

        } catch (Exception e) {
            log.warn("删除旧头像文件失败: {}, 错误: {}", oldAvatarUrl, e.getMessage());
        }
    }

    /** ================= 更新用户信息 ============================== */

    // 更新用户资料
    public UserProfile updateUserProfile(Long userId, UserProfileDTO userProfileDTO) {
        // 获取用户信息
        UserProfile profile = getUserProfile(userId);

        try {
            updateProfileFieldsSafely(profile, userProfileDTO);
            validateBusinessRules(profile);

            if (profile.getId() == null) {
                userProfileMapper.insert(profile);
            } else {
                userProfileMapper.updateByUserId(profile);
            }

            log.info("用户资料更新成功 - 用户ID: {}", userId);
            return profile;

        } catch (DataIntegrityViolationException e) {
            log.error("数据唯一性约束冲突 - 用户ID: {}, 错误: {}", userId, e.getMessage());
            throw new RuntimeException("手机号或邮箱已被其他用户使用");
        } catch (RuntimeException e) {
            log.warn("业务规则校验失败 - 用户ID: {}, 错误: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("更新用户资料失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            throw new RuntimeException("系统错误，请稍后重试");
        }
    }

    // 安全更新字段：处理空字符串和空值逻辑
    private void updateProfileFieldsSafely(UserProfile profile, UserProfileDTO dto) {
        // 检查名称
        if (dto.getNickname() != null) {
            String nickname = dto.getNickname().trim();
            profile.setNickname(nickname.isEmpty() ? null : nickname);
        }

        // 检查性别
        if (dto.getGender() != null) {
            UserProfile.Gender gender = convertGenderWithValidation(dto.getGender());
            profile.setGender(gender);
        }

        // 检查生日
        if (dto.getBirthday() != null) {
            profile.setBirthday(dto.getBirthday());
        }

        // 检查地区
        if (dto.getRegion() != null) {
            String region = dto.getRegion().trim();
            profile.setRegion(region.isEmpty() ? null : region);
        }

        // 检查个人简介
        if (dto.getBio() != null) {
            String bio = dto.getBio().trim();
            profile.setBio(bio.isEmpty() ? null : bio);
        }

        // 检查电话号
        if (dto.getPhone() != null) {
            String phone = dto.getPhone().trim();
            if (phone.isEmpty()) {
                profile.setPhone(null);
                log.info("用户ID: {} 清空手机号", profile.getUserId()); // 修正：使用getUserId()
            } else {
                profile.setPhone(phone);
            }
        }

        // 检查邮箱
        if (dto.getEmail() != null) {
            String email = dto.getEmail().trim();
            if (email.isEmpty()) {
                profile.setEmail(null);
                profile.setEmailVerified(false);
                log.info("用户ID: {} 清空邮箱地址", profile.getUserId());
            } else if (!email.equals(profile.getEmail())) {
                profile.setEmail(email);
                profile.setEmailVerified(false);
                log.info("用户ID: {} 更新邮箱并重置验证状态", profile.getUserId()); // 修正：使用getUserId()
            }
        }
    }

    // 增强的性别转换方法，支持更多格式和严格验证
    private UserProfile.Gender convertGenderWithValidation(String genderStr) {
        if (genderStr == null || genderStr.trim().isEmpty()) {
            return UserProfile.Gender.UNKNOWN;
        }

        String normalized = genderStr.trim().toUpperCase();

        switch (normalized) {
            case "MALE":
                return UserProfile.Gender.MALE;
            case "FEMALE":
                return UserProfile.Gender.FEMALE;
            case "UNKNOWN": case "":
                return UserProfile.Gender.UNKNOWN;
            default:
                log.warn("无法识别的性别值: {}, 使用默认值UNKNOWN", genderStr);
                return UserProfile.Gender.UNKNOWN;
        }
    }

    // 业务规则校验
    private void validateBusinessRules(UserProfile profile) {
        if (profile.getEmail() != null && !isValidEmail(profile.getEmail())) {
            throw new RuntimeException("邮箱格式不正确");
        }

        if (profile.getPhone() != null && !isValidPhone(profile.getPhone())) {
            throw new RuntimeException("手机号格式不正确");
        }

        if (profile.getNickname() != null && profile.getNickname().length() > 16) {
            throw new RuntimeException("昵称长度不能超过16个字符");
        }

        if (profile.getBirthday() != null) {
            Period age = Period.between(profile.getBirthday(), LocalDate.now());
            if (age.getYears() < 13) {
                throw new RuntimeException("用户年龄必须大于13岁");
            }
        }
    }

    // 邮箱格式验证
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return Pattern.matches(emailRegex, email);
    }

    // 手机号格式验证
    private boolean isValidPhone(String phone) {
        String phoneRegex = "^1[3-9]\\d{9}$";
        return Pattern.matches(phoneRegex, phone);
    }


    // ---------------------- 验证获取用户操作 ---------------------------

    // 从安全上下文中获取当前用户ID
    public Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        } else {
            throw new RuntimeException("认证信息格式错误");
        }
    }

    // 获取完整的用户信息
    public User getCurrentUser(Long userId) {
        return userMapper.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    // 获取用户个人资料
    public UserProfile getUserProfile(Long userId) {
        return userProfileMapper.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("用户资料不存在"));
    }


}