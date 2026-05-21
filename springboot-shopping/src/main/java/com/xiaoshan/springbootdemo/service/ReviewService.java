package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.*;
import com.xiaoshan.springbootdemo.mapper.*;
import com.xiaoshan.springbootdemo.entity.dto.ReviewSubmitDTO;
import com.xiaoshan.springbootdemo.entity.vo.UserReviewVO;
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

    // 允许的图片类型
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final UserProfileMapper userProfileMapper;


    /**
     * 提交商品评论
     * @param userId 用户ID
     * @param reviewData 评论数据
     * @param images 评论图片列表
     */
    @Transactional
    public void submitReview(Long userId, ReviewSubmitDTO reviewData, List<MultipartFile> images) {
        log.info("开始提交评论: userId={}, orderItemId={}", userId, reviewData.getOrderItemId());

        // 校验订单项
        OrderItem orderItem = validateOrderItem(userId, reviewData.getOrderItemId());

        // 校验图片
        validateImages(images);

        // 创建评论实体
        Review review = createReview(userId, orderItem, reviewData);

        // 保存评论
        reviewMapper.insert(review);

        // 保存评论图片
        if (images != null && !images.isEmpty()) {
            saveReviewImages(review.getId(), images);
        }

        // 更新订单项评论状态
        orderItemMapper.updateReviewedStatus(reviewData.getOrderItemId(), true);

        log.info("评论提交成功: reviewId={}, orderItemId={}", review.getId(), reviewData.getOrderItemId());
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
     * 校验图片
     * @param images 图片列表
     */
    private void validateImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        // 判断图片数量是否超过限制
        if (images.size() > 9) {
            throw new RuntimeException("最多只能上传9张评论图片");
        }

        // 遍历校验每张图片
        for (int i = 0; i < images.size(); i++) {
            validateImage(images.get(i), i + 1);
        }
    }

    /**
     * 校验单张图片
     * @param file 图片文件
     * @param index 图片序号
     */
    private void validateImage(MultipartFile file, int index) {
        if (file.isEmpty()) {
            throw new RuntimeException("第" + index + "张图片不能为空");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("第" + index + "张图片大小不能超过5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("第" + index + "张图片格式不支持，请上传 JPG、PNG 或 WEBP 格式");
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
            String imageUrl = uploadReviewImage(images.get(i), reviewId, i);

            ReviewImage reviewImage = new ReviewImage();
            reviewImage.setReviewId(reviewId);
            reviewImage.setImage(imageUrl);
            reviewImage.setSortOrder(i);
            reviewImage.setCreatedAt(LocalDateTime.now());

            reviewImageMapper.insert(reviewImage);
        }
        log.info("评论图片保存成功: reviewId={}, count={}", reviewId, images.size());
    }

    /**
     * 上传评论图片到服务器
     * @param file 图片文件
     * @param reviewId 评论ID
     * @param index 图片序号
     * @return 图片访问URL
     */
    private String uploadReviewImage(MultipartFile file, Long reviewId, int index) {
        try {
            // 生成唯一文件名：序号_UUID.扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = index + "_" + UUID.randomUUID() + extension;

            // 构建上传目录路径：/uploads/reviews/{reviewId}/
            Path uploadPath = Paths.get(uploadDir, "reviews", String.valueOf(reviewId)).toAbsolutePath();

            // 创建目录（如果不存在）
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 构建完整文件路径
            Path filePath = uploadPath.resolve(fileName);

            // 保存文件到磁盘
            file.transferTo(filePath);

            // 返回图片访问URL
            return "/uploads/reviews/" + reviewId + "/" + fileName;

        } catch (IOException e) {
            log.error("评论图片上传失败: reviewId={}", reviewId, e);
            throw new RuntimeException("图片上传失败: " + e.getMessage());
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
     * @return 包含评论列表和分页信息的Map
     */
    public Map<String, Object> getProductReviewsWithUser(Long productId, int page, int size, Integer rating) {
        // 计算偏移量
        int offset = (page - 1) * size;

        // 查询评论列表
        List<Review> reviews = reviewMapper.findByProductId(productId, offset, size, rating);

        // 查询总数量
        long total = reviewMapper.countByProductId(productId, rating);

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
        // 收集所有评论ID
        List<Long> reviewIds = reviews.stream()
                .map(Review::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 批量查询所有评论的图片
        List<ReviewImage> allReviewImages = reviewImageMapper.findByReviewIds(reviewIds);

        // 按评论ID分组，转换为 Map<reviewId, List<ReviewImage>>
        Map<Long, List<ReviewImage>> reviewImagesMap = allReviewImages.stream()
                .collect(Collectors.groupingBy(ReviewImage::getReviewId));

        // 组装VO
        List<UserReviewVO> reviewList = reviews.stream()
                .map(review -> {
                    List<ReviewImage> images = reviewImagesMap.getOrDefault(review.getId(), Collections.emptyList());
                    UserProfile userProfile = userProfileMap.get(review.getUserId());
                    return new UserReviewVO(review, images, userProfile);
                })
                .collect(Collectors.toList());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviewList);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("hasMore", hasMore);

        log.debug("查询商品评论: productId={}, page={}, size={}, rating={}, total={}, hasMore={}",
                productId, page, size, rating, total, hasMore);

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

        // 组装VO（用户评论 + 商品信息 + 评论图片）
        List<UserReviewVO> reviewList = reviews.stream()
                .map(review -> {
                    List<ReviewImage> images = reviewImagesMap.getOrDefault(review.getId(), Collections.emptyList());
                    // 如果需要商品信息，可以在这里查询并设置
                    return new UserReviewVO(review, images, null);
                })
                .collect(Collectors.toList());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviewList);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("hasMore", hasMore);

        log.debug("查询我的评价: userId={}, page={}, size={}, total={}, hasMore={}",
                userId, page, size, total, hasMore);

        return result;
    }



}