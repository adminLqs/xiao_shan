package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Category;
import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.ProductSku;
import com.xiaoshan.springbootdemo.entity.dto.ProductDTO;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.ProductSkuMapper;
import com.xiaoshan.springbootdemo.service.CategoryService;
import com.xiaoshan.springbootdemo.service.ProductService;
import com.xiaoshan.springbootdemo.service.SellerProfileService;
import com.xiaoshan.springbootdemo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final UserService userService;
    private final SellerProfileService sellerProfileService;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductService productService;
    private final CategoryService categoryService;

    // 商家商品发布 几十毫秒到200毫秒即0.1秒左右
    @PostMapping("/seller/products")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> addProduct(
            Authentication authentication,
            @RequestPart("products") @Valid ProductDTO productDTO,
            @RequestPart("images") List<MultipartFile> files,
            @RequestPart(value = "skuImages", required = false) List<MultipartFile> skuImages
    ) {
        try {
            Long id = userService.getCurrentUserId(authentication);
            productService.addProduct(id, productDTO, files, skuImages);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "商品添加成功"
            ));
        } catch (Exception e) {
            log.error("商品添加失败", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "商品添加失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/seller/products/{productId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> deleteProducts(
            Authentication authentication,
            @PathVariable("productId") Long productId) {
        try {
            // 获取账号ID
            Long sellerId = userService.getCurrentUserId(authentication);

            // 先查询商品是否存在且属于当前商家
            Product product = productMapper.findById(productId)
                    .orElseThrow(() -> new RuntimeException("商品不存在"));

            if (!product.getSellerId().equals(sellerId)) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "无权删除此商品"
                ));
            }

            // 使用Service层删除商品（会先删除关联的order_items等）
            productService.deleteProduct(productId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "删除成功"
            ));
        } catch(Exception e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 批量删除商品
     * DELETE /seller/products/batch
     * 请求体: { "productIds": [1, 2, 3] }
     */
    @DeleteMapping("/seller/products/batch")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> batchDeleteProducts(
            Authentication authentication,
            @RequestBody Map<String,List<Long>> requestBody) {
        try {
            // 获取商家ID（验证权限）
            Long seller_id = userService.getCurrentUserId(authentication);

            // 获取并验证productIds
            List<Long> productIds = requestBody.get("productIds");

            // 参数验证
            if (productIds == null || productIds.isEmpty()) {
                return ResponseEntity.ok().body(Map.of(
                        "success", false,
                        "message", "商品ID列表不能为空"
                ));
            }

            // 验证商品是否属于当前商家（防止越权删除）
            for (Long id : productIds) {
                Product product = productMapper.findById(id)
                        .orElseThrow(() -> new RuntimeException("不对"));
                if (product == null || !product.getSellerId().equals(seller_id)) {
                    return ResponseEntity.ok().body(Map.of(
                            "success", false,
                            "message", "商品不存在或无权删除"
                    ));
                }
            }

            // 批量删除（使用Service层，会先删除关联的order_items等）
            int deletedCount = productService.batchDeleteProducts(productIds);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "成功删除 " + deletedCount + " 个商品",
                    "count", deletedCount
            ));
        } catch (ClassCastException e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "请求参数格式错误，productIds应为数组"
            ));
        } catch (Exception e) {
            log.error("批量删除失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "批量删除失败: " + e.getMessage()
            ));
        }
    }

    /** 修改商品状态 */
    @PatchMapping("/seller/products/{product_id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> updateProductStatus(
            @PathVariable("product_id") Long productId,
            @RequestBody Map<String,Object> requestBody
            ) {
        // 从requestBody中提取status
        Integer status = (Integer) requestBody.get("status");

        try {
            // 更改商品状态
            productMapper.updateStatus(productId,status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "商品状态更新成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }

    }

    /**
     * 获取商家商品列表（分页）
     * GET /api/v1/seller/products?page=1
     */
    @GetMapping("/seller/products")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getProducts(
            Authentication authentication,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        try {
            // 获取商家id
            Long userId = userService.getCurrentUserId(authentication);
            // 计算偏移量
            int offset = (page - 1) * pageSize;

            // 查询商品列表
            List<Product> products = productService.getProductsBySellerId(userId, offset, pageSize, keyword, status);

            // 查询总数
            long total = productService.countProductsBySellerId(userId, keyword, status);

            // 构建返回数据
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "records", products,
                            "total", total,
                            "page", page,
                            "size", pageSize,
                            "totalPages", (int) Math.ceil((double) total / pageSize)
                    )
            ));

        } catch (Exception e) {
            log.error("获取商品列表失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 修改商品
     * PUT /api/v1/seller/products/{id}
     */
    @PutMapping("/seller/products/{productId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> updateProduct(
            Authentication authentication,
            @PathVariable("productId") Long productId,
            @RequestPart("products") @Valid ProductDTO productDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> files,
            @RequestPart(value = "skuImages", required = false) List<MultipartFile> skuImages,
            @RequestParam(value = "imageSortOrder", required = false) String imageSortOrder,
            @RequestParam(value = "existingImageIds", required = false) String existingImageIds) {
        try {
            // 获取当前商家ID
            Long userId = userService.getCurrentUserId(authentication);

            // 更新商品
            productService.updateProduct(userId, productId, productDTO, files, skuImages, imageSortOrder, existingImageIds);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "商品更新成功");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("更新商品失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage() != null ? e.getMessage() : "更新商品失败");
            return ResponseEntity.ok(result);
        }
    }



    /**
     * 获取存在分类（一次性返回所有分类）
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getCategoryActive() {
        try {
            List<Category> tree = categoryService.findAllActive();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "获取分类成功",
                    "data", Map.of("categories", tree)
            ));
        } catch (Exception e) {
            log.error("获取分类树失败: {}", e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取分类失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 根据商品ID查询商家信息（公开访问）
     * GET /api/v1/products/{productId}/seller
     */
    @GetMapping("/products/{productId}/seller")
    public ResponseEntity<?> getSellerByProductId(@PathVariable Long productId) {
        try {
            // 查询商品
            Product product = productMapper.findById(productId).orElse(null);
            if (product == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商品不存在"));
            }

            Long sellerId = product.getSellerId();
            if (sellerId == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商家信息不存在"));
            }

            // 查询商家信息
            var seller = sellerProfileService.getUserProfile(sellerId);
            if (seller == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商家信息不存在"));
            }

            // 查询商家统计信息（粉丝数、评分、好评率）
            var statistics = sellerProfileService.getSellerStatistics(seller.getUserId());

            // 将统计数据添加到商家对象中
            Map<String, Object> sellerData = new java.util.HashMap<>();
            sellerData.put("id", seller.getId());
            sellerData.put("userId", seller.getUserId());
            sellerData.put("storeName", seller.getStoreName());
            sellerData.put("storeAvatar", seller.getStoreAvatar());
            sellerData.put("storeDetail", seller.getStoreDetail());
            sellerData.put("businessHours", seller.getBusinessHours());
            sellerData.put("contactPhone", seller.getContactPhone());
            sellerData.putAll(statistics);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("seller", sellerData)
            ));
        } catch (Exception e) {
            log.error("获取商家信息失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 获取商品优惠券列表（公开访问）
     * GET /api/v1/products/{productId}/coupons
     */
    @GetMapping("/products/{productId}/coupons")
    public ResponseEntity<?> getProductCoupons(@PathVariable Long productId) {
        try {
            // 查询商品是否存在
            Product product = productMapper.findById(productId).orElse(null);
            if (product == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商品不存在"));
            }

            // 返回空数组（如果没有优惠券服务）
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("coupons", List.of())
            ));
        } catch (Exception e) {
            log.error("获取优惠券失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 获取商品SKU列表（公开访问，用户端）
     * GET /api/v1/products/{productId}/skus
     * 用户端使用Redis库存（性能，防超卖）
     */
    @GetMapping("/products/{productId}/skus")
    public ResponseEntity<?> getProductSkus(@PathVariable Long productId) {
        try {
            // 查询商品是否存在
            Product product = productMapper.findById(productId).orElse(null);
            if (product == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商品不存在"));
            }

            // 查询SKU列表
            List<ProductSku> skus = productSkuMapper.findByProductId(productId);

            // 用户端：替换为Redis实时可用库存（性能，防超卖）
            for (ProductSku sku : skus) {
                Integer realTimeStock = productService.getSkuAvailableStock(sku.getId());
                sku.setStock(realTimeStock);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("skus", skus)
            ));
        } catch (Exception e) {
            log.error("获取SKU列表失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 获取商品SKU列表（商家端）
     * GET /api/v1/seller/products/{productId}/skus
     * 商家端使用数据库库存（准确性，管理需要）
     */
    @GetMapping("/seller/products/{productId}/skus")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getSellerProductSkus(Authentication authentication, @PathVariable Long productId) {
        try {
            // 查询商品是否存在
            Product product = productMapper.findById(productId).orElse(null);
            if (product == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "商品不存在"));
            }

            // 验证商品属于当前商家
            Long sellerId = userService.getCurrentUserId(authentication);
            if (!product.getSellerId().equals(sellerId)) {
                return ResponseEntity.ok(Map.of("success", false, "message", "无权访问此商品"));
            }

            // 查询SKU列表（直接从数据库读取，不经过Redis）
            List<ProductSku> skus = productSkuMapper.findByProductId(productId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("skus", skus)
            ));
        } catch (Exception e) {
            log.error("获取SKU列表失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 搜索商品（公开访问，支持排序、价格区间、分类筛选）
     * GET /api/v1/products?keyword=xxx&sort=xxx&minPrice=xxx&maxPrice=xxx&categoryId=xxx&page=1&pageSize=20
     * 支持首页列表：?sellerId=xxx&level1CategoryId=xxx&level2CategoryId=xxx
     */
    @GetMapping("/products")
    public ResponseEntity<?> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long sellerId,           // 商家ID（店铺页使用）
            @RequestParam(required = false) Long level1CategoryId,  // 一级分类ID（首页分类浏览）
            @RequestParam(required = false) Long level2CategoryId,   // 二级分类ID（首页分类浏览）
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        try {
            // 计算偏移量
            int offset = (page - 1) * pageSize;

            List<Product> products;
            long total;

            // 判断使用哪种查询方式：
            // 1. 如果有搜索筛选参数（sort/minPrice/maxPrice/categoryId），使用搜索方法
            // 2. 如果有 sellerId 或 level1CategoryId/level2CategoryId，使用首页商品列表方法
            // 3. 默认使用首页商品列表方法
            boolean hasSearchParams = (sort != null && !sort.isEmpty()) || minPrice != null || maxPrice != null || categoryId != null;
            boolean hasHomeParams = sellerId != null || level1CategoryId != null || level2CategoryId != null;

            if (hasSearchParams) {
                // 使用搜索方法
                products = productService.searchProducts(offset, pageSize, keyword, categoryId, minPrice, maxPrice, sort);
                total = productService.countSearchProducts(keyword, categoryId, minPrice, maxPrice);
            } else {
                // 使用首页商品列表方法
                products = productService.getProductsForHome(offset, pageSize, keyword, sellerId, level1CategoryId, level2CategoryId);
                total = productService.countProductsForHome(keyword, sellerId, level1CategoryId, level2CategoryId);
            }

            // 构建返回数据
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "records", products,
                            "total", total,
                            "page", page,
                            "size", pageSize,
                            "totalPages", (int) Math.ceil((double) total / pageSize)
                    )
            ));

        } catch (Exception e) {
            log.error("搜索商品失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
