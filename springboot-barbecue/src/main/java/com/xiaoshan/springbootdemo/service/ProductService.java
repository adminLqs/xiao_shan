package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.dto.ProductDTO;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.SellerProfileMapper;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final SellerProfileMapper sellerProfileMapper;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    // =================== 商家商品添加 =======================

    public void addProduct(ProductDTO productDTO, MultipartFile file) throws IOException {

        // 校验商品信息
        Product product = validateProduct(productDTO);

        // 商品图片唯一文件名
        String filename = generateUniqueFileName(file, "product");

        // 图片保存至服务器
        String newImageUrl = saveImageFile(file, filename);

        // 存储到实体类
        product.setImage(newImageUrl);

        // 保存商品
        productMapper.insert(product);


    }

    // 验证商品逻辑
    public Product validateProduct(ProductDTO productDTO) {
        // 创建商品信息类
        Product product = new Product();

        // 商品名称不能为空
        if (productDTO.getName() == null) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (productDTO.getName().length() > 30 || productDTO.getName().length() < 1) {
            throw new RuntimeException("商品名称必须在1-30字之间");
        }
        product.setName(productDTO.getName());

        // 商品描述校验
        if (productDTO.getDescription().length() > 500){
            throw new RuntimeException("商品描述不得大于500字");
        }
        product.setDescription(productDTO.getDescription());

        // 商品价格不能为空且必须大于0
        if (productDTO.getPrice() == null || productDTO.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("商品价格不能为空且必须大于0");
        }
        product.setPrice(productDTO.getPrice());
        product.setOriginalPrice(productDTO.getPrice());

        // 商品分类不能为空
        if (StringUtils.isBlank(productDTO.getCategory())) {
            throw new RuntimeException("商品分类不能为空");
        }
        product.setCategory(productDTO.getCategory());

        return product;
    }


    // 保存商品图片到服务器
    public String saveImageFile(MultipartFile file, String filename) throws IOException {
        // 图片目录路径
        Path imageDir = Paths.get(uploadDir, "product_images").toAbsolutePath();

        // 若为空创建目标目录
        if (!Files.exists(imageDir)){
            Files.createDirectories(imageDir);
        }

        // 拼接完整路径
        Path targetPath = imageDir.resolve(filename);

        // 保存图片
        file.transferTo(targetPath);

        return "/uploads/product_images/" + filename;
    }

    /**
     * 生成唯一文件名
     *
     * @param file   上传的文件
     * @param prefix 文件名前缀（如：product、avatar、seller）
     * @return 唯一文件名，格式：{prefix}_{timestamp}_{uuid}{extension}
     */
    public String generateUniqueFileName(MultipartFile file, String prefix) {
        // 获取文件后缀名
        String fileExtension = getFileExtension(file.getOriginalFilename());

        // 生成毫秒级时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());

        // 生成UUID（取前8位，保证唯一性）
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        return prefix + "_" + timestamp + "_" + uuid + fileExtension;
    }

    // 获取文件扩展名
    public String getFileExtension(String filename) {
        // 若为空或没有点则返回jpg
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")); // 从起始截取到末尾
    }

    // 清理旧商品图片
    private void cleanupOldProductImageFile(String oldAvatarUrl) {
        // 判断旧头像路径是否为空或基础头像
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty() && !oldAvatarUrl.contains("seller")) {
            try {
                // 旧文件名
                String oldFileName = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);
                // 旧文件路径
                Path oldFilePath = Paths.get(uploadDir, "product_images", oldFileName).toAbsolutePath();

                // 文件删除判断
                if (Files.exists(oldFilePath)) {
                    Files.delete(oldFilePath);
                    log.debug("旧头像文件已删除: {}", oldFileName);
                }
            } catch (Exception e) {
                log.warn("删除旧头像文件失败: {}, 错误: {}", oldAvatarUrl, e.getMessage());
            }
        }
    }

    // ================ 商品更改 =================

    /**
     * 更新商品
     */
    @Transactional
    public Product updateProduct(Long id, ProductDTO productDTO, MultipartFile imageFile) throws IOException {
        // 查询原商品
        Product existingProduct = productMapper.findById(id);
        if (existingProduct == null) {
            throw new RuntimeException("商品不存在");
        }

        // 处理图片
        String newImageUrl = existingProduct.getImage(); // 默认使用原图片

        if (imageFile != null && !imageFile.isEmpty()) {
            // 生成唯一文件名
            String filename = generateUniqueFileName(imageFile, "product");

            // 有新图片上传，保存新图片
            newImageUrl = saveImageFile(imageFile, filename);

            // 删除旧图片
            cleanupOldProductImageFile(existingProduct.getImage());

        }

        // 更新商品信息
        existingProduct.setName(productDTO.getName());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setOriginalPrice(productDTO.getOriginalPrice());
        existingProduct.setImage(newImageUrl);
        existingProduct.setCategory(productDTO.getCategory());

        // 保存到数据库
        productMapper.update(existingProduct);

        log.info("商品更新成功: id={}, name={}", id, existingProduct.getName());
        return existingProduct;
    }

    /**
     * 删除商品
     */
    @Transactional
    public void deleteProduct(Long id) {
        // 查询商品
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 删除商品图片
        cleanupOldProductImageFile(product.getImage());

        // 删除数据库记录
        productMapper.deleteById(id);

        log.info("商品删除成功: id={}, name={}", id, product.getName());
    }

    // ==================== 查询方法 ====================

    /**
     * 查询所有商品
     *
     * @return 商品列表
     */
    public List<Product> findAll() {
        return productMapper.findAll();
    }

}
