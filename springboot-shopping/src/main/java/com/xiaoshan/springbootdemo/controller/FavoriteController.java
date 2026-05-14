package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.vo.FavoriteVO;
import com.xiaoshan.springbootdemo.service.FavoriteService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FavoriteController {

    private final UserService userService;
    private final FavoriteService favoriteService;

    /**
     * 添加收藏
     * POST /api/v1/favorites
     */
    @PostMapping("/favorites")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> addFavorite(
            Authentication authentication,
            @RequestBody Map<String, Long> request
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            Long productId = request.get("productId");

            if (productId == null) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "商品ID不能为空"
                ));
            }

            favoriteService.addFavorite(userId, productId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "收藏成功"
            ));

        } catch (Exception e) {
            log.error("添加收藏失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 取消收藏
     * DELETE /api/v1/favorites/{favoriteId}
     */
    @DeleteMapping("/favorites/{favoriteId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> removeFavorite(
            Authentication authentication,
            @PathVariable Long favoriteId
    ) {
        try {
            // 获取认证用户id标识
            Long userId = userService.getCurrentUserId(authentication);

            // 删除收藏商品
            favoriteService.removeFavorite(userId, favoriteId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "取消收藏成功"
            ));

        } catch (Exception e) {
            log.error("取消收藏失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 检查是否已收藏
     * GET /api/v1/favorites/check?productId=xxx
     */
    @GetMapping("/favorites/check")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> checkFavorited(
            Authentication authentication,
            @RequestParam Long productId
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            boolean isFavorited = favoriteService.isFavorited(userId, productId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("isFavorited", isFavorited)
            ));

        } catch (Exception e) {
            log.error("检查收藏状态失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取用户收藏列表（分页）
     */
    @GetMapping("/favorites")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getUserFavorites(
            Authentication authentication,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Long userId = userService.getCurrentUserId(authentication);

        int offset = (page - 1) * pageSize;
        List<FavoriteVO> favorites = favoriteService.getUserFavorites(userId, offset, pageSize);
        long total = favoriteService.getFavoriteCount(userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", favorites,
                "total", total,
                "page", page,
                "size", pageSize,
                "totalPages", (int) Math.ceil((double) total / pageSize)
        ));
    }

    /**
     * 管理员查询指定用户的收藏（不分页）
     * GET /api/v1/favorites/admin/{userId}
     */
    @GetMapping("/favorites/admin/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getUserFavoritesByAdmin(@PathVariable Long userId) {
        try {
            List<FavoriteVO> favorites = favoriteService.getAllFavorites(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", favorites,
                    "total", favorites.size()
            ));
        } catch (Exception e) {
            log.error("管理员查询收藏失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }



    /**
     * 获取用户收藏数量
     * GET /api/v1/favorites/count
     */
    @GetMapping("/favorites/count")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getFavoriteCount(Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            int count = favoriteService.getFavoriteCount(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", count
            ));

        } catch (Exception e) {
            log.error("获取收藏数量失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}