package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.ProductImage;
import com.xiaoshan.springbootdemo.entity.dto.ProductDTO;
import com.xiaoshan.springbootdemo.mapper.ProductImageMapper;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
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
@Slf4j // 日志注解，提供日志记录功能
@Service // 标识这是一个服务层组件，Spring 会自动扫描和管理
@RequiredArgsConstructor // 生成构造器注入，用于 final 字段的依赖注入
public class ProductService {

    private final ProductMapper productMapper;  // 商品数据库操作Mapper
    private final ProductImageMapper productImageMapper;  // 商品图片数据库操作Mapper
    private final RedisTemplate<String, Object> redisTemplate;  // Redis 操作模板，用于缓存和预扣库存

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/jpg", "image/webp");  // 允许上传的图片格式列表
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;  // 图片最大大小限制为 5MB

    /** ============== 商品添加 ========== */

    /**
     * 商家添加商品
     * @param sellerId 商家ID
     * @param productDTO 商品信息传输对象
     * @param images 商品图片列表
     */
    @Transactional  // 开启事务，保证数据一致性
    public void addProduct(Long sellerId, ProductDTO productDTO, List<MultipartFile> images) {
        log.info("开始添加商品: sellerId={}, name={}", sellerId, productDTO.getName());  // 记录开始添加商品的日志

        // 校验商品信息和图片
        validateProduct(productDTO, images);

        // 商品基本信息保存到 MySQL 数据库
        Product product = saveProductToMySQL(sellerId, productDTO);

        // 商品图片保存到 MySQL 数据库
        saveImagesToMySQL(product.getId(), images);

        // 同步商品库存到 Redis，用于预扣库存
        syncStockToRedis(product.getId(), productDTO.getStock());

        log.info("商品添加成功: productId={}, stock={}", product.getId(), productDTO.getStock());  // 记录添加成功的日志
    }

