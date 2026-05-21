package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.SellerProfile;
import com.xiaoshan.springbootdemo.entity.dto.ProductDTO;
import com.xiaoshan.springbootdemo.entity.dto.SellerProfileDTO;
import com.xiaoshan.springbootdemo.mapper.*;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class SellerProfileService {

    private final ProductMapper productMapper;
    private final SellerProfileMapper sellerProfileMapper;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;


    // ================ 商家信息处理 ====================

    /**
     * 获取商家信息
     */
    public SellerProfile getSellerProfile() {
        return sellerProfileMapper.findById()
                .orElseGet(() -> {
                    log.info("商家信息不存在，返回默认对象");
                    SellerProfile defaultProfile = new SellerProfile();
                    defaultProfile.setStoreName("杉杉烤肉坊");
                    defaultProfile.setSlogan("炭火匠心 · 深夜烧烤");
                    defaultProfile.setStoreAvatar("/images/seller-avatar.jpg");
                    defaultProfile.setIsOpen(true);
                    sellerProfileMapper.insert(defaultProfile);
                    return defaultProfile;
                });
    }

    /**
     * 更新商家信息
     */
    @Transactional
    public SellerProfile saveSellerProfile(SellerProfileDTO profile) {
        // 先获取或创建商家信息
        SellerProfile existing = sellerProfileMapper.findById()
                .orElseGet(() -> {
                    SellerProfile defaultProfile = new SellerProfile();
                    defaultProfile.setStoreName("杉杉烤肉坊");
                    defaultProfile.setSlogan("炭火匠心 · 深夜烧烤");
                    defaultProfile.setStoreAvatar("/images/seller-avatar.jpg");
                    defaultProfile.setPhone("");
                    defaultProfile.setAddress("");
                    defaultProfile.setBusiness("10:00-22:00");
                    defaultProfile.setIsOpen(true);
                    sellerProfileMapper.insert(defaultProfile);
                    return defaultProfile;
                });

        // 执行更新
        sellerProfileMapper.update(profile);

        return existing;
    }

    // ============= 更新头像 ===============

    /**
     * 更新头像
     */
    public String updateSellerAvatar(MultipartFile avatarFile) {
        try {
            // 验证头像数据
            validateAvatarFile(avatarFile);

            // 获取商家信息
            SellerProfile profile = sellerProfileMapper.findById()
                    .orElseThrow(() -> new RuntimeException("商家信息不存在"));;

            // 获取旧头像路径
            String oldAvatarUrl = profile.getStoreAvatar();

            // 生成唯一文件名
            String fileName = generateUniqueFileName(avatarFile, "avatar");

            // 保存新头像到服务器
            String newAvatarUrl = saveAvatarFile(avatarFile, fileName);

            // 数据库更新头像路径
            profile.setStoreAvatar(newAvatarUrl);

            // 更新个人头像
            sellerProfileMapper.updateAvatar(newAvatarUrl);

            // 删除原始头像
            cleanupOldAvatarFile(oldAvatarUrl);
            log.info("商家头像更新成功, 文件名: {}", fileName);
            return newAvatarUrl;

        } catch (Exception e) {
            log.error("头像更新失败, 错误: {}", e.getMessage());
            throw new RuntimeException("头像更新失败: " + e.getMessage());
        }
    }

    // 验证商家头像文件
    private void validateAvatarFile(MultipartFile file) {
        // 检查文件空值
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("头像文件不能为空");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp").contains(contentType)) {
            throw new RuntimeException("不支持的文件格式，仅支持JPEG、PNG、GIF、WebP");
        }

        // 检查文件大小
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过5MB");
        }

        // 检查文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
    }


    // 保存头像文件到服务器
    private String saveAvatarFile(MultipartFile file, String fileName) throws IOException {
        // 头像上传目录
        Path avatarDir = Paths.get(uploadDir, "seller_avatars").toAbsolutePath();

        // 创建目录结构（如果目录不存在）
        if (!Files.exists(avatarDir)) {
            Files.createDirectories(avatarDir);
        }

        // 拼接完整的文件路径
        Path targetPath = avatarDir.resolve(fileName);

        // 将上传的文件内容写入到目标路径
        file.transferTo(targetPath);

        log.info("文件已保存到: {}", targetPath.toAbsolutePath());

        return "/uploads/seller_avatars/" + fileName;
    }

    // 清理旧头像文件
    private void cleanupOldAvatarFile(String oldAvatarUrl) {
        // 判断旧头像路径是否为空或基础头像
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty() && !oldAvatarUrl.contains("seller-avatar")) {
            try {
                // 旧文件名
                String oldFileName = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);
                // 旧文件路径
                Path oldFilePath = Paths.get(oldAvatarUrl.substring(1)).toAbsolutePath();

                // 文件删除判断
                if (Files.exists(oldFilePath)) {
                    Files.delete(oldFilePath);
                    log.debug("旧头像文件已删除: {}", oldFileName);
                }
            } catch (Exception e) {
                log.warn("删除旧头像文件失败: {}, 错误: {}", oldAvatarUrl, e.getMessage());
            }
        }
    }

    /**
     * 生成唯一文件名
     *
     * @param file   上传的文件
     * @param prefix 文件名前缀（如：product、avatar、seller）
     * @return 唯一文件名，格式：{prefix}_{timestamp}_{uuid}{extension}
     */
    public String generateUniqueFileName(MultipartFile file, String prefix) {
        // 获取文件后缀名
        String fileExtension = getFileExtension(file.getOriginalFilename());

        // 生成毫秒级时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());

        // 生成UUID（取前8位，保证唯一性）
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        return prefix + "_" + timestamp + "_" + uuid + fileExtension;
    }

    // 获取文件扩展名
    public String getFileExtension(String filename) {
        // 若为空或没有点则返回jpg
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")); // 从起始截取到末尾
    }

}
