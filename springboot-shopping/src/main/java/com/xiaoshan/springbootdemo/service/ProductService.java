package com.xiaoshan.springbootdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.ProductImage;
import com.xiaoshan.springbootdemo.entity.ProductParam;
import com.xiaoshan.springbootdemo.entity.ProductSku;
import com.xiaoshan.springbootdemo.entity.SellerProfile;
import com.xiaoshan.springbootdemo.entity.UserProfile;
import com.xiaoshan.springbootdemo.entity.dto.ProductDTO;
import com.xiaoshan.springbootdemo.entity.dto.SkuDTO;
import com.xiaoshan.springbootdemo.entity.vo.ProductImageVO;
import com.xiaoshan.springbootdemo.entity.vo.ProductParamVO;
import com.xiaoshan.springbootdemo.entity.vo.ProductVO;
import com.xiaoshan.springbootdemo.mapper.*;
import com.xiaoshan.springbootdemo.service.SellerPackageService;
import com.xiaoshan.springbootdemo.entity.vo.CheckoutItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final ProductParamMapper productParamMapper;
    private final CartItemMapper cartItemMapper;
    private final FavoriteMapper favoriteMapper;
    private final ProductFreezeLogMapper productFreezeLogMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final ReviewVideoMapper reviewVideoMapper;
    private final SellerProfileMapper sellerProfileMapper;
    private final UserProfileMapper userProfileMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SellerPackageService sellerPackageService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/jpg", "image/webp");
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    @Transactional
    public void addProduct(Long sellerId, ProductDTO productDTO, List<MultipartFile> images, List<MultipartFile> skuImages) {
        // 不检查套餐，直接保存为下架状态(status=0)
        validateProduct(productDTO, images);

        Product product = saveProductToMySQL(sellerId, productDTO);
        saveImagesToMySQL(product.getId(), images);

        // 保存 SKU 列表
        if (productDTO.getSkus() != null && !productDTO.getSkus().isEmpty()) {
            saveSkus(product.getId(), productDTO.getSkus(), skuImages);
            // 商品发布成功后，将每个 SKU 的真实库存同步到 Redis
            List<ProductSku> skus = productSkuMapper.findByProductId(product.getId());
            for (ProductSku sku : skus) {
                String realStockKey = "sku:stock:" + sku.getId();
                redisTemplate.opsForValue().set(realStockKey, sku.getStock(),
                    60 + new java.util.Random().nextInt(60), java.util.concurrent.TimeUnit.SECONDS);
                log.info("同步 SKU 真实库存到 Redis: skuId={}, stock={}", sku.getId(), sku.getStock());
            }
        }


    }

    private void saveSkus(Long productId, List<SkuDTO> skus, List<MultipartFile> skuImages) {
        int skuImageIndex = 0;
        
        for (int i = 0; i < skus.size(); i++) {
            SkuDTO skuDTO = skus.get(i);
            
            ProductSku sku = new ProductSku();
            sku.setId(snowflakeIdGenerator.nextId());
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
        if (images.size() > 15) {
            throw new RuntimeException("最多只能上传15张商品图片");
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
        
        // 设置新增字段
        product.setWeight(productDTO.getWeight());
        product.setIsFreeShipping(productDTO.getIsFreeShipping() != null ? productDTO.getIsFreeShipping() : false);
        product.setServiceGuarantee(productDTO.getServiceGuarantee());
        product.setDeliveryCity(productDTO.getDeliveryCity());
        product.setDetailHtml(productDTO.getDetailHtml());
        product.setStatus(0); // 保存为下架状态，上架时再检查配额
        
        // 先生成雪花 ID，确保 product.getId() 不为 null
        product.setId(snowflakeIdGenerator.nextId());
        
        productMapper.insert(product);
        
        // 保存商品参数
        if (productDTO.getParams() != null && !productDTO.getParams().isEmpty()) {
            saveProductParams(product.getId(), productDTO.getParams());
        }
        
        return product;
    }
    
    /**
     * 保存商品参数
     */
    private void saveProductParams(Long productId, List<com.xiaoshan.springbootdemo.entity.dto.ParamDTO> params) {
        int sortOrder = 0;
        for (com.xiaoshan.springbootdemo.entity.dto.ParamDTO paramDTO : params) {
            if (paramDTO.getName() != null && !paramDTO.getName().trim().isEmpty() &&
                paramDTO.getValue() != null && !paramDTO.getValue().trim().isEmpty()) {
                
                ProductParam param = new ProductParam();
                param.setId(snowflakeIdGenerator.nextId());
                param.setProductId(productId);
                param.setParamName(paramDTO.getName().trim());
                param.setParamValue(paramDTO.getValue().trim());
                param.setSortOrder(sortOrder++);
                productParamMapper.insert(param);
            }
        }
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
            productImage.setId(snowflakeIdGenerator.nextId());
            productImage.setProductId(productId);
            productImage.setImage(imageUrl);
            productImage.setSortOrder(i);
            productImage.setCreatedAt(LocalDateTime.now());

            productImageMapper.insert(productImage);
        }

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
        redisTemplate.opsForValue().set(stockKey, stock,
            60 + new java.util.Random().nextInt(60), java.util.concurrent.TimeUnit.SECONDS);
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
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long userId, Long productId, ProductDTO productDTO, List<MultipartFile> files, List<MultipartFile> skuImages, String imageSortOrder, String existingImageIds) throws IOException {
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
        
        // 更新新增字段
        if (productDTO.getWeight() != null) {
            existingProduct.setWeight(productDTO.getWeight());
        }
        if (productDTO.getIsFreeShipping() != null) {
            existingProduct.setIsFreeShipping(productDTO.getIsFreeShipping());
        }
        if (productDTO.getServiceGuarantee() != null) {
            existingProduct.setServiceGuarantee(productDTO.getServiceGuarantee());
        }
        if (productDTO.getDeliveryCity() != null) {
            existingProduct.setDeliveryCity(productDTO.getDeliveryCity());
        }
        if (productDTO.getDetailHtml() != null) {
            existingProduct.setDetailHtml(productDTO.getDetailHtml());
        }

        // 1. 先更新商品主表
        productMapper.update(existingProduct);

        // 2. 更新商品参数（先删后插）
        productParamMapper.deleteByProductId(productId);
        if (productDTO.getParams() != null && !productDTO.getParams().isEmpty()) {
            saveProductParams(productId, productDTO.getParams());
        }

        // 3. 更新 SKU 列表
        // 收集前端传来的所有有效 SKU ID（用于后续删除不在列表中的旧 SKU）
        List<Long> keepSkuIds = new ArrayList<>();
        int skuImageIndex = 0;

        if (productDTO.getSkus() != null) {
            // 计算总库存用于 Redis 同步（排除已删除的SKU）
            int totalStock = productDTO.getSkus().stream()
                    .filter(sku -> !Boolean.TRUE.equals(sku.getDeleted()))
                    .mapToInt(SkuDTO::getStock)
                    .sum();

            // 获取当前最大 sortOrder，新增 SKU 从最大值 + 1 开始
            int maxSort = productSkuMapper.getMaxSortOrder(productId);

            // 遍历 SKU：有 ID 则 UPDATE，无 ID 则 INSERT，标记删除则 DELETE
            for (int i = 0; i < productDTO.getSkus().size(); i++) {
                SkuDTO dto = productDTO.getSkus().get(i);

                // 处理标记删除的 SKU
                if (Boolean.TRUE.equals(dto.getDeleted()) && dto.getId() != null && dto.getId() > 0) {
                    // 删除 SKU，保留图片文件（订单可能引用）
                    productSkuMapper.deleteById(dto.getId());
                    // SKU 被删除，清除 Redis 中的真实库存和预扣库存
                    String realStockKey = "sku:stock:" + dto.getId();
                    String reservedStockKey = "sku:stock:reserved:" + dto.getId();
                    redisTemplate.delete(realStockKey);
                    redisTemplate.delete(reservedStockKey);
                    log.info("删除 SKU 库存缓存: skuId={}", dto.getId());
                    continue;
                }

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

                // 先保存旧库存值（在更新数据库之前获取）
                int oldStock = 0;
                if (dto.getId() != null) {
                    ProductSku existingSku = productSkuMapper.findById(dto.getId()).orElse(null);
                    if (existingSku != null) {
                        oldStock = existingSku.getStock() != null ? existingSku.getStock() : 0;
                    }
                }
                int newStock = dto.getStock() != null ? dto.getStock() : 0;
                int diff = newStock - oldStock;

                // 处理 SKU 图片增量更新
                if (dto.getId() != null && dto.getId() > 0) {
                    // 有 ID → 旧 SKU，执行 UPDATE
                    keepSkuIds.add(dto.getId());

                    // 查询原 SKU 信息
                    ProductSku existingSku = productSkuMapper.findById(dto.getId()).orElse(null);
                    String oldImageUrl = existingSku != null ? existingSku.getSkuImage() : null;

                    // sortOrder：前端传了就用前端的，否则用原值
                    if (dto.getSortOrder() != null) {
                        sku.setSortOrder(dto.getSortOrder());
                    } else if (existingSku != null) {
                        sku.setSortOrder(existingSku.getSortOrder());
                    } else {
                        sku.setSortOrder(0);
                    }

                    // 处理图片删除标记
                    if (Boolean.TRUE.equals(dto.getSkuImageDeleted())) {
                        // 标记删除图片，保留文件（订单可能引用）
                        sku.setSkuImage(null);
                    } else if (skuImages != null && skuImageIndex < skuImages.size()) {
                        // 有新图片上传 → 上传新文件，保留旧文件（订单可能引用）
                        MultipartFile newImage = skuImages.get(skuImageIndex);
                        if (!newImage.isEmpty()) {
                            // 上传新图片，不删除旧文件
                            String newImageUrl = uploadSkuImageToServer(newImage, productId, i);
                            sku.setSkuImage(newImageUrl);
                        }
                        skuImageIndex++;
                    } else {
                        // 没有新图片，保留原图片URL
                        sku.setSkuImage(dto.getSkuImage());
                    }

                    sku.setId(dto.getId());
                    productSkuMapper.update(sku);
                } else {
                    // 无 ID → 新 SKU，执行 INSERT
                    Long newSkuId = snowflakeIdGenerator.nextId();
                    sku.setId(newSkuId);
                    keepSkuIds.add(newSkuId);

                    // 新增 SKU 的 sortOrder 设为当前最大值 + 1
                    sku.setSortOrder(++maxSort);

                    // 处理新 SKU 的图片
                    if (skuImages != null && skuImageIndex < skuImages.size()) {
                        MultipartFile newImage = skuImages.get(skuImageIndex);
                        if (!newImage.isEmpty()) {
                            String newImageUrl = uploadSkuImageToServer(newImage, productId, i);
                            sku.setSkuImage(newImageUrl);
                        }
                        skuImageIndex++;
                    } else {
                        sku.setSkuImage(dto.getSkuImage());
                    }

                    productSkuMapper.insert(sku);
                }

                // 更新 SKU 真实库存到 Redis（增量更新）
                Long skuId = dto.getId() != null ? dto.getId() : sku.getId();
                String realStockKey = "sku:stock:" + skuId;
                
                log.info("修改SKU库存: skuId={}, oldStock={}, newStock={}, diff={}", 
                    skuId, oldStock, newStock, diff);
                
                // 从 Redis 获取当前真实库存
                Object currentRealStockObj = redisTemplate.opsForValue().get(realStockKey);
                Long currentRealStock = currentRealStockObj instanceof Number 
                    ? ((Number) currentRealStockObj).longValue() 
                    : null;
                
                log.info("Redis当前真实库存: skuId={}, key={}, value={}", skuId, realStockKey, currentRealStock);
                
                if (currentRealStock == null) {
                    // Redis 中不存在该 key，从数据库加载旧值并设置
                    currentRealStock = (long) oldStock;
                    redisTemplate.opsForValue().set(realStockKey, currentRealStock,
                        60 + new java.util.Random().nextInt(60), java.util.concurrent.TimeUnit.SECONDS);
                    log.info("初始化 SKU 真实库存缓存: skuId={}, stock={}", skuId, currentRealStock);
                }
                
                // 使用差值增量更新真实库存
                redisTemplate.opsForValue().increment(realStockKey, diff);
                log.info("增量更新 SKU 真实库存缓存完成: skuId={}, key={}, diff={}, newStock={}", 
                    skuId, realStockKey, diff, currentRealStock + diff);
            }
        }

        // 删除前端没传的旧 SKU（不在 keepSkuIds 列表中的）
        if (!keepSkuIds.isEmpty()) {
            List<ProductSku> oldSkus = productSkuMapper.findByProductId(productId);
            for (ProductSku sku : oldSkus) {
                if (!keepSkuIds.contains(sku.getId())) {
                    productSkuMapper.deleteById(sku.getId());
                    String realStockKey = "sku:stock:" + sku.getId();
                    String reservedStockKey = "sku:stock:reserved:" + sku.getId();
                    redisTemplate.delete(realStockKey);
                    redisTemplate.delete(reservedStockKey);
                    log.info("删除 SKU 库存缓存: skuId={}", sku.getId());
                }
            }
        } else {
            // 如果前端传了空列表，删除所有旧 SKU
            List<ProductSku> oldSkus = productSkuMapper.findByProductId(productId);
            for (ProductSku sku : oldSkus) {
                productSkuMapper.deleteById(sku.getId());
                String realStockKey = "sku:stock:" + sku.getId();
                String reservedStockKey = "sku:stock:reserved:" + sku.getId();
                redisTemplate.delete(realStockKey);
                redisTemplate.delete(reservedStockKey);
                log.info("删除 SKU 库存缓存: skuId={}", sku.getId());
            }
        }

        // 4. 处理商品主图（增量更新）
        // 解析前端传递的已有图片ID列表
        List<Long> keepImageIds = new ArrayList<>();
        if (existingImageIds != null && !existingImageIds.isEmpty()) {
            try {
                Long[] ids = objectMapper.readValue(existingImageIds, Long[].class);
                for (Long id : ids) {
                    if (id != null) {
                        keepImageIds.add(id);
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("解析 existingImageIds 失败", e);
            }
        }

        // 解析前端传递的图片排序信息
        List<Long> sortedImageIds = new ArrayList<>();
        if (imageSortOrder != null && !imageSortOrder.isEmpty()) {
            try {
                List<java.util.Map<String, Object>> sortList = objectMapper.readValue(imageSortOrder, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {});
                for (java.util.Map<String, Object> item : sortList) {
                    Number idNum = (Number) item.get("id");
                    if (idNum != null) {
                        sortedImageIds.add(idNum.longValue());
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("解析 imageSortOrder 失败", e);
            }
        }

        // 获取当前商品的所有旧图片
        List<ProductImage> oldImages = productImageMapper.findByProductId(productId);

        // 更新已有图片的排序
        for (int i = 0; i < sortedImageIds.size(); i++) {
            Long imageId = sortedImageIds.get(i);
            productImageMapper.updateSortOrder(imageId, i);
        }

        // 删除不在保留列表中的旧图片记录（不删文件）
        for (ProductImage image : oldImages) {
            if (!keepImageIds.contains(image.getId())) {
                productImageMapper.deleteById(image.getId());
            }
        }

        // 保存新上传的图片（sortOrder 接在已有图片后面）
        int nextSortOrder = sortedImageIds.size();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String imageUrl = uploadImageToServer(file, productId, nextSortOrder);
                    ProductImage productImage = new ProductImage();
                    productImage.setId(snowflakeIdGenerator.nextId());
                    productImage.setProductId(productId);
                    productImage.setImage(imageUrl);
                    productImage.setSortOrder(nextSortOrder);
                    productImageMapper.insert(productImage);
                    nextSortOrder++;
                }
            }
        }
    }

    public void deleteImageFile(String imageUrl) throws IOException {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return;
            }

            // 处理相对路径：去掉开头的 /uploads/，拼接实际存储路径
            String relativePath = imageUrl.startsWith("/uploads/") 
                ? imageUrl.substring("/uploads/".length()) 
                : imageUrl;
            
            Path filePath = Paths.get(uploadDir, relativePath).toAbsolutePath();

            if (Files.exists(filePath)) {
                Files.delete(filePath);

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

        // 检查商品状态（用户端只能购买上架商品）
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new RuntimeException("商品已下架，无法购买");
        }

        // 查询商品主图
        String mainImage = productImageMapper.findMainImageByProductId(productId);

        // 构建返回对象
        CheckoutItemVO item = new CheckoutItemVO();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setBrand(product.getBrand());
        item.setQuantity(quantity);
        item.setProductImage(mainImage);
        item.setProductStatus(product.getStatus());

        item.setSellerId(product.getSellerId());

        // 查询商家信息
        String sellerName = "商家";
        String sellerAvatar = null;
        if (product.getSellerId() != null) {
            Optional<SellerProfile> sellerProfile = sellerProfileMapper.findByUserId(product.getSellerId());
            if (sellerProfile.isPresent()) {
                SellerProfile sp = sellerProfile.get();
                sellerName = sp.getStoreName();
                sellerAvatar = sp.getStoreAvatar();
            }
            if (sellerName == null || sellerName.isEmpty()) {
                Optional<UserProfile> userProfile = userProfileMapper.findByUserId(product.getSellerId());
                if (userProfile.isPresent()) {
                    UserProfile up = userProfile.get();
                    sellerName = up.getNickname();
                    if (sellerAvatar == null) {
                        sellerAvatar = up.getAvatar();
                    }
                }
            }
            if (sellerName == null || sellerName.isEmpty()) {
                sellerName = "商家";
            }
        }
        item.setSellerName(sellerName);
        item.setSellerAvatar(sellerAvatar);

        // 如果指定了 SKU，从 SKU 获取价格和规格信息
        if (skuId != null) {
            ProductSku sku = productSkuMapper.findById(skuId)
                    .orElseThrow(() -> {
                        log.error("SKU不存在: skuId={}, productId={}", skuId, productId);
                        return new RuntimeException("所选商品规格不存在或已被删除，请重新选择");
                    });
            
            // 检查 SKU 是否属于当前商品
            if (!sku.getProductId().equals(productId)) {
                log.error("SKU不属于该商品: skuId={}, productId={}, sku.productId={}", 
                        skuId, productId, sku.getProductId());
                throw new RuntimeException("所选商品规格不属于该商品");
            }
            
            item.setSkuId(sku.getId());
            item.setSkuName(sku.getSkuName());
            item.setPrice(sku.getPrice() != null ? sku.getPrice() : BigDecimal.ZERO);
            item.setOriginalPrice(sku.getOriginalPrice());
            Integer realTimeStock = getSkuAvailableStock(sku.getId());
            item.setStock(realTimeStock);
            if (sku.getSkuImage() != null && !sku.getSkuImage().isEmpty()) {
                item.setProductImage(sku.getSkuImage());
            }
        } else {
            // 否则从SKU表获取最低价格和总库存
            BigDecimal minPrice = productSkuMapper.getMinPriceByProductId(product.getId());
            item.setPrice(minPrice != null ? minPrice : BigDecimal.ZERO);
            // 商品表不再存储原价，需要从SKU表获取最低原价
            List<ProductSku> skus = productSkuMapper.findByProductId(product.getId());
            BigDecimal minOriginalPrice = skus.stream()
                    .map(ProductSku::getOriginalPrice)
                    .filter(p -> p != null)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
            item.setOriginalPrice(minOriginalPrice);
            Integer productStock = productSkuMapper.getTotalStockByProductId(product.getId());
            item.setStock(productStock != null ? productStock : 0);
        }

        // 设置包邮状态和卖家信息
        item.setIsFreeShipping(product.getIsFreeShipping());

        return item;
    }

    /**
     * 获取商品详情
     * @param productId 商品ID
     * @param isSeller 是否商家端访问（商家端可见下架商品，用户端只能看上架商品）
     */
    public ProductVO getProductDetail(Long productId, boolean isSeller) {
        // 查询商品基础信息
        ProductVO productVO = productMapper.findProductVOById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 用户端访问：区分已下架和已删除
        if (!isSeller) {
            if (productVO.getStatus() == null) {
                throw new RuntimeException("商品不存在");
            }
            if (productVO.getStatus() == 2) {
                throw new RuntimeException("商品不存在");
            }
            if (productVO.getStatus() == 0) {
                throw new RuntimeException("商品已下架");
            }
        }

        // 查询商品图片列表
        List<ProductImage> images = productImageMapper.findByProductId(productId);
        List<ProductImageVO> imageVOs = images.stream()
                .map(img -> {
                    ProductImageVO vo = new ProductImageVO();
                    vo.setId(img.getId());
                    vo.setImage(img.getImage());
                    vo.setSortOrder(img.getSortOrder());
                    return vo;
                })
                .toList();
        productVO.setProductImages(imageVOs);

        // 查询商品参数列表
        List<ProductParam> params = productParamMapper.findByProductId(productId);
        List<ProductParamVO> paramVOs = params.stream()
                .map(p -> {
                    ProductParamVO vo = new ProductParamVO();
                    vo.setId(p.getId());
                    vo.setParamName(p.getParamName());
                    vo.setParamValue(p.getParamValue());
                    return vo;
                })
                .toList();
        productVO.setParams(paramVOs);

        // 查询商品 SKU 列表
        List<ProductSku> skus = productSkuMapper.findByProductId(productId);
        // 替换为实时可用库存
        for (ProductSku sku : skus) {
            Integer realTimeStock = getSkuAvailableStock(sku.getId());
            sku.setStock(realTimeStock);
        }
        productVO.setSkus(skus);

        // 异步更新浏览量（避免影响响应速度）
        CompletableFuture.runAsync(() -> {
            productMapper.incrementViewCount(productId);
        });

        return productVO;
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
    public List<Product> getProductsForHome(int offset, int limit, String keyword, Long sellerId, Long level1CategoryId, Long level2CategoryId) {
        try {
            return productMapper.findUserProducts(offset, limit, keyword, sellerId, level1CategoryId, level2CategoryId);
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
    public long countProductsForHome(String keyword, Long sellerId, Long level1CategoryId, Long level2CategoryId) {
        try {
            return productMapper.countUserProducts(keyword, sellerId, level1CategoryId, level2CategoryId);
        } catch (Exception e) {
            log.error("统计首页商品总数失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 搜索商品（支持排序、价格区间、分类筛选）
     *
     * @param offset 偏移量
     * @param limit 每页数量
     * @param keyword 搜索关键词
     * @param categoryId 分类ID
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param sort 排序方式
     * @return 商品列表
     */
    public List<Product> searchProducts(int offset, int limit, String keyword, Long categoryId, 
                                        java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, String sort) {
        try {
            return productMapper.searchProducts(offset, limit, keyword, categoryId, minPrice, maxPrice, sort);
        } catch (Exception e) {
            log.error("搜索商品失败: {}", e.getMessage());
            throw new RuntimeException("搜索商品失败");
        }
    }

    /**
     * 统计搜索商品总数
     *
     * @param keyword 搜索关键词
     * @param categoryId 分类ID
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 商品总数
     */
    public long countSearchProducts(String keyword, Long categoryId, 
                                    java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        try {
            return productMapper.countSearchProducts(keyword, categoryId, minPrice, maxPrice);
        } catch (Exception e) {
            log.error("统计搜索商品数量失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 软删除商品（status = 2）
     * 
     * @param productId 商品ID
     */
    @Transactional
    public void deleteProduct(Long productId) {
        // 软删除：设置状态为已删除
        productMapper.softDelete(productId);
        
        // 删除购物车中的该商品
        cartItemMapper.deleteByProductId(productId);
        
        // 删除收藏中的该商品
        favoriteMapper.deleteByProductId(productId);
        
        log.info("商品软删除成功: productId={}", productId);
    }

    /**
     * 批量软删除商品（status = 2）
     * 
     * @param productIds 商品ID列表
     * @return 删除成功的数量
     */
    @Transactional
    public int batchDeleteProducts(List<Long> productIds) {
        int deletedCount = 0;
        
        for (Long productId : productIds) {
            try {
                // 软删除：设置状态为已删除
                productMapper.softDelete(productId);
                
                // 删除购物车中的该商品
                cartItemMapper.deleteByProductId(productId);
                
                // 删除收藏中的该商品
                favoriteMapper.deleteByProductId(productId);
                
                deletedCount++;
                log.info("商品批量软删除成功: productId={}", productId);
            } catch (Exception e) {
                log.warn("商品软删除失败: productId={}, error={}", productId, e.getMessage());
            }
        }
        
        return deletedCount;
    }

    /**
     * 恢复已删除商品（status = 0，恢复到下架状态）
     * 
     * @param productId 商品ID
     */
    @Transactional
    public void restoreProduct(Long productId) {
        productMapper.restoreProduct(productId);
        log.info("商品恢复成功: productId={}", productId);
    }

    /**
     * 获取 SKU 实时可用库存（Redis 优先）
     * 可售库存 = 真实库存 - 预扣库存
     */
    public Integer getSkuAvailableStock(Long skuId) {
        String realStockKey = "sku:stock:" + skuId;
        String reservedStockKey = "sku:stock:reserved:" + skuId;

        // 1. 先从 Redis 获取真实库存
        Object realStockObj = redisTemplate.opsForValue().get(realStockKey);
        Long realStock = realStockObj instanceof Number 
            ? ((Number) realStockObj).longValue() 
            : null;

        // 2. 如果 Redis 没有真实库存，从数据库查询并回写
        if (realStock == null) {
            ProductSku sku = productSkuMapper.findById(skuId).orElse(null);
            if (sku == null) return 0;
            realStock = sku.getStock() != null ? sku.getStock().longValue() : 0L;
            redisTemplate.opsForValue().set(realStockKey, realStock,
                60 + new java.util.Random().nextInt(60), java.util.concurrent.TimeUnit.SECONDS);
        }

        // 3. 从 Redis 获取预扣库存
        Object reservedStockObj = redisTemplate.opsForValue().get(reservedStockKey);
        Long reservedStock = reservedStockObj instanceof Number 
            ? ((Number) reservedStockObj).longValue() 
            : 0L;

        // 4. 计算可售库存 = 真实库存 - 预扣库存
        int available = (int) Math.max(0, realStock - reservedStock);
        return available;
    }

    /**
     * 获取推荐商品
     * 优先级：
     * 1. 有浏览记录 → 根据浏览品类推荐同类热销商品
     * 2. 无浏览记录 → 推荐全站销量最高的商品
     *
     * @param userId 用户ID（可为null，未登录时只返回热销）
     * @param limit 返回数量
     * @return 推荐商品列表
     */
    public List<Product> getRecommendProducts(Long userId, int limit) {
        if (userId != null) {
            try {
                List<Long> categoryIds = productMapper.findBrowsedCategoryIds(userId);
                if (categoryIds != null && !categoryIds.isEmpty()) {
                    List<Product> categoryProducts = productMapper.findHotProductsByCategories(categoryIds, limit);
                    if (categoryProducts != null && !categoryProducts.isEmpty()) {
                        return categoryProducts;
                    }
                }
            } catch (Exception e) {
                log.warn("查询浏览品类推荐失败: {}", e.getMessage());
            }
        }
        return productMapper.findHotProducts(limit);
    }

    public List<String> suggest(String keyword, int limit) {
        return productMapper.suggest(keyword, limit);
    }

}