    /**
     * 校验商品信息
     * @param productDTO 商品信息传输对象
     * @param images 商品图片列表
     */
    private void validateProduct(ProductDTO productDTO, List<MultipartFile> images) {
        // 判断图片列表是否为空
        if (images == null || images.isEmpty()) {
            // 没有图片则抛出异常
            throw new RuntimeException("请至少上传一张商品图片");
        }
        // 判断图片数量是否超过限制
        if (images.size() > 5) {
            // 超过5张则抛出异常
            throw new RuntimeException("最多只能上传5张商品图片");
        }

        // 遍历所有图片
        for (int i = 0; i < images.size(); i++) {
            // 逐张校验图片格式和大小
            validateImage(images.get(i), i + 1);
        }

        // 判断价格是否低于0.01元
        if (productDTO.getPrice().compareTo(new BigDecimal("0.01")) < 0) {
            // 价格过低则抛出异常
            throw new RuntimeException("商品售价不能低于0.01元");
        }
        // 判断原价是否不为空 && 判断原价是否低于现价
        if (productDTO.getOriginalPrice() != null && productDTO.getOriginalPrice().compareTo(productDTO.getPrice()) < 0) {
            // 原价低于现价则抛出异常
            throw new RuntimeException("商品原价不能低于现价");
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
        Product product = new Product(  // 创建商品实体对象
                sellerId, productDTO.getCategoryId(),  // 商家ID和分类ID
                productDTO.getName(), productDTO.getBrand(),  // 商品名称和品牌
                productDTO.getDescription(),  // 商品描述
                productDTO.getPrice(), productDTO.getOriginalPrice(),  // 商品价格和原价
                productDTO.getStock()  // 商品库存
        );

        // 调用Mapper插入商品数据到数据库
        productMapper.insert(product);

        // 返回商品对象（此时 product.getId() 已自动生成）
        return product;
    }

    /**
     * 保存图片到 MySQL 数据库
     * @param productId 商品ID
     * @param images 图片文件列表
     */
    private void saveImagesToMySQL(Long productId, List<MultipartFile> images) {
        // 遍历所有图片
        for (int i = 0; i < images.size(); i++) {
            // 上传图片到服务器，获取访问URL
            String imageUrl = uploadImageToServer(images.get(i), productId, i);

            ProductImage productImage = new ProductImage();  // 创建商品图片实体对象
            productImage.setProductId(productId);  // 设置关联的商品ID
            productImage.setImage(imageUrl);  // 设置图片访问URL
            productImage.setSortOrder(i);  // 设置图片排序序号
            productImage.setCreatedAt(LocalDateTime.now());  // 设置创建时间

            productImageMapper.insert(productImage);  // 调用Mapper插入图片记录到数据库
        }
        log.info("图片保存成功: productId={}, count={}", productId, images.size());  // 记录图片保存成功的日志
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
            String fileName = index + "_" + UUID.randomUUID() + getFileExtension(file.getOriginalFilename());  // 生成唯一文件名：序号_UUID.扩展名
            Path uploadPath = Paths.get(uploadDir, "products_images", String.valueOf(productId)).toAbsolutePath();  // 构建完整上传目录路径

            // 判断目录是否存在
            if (!Files.exists(uploadPath)) {
                // 目录不存在则创建
                Files.createDirectories(uploadPath);
            }

            // 构建完整文件路径
            Path filePath = uploadPath.resolve(fileName);

            // 将文件写入磁盘
            file.transferTo(filePath);

            // 返回图片访问URL
            return "/uploads/products_images/" + productId + "/" + fileName;

        } catch (IOException e) {  // 捕获IO异常
            // 记录错误日志
            log.error("图片上传失败: productId={}", productId, e);

            // 抛出运行时异常
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 同步库存到 Redis
     * @param productId 商品ID
     * @param stock 库存数量
     */
    private void syncStockToRedis(Long productId, Integer stock) {
        // 构建Redis库存Key
        String stockKey = "product:stock:" + productId;

        // 将库存存入Redis
        redisTemplate.opsForValue().set(stockKey, stock);

        // 记录同步成功的日志
        log.info("同步库存到Redis: productId={}, stock={}", productId, stock);
    }

    /**
     * 获取文件扩展名
     * @param filename 文件名
     * @return 文件扩展名（如 .jpg）
     */
    private String getFileExtension(String filename) {
        // 判断文件名是否为空或没有扩展名
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg";  // 默认返回 .jpg
        }

        // 返回文件扩展名
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

        // 更新商品基本信息
        existingProduct.setName(productDTO.getName());
        existingProduct.setBrand(productDTO.getBrand());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setOriginalPrice(productDTO.getOriginalPrice());
        existingProduct.setStock(productDTO.getStock());
        existingProduct.setCategoryId(productDTO.getCategoryId());
        existingProduct.setStatus(productDTO.getStatus());
        existingProduct.setUpdatedAt(LocalDateTime.now());
        productMapper.update(existingProduct);

        // 同步库存到 Redis（库存发生变化时更新）
        syncStockToRedis(productId, productDTO.getStock());

        // 处理新上传的图片（如果有）
        if (files != null && !files.isEmpty()) {
            // 获取所有图片
            List<ProductImage> images = productImageMapper.findByProductId(productId);

            // 图片数组检查
            if (images.isEmpty()) {
                throw new RuntimeException("图片为空，删除失败");
            }

            // 删除服务器上的旧图片文件
            for (ProductImage image : images) {
                deleteImageFile(image.getImage());
            }

            // 删除数据库中的旧图片记录
            productImageMapper.deleteByProductId(productId);

            // 保存新图片
            for (int i = 0; i < files.size(); i++) {
                // 图片路径
                String imageUrl = uploadImageToServer(files.get(i), productId, i);
                ProductImage productImage = new ProductImage();
                productImage.setProductId(productId);
                productImage.setImage(imageUrl);
                productImage.setSortOrder(i);
                productImageMapper.insert(productImage);
            }
        }
    }

    /** 删除服务器图片 */
    public void deleteImageFile(String imageUrl) throws IOException {
        try {
            // 参数校验
            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new RuntimeException("旧图片路径为空");
            }

            // 拼接旧图片文件路径
            Path filePath = Paths.get(imageUrl).toAbsolutePath();

            if (Files.exists(filePath)) {
                // 删除旧图片
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
    public CheckoutItemVO getCheckoutItem(Long productId, Integer quantity) {
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
        item.setPrice(product.getPrice());
        item.setOriginalPrice(product.getOriginalPrice());
        item.setQuantity(quantity);
        item.setStock(product.getStock());
        item.setProductImage(mainImage);

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