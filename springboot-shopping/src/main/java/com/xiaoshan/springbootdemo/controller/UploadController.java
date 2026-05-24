package com.xiaoshan.springbootdemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 通用文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 上传文件
     * POST /api/v1/upload
     */
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请选择要上传的文件"
                ));
            }

            // 验证文件类型（仅允许图片）
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "只允许上传图片文件"
                ));
            }

            // 验证文件大小（5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "文件大小不能超过 5MB"
                ));
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            // 按日期创建目录
            LocalDate now = LocalDate.now();
            String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
            String month = now.format(DateTimeFormatter.ofPattern("MM"));
            String day = now.format(DateTimeFormatter.ofPattern("dd"));

            // 构建上传目录路径（项目根目录下的 uploads/refund/年/月/日）
            Path uploadPath = Paths.get(uploadDir, "refund", year, month, day).toAbsolutePath().normalize();
            File destDir = uploadPath.toFile();

            // 确保目录存在
            if (!destDir.exists()) {
                boolean created = destDir.mkdirs();
                if (created) {
                    log.info("创建上传目录: {}", destDir.getAbsolutePath());
                } else {
                    log.error("创建上传目录失败: {}", destDir.getAbsolutePath());
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "创建上传目录失败"
                    ));
                }
            }

            // 构建文件完整路径
            File destFile = new File(destDir, fileName);

            // 保存文件
            file.transferTo(destFile);

            // 构建访问 URL
            String fileUrl = "/uploads/refund/" + year + "/" + month + "/" + day + "/" + fileName;

            log.info("文件上传成功: {} -> {}", fileUrl, destFile.getAbsolutePath());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "上传成功",
                    "data", Map.of(
                            "url", fileUrl,
                            "fileName", fileName
                    )
            ));

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "文件上传失败: " + e.getMessage()
            ));
        }
    }
}
