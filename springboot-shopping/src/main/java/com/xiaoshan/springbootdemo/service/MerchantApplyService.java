package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.MerchantApply;
import com.xiaoshan.springbootdemo.entity.dto.MerchantApplyDTO;
import com.xiaoshan.springbootdemo.mapper.MerchantApplyMapper;
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
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class MerchantApplyService {

    private final MerchantApplyMapper merchantApplyMapper;

    /// 配置文件获取上传目录
    @Value("${app.file.uploads-dir:uploads}")
    private String uploadDir;


    // ================= 商家申请处理 =====================

    /**
     * 处理商家入驻申请
     * 包含业务验证、文件上传、数据保存等完整流程
     *
     * @param userId 申请用户ID
     * @param applyDTO 商家申请信息
     * @return 返回处理结果
     */
    @Transactional
    public Map<String, Object> processMerchantApply(Long userId, MerchantApplyDTO applyDTO) {
        log.info("开始处理用户 {} 的商家申请", userId);

        try {
            // 1. 基础信息校验
            validateBasicInfo(applyDTO);

            // 2. 检查用户是否已有待审核的申请
            checkExistingApply(userId);

            // 3. 验证文件合法性
            validateApplyFiles(applyDTO);

            // 4. 上传文件并获取URL
            Map<String, String> fileUrls = uploadApplyFiles(applyDTO, userId);

            // 5. 保存申请信息到数据库
            MerchantApply apply = saveApplyToDatabase(userId, applyDTO, fileUrls);

            // 6. 返回结果
            return buildSuccessResponse(apply);

        } catch (RuntimeException e) {
            log.error("商家申请业务异常 - 用户ID: {}, 错误: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("商家申请处理系统异常 - 用户ID: {}", userId, e);
            throw new RuntimeException("系统异常，请稍后重试");
        }
    }

    /**
     * 验证基础信息
     * 检查必填字段和格式
     */
    private void validateBasicInfo(MerchantApplyDTO applyDTO) {
        // 联系人信息校验已在DTO中通过注解完成，这里做二次校验
        if (applyDTO.getContactName() == null || applyDTO.getContactName().trim().isEmpty()) {
            throw new RuntimeException("联系人姓名不能为空");
        }

        if (applyDTO.getContactPhone() == null || applyDTO.getContactPhone().trim().isEmpty()) {
            throw new RuntimeException("联系电话不能为空");
        }

        // 手机号格式校验（简单校验）
        if (!applyDTO.getContactPhone().matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("请输入正确的手机号码格式");
        }

        if (applyDTO.getContactEmail() == null || applyDTO.getContactEmail().trim().isEmpty()) {
            throw new RuntimeException("联系邮箱不能为空");
        }

        // 邮箱格式校验
        if (!applyDTO.getContactEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new RuntimeException("请输入正确的邮箱格式");
        }

        if (applyDTO.getStoreName() == null || applyDTO.getStoreName().trim().isEmpty()) {
            throw new RuntimeException("店铺名称不能为空");
        }

        if (applyDTO.getBusinessType() == null || applyDTO.getBusinessType().trim().isEmpty()) {
            throw new RuntimeException("经营类型不能为空");
        }

        if (applyDTO.getMainCategory() == null || applyDTO.getMainCategory().trim().isEmpty()) {
            throw new RuntimeException("主营类目不能为空");
        }
    }

    /**
     * 检查用户是否已有待审核的申请
     *
     * @param userId 用户ID
     */
    private void checkExistingApply(Long userId) {
        if (merchantApplyMapper.existsPendingByUserId(userId)) {
            throw new RuntimeException("您已有待审核的商家申请，请等待审核结果");
        }
    }

    /**
     * 验证申请文件
     * 检查文件大小、格式等
     *
     * @param applyDTO 商家申请DTO
     */
    private void validateApplyFiles(MerchantApplyDTO applyDTO) {
        // 验证营业执照
        validateFile(applyDTO.getBusinessLicense(), "营业执照", 5 * 1024 * 1024,
                Arrays.asList("image/jpeg", "image/png", "image/jpg", "application/pdf"));

        // 验证身份证正面
        validateFile(applyDTO.getIdCardFront(), "身份证正面", 2 * 1024 * 1024,
                Arrays.asList("image/jpeg", "image/png", "image/jpg"));

        // 验证身份证反面
        validateFile(applyDTO.getIdCardBack(), "身份证反面", 2 * 1024 * 1024,
                Arrays.asList("image/jpeg", "image/png", "image/jpg"));
    }

    /**
     * 通用文件验证方法
     *
     * @param file 待验证的文件
     * @param fileType 文件类型名称（用于错误提示）
     * @param maxSize 最大允许大小（字节）
     * @param allowedTypes 允许的MIME类型列表
     */
    private void validateFile(MultipartFile file, String fileType, long maxSize, List<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException(fileType + "不能为空");
        }

        if (file.getSize() > maxSize) {
            throw new RuntimeException(fileType + "大小不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new RuntimeException(fileType + "文件类型无效");
        }

        // 检查MIME类型是否在允许列表中
        boolean isAllowed = allowedTypes.stream()
                .anyMatch(allowedType -> contentType.equalsIgnoreCase(allowedType));

        if (!isAllowed) {
            throw new RuntimeException(fileType + "格式不支持，请上传 " + String.join("、", allowedTypes) + " 格式的文件");
        }

        // 可选：验证文件名后缀
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.isEmpty()) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            boolean hasValidExtension = allowedTypes.stream()
                    .anyMatch(type -> type.contains(extension) ||
                            (type.equals("image/jpeg") && extension.equals("jpg")));

            if (!hasValidExtension) {
                log.warn("文件扩展名与MIME类型不匹配: 文件名={}, MIME类型={}", originalFilename, contentType);
            }
        }
    }

    /**
     * 上传申请文件
     * 将营业执照、身份证正反面照片上传到服务器指定目录
     *
     * @param applyDTO 商家申请DTO，包含上传的文件
     * @return 返回文件访问路径的Map集合
     */
    private Map<String, String> uploadApplyFiles(MerchantApplyDTO applyDTO, Long userId) {
        // 存储各类型文件的访问路径
        Map<String, String> fileUrls = new HashMap<>();

        try {
            // 统一目录: uploads/user/{userId}/merchant_apply/
            String subDirectory = String.format("user/%d/merchant_apply", userId);

            // 上传营业执照
            String licensePath = storeFile(applyDTO.getBusinessLicense(), subDirectory);
            fileUrls.put("businessLicense", licensePath);

            // 上传身份证正面
            String idCardFrontPath = storeFile(applyDTO.getIdCardFront(), subDirectory);
            fileUrls.put("idCardFront", idCardFrontPath);

            // 上传身份证反面
            String idCardBackPath = storeFile(applyDTO.getIdCardBack(), subDirectory);
            fileUrls.put("idCardBack", idCardBackPath);

            log.info("商家申请文件上传成功，用户ID: {}, 文件路径: {}", userId, fileUrls);

        } catch (Exception e) {
            log.error("商家申请文件上传失败，用户ID: {}, 错误信息: {}", userId, e.getMessage());
            throw new RuntimeException("文件上传失败，请稍后重试");
        }

        return fileUrls;
    }

    /**
     * 存储文件到服务器
     * 生成唯一文件名，避免文件名冲突
     *
     * @param file 待上传的文件
     * @param subDirectory 子目录路径（相对于上传根目录）
     * @return 返回文件的相对访问路径
     */
    private String storeFile(MultipartFile file, String subDirectory) {
        try {
            // 构建完整的文件存储路径（上传根目录 + 子目录）
            Path uploadPath = Paths.get(uploadDir, subDirectory);

            // 如果目录不存在，则创建（包括父级目录）
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.debug("创建上传目录: {}", uploadPath.toAbsolutePath());
            }

            // 获取原始文件名
            String originalFileName = file.getOriginalFilename();

            // 提取文件扩展名（如 .jpg, .png）
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // 使用UUID生成唯一文件名，避免重名覆盖
            String fileName = UUID.randomUUID() + fileExtension;

            // 构建完整的文件路径
            Path filePath = uploadPath.resolve(fileName);

            // 将文件流写入文件路径中
            file.transferTo(filePath);

            log.info("文件保存成功: 原始文件名={}, 保存路径={}", originalFileName, filePath.toAbsolutePath());

            // 返回相对访问路径（供前端访问使用）
            return "/uploads/" + subDirectory + "/" + fileName;

        } catch (IOException e) {
            log.error("文件存储失败: 文件名={}, 错误={}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 保存申请到数据库
     */
    private MerchantApply saveApplyToDatabase(Long userId, MerchantApplyDTO applyDTO, Map<String, String> fileUrls) {
        // 创建实体对象
        MerchantApply apply = new MerchantApply();

        // 设置基本信息
        apply.setUserId(userId);
        apply.setContactName(applyDTO.getContactName());
        apply.setContactPhone(applyDTO.getContactPhone());
        apply.setContactEmail(applyDTO.getContactEmail());
        apply.setStoreName(applyDTO.getStoreName());
        apply.setStoreDetail(applyDTO.getStoreDetail());
        apply.setBusinessType(applyDTO.getBusinessType());
        apply.setMainCategory(applyDTO.getMainCategory());

        // 设置文件路径
        apply.setBusinessLicense(fileUrls.get("businessLicense"));
        apply.setIdCardFront(fileUrls.get("idCardFront"));
        apply.setIdCardBack(fileUrls.get("idCardBack"));

        // 设置状态和时间
        apply.setStatus(String.valueOf(MerchantApply.Status.PENDING));
        apply.setCreatedAt(LocalDateTime.now());
        apply.setUpdatedAt(LocalDateTime.now());

        try {
            int result = merchantApplyMapper.insert(apply);
            if (result > 0) {
                log.info("商家申请保存成功，申请ID: {}", apply.getId());
                return apply;
            } else {
                throw new RuntimeException("申请提交失败");
            }
        } catch (Exception e) {
            log.error("保存商家申请失败: {}", e.getMessage());
            throw new RuntimeException("申请提交失败，请稍后重试");
        }
    }

    /**
     * 构建成功响应
     */
    private Map<String, Object> buildSuccessResponse(MerchantApply apply) {
        Map<String, Object> result = new HashMap<>();
        result.put("applicationId", apply.getId());
        result.put("applyTime", apply.getCreatedAt());
        result.put("storeName", apply.getStoreName());
        result.put("status", apply.getStatus());
        result.put("estimatedReviewTime", "1-3个工作日");
        return result;
    }


    /**
     * 获取用户申请状态
     */
    public Map<String, Object> getMerchantApplyStatus(Long userId) {
        Optional<MerchantApply> application = merchantApplyMapper.selectPendingByUserId(userId);

        Map<String, Object> status = new HashMap<>();
        status.put("hasPendingApplication", application.isPresent());

        if (application.isPresent()) {
            MerchantApply app = application.get();
            status.put("applicationId", app.getId());
            status.put("storeName", app.getStoreName());
            status.put("status", app.getStatus());
            status.put("applyTime", app.getCreatedAt());
        } else {
            status.put("status", "NO_APPLICATION");
        }

        return status;
    }

    // 商家申请分页查询
    public Map<String, Object> getMerchantApplicationsWithPagination(
            int page, int size, String status, String businessType,
            String dateFrom, String dateTo, String search) {

        try {
            // 参数验证
            if (page < 1) page = 1;
            if (size < 1) size = 10;
            if (size > 100) size = 100; // 限制最大页大小

            // 计算偏移分页参数
            int offset = (page - 1) * size;

            // 分页查询数据（带筛选条件）
            List<MerchantApply> applications = merchantApplyMapper.selectApplicationsWithPagination(
                    size, offset, status, businessType, dateFrom, dateTo, search);

            // 获取总记录数（带筛选条件）
            int total = merchantApplyMapper.countWithFilters(
                    status, businessType, dateFrom, dateTo, search);
            int totalPages = (int) Math.ceil((double) total / size);

            // 返回分页结果
            return Map.of(
                    "data", applications,
                    "currentPage", page,
                    "pageSize", size,
                    "total", total,
                    "totalPages", totalPages,
                    "success", true
            );
        } catch (Exception e) {
            log.error("获取商家申请列表失败", e);
            return Map.of(
                    "data", List.of(),
                    "currentPage", page,
                    "pageSize", size,
                    "total", 0,
                    "totalPages", 0,
                    "success", false,
                    "message", "获取数据失败"
            );
        }
    }

    // 获取商家申请统计（支持筛选）
    public Map<String, Object> getApplicationStats(String status, String businessType, String dateFrom, String dateTo, String search) {
        try {
            // 使用正确的方法名获取统计数据
            Map<String, Object> stats = merchantApplyMapper.countStatusWithFilters(
                    status, businessType, dateFrom, dateTo, search);

            // 确保所有字段都有值并转换为Long类型
            return Map.of(
                    "total", ((Number) stats.getOrDefault("total", 0)).longValue(),
                    "pending", ((Number) stats.getOrDefault("pending", 0)).longValue(),
                    "approved", ((Number) stats.getOrDefault("approved", 0)).longValue(),
                    "rejected", ((Number) stats.getOrDefault("rejected", 0)).longValue(),
                    "success", true
            );
        } catch (Exception e) {
            log.error("获取商家申请状态统计失败", e);
            return Map.of(
                    "total", 0L,
                    "pending", 0L,
                    "approved", 0L,
                    "rejected", 0L,
                    "success", false,
                    "message", "获取统计数据失败"
            );
        }
    }

    // 获取所有申请记录（管理端使用）
    public Map<String, Object> getAllApplications() {
        try {
            List<MerchantApply> applications = merchantApplyMapper.selectAllApplications();
            return Map.of(
                    "data", applications,
                    "success", true,
                    "total", applications.size()
            );
        } catch (Exception e) {
            log.error("获取所有商家申请失败", e);
            return Map.of(
                    "data", List.of(),
                    "success", false,
                    "message", "获取数据失败"
            );
        }
    }

    // 获取申请状态统计（总览）
    public Map<String, Object> getApplicationStatusOverview() {
        try {
            Map<String, Object> stats = merchantApplyMapper.countApplicationStatus();
            return Map.of(
                    "total", ((Number) stats.getOrDefault("total", 0)).longValue(),
                    "pending", ((Number) stats.getOrDefault("pending", 0)).longValue(),
                    "approved", ((Number) stats.getOrDefault("approved", 0)).longValue(),
                    "rejected", ((Number) stats.getOrDefault("rejected", 0)).longValue(),
                    "success", true
            );
        } catch (Exception e) {
            log.error("获取申请状态总览失败", e);
            return Map.of(
                    "total", 0L,
                    "pending", 0L,
                    "approved", 0L,
                    "rejected", 0L,
                    "success", false,
                    "message", "获取统计数据失败"
            );
        }
    }

    // 获取所有商家入驻申请信息
    public List<MerchantApply> getAllapplies() {
        return merchantApplyMapper.selectAllApplications();
    };


}
