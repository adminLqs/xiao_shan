package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.dto.ReviewSubmitDTO;
import com.xiaoshan.springbootdemo.service.ReviewService;
import com.xiaoshan.springbootdemo.service.UserService;
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

    /**
     * 提交商品评论
     * POST /api/v1/reviews
     *
     * @param authentication 认证信息
     * @param reviewData 评论数据（JSON字符串，包含orderItemId、rating、comment）
     * @param images 评论图片列表（可选，最多9张）
     * @return 提交结果
     */
    @PostMapping("/reviews")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> submitReview(
            Authentication authentication,
            @RequestPart("reviewData") @Valid ReviewSubmitDTO reviewData,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        try {
            // 获取当前登录用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 调用服务层提交评论
            reviewService.submitReview(userId, reviewData, images);

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
     * @param size 每页数量（默认10）
     * @param rating 评分筛选（可选，1-5）
     * @return 评论列表 + 分页信息
     */
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<?> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer rating
    ) {
        try {
            // 调用服务层查询评论（包含用户信息）
            Map<String, Object> result = reviewService.getProductReviewsWithUser(productId, page, size, rating);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result.get("reviews"),
                    "total", result.get("total"),
                    "page", page,
                    "size", size,
                    "totalPages", result.get("totalPages"),
                    "hasMore", result.get("hasMore")
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
     * 获取我的评价列表
     * GET /api/v1/users/reviews?page=1&size=10
     */
    @GetMapping("/users/reviews")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getMyReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            Map<String, Object> result = reviewService.getUserReviewsWithProduct(userId, page, size);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result.get("reviews"),
                    "total", result.get("total"),
                    "page", page,
                    "size", size,
                    "totalPages", result.get("totalPages"),
                    "hasMore", result.get("hasMore")
            ));
        } catch (Exception e) {
            log.error("获取我的评价失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

}