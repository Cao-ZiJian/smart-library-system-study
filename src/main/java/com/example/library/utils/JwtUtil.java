package com.example.library.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public final class JwtUtil {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_SESSION_JTI = "sessionJti";
    public static final String CLAIM_ACCESS_JTI = "accessJti";
    public static final String CLAIM_REFRESH_JTI = "refreshJti";
    public static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    private JwtUtil() {
    }

    public static String createAccessToken(Long userId, String sessionJti, String accessJti,
                                           Integer tokenVersion, long ttlSeconds, String secret) {
        return createToken(userId, sessionJti, accessJti, CLAIM_ACCESS_JTI,
                tokenVersion, TOKEN_TYPE_ACCESS, ttlSeconds, secret);
    }

    public static String createRefreshToken(Long userId, String sessionJti, String refreshJti,
                                            Integer tokenVersion, long ttlSeconds, String secret) {
        return createToken(userId, sessionJti, refreshJti, CLAIM_REFRESH_JTI,
                tokenVersion, TOKEN_TYPE_REFRESH, ttlSeconds, secret);
    }

    public static String createToken(Long userId, String jti, Integer tokenVersion, String secret) {
        return createAccessToken(userId, jti, jti, tokenVersion, 7200L, secret);
    }

    public static Claims parseToken(String token, String secret) {
        return Jwts.parserBuilder()
                .setSigningKey(buildSigningKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static boolean isTokenType(Claims claims, String tokenType) {
        return claims != null && tokenType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public static long getRemainingSeconds(Claims claims) {
        if (claims == null || claims.getExpiration() == null) {
            return 0L;
        }
        long seconds = claims.getExpiration().toInstant().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(seconds, 0L);
    }

    private static String createToken(Long userId, String sessionJti, String tokenJti, String tokenJtiClaim,
                                      Integer tokenVersion, String tokenType, long ttlSeconds, String secret) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(tokenJti)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_SESSION_JTI, sessionJti)
                .claim(tokenJtiClaim, tokenJti)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(buildSigningKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    private static SecretKey buildSigningKey(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("JWT secret cannot be blank");
        }
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception ignore) {
            byte[] utf8Bytes = secret.getBytes(StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(utf8Bytes);
        }
    }
}
