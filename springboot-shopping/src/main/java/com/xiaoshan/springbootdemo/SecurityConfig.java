package com.xiaoshan.springbootdemo;

import com.xiaoshan.springbootdemo.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.function.Supplier;

@Configuration // 标识这是配置类，其中的@Bean方法会被Spring管理
@EnableWebSecurity // 专门启用Spring Security的Web安全功能
@RequiredArgsConstructor // 是Lombok提供的注解，用于自动生成包含所有final字段的构造函数，实现依赖注入。
public class SecurityConfig {

    // 注入Jwt认证过滤器
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 配置密码编码器 Bcrypt是一种安全的密码哈希算法,会自动加盐(salt)处理
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ 确保 CORS 配置被注入
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 创建CORS配置对象
        CorsConfiguration configuration = new CorsConfiguration();

        // 设置允许访问的来源（前端地址） 这告诉浏览器哪些源（协议+域名+端口）可以访问这个API
        // 如果设置为 "*" 表示允许所有来源，但不能与 allowCredentials(true) 同时使用
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://*.natappfree.cc",
                "http://62.234.79.156",            // 你的云服务器 IP（前端访问地址）
                "http://62.234.79.156:80"          // 如果你用 80 端口

        ));

        // 设置允许可以用于跨域请求的HTTP方法 OPTIONS方法必须包含，因为浏览器会先发送OPTIONS预检请求
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // 设置允许的请求头
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "X-XSRF-TOKEN",
                "X-CSRF-TOKEN",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // 设置是否允许发送凭证（cookies、认证信息等）
        configuration.setAllowCredentials(true);

        // 设置预检请求的缓存时间（秒）
        configuration.setMaxAge(3600L);

        // 创建URL基础的CORS配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 注册CORS配置 "/**" 表示应用到所有路径
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    // 安全过滤器链配置 - 核心安全配置
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 关键：启用 CORS 并引用上面的配置
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // 配置CSRF CsrfFilter: "这个请求需要CSRF保护吗？CSRF token有效吗？"
        http.csrf(csrf -> csrf
                    // 允许前端js读取cookie中XSRF-TOKEN
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    // 确保每次请求生成新令牌
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler() {
                        @Override
                        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
                            // 强制生成新令牌
                            CsrfToken newToken = csrfToken.get();
                            response.setHeader(newToken.getHeaderName(), newToken.getToken());
                        }
                    })
                .ignoringRequestMatchers(
                        "/api/v1/payment/callback",
                                "/api/v1/logistics/callback"
                )
            );

        // JWT过滤器认证 JwtAuthenticationFilter: "Cookie中有JWT吗？token有效吗？用户存在吗？"
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 配置授权请求网站
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                // 静态资源放行(无需认证)
                "/images/**",      // 放行所有图片
                "/favicon.ico",    // 放行网站图标
                "/uploads/**",    // 放行上传的文件

                // API接口放行
                "/api/v1/auth/login", // 游客登录
                "/api/v1/auth/register", // 游客注册
                "/api/v1/account/profile", // 获取账号状态
                "/api/v1/categories", // 获取分类
                "/api/v1/products/**", // 获取商品


                // 放行支付api回调路径
                "/api/v1/payment/callback",
                "/api/v1/logistics/callback"



                ).permitAll() // 允许匿名访问的路径

            .anyRequest().authenticated() // 其他所有请求需要认证( @PreAuthorize 生效 )
        );



        return http.build();
    }

}

