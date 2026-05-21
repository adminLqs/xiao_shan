package com.xiaoshan.springbootdemo.util;

import com.xiaoshan.springbootdemo.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;

@Component
@Slf4j // 日志注解
public class JwtUtil {

    // 从配置文件（如application.yml或application.properties）中读取jwt.secret的值注入到：secret 成员变量 用途：JWT签名使用的密钥，这是最重要的安全参数
    @Value("${jwt.secret}")
    private String secret;

    /* 生成Token */
    public String generateToken(User user) {
        // 获取当前时间
        ZonedDateTime now = ZonedDateTime.now();

        // 获取下个月1日时间
        ZonedDateTime expirationTime = now.plusMonths(1) // 1. 计算基础时间点：在当前时间的基础上增加一个月。
                .withDayOfMonth(1) // // 2. 将日期调整到计算后日期所在月份的第一天。
                .withHour(0) // 3. 将时间部分的小时设置为0，即午夜0点。
                .withMinute(0) // 4. 将时间部分的分钟设置为0。
                .withSecond(0) // 5. 将时间部分的秒设置为0。
                .withNano(0); // 6. 将时间部分的纳秒设置为0，清除所有更精细的时间单位

        //  将 `ZonedDateTime` 对象转换为传统的 `java.util.Date` 对象
        Date expirationDate = Date.from(expirationTime.toInstant());

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
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
}