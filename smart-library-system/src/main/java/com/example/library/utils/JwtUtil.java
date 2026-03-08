package com.example.library.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类
 *
 * 用于生成和解析登录令牌。
 */
@Component
public class JwtUtil {

    /**
     * 秘钥，从配置文件中读取
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * 过期时间（分钟）
     */
    @Value("${jwt.expire-minutes}")
    private long expireMinutes;

    /**
     * 生成 JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色
     * @return token 字符串
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireMinutes * 60 * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * 解析 Token
     *
     * @param token JWT 字符串
     * @return Claims，包含 subject、username、role 等
     * @throws ExpiredJwtException token 过期
     * @throws io.jsonwebtoken.JwtException 其他解析异常
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();//返回claims，及payload
    }
}

