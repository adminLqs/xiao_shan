package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.dto.ProductDTO;
import com.xiaoshan.springbootdemo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ==================== 商品管理 ====================

    /**
     * 获取所有商品列表
     * <p>
     * 查询并返回商家的所有商品信息，用于商家后台的商品管理页面。
     *
     * @return 响应结果，包含商品列表数据
     */
    @GetMapping("/seller/products")
    public ResponseEntity<?> getAllProducts() {
        try {
            List<Product> products = productService.findAll();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("products", products)
            ));
        } catch (Exception e) {
            log.error("获取商品列表失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "获取商品失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 添加商品
     * <p>
     * 商家添加新商品，包含商品信息和商品图片。
     *
     * @param productDTO 商品信息DTO，包含名称、价格、分类等
     * @param file       商品图片文件
     * @return 响应结果，表示添加是否成功
     */
    @PostMapping(value = "/seller/products")
    public ResponseEntity<?> addProduct(
            @RequestPart("product") @Valid ProductDTO productDTO,
            @RequestPart("image") MultipartFile file
    ) {
        try {
            productService.addProduct(productDTO, file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "商品添加成功"
            ));
        } catch (Exception e) {
            log.error("添加商品失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "商品添加失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 更新商品信息
     * <p>
     * 更新指定商品的名称、价格、描述等信息，可选择性地更新商品图片。
     *
     * @param productId  商品ID
     * @param productDTO 商品信息DTO，包含需要更新的字段
     * @param imageFile  商品图片文件（可选，不更新图片时可省略）
     * @return 响应结果，包含更新后的商品信息
     */
    @PutMapping(value = "/seller/products/{productId}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long productId,
            @RequestPart("product") ProductDTO productDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            Product product = productService.updateProduct(productId, productDTO, imageFile);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "商品更新成功",
                    "data", Map.of("product", product)
            ));
        } catch (Exception e) {
            log.error("商品更新失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "商品更新失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 删除商品
     * <p>
     * 根据商品ID删除指定商品。删除前请确认该商品没有关联的未完成订单。
     *
     * @param productId 商品ID
     * @return 响应结果，表示删除是否成功
     */
    @DeleteMapping("/seller/products/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long productId) {
        try {
            productService.deleteProduct(productId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "商品删除成功"
            ));
        } catch (Exception e) {
            log.error("商品删除失败", e);
            return ResponseEntity.ok().body(Map.of(
                    "success", false,
                    "message", "商品删除失败: " + e.getMessage()
            ));
        }
    }
}
