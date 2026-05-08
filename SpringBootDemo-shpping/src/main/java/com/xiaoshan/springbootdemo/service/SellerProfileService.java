package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.*;
import com.xiaoshan.springbootdemo.entity.dto.SellerProfileDTO;
import com.xiaoshan.springbootdemo.mapper.ProductImageMapper;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.SellerProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerProfileMapper sellerProfileMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;


    /**
     * 获取商家个人资料
     * 如果不存在则自动创建默认商家资料
     *
     * @param userId 用户ID
     * @return 商家资料
     */
    public SellerProfile getUserProfile(Long userId) {
        // 根据用户ID查询商家资料
        Optional<SellerProfile> sellerProfileOpt = sellerProfileMapper.findByUserId(userId);

        // 如果商家资料存在，直接返回
        if (sellerProfileOpt.isPresent()) {
            return sellerProfileOpt.get();
        }

        // 商家资料不存在，创建默认商家资料
        SellerProfile newProfile = new SellerProfile();
        // 设置用户ID
        newProfile.setUserId(userId);
        // 设置默认店铺名称
        newProfile.setStoreName("商家店铺");
        // 设置默认店铺Logo
        newProfile.setStoreAvatar("/images/seller-avatar.jpg");
        // 设置默认店铺横幅
        newProfile.setStoreBanner("/images/default-banner.jpg");
        // 设置默认营业时间
        newProfile.setBusinessHours("09:00 - 21:00");

        // 插入默认商家资料到数据库
        sellerProfileMapper.insert(newProfile);
        log.info("创建默认商家资料成功: userId={}", userId);

        return newProfile;
    }

    /**
     * 更新商家信息（统一接口：支持基本信息 + 头像 + 横幅）
     *
     * @param userId 用户ID
     * @param storeInfoDTO 基本信息DTO（店铺名称、简介、营业时间、联系电话）
     * @param avatar 店铺头像文件（可选）
     * @param banner 店铺横幅文件（可选）
     */
    @Transactional
    public void updateSellerProfile(Long userId, SellerProfileDTO storeInfoDTO,
                                    MultipartFile avatar, MultipartFile banner) {
        // 查询商家资料，不存在则创建默认资料
        SellerProfile profile = sellerProfileMapper.findByUserId(userId)
                .orElseGet(() -> {
                    // 创建新的商家资料对象
                    SellerProfile newProfile = new SellerProfile();
                    // 设置用户ID
                    newProfile.setUserId(userId);
                    // 设置默认店铺名称
                    newProfile.setStoreName("商家店铺");
                    // 设置默认店铺头像
                    newProfile.setStoreAvatar("/images/seller-avatar.jpg");
                    // 设置默认营业时间
                    newProfile.setBusinessHours("09:00 - 21:00");
                    return newProfile;
                });

        // 更新店铺名称（非空则更新）
        if (storeInfoDTO.getStoreName() != null && !storeInfoDTO.getStoreName().isEmpty()) {
            profile.setStoreName(storeInfoDTO.getStoreName());
        }

        // 更新店铺简介（非空则更新）
        if (storeInfoDTO.getStoreDetail() != null && !storeInfoDTO.getStoreDetail().isEmpty()) {
            profile.setStoreDetail(storeInfoDTO.getStoreDetail());
        }

        // 更新营业时间（非空则更新）
        if (storeInfoDTO.getBusinessHours() != null && !storeInfoDTO.getBusinessHours().isEmpty()) {
            profile.setBusinessHours(storeInfoDTO.getBusinessHours());
        }

        // 更新联系电话（非空则更新）
        if (storeInfoDTO.getContactPhone() != null && !storeInfoDTO.getContactPhone().isEmpty()) {
            profile.setContactPhone(storeInfoDTO.getContactPhone());
        }

        // 上传并更新店铺头像（有文件时）
        if (avatar != null && !avatar.isEmpty()) {
            // 删除旧头像
            cleanupOldFile(profile.getStoreAvatar());
            // 上传头像文件，返回访问URL
            String avatarUrl = uploadImage(avatar, userId, "avatar");
            // 设置新的头像URL
            profile.setStoreAvatar(avatarUrl);
        }

        // 上传并更新店铺横幅（有文件时）
        if (banner != null && !banner.isEmpty()) {
            // 删除旧横幅
            cleanupOldFile(profile.getStoreBanner());
            // 上传横幅文件，返回访问URL
            String bannerUrl = uploadImage(banner, userId, "banner");
            // 设置新的横幅URL
            profile.setStoreBanner(bannerUrl);
        }

        // 设置更新时间
        profile.setUpdatedAt(LocalDateTime.now());

        // 保存到数据库（新增或更新）
        if (profile.getId() == null) {
            // 新增商家资料
            sellerProfileMapper.insert(profile);
        } else {
            // 更新商家资料
            sellerProfileMapper.updateById(profile);
        }

        log.info("商家信息更新成功: userId={}", userId);
    }

    /**
     * 上传图片
     *
     * @param file 图片文件
     * @param userId 用户ID
     * @param type 图片类型（avatar/banner）
     * @return 图片URL
     */
    private String uploadImage(MultipartFile file, Long userId, String type) {
        try {
            // 目录结构: uploads/seller/{userId}/avatar/ 或 uploads/seller/{userId}/banner/
            String subDirectory = "seller/" + userId + "/" + type;
            Path uploadPath = Paths.get(uploadDir, subDirectory);

            // 创建目录
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 获取文件扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成文件名: 时间戳_UUID.扩展名
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;

            // 保存文件
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toAbsolutePath());

            // 返回访问路径
            return "/uploads/" + subDirectory + "/" + fileName;

        } catch (IOException e) {
            log.error("图片上传失败: {}", e.getMessage());
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }

    // 清理旧文件
    private void cleanupOldFile(String oldFileUrl) {
        // 路径为空，直接返回
        if (oldFileUrl == null || oldFileUrl.isEmpty()) {
            return;
        }

        // 默认头像不删除，直接返回
        if (oldFileUrl.contains("seller-avatar.jpg") || oldFileUrl.contains("default-banner.jpg")) {
            return;
        }

        try {
            // 提取文件名
            String oldFileName = oldFileUrl.substring(oldFileUrl.lastIndexOf("/") + 1);

            // 构建物理路径（去掉前导斜杠）
            Path oldFilePath = Paths.get(oldFileUrl.substring(1)).toAbsolutePath();

            // 旧文件路径判断
            if (Files.exists(oldFilePath)) {
                // 删除文件
                Files.delete(oldFilePath);

                log.debug("旧头像文件已删除: {}", oldFileName);
            }
        } catch (Exception e) {
            log.warn("删除旧头像文件失败: {}, 错误: {}", oldFileUrl, e.getMessage());
        }
    }

    // 获取文件扩展名
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

}