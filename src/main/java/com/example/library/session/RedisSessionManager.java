package com.example.library.session;

import com.example.library.config.AuthSessionProperties;
import com.example.library.constant.RedisKeyConstants;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.UserMapper;
import com.example.library.utils.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Redis + JWT 的统一会话管理实现。
 *
 * 认证关系：
 * 1. JWT 只承载 userId、jti、tokenVersion；
 * 2. Redis session 决定该 jti 是否仍在线；
 * 3. blacklist 用于登出后阻断旧 jti；
 * 4. tokenVersion 与数据库用户版本对齐，用于后台禁用/踢下线；
 * 5. 通过校验后按阈值执行滑动续期。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSessionManager implements SessionManager {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthSessionProperties authSessionProperties;
    private final UserMapper userMapper;

    @Value("${auth.jwt.secret}")
    private String jwtSecret;

    @Value("${auth.jwt.blacklist-ttl-seconds:7200}")
    private long blacklistTtlSeconds;

    @Override
    public String createSession(User user) {
        String jti = newJti();
        int tokenVersion = resolveTokenVersion(user);
        String token = JwtUtil.createToken(user.getId(), jti, tokenVersion, jwtSecret);
        LoginSession session = new LoginSession(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                System.currentTimeMillis(),
                tokenVersion
        );
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.loginToken(jti),
                    objectMapper.writeValueAsString(session),
                    resolveSessionTtlSeconds(),
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize login session failed", e);
        }
        return token;
    }

    @Override
    public LoginSession getSessionByJti(String jti) {
        if (!StringUtils.hasText(jti)) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(RedisKeyConstants.loginToken(jti));
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoginSession.class);
        } catch (Exception e) {
            log.warn("deserialize login session failed, jti={}", maskToken(jti), e);
            return null;
        }
    }

    @Override
    public void removeSession(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return;
        }
        try {
            Claims claims = JwtUtil.parseToken(accessToken, jwtSecret);
            removeSessionByJti(claims.getId());
        } catch (Exception e) {
            log.warn("remove session skipped because token parse failed, tokenPrefix={}", maskToken(accessToken), e);
        }
    }

    @Override
    public void removeSessionByJti(String jti) {
        if (!StringUtils.hasText(jti)) {
            return;
        }
        stringRedisTemplate.delete(RedisKeyConstants.loginToken(jti));
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.loginBlacklist(jti),
                "1",
                resolveBlacklistTtlSeconds(),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void renewIfNeededByJti(String jti) {
        if (!StringUtils.hasText(jti)) {
            return;
        }
        String key = RedisKeyConstants.loginToken(jti);
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return;
        }
        if (shouldRenew(ttl)) {
            stringRedisTemplate.expire(key, resolveSessionTtlSeconds(), TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean isBlacklistedJti(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.loginBlacklist(jti)));
    }

    @Override
    public AuthenticatedSession authenticate(String accessToken) {
        Claims claims = parseClaims(accessToken);
        Long userId = extractUserId(claims);
        String jti = extractJti(claims);
        Integer tokenVersion = extractTokenVersion(claims);
        if (isBlacklistedJti(jti)) {
            throw new BusinessException(BusinessException.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }

        LoginSession session = getSessionByJti(jti);
        if (session == null) {
            throw new BusinessException(BusinessException.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (!userId.equals(session.getUserId()) || !tokenVersion.equals(session.getTokenVersion())) {
            removeSessionByJti(jti);
            throw new BusinessException(BusinessException.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            removeSessionByJti(jti);
            throw new BusinessException(BusinessException.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            removeSessionByJti(jti);
            throw new BusinessException(BusinessException.FORBIDDEN, "账号已被禁用");
        }
        if (resolveTokenVersion(user) != session.getTokenVersion()) {
            removeSessionByJti(jti);
            throw new BusinessException(BusinessException.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }

        renewIfNeededByJti(jti);
        return new AuthenticatedSession(user, session, jti);
    }

    long resolveSessionTtlSeconds() {
        return Duration.ofMinutes(authSessionProperties.getTtlMinutes()).getSeconds();
    }

    long resolveBlacklistTtlSeconds() {
        return Math.max(resolveSessionTtlSeconds(), blacklistTtlSeconds);
    }

    boolean shouldRenew(long currentTtlSeconds) {
        return currentTtlSeconds < authSessionProperties.getRenewThresholdSeconds();
    }

    int resolveTokenVersion(User user) {
        return user == null || user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private Claims parseClaims(String accessToken) {
        try {
            return JwtUtil.parseToken(accessToken, jwtSecret);
        } catch (Exception e) {
            throw new BusinessException(BusinessException.UNAUTHORIZED, "登录凭证无效或已过期");
        }
    }

    private Long extractUserId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            throw new BusinessException(BusinessException.UNAUTHORIZED, "登录凭证无效或已过期");
        }
    }

    private String extractJti(Claims claims) {
        String jti = claims.getId();
        if (!StringUtils.hasText(jti)) {
            throw new BusinessException(BusinessException.UNAUTHORIZED, "登录凭证无效或已过期");
        }
        return jti;
    }

    private Integer extractTokenVersion(Claims claims) {
        Integer tokenVersion = claims.get("tokenVersion", Integer.class);
        if (tokenVersion == null) {
            throw new BusinessException(BusinessException.UNAUTHORIZED, "登录凭证无效或已过期");
        }
        return tokenVersion;
    }

    private static String newJti() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "***";
        }
        if (token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
