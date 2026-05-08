package com.xiaoshan.springbootdemo.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration // 标识这是一个配置类，Spring 启动时会加载
public class RedisConfig {

    @Bean // 将 RedisTemplate 注册为 Spring Bean，供其他类注入使用
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 设置连接工厂（用于建立与 Redis 服务器的连接）
        template.setConnectionFactory(factory);

        // Key 序列化使用 String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 序列化使用 JSON
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 初始化模板配置
        template.afterPropertiesSet();

        return template;
    }
}