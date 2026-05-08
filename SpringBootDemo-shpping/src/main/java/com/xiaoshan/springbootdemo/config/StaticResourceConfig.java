package com.xiaoshan.springbootdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 获取项目根目录下的uploads文件夹绝对路径，构建文件系统URL前缀 (file: 协议)
        registry.addResourceHandler("/uploads/**") // URL访问路径
                .addResourceLocations("file:" + Paths.get("uploads").toAbsolutePath()); // 对应的物理文件存储位置

        // 可以添加日志来确认配置生效
        System.out.println("动态资源映射配置 - 上传目录: " + Paths.get("uploads").toAbsolutePath());
    }
}
