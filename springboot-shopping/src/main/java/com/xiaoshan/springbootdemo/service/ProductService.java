package com.xiaoshan.springbootdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.ProductImage;
import com.xiaoshan.springbootdemo.entity.ProductSku;
import com.xiaoshan.springbootdemo.entity.dto.ProductDTO;
import com.xiaoshan.springbootdemo.entity.dto.SkuDTO;
import com.xiaoshan.springbootdemo.mapper.ProductImageMapper;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.ProductSkuMapper;
import com.xiaoshan.springbootdemo.entity.vo.CheckoutItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 商品服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductSkuMapper productSkuMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/jpg", "image/webp");
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    @Transactional
    public void addProduct(Long sellerId, ProductDTO productDTO, List<MultipartFile> images, List<MultipartFile> skuImages) {
        log.info("开始添加商品: sellerId={}, name={}", sellerId, productDTO.getName());

        validateProduct(productDTO, images);

        Product product = saveProductToMySQL(sellerId, productDTO);
        saveImagesToMySQL(product.getId(), images);

        // 保存 SKU 列表
        if (productDTO.getSkus() != null && !productDTO.getSkus().isEmpty()) {
            saveSkus(product.getId(), productDTO.getSkus(), skuImages);
            Integer totalStock = productSkuMapper.getTotalStockByProductId(product.getId());
            syncStockToRedis(product.getId(), totalStock != null ? totalStock : 0);
        }

        log.info("商品添加成功: productId={}", product.getId());
    }

    private void saveSkus(Long productId, List<SkuDTO> skus, List<MultipartFile> skuImages) {
        int skuImageIndex = 0;
        
        for (int i = 0; i < skus.size(); i++) {
            SkuDTO skuDTO = skus.get(i);
            
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setSkuName(skuDTO.getSkuName());
            sku.setPrice(skuDTO.getPrice());
            sku.setOriginalPrice(skuDTO.getOriginalPrice());
            sku.setStock(skuDTO.getStock() != null ? skuDTO.getStock() : 0);
            sku.setSkuCode(skuDTO.getSkuCode());
            sku.setSortOrder(i);

            // 处理SKU图片上传
            if (skuImages != null && skuImageIndex < skuImages.size()) {
                MultipartFile skuImage = skuImages.get(skuImageIndex);
                if (!skuImage.isEmpty()) {
                    String imageUrl = uploadSkuImageToServer(skuImage, productId, i);
                    sku.setSkuImage(imageUrl);
                }
                skuImageIndex++;
            }

            if (skuDTO.getSpecInfo() != null) {
                try {
                    sku.setSpecInfo(objectMapper.writeValueAsString(skuDTO.getSpecInfo()));
                } catch (JsonProcessingException e) {
                    log.error("规格信息序列化失败", e);
                }
            }

            int result = productSkuMapper.insert(sku);
            if (result != 1) {
                log.error("SKU插入失败: productId={}, skuName={}", productId, sku.getSkuName());
            }
        }
        log.info("SKU保存成功: productId={}, count={}", productId, skus.size());
    }
    
    private String uploadSkuImageToServer(MultipartFile file, Long productId, int skuIndex) {
        try {
            String fileName = "sku_" + skuIndex + "_" + UUID.randomUUID() + getFileExtension(file.getOriginalFilename());
            Path uploadPath = Paths.get(uploadDir, "products_images", String.valueOf(productId)).toAbsolutePath();
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath);
            
            return "/uploads/products_images/" + productId + "/" + fileName;
            
        } catch (IOException e) {
            log.error("SKU图片上传失败: productId={}, skuIndex={}", productId, skuIndex, e);
            throw new RuntimeException("SKU图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 校验商品信息
     * @param productDTO 商品信息传输对象
     * @param images 商品图片列表
     */
    private void validateProduct(ProductDTO productDTO, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new RuntimeException("请至少上传一张商品图片");
        }
        if (images.size() > 5) {
            throw new RuntimeException("最多只能上传5张商品图片");
        }

        for (int i = 0; i < images.size(); i++) {
            validateImage(images.get(i), i + 1);
        }

        if (productDTO.getSkus() == null || productDTO.getSkus().isEmpty()) {
            throw new RuntimeException("请至少添加一个规格");
        }

        for (SkuDTO sku : productDTO.getSkus()) {
            if (sku.getPrice() == null || sku.getPrice().compareTo(new BigDecimal("0.01")) < 0) {
                throw new RuntimeException("SKU价格不能低于0.01元");
            }
            if (sku.getStock() == null || sku.getStock() < 0) {
                throw new RuntimeException("SKU库存不能为负数");
            }
        }
    }

    /**
     * 校验单张图片
     * @param file 图片文件
     * @param index 图片序号（用于错误提示）
     */
    private void validateImage(MultipartFile file, int index) {
        // 判断图片是否为空
        if (file.isEmpty()) {
            // 为空则抛出异常
            throw new RuntimeException("第" + index + "张图片不能为空");
        }

        // 判断图片大小是否超过限制
        if (file.getSize() > MAX_IMAGE_SIZE) {
            // 超过大小则抛出异常
            throw new RuntimeException("第" + index + "张图片大小不能超过5MB");
        }

        // 获取图片的 MIME 类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {  // 判断图片格式是否允许
            throw new RuntimeException("第" + index + "张图片格式不支持，请上传 JPG、PNG 或 WEBP 格式");  // 格式不支持则抛出异常
        }
    }

    /**
     * 保存商品到 MySQL 数据库
     * @param sellerId 商家ID
     * @param productDTO 商品信息传输对象
     * @return 保存后的商品对象（包含自动生成的ID）
     */
    private Product saveProductToMySQL(Long sellerId, ProductDTO productDTO) {
        Product product = new Product(
                sellerId, productDTO.getCategoryId(),
                productDTO.getName(), productDTO.getBrand(),
                productDTO.getDescription()
        );
        
        productMapper.insert(product);
        return product;
    }

    /**
     * 保存图片到 MySQL 数据库
     * @param productId 商品ID
     * @param images 图片文件列表
     */
    private void saveImagesToMySQL(Long productId, List<MultipartFile> images) {
        for (int i = 0; i < images.size(); i++) {
            String imageUrl = uploadImageToServer(images.get(i), productId, i);

            ProductImage productImage = new ProductImage();
            productImage.setProductId(productId);
            productImage.setImage(imageUrl);
            productImage.setSortOrder(i);
            productImage.setCreatedAt(LocalDateTime.now());

            productImageMapper.insert(productImage);
        }
        log.info("图片保存成功: productId={}, count={}", productId, images.size());
    }

    /**
     * 上传图片到服务器
     * @param file 图片文件
     * @param productId 商品ID
     * @param index 图片序号
     * @return 图片的访问URL
     */
    private String uploadImageToServer(MultipartFile file, Long productId, int index) {
        try {
            String fileName = index + "_" + UUID.randomUUID() + getFileExtension(file.getOriginalFilename());
            Path uploadPath = Paths.get(uploadDir, "products_images", String.valueOf(productId)).toAbsolutePath();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath);

            return "/uploads/products_images/" + productId + "/" + fileName;

        } catch (IOException e) {
            log.error("图片上传失败: productId={}", productId, e);
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 同步库存到 Redis
     * @param productId 商品ID
     * @param stock 库存数量
     */
    private void syncStockToRedis(Long productId, Integer stock) {
        String stockKey = "product:stock:" + productId;
        redisTemplate.opsForValue().set(stockKey, stock);
        log.info("同步库存到Redis: productId={}, stock={}", productId, stock);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    // =============== 修改商品 =============

    /**
     * 更新商品
     */
    @Transactional
    public void updateProduct(Long userId, Long productId, ProductDTO productDTO, List<MultipartFile> files) throws IOException {
        // 查询原商品
        Product existingProduct = productMapper.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 验证权限
        if (!existingProduct.getSellerId().equals(userId)) {
            throw new RuntimeException("无权修改该商品");
        }

        // 更新商品基本信息（价格和库存从 SKU 汇总计算）
        existingProduct.setName(productDTO.getName());
        existingProduct.setBrand(productDTO.getBrand());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setCategoryId(productDTO.getCategoryId());
        existingProduct.setStatus(productDTO.getStatus());
        existingProduct.setUpdatedAt(LocalDateTime.now());

        // 更新 SKU 列表（价格和库存由 @Transient 字段从查询时的子查询获取，不需要保存）
        if (productDTO.getSkus() != null && !productDTO.getSkus().isEmpty()) {
            // 计算总库存用于 Redis 同步
            int totalStock = productDTO.getSkus().stream()
                    .mapToInt(SkuDTO::getStock)
                    .sum();

            // 删除旧的 SKU
            productSkuMapper.deleteByProductId(productId);

            // 转换为 ProductSku 实体并批量插入
            List<ProductSku> skuEntities = productDTO.getSkus().stream()
                    .map(dto -> {
                        ProductSku sku = new ProductSku();
                        sku.setProductId(productId);
                        sku.setSkuName(dto.getSkuName());
                        if (dto.getSpecInfo() != null) {
                            try {
                                sku.setSpecInfo(objectMapper.writeValueAsString(dto.getSpecInfo()));
                            } catch (JsonProcessingException e) {
                                log.error("规格信息序列化失败", e);
                            }
                        }
                        sku.setPrice(dto.getPrice());
                        sku.setOriginalPrice(dto.getOriginalPrice());
                        sku.setStock(dto.getStock());
                        sku.setSkuImage(dto.getSkuImage());
                        sku.setSkuCode(dto.getSkuCode());
                        sku.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
                        return sku;
                    })
                    .toList();
            productSkuMapper.batchInsert(productId, skuEntities);

            // 同步库存到 Redis
            syncStockToRedis(productId, totalStock);
        }

        // 更新商品基本信息
        productMapper.update(existingProduct);

        // 处理新上传的商品主图（如果有）
        if (files != null && !files.isEmpty()) {
            // 删除服务器上的旧图片文件
            List<ProductImage> oldImages = productImageMapper.findByProductId(productId);
            for (ProductImage image : oldImages) {
                deleteImageFile(image.getImage());
            }
            // 删除数据库中的旧图片记录
            productImageMapper.deleteByProductId(productId);

            // 保存新图片
            for (int i = 0; i < files.size(); i++) {
                String imageUrl = uploadImageToServer(files.get(i), productId, i);
                ProductImage productImage = new ProductImage();
                productImage.setProductId(productId);
                productImage.setImage(imageUrl);
                productImage.setSortOrder(i);
                productImageMapper.insert(productImage);
            }
        }
    }

    public void deleteImageFile(String imageUrl) throws IOException {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new RuntimeException("旧图片路径为空");
            }

            Path filePath = Paths.get(imageUrl).toAbsolutePath();

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("删除旧图片成功: {}", filePath.toAbsolutePath());
            } else {
                log.warn("旧图片文件不存在: {}", filePath.toAbsolutePath());
            }

        } catch(IOException e) {
            log.error("删除图片文件失败: {}", e.getMessage());
            throw new IOException(e.getMessage());
        }
    }

    // =============== 商品查询 =============

    /**
     * 分页查询商家商品
     */
    public List<Product> getProductsBySellerId(Long sellerId, int offset, int limit, String keyword, Integer status) {
        return productMapper.findSellerProducts(sellerId, offset, limit, keyword, status);
    }

    /**
     * 统计商家商品数量（支持关键词和状态筛选）
     *
     * @param sellerId 商家ID
     * @param keyword 搜索关键词
     * @param status 商品状态
     * @return 商品总数
     */
    public long countProductsBySellerId(Long sellerId, String keyword, Integer status) {
        return productMapper.countSellerProducts(sellerId, keyword, status);
    }

    /**
     * 获取结算页商品信息（立即购买）
     */
    public CheckoutItemVO getCheckoutItem(Long productId, Integer quantity, Long skuId) {
        // 查询商品信息
        Product product = productMapper.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 查询商品主图
        String mainImage = productImageMapper.findMainImageByProductId(productId);

        // 构建返回对象
        CheckoutItemVO item = new CheckoutItemVO();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setBrand(product.getBrand());
        item.setQuantity(quantity);
        item.setProductImage(mainImage);

        // 如果指定了 SKU，从 SKU 获取价格和规格信息
        if (skuId != null) {
            ProductSku sku = productSkuMapper.findById(skuId)
                    .orElseThrow(() -> new RuntimeException("SKU不存在"));
            item.setSkuId(sku.getId());
            item.setSkuName(sku.getSkuName());
            item.setPrice(sku.getPrice() != null ? sku.getPrice() : BigDecimal.ZERO);
            item.setOriginalPrice(sku.getOriginalPrice());
            Integer skuStock = sku.getStock();
            item.setStock(skuStock != null ? skuStock : 0);
            if (sku.getSkuImage() != null && !sku.getSkuImage().isEmpty()) {
                item.setProductImage(sku.getSkuImage());
            }
        } else {
            // 否则使用商品表汇总的价格和库存
            item.setPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
            item.setOriginalPrice(product.getOriginalPrice());
            Integer productStock = product.getStock();
            item.setStock(productStock != null ? productStock : 0);
        }

        return item;
    }

    /**
     * 获取商品详情
     */
    public Product getProductDetail(Long productId) {
        return productMapper.findByIdWithDetails(productId)
                .orElseThrow(() -> new RuntimeException("获取商品详情失败"));
    }

    /** ======== 用户首页查询商品（分页筛选）======== */

    /**
     * 首页商品列表（只显示上架商品）
     *
     * @param offset 偏移量
     * @param limit 每页数量
     * @param keyword 搜索关键词
     * @param level1CategoryId 一级分类ID
     * @param level2CategoryId 二级分类ID
     * @return 商品列表
     */
    public List<Product> getProductsForHome(int offset, int limit, String keyword, Long level1CategoryId, Long level2CategoryId) {
        try {
            return productMapper.findUserProducts(offset, limit, keyword, level1CategoryId, level2CategoryId);
        } catch (Exception e) {
            log.error("查询首页商品列表失败: {}", e.getMessage());
            throw new RuntimeException("查询商品列表失败");
        }
    }

    /**
     * 统计首页商品总数
     *
     * @param keyword 搜索关键词
     * @param level1CategoryId 一级分类ID
     * @param level2CategoryId 二级分类ID
     * @return 商品总数
     */
    public long countProductsForHome(String keyword, Long level1CategoryId, Long level2CategoryId) {
        try {
            return productMapper.countUserProducts(keyword, level1CategoryId, level2CategoryId);
        } catch (Exception e) {
            log.error("统计首页商品总数失败: {}", e.getMessage());
            return 0;
        }
    }



}