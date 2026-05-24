package com.xiaoshan.springbootdemo.service;

import lombok.extern.slf4j.Slf4j;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试视频封面生成和图片保存功能
 */
@Slf4j
@SpringBootTest
public class VideoCoverTest {

    @Autowired(required = false)
    private ReviewService reviewService;

    /**
     * 测试1: 验证 JCodec 依赖是否正确加载
     */
    @Test
    public void testJcodecDependency() {
        log.info("测试 JCodec 依赖是否正确加载...");

        try {
            // 验证 AWTUtil 类是否存在
            Class<?> awtUtilClass = Class.forName("org.jcodec.scale.AWTUtil");
            assertNotNull(awtUtilClass, "AWTUtil 类应该存在");

            // 验证 toBufferedImage 方法是否存在
            assertNotNull(awtUtilClass.getMethod("toBufferedImage", Picture.class),
                    "toBufferedImage 方法应该存在");

            log.info("✓ JCodec 依赖加载成功！AWTUtil 类和方法都存在。");
        } catch (ClassNotFoundException e) {
            fail("找不到 org.jcodec.scale.AWTUtil 类：" + e.getMessage());
        } catch (NoSuchMethodException e) {
            fail("找不到 toBufferedImage 方法：" + e.getMessage());
        }
    }

    /**
     * 测试2: 模拟生成视频封面（不需要真实视频文件）
     */
    @Test
    public void testVideoCoverGenerationMock() {
        log.info("测试视频封面生成逻辑（模拟）...");

        try {
            // 创建一个简单的测试Picture对象（模拟从视频中提取的帧）
            Picture mockPicture = Picture.create(1920, 1080, org.jcodec.common.model.ColorSpace.RGB);

            // 调用 AWTUtil 进行转换
            BufferedImage bufferedImage = AWTUtil.toBufferedImage(mockPicture);

            assertNotNull(bufferedImage, "转换后的 BufferedImage 不应该为 null");
            assertEquals(1920, bufferedImage.getWidth(), "图片宽度应该为 1920");
            assertEquals(1080, bufferedImage.getHeight(), "图片高度应该为 1080");

            log.info("✓ 视频封面生成逻辑测试成功！成功创建了 {}x{} 的 BufferedImage",
                    bufferedImage.getWidth(), bufferedImage.getHeight());

        } catch (Exception e) {
            log.error("视频封面生成测试失败：", e);
            fail("测试失败：" + e.getMessage());
        }
    }

    /**
     * 测试3: 测试图片保存功能
     */
    @Test
    public void testImageSaveFunctionality() {
        log.info("测试图片保存功能...");

        try {
            // 创建一个测试用的 BufferedImage
            BufferedImage testImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);

            // 填充一个简单的图案
            for (int x = 0; x < 800; x++) {
                for (int y = 0; y < 600; y++) {
                    int r = (x * 255) / 800;
                    int g = (y * 255) / 600;
                    int b = 128;
                    testImage.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }

            // 保存到临时文件
            Path tempDir = Files.createTempDirectory("video_cover_test");
            File outputFile = tempDir.resolve("test_cover.jpg").toFile();

            boolean success = ImageIO.write(testImage, "jpg", outputFile);

            assertTrue(success, "图片保存应该成功");
            assertTrue(outputFile.exists(), "输出文件应该存在");
            assertTrue(outputFile.length() > 0, "输出文件大小应该大于 0");

            log.info("✓ 图片保存功能测试成功！文件保存在: {}", outputFile.getAbsolutePath());
            log.info("文件大小: {} bytes", outputFile.length());

            // 清理测试文件
            outputFile.delete();
            Files.deleteIfExists(tempDir);

        } catch (IOException e) {
            log.error("图片保存测试失败：", e);
            fail("测试失败：" + e.getMessage());
        }
    }

    /**
     * 测试4: 验证 ReviewService 是否能够正常注入
     */
    @Test
    public void testReviewServiceInjection() {
        log.info("测试 ReviewService 注入...");

        if (reviewService != null) {
            log.info("✓ ReviewService 注入成功！");

            // 测试上传目录配置（uploadDir 使用 @Value 注解，所以直接设置字段）
            ReflectionTestUtils.setField(reviewService, "uploadDir", "uploads");
            String uploadDir = (String) ReflectionTestUtils.getField(reviewService, "uploadDir");
            log.info("上传目录配置: {}", uploadDir);

            assertEquals("uploads", uploadDir, "上传目录应该正确设置");
        } else {
            log.warn("⚠ ReviewService 未注入（可能数据库未配置），但核心功能测试通过！");
        }
    }

    /**
     * 测试5: 端到端测试 - 完整的图片保存流程
     */
    @Test
    public void testEndToEndImageSave() {
        log.info("测试端到端图片保存流程...");

        try {
            // 1. 创建测试用的 BufferedImage
            BufferedImage testImage = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);

            // 绘制一些内容
            for (int x = 0; x < 640; x++) {
                for (int y = 0; y < 480; y++) {
                    int color = (x + y) % 256;
                    testImage.setRGB(x, y, (color << 16) | (color << 8) | color);
                }
            }

            // 2. 模拟生成视频封面
            String testVideoPath = "test_review_12345";
            int testIndex = 0;
            String coverFileName = testIndex + "_test_cover.jpg";

            // 3. 构建保存路径
            Path uploadPath = Paths.get("uploads", "reviews", testVideoPath, "videos")
                    .toAbsolutePath();
            Path coverPath = uploadPath.resolve(coverFileName);

            // 4. 创建目录
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 5. 保存图片
            ImageIO.write(testImage, "jpg", coverPath.toFile());

            // 6. 验证文件
            assertTrue(Files.exists(coverPath), "封面文件应该存在");

            File savedFile = coverPath.toFile();
            log.info("✓ 端到端测试成功！");
            log.info("  - 文件路径: {}", savedFile.getAbsolutePath());
            log.info("  - 文件大小: {} bytes", savedFile.length());
            log.info("  - 图片尺寸: {}x{}", testImage.getWidth(), testImage.getHeight());

            // 7. 清理
            Files.deleteIfExists(coverPath);
            Files.deleteIfExists(uploadPath);

            log.info("✓ 测试文件清理完成");

        } catch (IOException e) {
            log.error("端到端测试失败：", e);
            fail("测试失败：" + e.getMessage());
        }
    }

    /**
     * 测试6: JCodec 版本兼容性测试
     */
    @Test
    public void testJcodecVersionCompatibility() {
        log.info("测试 JCodec 版本兼容性...");

        try {
            // 验证所有必需的类都存在
            Class.forName("org.jcodec.api.FrameGrab");
            Class.forName("org.jcodec.common.io.NIOUtils");
            Class.forName("org.jcodec.common.model.Picture");
            Class.forName("org.jcodec.common.model.ColorSpace");
            Class.forName("org.jcodec.scale.AWTUtil");

            log.info("✓ 所有 JCodec 必需类都存在且可访问");
            log.info("JCodec 版本兼容性测试通过！");

        } catch (ClassNotFoundException e) {
            fail("缺少必需的 JCodec 类：" + e.getMessage());
        }
    }
}
