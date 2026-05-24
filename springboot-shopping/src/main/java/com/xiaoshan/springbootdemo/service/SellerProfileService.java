package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.SellerProfile;
import com.xiaoshan.springbootdemo.entity.dto.SellerProfileDTO;
import com.xiaoshan.springbootdemo.mapper.ProductImageMapper;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.SellerProfileMapper;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerProfileMapper sellerProfileMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

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
        newProfile.setId(snowflakeIdGenerator.nextId()); // 生成雪花ID
        // 设置用户ID
        newProfile.setUserId(userId);
        // 设置默认店铺名称
        newProfile.setStoreName("商家店铺");
        // 店铺头像留空，让前端显示默认头像
        // 设置默认营业时间
        newProfile.setBusinessHours("09:00 - 21:00");

        // 插入默认商家资料到数据库
        sellerProfileMapper.insert(newProfile);


        return newProfile;
    }

    /**
     * 获取商家在售商品数量
     *
     * @param sellerId 商家ID
     * @return 在售商品数量
     */
    public long getSellerProductCount(Long sellerId) {
        return productMapper.countActiveBySellerId(sellerId);
    }

    /**
     * 获取商家的统计信息（粉丝数、评分、好评率、平均发货时间、发货准时率）
     *
     * @param sellerId 商家ID（店铺资料ID）
     * @return 包含统计信息的Map
     */
    public Map<String, Object> getSellerStatistics(Long sellerId) {
        long fansCount = sellerProfileMapper.countFollowers(sellerId);

        double rating = sellerProfileMapper.getAverageRating(sellerId);

        double positiveRate = sellerProfileMapper.getPositiveRate(sellerId);

        double avgDeliveryHours = sellerProfileMapper.getAverageDeliveryHours(sellerId);

        double onTimeRate = sellerProfileMapper.getOnTimeRate(sellerId);

        // 格式化评分为一位小数
        double formattedRating = Math.round(rating * 10) / 10.0;

        // 格式化好评率为整数
        int formattedPositiveRate = (int) Math.round(positiveRate);

        // 格式化平均发货时间（保留整数）
        int formattedAvgDeliveryHours = (int) Math.round(avgDeliveryHours);

        // 格式化准时率为整数
        int formattedOnTimeRate = (int) Math.round(onTimeRate);

        return Map.of(
                "fansCount", fansCount,
                "rating", formattedRating,
                "positiveRate", formattedPositiveRate,
                "avgDeliveryHours", formattedAvgDeliveryHours,
                "onTimeRate", formattedOnTimeRate
        );
    }

    /**
     * 更新商家信息（统一接口：支持基本信息 + 头像）
     *
     * @param userId 用户ID
     * @param storeInfoDTO 基本信息DTO（店铺名称、简介、营业时间、联系电话）
     * @param avatar 店铺头像文件（可选）
     */
    @Transactional
    public void updateSellerProfile(Long userId, SellerProfileDTO storeInfoDTO,
                                    MultipartFile avatar) {
        // 查询商家资料，不存在则创建默认资料
        SellerProfile profile = sellerProfileMapper.findByUserId(userId)
                .orElseGet(() -> {
                    // 创建新的商家资料对象
                    SellerProfile newProfile = new SellerProfile();
                    newProfile.setId(snowflakeIdGenerator.nextId()); // 生成雪花ID
                    // 设置用户ID
                    newProfile.setUserId(userId);
                    // 设置默认店铺名称
                    newProfile.setStoreName("商家店铺");
                    // 店铺头像留空，让前端显示默认头像
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

        // 更新店铺地址（非空则更新）
        if (storeInfoDTO.getAddress() != null && !storeInfoDTO.getAddress().isEmpty()) {
            profile.setAddress(storeInfoDTO.getAddress());
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


    }

    /**
     * 验证图片文件
     *
     * @param file 图片文件
     * @param type 文件类型描述
     */
    private void validateImageFile(MultipartFile file, String type) {
        // 检查文件空值
        if (file.isEmpty()) {
            throw new RuntimeException(type + "文件不能为空");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/jpg", "image/png").contains(contentType)) {
            throw new RuntimeException(type + "不支持的文件格式，仅支持JPEG、PNG");
        }

        // 检查文件大小
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException(type + "大小不能超过5MB");
        }
    }

    /**
     * 上传图片
     *
     * @param file 图片文件
     * @param userId 用户ID
     * @param type 图片类型（avatar）
     * @return 图片URL
     */
    private String uploadImage(MultipartFile file, Long userId, String type) {
        // 验证文件
        String typeDescription = "店铺头像";
        validateImageFile(file, typeDescription);

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

        try {
            // 提取文件名
            String oldFileName = oldFileUrl.substring(oldFileUrl.lastIndexOf("/") + 1);

            // 构建物理路径（去掉前导斜杠）
            Path oldFilePath = Paths.get(oldFileUrl.substring(1)).toAbsolutePath();

            // 旧文件路径判断
            if (Files.exists(oldFilePath)) {
                // 删除文件
                Files.delete(oldFilePath);
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

    /**
     * 根据用户ID获取商家资料（返回 Map 格式，用于前端）
     *
     * @param userId 用户ID
     * @return 商家资料（Map 格式），不存在返回 null
     */
    public Map<String, Object> getByUserId(Long userId) {
        Optional<SellerProfile> sellerProfileOpt = sellerProfileMapper.findByUserId(userId);
        if (sellerProfileOpt.isEmpty()) {
            return null;
        }
        SellerProfile profile = sellerProfileOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("id", profile.getId());
        result.put("userId", profile.getUserId());
        result.put("shopName", profile.getStoreName());
        result.put("phone", profile.getContactPhone());
        result.put("province", "");
        result.put("city", "");
        result.put("address", profile.getAddress());
        result.put("storeAvatar", profile.getStoreAvatar());
        result.put("storeDetail", profile.getStoreDetail());
        result.put("businessHours", profile.getBusinessHours());
        return result;
    }

}