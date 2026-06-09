package com.xiaoshan.springbootdemo.component;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.entity.ProductSku;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import com.xiaoshan.springbootdemo.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 项目启动时，将商品库存同步到 Redis
 * CommandLineRunner：Spring 启动后自动执行 run 方法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockInitializer implements CommandLineRunner {

    private final ProductMapper productMapper;  // 商品数据库操作
    private final ProductSkuMapper productSkuMapper;  // SKU数据库操作
    private final RedisTemplate<String, Object> redisTemplate;  // Redis 操作

    @Override
    public void run(String... args) throws Exception {
        log.info("========== 开始初始化 SKU 库存到 Redis ==========");

        // 查询所有商品
        List<Product> products = productMapper.findAll();
        int totalSkus = 0;

        for (Product product : products) {
            // 查询该商品的所有 SKU
            List<ProductSku> skus = productSkuMapper.findByProductId(product.getId());

            for (ProductSku sku : skus) {
                // Redis Key：sku:stock:SKU_ID（真实库存）
                String realStockKey = "sku:stock:" + sku.getId();

                // 将 SKU 真实库存存入 Redis（加随机过期时间防止缓存雪崩）
                redisTemplate.opsForValue().set(realStockKey, sku.getStock(),
                    60 + new Random().nextInt(60), TimeUnit.SECONDS);
                totalSkus++;
            }
        }

        log.info("SKU 库存初始化完成，共 {} 个 SKU", totalSkus);
    }
}