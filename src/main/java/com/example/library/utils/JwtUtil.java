package com.example.library.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：HS256 签发与解析
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    public static String createToken(Long userId, String jti, Integer tokenVersion, String secret) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(jti)
                .claim("tokenVersion", tokenVersion)
                .setIssuedAt(new Date())
                .signWith(buildSigningKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parseToken(String token, String secret) {
        return Jwts.parserBuilder()
                .setSigningKey(buildSigningKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private static SecretKey buildSigningKey(String secret) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception ignore) {
            byte[] utf8Bytes = secret.getBytes(StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(utf8Bytes);
        }
    }
}
