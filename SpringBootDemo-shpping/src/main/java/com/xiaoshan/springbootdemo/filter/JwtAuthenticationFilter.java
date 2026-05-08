package com.xiaoshan.springbootdemo.filter;

import com.xiaoshan.springbootdemo.entity.User;
import com.xiaoshan.springbootdemo.mapper.UserMapper;
import com.xiaoshan.springbootdemo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 从Cookie提取JWT Token
        String token = jwtUtil.extractTokenFromCookie(request);

        // 验证Token有效性
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                // 解析Token获取用户ID
                Long userId = jwtUtil.getUserIdFromToken(token);
                log.debug("开始处理用户认证，用户ID: {}", userId);

                // 查询数据库验证用户存在性
                User user = userMapper.findById(userId)
                        .orElseThrow(() -> {
                            log.warn("用户不存在，用户ID: {}", userId);
                            return new RuntimeException("用户不存在");
                        });

                // 检查用户账号状态
                if (!user.getStatus()) {
                    log.warn("用户: {} 账号已被禁用", userId);
                    clearAuthCookie(response);
                    sendErrorResponse(response, "账号已被禁用");
                    return;
                }

                /**
                 * 这个构造函数的三个参数：
                 * principal：用户标识（这里用 userId）
                 * credentials：凭证（密码等，这里为 null 因为 JWT 已经验证过了）
                 * authorities：用户权限列表
                 * */
                // 创建用户权限列表
                List<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority(user.getRole().name())
                );

                // 创建认证对象并设置到安全上下文
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("用户: {} 认证成功，角色: {}", userId, user.getRole().name());

            } catch (RuntimeException e) {
                // 认证失败处理：清除Cookie并返回错误
                log.error("JWT认证失败: {}", e.getMessage());
                clearAuthCookie(response);
                sendErrorResponse(response, "认证失败: " + e.getMessage());
                return;
            }
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 清除认证Cookie
     */
    private void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", "")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("已清除认证Cookie");
    }

    /**
     * 发送认证错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
        response.getWriter().flush();
    }
}