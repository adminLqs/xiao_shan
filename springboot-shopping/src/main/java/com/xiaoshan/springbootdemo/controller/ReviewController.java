package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.SellerProfile;
import com.xiaoshan.springbootdemo.entity.dto.ReviewSubmitDTO;
import com.xiaoshan.springbootdemo.service.ReviewService;
import com.xiaoshan.springbootdemo.service.SellerProfileService;
import com.xiaoshan.springbootdemo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 评论控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final UserService userService;
    private final ReviewService reviewService;
    private final SellerProfileService sellerProfileService;

    /**
     * 提交商品评论
     * POST /api/v1/reviews
     *
     * @param authentication 认证信息
     * @param orderItemId 订单项ID
     * @param rating 评分（1-5）
     * @param comment 评论内容（可选）
     * @param images 评论图片列表（可选，最多9张）
     * @param videos 评论视频列表（可选，最多3个）
     * @param videoCovers 评论视频封面列表（可选，与视频一一对应）
     * @return 提交结果
     */
    @PostMapping("/reviews")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> submitReview(
            Authentication authentication,
            HttpServletRequest request,
            @RequestParam Long orderItemId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "videos", required = false) List<MultipartFile> videos,
            @RequestParam(value = "videoCovers", required = false) List<MultipartFile> videoCovers
    ) {
        try {
            // 获取当前登录用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 构建 ReviewSubmitDTO
            ReviewSubmitDTO review = new ReviewSubmitDTO();
            review.setOrderItemId(orderItemId);
            review.setRating(rating);
            review.setComment(comment);

            // 调用服务层提交评论，传入 HttpServletRequest
            reviewService.submitReview(userId, review, images, videos, videoCovers, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "评价成功"
            ));

        } catch (Exception e) {
            log.error("提交评论失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取商品评论列表（滚动加载）
     * GET /api/v1/products/{productId}/reviews?page=1&size=10&rating=5
     *
     * @param productId 商品ID
     * @param page 页码（从1开始）
     * @param pageSize 每页数量（默认10）
     * @param rating 评分筛选（可选，1-5）
     * @return 评论列表 + 分页信息
     */
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<?> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) Boolean hasImages
    ) {
        try {
            // 调用服务层查询评论（包含用户信息）
            Map<String, Object> result = reviewService.getProductReviewsWithUser(productId, page, pageSize, rating, minRating, maxRating, hasImages);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "records", result.get("reviews"),
                            "total", result.get("total"),
                            "page", page,
                            "pageSize", pageSize,
                            "totalPages", result.get("totalPages"),
                            "hasMore", result.get("hasMore")
                    )
            ));
        } catch (Exception e) {
            log.error("获取商品评价失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取商家评价列表
     * GET /api/v1/seller/{sellerId}/reviews?page=1&pageSize=10
     */
    @GetMapping("/seller/{sellerId}/reviews")
    public ResponseEntity<?> getSellerReviews(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        try {
            // 获取商家资料以获取真正的 userId（users.id）
            SellerProfile profile = sellerProfileService.getUserProfile(sellerId);
            if (profile == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商家不存在"));
            }
            
            // 使用真正的 userId 查询评论
            Long userId = profile.getUserId();
            log.info("获取商家评论列表: sellerId={}, userId={}", sellerId, userId);
            
            Map<String, Object> result = reviewService.getSellerReviewsWithUserAndProduct(userId, page, pageSize);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "records", result.get("reviews"),
                            "total", result.get("total"),
                            "page", page,
                            "pageSize", pageSize,
                            "totalPages", result.get("totalPages"),
                            "hasMore", result.get("hasMore")
                    )
            ));
        } catch (Exception e) {
            log.error("获取商家评价失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取我的评价列表
     * GET /api/v1/users/reviews?page=1&pageSize=10
     */
    @GetMapping("/users/reviews")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getMyReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            Map<String, Object> result = reviewService.getUserReviewsWithProduct(userId, page, pageSize);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "records", result.get("reviews"),
                            "total", result.get("total"),
                            "page", page,
                            "pageSize", pageSize,
                            "totalPages", result.get("totalPages"),
                            "hasMore", result.get("hasMore")
                    )
            ));
        } catch (Exception e) {
            log.error("获取我的评价失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取商品评价统计
     * GET /api/v1/products/{productId}/reviews/statistics
     */
    @GetMapping("/products/{productId}/reviews/statistics")
    public ResponseEntity<?> getProductReviewStats(@PathVariable Long productId) {
        try {
            Map<String, Object> stats = reviewService.getProductReviewStats(productId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));
        } catch (Exception e) {
            log.error("获取商品评价统计失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

}