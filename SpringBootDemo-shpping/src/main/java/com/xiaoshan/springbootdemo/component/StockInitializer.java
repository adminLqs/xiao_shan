package com.xiaoshan.springbootdemo.component;

import com.xiaoshan.springbootdemo.entity.Product;
import com.xiaoshan.springbootdemo.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 项目启动时，将商品库存同步到 Redis
 * CommandLineRunner：Spring 启动后自动执行 run 方法
 */
@Slf4j  // 日志注解
@Component  // 注册为 Spring 组件
@RequiredArgsConstructor  // 生成构造器注入
public class StockInitializer implements CommandLineRunner {

    private final ProductMapper productMapper;  // 商品数据库操作
    private final RedisTemplate<String, Object> redisTemplate;  // Redis 操作

    @Override
    public void run(String... args) throws Exception {
        log.info("========== 开始初始化商品库存到 Redis ==========");

        // 1. 查询所有商品（从 MySQL 数据库）
        List<Product> products = productMapper.findAll();

        // 2. 遍历每个商品，将库存同步到 Redis
        for (Product product : products) {
            // Redis Key 格式：product:stock:商品ID
            String stockKey = "product:stock:" + product.getId();

            // 将库存存入 Redis（String 类型）
            redisTemplate.opsForValue().set(stockKey, product.getStock());

            log.info("初始化库存: productId={}, stock={}", product.getId(), product.getStock());
        }

        log.info("商品库存初始化完成，共 {} 件商品", products.size());
    }
}