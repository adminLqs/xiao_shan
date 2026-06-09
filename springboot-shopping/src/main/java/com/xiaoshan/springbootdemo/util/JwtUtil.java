package com.xiaoshan.springbootdemo.util;

import com.xiaoshan.springbootdemo.entity.Role;
import com.xiaoshan.springbootdemo.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Slf4j // 日志注解
public class JwtUtil {

    // 从配置文件（如application.yml或application.properties）中读取jwt.secret的值注入到：secret 成员变量 用途：JWT签名使用的密钥，这是最重要的安全参数
    @Value("${jwt.secret}")
    private String secret;

    /* 生成Token */
    public String generateToken(User user) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expirationTime = now.plusMonths(1);
        Date expirationDate = Date.from(expirationTime.toInstant());

        List<String> roles = new ArrayList<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                roles.add(role.getName());
            }
        } else if (user.getRole() != null) {
            roles.add(user.getRole());
        }

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .claim("roles", roles)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        return claims.get("roles", List.class);
    }

    // 从请求中提取Token
    public String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // 验证token有效性
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT token验证失败: {}", e.getMessage());
            return false;
        }
    }

    // 从token中获取用户ID
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody()
                .getSubject());
    }

    /**
     * 获取token剩余有效时间（毫秒）
     */
    public Long getRemainTime(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("获取token剩余时间失败: {}", e.getMessage());
            return 0L;
        }
    }
}