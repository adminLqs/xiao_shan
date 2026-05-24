package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.*;
import com.xiaoshan.springbootdemo.mapper.*;
import com.xiaoshan.springbootdemo.entity.dto.ReviewSubmitDTO;
import com.xiaoshan.springbootdemo.entity.vo.UserReviewVO;
import com.xiaoshan.springbootdemo.util.IpUtils;
import com.xiaoshan.springbootdemo.util.IpLocationUtil;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评论服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    // 图片上传目录
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    // 最大图片大小（5MB）
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    // 最大视频大小（50MB）
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024;

    // 允许的图片类型
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    // 允许的视频类型
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/x-msvideo", "video/webm"
    );

    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final ReviewVideoMapper reviewVideoMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final UserProfileMapper userProfileMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final IpLocationUtil ipLocationUtil;

    /**
     * 上传结果封装类
     */
    private static class UploadResult {
        String url;
        String filePath;
        
        UploadResult(String url, String filePath) {
            this.url = url;
            this.filePath = filePath;
        }
    }


    /**
     * 提交商品评论
     * @param userId 用户ID
     * @param reviewData 评论数据
     * @param images 评论图片列表
     * @param videos 评论视频列表（可选，最多3个）
     * @param videoCovers 评论视频封面列表（可选，与视频一一对应）
     * @param request HTTP请求对象，用于获取IP
     */
    @Transactional
    public void submitReview(Long userId, ReviewSubmitDTO reviewData, 
                            List<MultipartFile> images, 
                            List<MultipartFile> videos,
                            List<MultipartFile> videoCovers,
                            HttpServletRequest request) {
        // 校验订单项
        OrderItem orderItem = validateOrderItem(userId, reviewData.getOrderItemId());

        // 校验图片
        validateImages(images);

        // 校验视频
        validateVideos(videos);

        // 获取IP地址和定位信息
        String ip = null;
        String location = null;
        try {
            ip = IpUtils.getIpAddress(request);
            location = ipLocationUtil.getLocation(ip);
            log.info("评论IP: {}, 定位: {}", ip, location);
            
            // 如果定位返回 private range 或结果为空，显示"本地"
            if (location == null || location.toLowerCase().contains("private")) {
                location = "本地";
            }
        } catch (Exception e) {
            log.warn("获取IP或定位失败", e);
            location = "本地";
        }

        // 1. 创建评论实体，手动设置雪花 ID
        Review review = new Review();
        review.setId(snowflakeIdGenerator.nextId());
        review.setUserId(userId);
        review.setProductId(orderItem.getProductId());
        review.setOrderId(orderItem.getOrderId());
        review.setOrderItemId(reviewData.getOrderItemId());
        review.setRating(reviewData.getRating());
        review.setComment(reviewData.getComment());
        review.setSkuSpec(orderItem.getSkuName());
        review.setIp(ip);
        review.setLocation(location);

        // 2. 插入评论
        reviewMapper.insert(review);

        // 3. 插入评论图片
        if (images != null && !images.isEmpty()) {
            int sortOrder = 0;
            for (MultipartFile file : images) {
                UploadResult uploadResult = uploadReviewMedia(file, review.getId(), sortOrder);
                ReviewImage reviewImage = new ReviewImage();
                reviewImage.setId(snowflakeIdGenerator.nextId());
                reviewImage.setReviewId(review.getId());
                reviewImage.setImage(uploadResult.url);
                reviewImage.setSortOrder(sortOrder++);
                reviewImageMapper.insert(reviewImage);
            }
        }

        // 4. 插入评论视频
        if (videos != null && !videos.isEmpty()) {
            int sortOrder = 0;
            for (int i = 0; i < videos.size(); i++) {
                MultipartFile videoFile = videos.get(i);
                
                // 上传视频文件
                UploadResult uploadResult = uploadReviewMedia(videoFile, review.getId(), sortOrder);
                String videoUrl = uploadResult.url;
                
                // 确定封面URL：优先使用前端上传的封面，否则尝试后端生成
                String coverUrl = null;
                if (videoCovers != null && videoCovers.size() > i && videoCovers.get(i) != null) {
                    // 使用前端上传的封面
                    MultipartFile coverFile = videoCovers.get(i);
                    UploadResult coverResult = uploadReviewMedia(coverFile, review.getId(), sortOrder + 1000); // 使用较大的序号避免冲突
                    coverUrl = coverResult.url;
    
                } else {
                    // 尝试后端生成封面
                    coverUrl = generateVideoCover(uploadResult.filePath, review.getId(), sortOrder);
                }
                
                // 保存视频信息到数据库
                ReviewVideo reviewVideo = new ReviewVideo();
                reviewVideo.setId(snowflakeIdGenerator.nextId());
                reviewVideo.setReviewId(review.getId());
                reviewVideo.setVideoUrl(videoUrl);
                reviewVideo.setCoverUrl(coverUrl);
                reviewVideo.setSize(videoFile.getSize());
                reviewVideo.setSortOrder(sortOrder++);
                reviewVideoMapper.insert(reviewVideo);
                
            }
        }

        // 5. 更新订单项评论状态
        orderItemMapper.updateReviewedStatus(reviewData.getOrderItemId(), true);


    }

    /**
     * 校验订单项
     * @param userId 用户ID
     * @param orderItemId 订单项ID
     * @return 订单项实体
     */
    private OrderItem validateOrderItem(Long userId, Long orderItemId) {
        // 查询订单项
        OrderItem orderItem = orderItemMapper.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        // 查询订单
        Order order = orderMapper.findById(orderItem.getOrderId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 校验订单所属用户
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        // 校验订单状态是否为已完成
        if (order.getStatus() != Order.OrderStatus.COMPLETED) {
            throw new RuntimeException("只有已完成订单才能评价");
        }

        // 校验是否已评价
        if (orderItem.getIsReviewed()) {
            throw new RuntimeException("该商品已评价过");
        }

        return orderItem;
    }

    /**
     * 校验媒体文件（图片和视频）
     * @param files 文件列表
     */
    private void validateImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        // 判断文件数量是否超过限制
        if (files.size() > 9) {
            throw new RuntimeException("最多只能上传9个媒体文件");
        }

        // 遍历校验每个文件
        for (int i = 0; i < files.size(); i++) {
            validateMediaFile(files.get(i), i + 1);
        }
    }

    /**
     * 校验视频文件列表
     * @param videos 视频文件列表
     */
    private void validateVideos(List<MultipartFile> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }

        // 判断文件数量是否超过限制
        if (videos.size() > 3) {
            throw new RuntimeException("最多只能上传3个视频");
        }

        // 遍历校验每个视频文件
        for (int i = 0; i < videos.size(); i++) {
            MultipartFile video = videos.get(i);
            if (video.isEmpty()) {
                throw new RuntimeException("第" + (i + 1) + "个视频不能为空");
            }

            // 校验视频大小
            if (video.getSize() > MAX_VIDEO_SIZE) {
                throw new RuntimeException("第" + (i + 1) + "个视频大小不能超过50MB");
            }

            // 校验视频格式
            String contentType = video.getContentType();
            if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
                throw new RuntimeException("第" + (i + 1) + "个视频格式不支持，请上传 MP4、MOV、AVI 或 WEBM 格式");
            }
        }
    }

    /**
     * 校验单个媒体文件（支持图片和视频）
     * @param file 文件
     * @param index 文件序号
     */
    private void validateMediaFile(MultipartFile file, int index) {
        if (file.isEmpty()) {
            throw new RuntimeException("第" + index + "个文件不能为空");
        }

        String contentType = file.getContentType();

        // 判断是图片还是视频
        if (contentType != null && contentType.startsWith("video/")) {
            // 视频限制 30MB
            if (file.getSize() > MAX_VIDEO_SIZE) {
                throw new RuntimeException("第" + index + "个视频大小不能超过30MB");
            }
        } else {
            // 图片限制 5MB
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new RuntimeException("第" + index + "张图片大小不能超过5MB");
            }

            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                throw new RuntimeException("第" + index + "张图片格式不支持，请上传 JPG、PNG 或 WEBP 格式");
            }
        }
    }

    /**
     * 创建评论实体
     * @param userId 用户ID
     * @param orderItem 订单项
     * @param reviewData 评论数据
     * @return 评论实体
     */
    private Review createReview(Long userId, OrderItem orderItem, ReviewSubmitDTO reviewData) {
        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(orderItem.getProductId());
        review.setOrderId(orderItem.getOrderId());
        review.setOrderItemId(reviewData.getOrderItemId());
        review.setRating(reviewData.getRating());
        review.setComment(reviewData.getComment());
        return review;
    }

    /**
     * 保存评论图片
     * @param reviewId 评论ID
     * @param images 图片列表
     */
    private void saveReviewImages(Long reviewId, List<MultipartFile> images) {
        for (int i = 0; i < images.size(); i++) {
            UploadResult result = uploadReviewMedia(images.get(i), reviewId, i);

            ReviewImage reviewImage = new ReviewImage();
            reviewImage.setReviewId(reviewId);
            reviewImage.setImage(result.url);
            reviewImage.setSortOrder(i);
            reviewImage.setCreatedAt(LocalDateTime.now());

            reviewImageMapper.insert(reviewImage);
        }

    }

    /**
     * 上传评论媒体文件到服务器（支持图片和视频）
     * @param file 媒体文件
     * @param reviewId 评论ID
     * @param index 文件序号
     * @return UploadResult 包含文件URL和物理路径
     */
    private UploadResult uploadReviewMedia(MultipartFile file, Long reviewId, int index) {
        try {
            // 生成唯一文件名：序号_UUID.扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = index + "_" + UUID.randomUUID() + extension;

            // 判断是图片还是视频，分别存储到不同目录
            String contentType = file.getContentType();
            String mediaType = "images";
            if (contentType != null && contentType.startsWith("video/")) {
                mediaType = "videos";
            }

            // 构建上传目录路径：/uploads/reviews/{reviewId}/{mediaType}/
            Path uploadPath = Paths.get(uploadDir, "reviews", String.valueOf(reviewId), mediaType).toAbsolutePath();

            // 创建目录（如果不存在）
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 构建完整文件路径
            Path filePath = uploadPath.resolve(fileName);

            // 保存文件到磁盘
            file.transferTo(filePath);

            // 构建访问URL
            String url = "/uploads/reviews/" + reviewId + "/" + mediaType + "/" + fileName;
            String filePathStr = filePath.toString();

            // 返回结果
            return new UploadResult(url, filePathStr);

        } catch (IOException e) {
            log.error("评论媒体文件上传失败: reviewId={}", reviewId, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 使用 JCodec 生成视频封面图
     * @param videoFilePath 视频文件物理路径
     * @param reviewId 评论ID
     * @param index 文件序号
     * @return 封面图URL，如果生成失败则返回null
     */
    private String generateVideoCover(String videoFilePath, Long reviewId, int index) {
        try {
            File videoFile = new File(videoFilePath);
            if (!videoFile.exists()) {
                log.error("视频文件不存在: {}", videoFilePath);
                return null;
            }

            // 生成封面文件名
            String coverFileName = index + "_" + UUID.randomUUID() + "_cover.jpg";
            
            // 封面保存路径：与视频同一目录
            Path coverPath = videoFile.getParentFile().toPath().resolve(coverFileName);

            // 使用 JCodec 截取视频第1秒的帧作为封面
            FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(videoFile));
            grab.seekToSecondPrecise(1.0); // 定位到第1秒
            Picture picture = grab.getNativeFrame();
            
            if (picture == null) {
                log.error("无法从视频中提取帧: {}", videoFilePath);
                return null;
            }

            // 将 Picture 转换为 BufferedImage
            BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
            
            // 保存为 JPEG 文件
            ImageIO.write(bufferedImage, "jpg", coverPath.toFile());

            // 返回封面URL
            String coverUrl = "/uploads/reviews/" + reviewId + "/videos/" + coverFileName;
            
            return coverUrl;

        } catch (Exception e) {
            log.error("生成视频封面失败: videoFilePath={}, error={}", videoFilePath, e.getMessage(), e);
            return null;
        }
    }

    // ==================== 商品评论查询（滚动加载） ====================

    /**
     * 获取商品评论列表（包含用户头像、昵称、评论图片）
     * 支持滚动加载：page从1开始，size默认10条
     *
     * @param productId 商品ID
     * @param page      页码（从1开始）
     * @param size      每页数量
     * @param rating    评分筛选（可选，1-5）
     * @param hasImages 是否只查询有图片的评论（可选）
     * @return 包含评论列表和分页信息的Map
     */
    public Map<String, Object> getProductReviewsWithUser(Long productId, int page, int size, Integer rating, Integer minRating, Integer maxRating, Boolean hasImages) {
        // 计算偏移量
        int offset = (page - 1) * size;

        // 查询评论列表
        List<Review> reviews = reviewMapper.findByProductId(productId, offset, size, rating, minRating, maxRating, hasImages);

        // 查询总数量（注：countByProductId 不支持 hasImages 筛选）
        long total = reviewMapper.countByProductId(productId, rating, minRating, maxRating);

        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / size);

        // 判断是否还有更多数据（用于前端滚动加载判断）
        boolean hasMore = (long) page * size < total;

        // 如果评论为空，直接返回空列表
        if (reviews.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("reviews", Collections.emptyList());
            result.put("total", total);
            result.put("totalPages", totalPages);
            result.put("hasMore", hasMore);
            return result;
        }

        // 批量查询用户资料（避免N+1查询问题）
        List<Long> userIds = reviews.stream()
                .map(Review::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<UserProfile> userProfiles = userProfileMapper.findByUserIds(userIds);

        // 转换为Map，便于快速查找
        Map<Long, UserProfile> userProfileMap = userProfiles.stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        // ========== 批量查询评论图片 ==========
        List<Long> reviewIds = reviews.stream()
                .map(Review::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ReviewImage> allReviewImages = reviewImageMapper.findByReviewIds(reviewIds);
        Map<Long, List<ReviewImage>> reviewImagesMap = allReviewImages.stream()
                .collect(Collectors.groupingBy(ReviewImage::getReviewId));

        // ========== 批量查询评论视频 ==========
        List<ReviewVideo> allReviewVideos = reviewVideoMapper.findByReviewIds(reviewIds);
        Map<Long, List<ReviewVideo>> reviewVideosMap = allReviewVideos.stream()
                .collect(Collectors.groupingBy(ReviewVideo::getReviewId));

        // 组装VO
        List<UserReviewVO> reviewList = reviews.stream()
                .map(review -> {
                    List<ReviewImage> images = reviewImagesMap.getOrDefault(review.getId(), Collections.emptyList());
                    List<ReviewVideo> videos = reviewVideosMap.getOrDefault(review.getId(), Collections.emptyList());
                    UserProfile userProfile = userProfileMap.get(review.getUserId());
                    return new UserReviewVO(review, images, videos, userProfile);
                })
                .collect(Collectors.toList());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviewList);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("hasMore", hasMore);

        return result;
    }

// ==================== 商家评价查询（滚动加载） ====================

    /**
     * 获取商家评价列表（包含用户信息、商品信息、评论图片、评论视频）
     * 支持滚动加载：page从1开始，size默认10条
     *
     * @param sellerId 商家ID
     * @param page     页码（从1开始）
     * @param size     每页数量
     * @return 包含评价列表和分页信息的Map
     */
    public Map<String, Object> getSellerReviewsWithUserAndProduct(Long sellerId, int page, int size) {
        // 计算偏移量
        int offset = (page - 1) * size;

        // 查询商家评价列表
        List<Review> reviews = reviewMapper.findBySellerId(sellerId, offset, size);

        // 查询总数量
        long total = reviewMapper.countBySellerId(sellerId);

        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / size);

        // 判断是否还有更多数据
        boolean hasMore = (long) page * size < total;

        // 如果评论为空，直接返回空列表
        if (reviews.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("reviews", Collections.emptyList());
            result.put("total", total);
            result.put("totalPages", totalPages);
            result.put("hasMore", hasMore);
            return result;
        }

        // 批量查询用户资料
        List<Long> userIds = reviews.stream()
                .map(Review::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<UserProfile> userProfiles = userProfileMapper.findByUserIds(userIds);
        Map<Long, UserProfile> userProfileMap = userProfiles.stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        // ========== 批量查询评论图片 ==========
        List<Long> reviewIds = reviews.stream()
                .map(Review::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ReviewImage> allReviewImages = reviewImageMapper.findByReviewIds(reviewIds);
        Map<Long, List<ReviewImage>> reviewImagesMap = allReviewImages.stream()
                .collect(Collectors.groupingBy(ReviewImage::getReviewId));

        // ========== 批量查询评论视频 ==========
        List<ReviewVideo> allReviewVideos = reviewVideoMapper.findByReviewIds(reviewIds);
        Map<Long, List<ReviewVideo>> reviewVideosMap = allReviewVideos.stream()
                .collect(Collectors.groupingBy(ReviewVideo::getReviewId));

        // 组装VO
        List<UserReviewVO> reviewList = reviews.stream()
                .map(review -> {
                    List<ReviewImage> images = reviewImagesMap.getOrDefault(review.getId(), Collections.emptyList());
                    List<ReviewVideo> videos = reviewVideosMap.getOrDefault(review.getId(), Collections.emptyList());
                    UserProfile userProfile = userProfileMap.get(review.getUserId());
                    return new UserReviewVO(review, images, videos, userProfile);
                })
                .collect(Collectors.toList());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviewList);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("hasMore", hasMore);

        return result;
    }

    // ==================== 我的评价查询（滚动加载） ====================

    /**
     * 获取当前用户的评价列表（包含商品信息、评论图片）
     * 支持滚动加载：page从1开始，size默认10条
     *
     * @param userId 用户ID
     * @param page   页码（从1开始）
     * @param size   每页数量
     * @return 包含评价列表和分页信息的Map
     */
    public Map<String, Object> getUserReviewsWithProduct(Long userId, int page, int size) {
        // 计算偏移量
        int offset = (page - 1) * size;

        // 查询用户评论列表
        List<Review> reviews = reviewMapper.findByUserId(userId, offset, size);

        // 查询总数量
        long total = reviewMapper.countByUserId(userId);

        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / size);

        // 判断是否还有更多数据
        boolean hasMore = (long) page * size < total;

        // 如果评论为空，直接返回空列表
        if (reviews.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("reviews", Collections.emptyList());
            result.put("total", total);
            result.put("totalPages", totalPages);
            result.put("hasMore", hasMore);
            return result;
        }

        // ========== 批量查询评论图片 ==========
        List<Long> reviewIds = reviews.stream()
                .map(Review::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ReviewImage> allReviewImages = reviewImageMapper.findByReviewIds(reviewIds);
        Map<Long, List<ReviewImage>> reviewImagesMap = allReviewImages.stream()
                .collect(Collectors.groupingBy(ReviewImage::getReviewId));

        // ========== 批量查询评论视频 ==========
        List<ReviewVideo> allReviewVideos = reviewVideoMapper.findByReviewIds(reviewIds);
        Map<Long, List<ReviewVideo>> reviewVideosMap = allReviewVideos.stream()
                .collect(Collectors.groupingBy(ReviewVideo::getReviewId));

        // 组装VO（用户评论 + 商品信息 + 评论图片 + 评论视频）
        List<UserReviewVO> reviewList = reviews.stream()
                .map(review -> {
                    List<ReviewImage> images = reviewImagesMap.getOrDefault(review.getId(), Collections.emptyList());
                    List<ReviewVideo> videos = reviewVideosMap.getOrDefault(review.getId(), Collections.emptyList());
                    return new UserReviewVO(review, images, videos, null);
                })
                .collect(Collectors.toList());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviewList);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("hasMore", hasMore);

        return result;
    }

    /**
     * 获取商品评价统计（好评率、各评分数量等）
     * @param productId 商品ID
     * @return 评价统计信息
     */
    public Map<String, Object> getProductReviewStats(Long productId) {
        // 查询总评价数量
        long total = reviewMapper.countByProductId(productId, null, null, null);

        // 如果没有评价，返回默认统计
        if (total == 0) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", 0);
            stats.put("averageRating", 0.0);
            stats.put("positiveRate", 0.0);
            stats.put("ratingDistribution", Map.of(
                    "1", 0, "2", 0, "3", 0, "4", 0, "5", 0
            ));
            stats.put("hasImages", 0);
            return stats;
        }

        // 查询各评分数量
        long count5 = reviewMapper.countByProductId(productId, 5, null, null);
        long count4 = reviewMapper.countByProductId(productId, 4, null, null);
        long count3 = reviewMapper.countByProductId(productId, 3, null, null);
        long count2 = reviewMapper.countByProductId(productId, 2, null, null);
        long count1 = reviewMapper.countByProductId(productId, 1, null, null);

        // 计算加权平均分
        double totalScore = count5 * 5 + count4 * 4 + count3 * 3 + count2 * 2 + count1 * 1;
        double averageRating = totalScore / total;

        // 计算好评率（4-5星占比）
        long positiveCount = count5 + count4;
        double positiveRate = (double) positiveCount / total * 100;

        // 查询有图片和视频的评价数量
        long hasImages = reviewImageMapper.countByProductId(productId);
        long hasVideos = reviewVideoMapper.countByProductId(productId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("averageRating", Math.round(averageRating * 10.0) / 10.0); // 保留一位小数
        stats.put("positiveRate", Math.round(positiveRate * 10.0) / 10.0); // 保留一位小数
        stats.put("ratingDistribution", Map.of(
                "1", count1, "2", count2, "3", count3, "4", count4, "5", count5
        ));
        stats.put("hasImages", hasImages);
        stats.put("hasVideos", hasVideos);

        return stats;
    }

}