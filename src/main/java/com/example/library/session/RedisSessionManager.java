package com.example.library.session;

import com.example.library.config.AuthSessionProperties;
import com.example.library.constant.RedisKeyConstants;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.UserMapper;
import com.example.library.utils.JwtUtil;
import com.example.library.vo.AuthTokenVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSessionManager implements SessionManager {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BEARER = "Bearer";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthSessionProperties authSessionProperties;
    private final UserMapper userMapper;
    private final RedissonClient redissonClient;

    @Value("${auth.jwt.secret}")
    private String jwtSecret;

    @Value("${auth.jwt.blacklist-ttl-seconds:7200}")
    private long blacklistTtlSeconds;

    @Override
    public AuthTokenVO createSession(User user) {
        String sessionJti = newJti();
        String accessJti = newJti();
        String refreshJti = newJti();
        int tokenVersion = resolveTokenVersion(user);
        long now = System.currentTimeMillis();
        LoginSession session = new LoginSession(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                sessionJti,
                refreshJti,
                tokenVersion,
                now,
                now
        );
        saveSession(session, resolveSessionTtlSeconds());
        return buildTokenVO(user.getId(), sessionJti, accessJti, refreshJti, tokenVersion);
    }

    @Override
    public AuthTokenVO refresh(String refreshToken) {
        Claims claims = parseClaims(refreshToken);
        requireTokenType(claims, JwtUtil.TOKEN_TYPE_REFRESH);
        Long userId = extractUserId(claims);
        String sessionJti = extractClaim(claims, JwtUtil.CLAIM_SESSION_JTI);
        String refreshJti = extractClaim(claims, JwtUtil.CLAIM_REFRESH_JTI);
        Integer tokenVersion = extractTokenVersion(claims);
        if (isBlacklistedRefreshJti(refreshJti)) {
            throw unauthorized();
        }

        RLock lock = redissonClient.getLock(RedisKeyConstants.lockRefreshSession(sessionJti));
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw unauthorized();
            }
            LoginSession session = getSessionByJti(sessionJti);
            if (session == null) {
                throw unauthorized();
            }
            if (!refreshJti.equals(session.getRefreshJti())) {
                deleteSession(sessionJti);
                throw unauthorized();
            }

            User user = validateSessionUser(userId, tokenVersion, session, sessionJti);
            String newAccessJti = newJti();
            String newRefreshJti = newJti();
            session.setRefreshJti(newRefreshJti);
            session.setLastActiveTime(System.currentTimeMillis());
            saveSession(session, resolveSessionTtlSeconds());
            blacklistRefreshJti(refreshJti, Math.max(JwtUtil.getRemainingSeconds(claims), 1L));
            return buildTokenVO(user.getId(), sessionJti, newAccessJti, newRefreshJti, tokenVersion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unauthorized();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public LoginSession getSessionByJti(String sessionJti) {
        if (!StringUtils.hasText(sessionJti)) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(RedisKeyConstants.loginSession(sessionJti));
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoginSession.class);
        } catch (Exception e) {
            log.warn("deserialize login session failed, sessionJti={}", maskToken(sessionJti), e);
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
            if (!JwtUtil.isTokenType(claims, JwtUtil.TOKEN_TYPE_ACCESS)) {
                return;
            }
            String sessionJti = extractClaim(claims, JwtUtil.CLAIM_SESSION_JTI);
            String accessJti = extractClaim(claims, JwtUtil.CLAIM_ACCESS_JTI);
            LoginSession session = getSessionByJti(sessionJti);
            Long sessionTtl = stringRedisTemplate.getExpire(RedisKeyConstants.loginSession(sessionJti), TimeUnit.SECONDS);
            deleteSession(sessionJti);
            blacklistAccessJti(accessJti, Math.max(JwtUtil.getRemainingSeconds(claims), 1L));
            if (session != null && StringUtils.hasText(session.getRefreshJti())) {
                blacklistRefreshJti(session.getRefreshJti(), positiveOrDefault(sessionTtl, resolveSessionTtlSeconds()));
            }
        } catch (Exception e) {
            log.warn("remove session skipped because token parse failed, tokenPrefix={}", maskToken(accessToken), e);
        }
    }

    @Override
    public void removeSessionByJti(String sessionJti) {
        deleteSession(sessionJti);
    }

    @Override
    public void renewIfNeededByJti(String sessionJti) {
        if (!StringUtils.hasText(sessionJti)) {
            return;
        }
        String key = RedisKeyConstants.loginSession(sessionJti);
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return;
        }
        if (shouldRenew(ttl)) {
            stringRedisTemplate.expire(key, resolveSessionTtlSeconds(), TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean isBlacklistedJti(String accessJti) {
        if (!StringUtils.hasText(accessJti)) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.accessBlacklist(accessJti)));
    }

    @Override
    public AuthenticatedSession authenticate(String accessToken) {
        Claims claims = parseClaims(accessToken);
        requireTokenType(claims, JwtUtil.TOKEN_TYPE_ACCESS);
        Long userId = extractUserId(claims);
        String sessionJti = extractClaim(claims, JwtUtil.CLAIM_SESSION_JTI);
        String accessJti = extractClaim(claims, JwtUtil.CLAIM_ACCESS_JTI);
        Integer tokenVersion = extractTokenVersion(claims);
        if (isBlacklistedJti(accessJti)) {
            throw unauthorized();
        }

        LoginSession session = getSessionByJti(sessionJti);
        if (session == null) {
            throw unauthorized();
        }
        User user = validateSessionUser(userId, tokenVersion, session, sessionJti);
        touchSession(session);
        return new AuthenticatedSession(user, session, sessionJti, accessJti);
    }

    long resolveAccessTokenTtlSeconds() {
        int minutes = authSessionProperties.getAccessTokenTtlMinutes() > 0
                ? authSessionProperties.getAccessTokenTtlMinutes()
                : authSessionProperties.getTtlMinutes();
        return Duration.ofMinutes(minutes).getSeconds();
    }

    long resolveSessionTtlSeconds() {
        return Duration.ofDays(authSessionProperties.getRefreshTokenTtlDays()).getSeconds();
    }

    long resolveBlacklistTtlSeconds() {
        return Math.max(resolveAccessTokenTtlSeconds(), blacklistTtlSeconds);
    }

    boolean shouldRenew(long currentTtlSeconds) {
        return currentTtlSeconds < authSessionProperties.getRenewThresholdSeconds();
    }

    int resolveTokenVersion(User user) {
        return user == null || user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private AuthTokenVO buildTokenVO(Long userId, String sessionJti, String accessJti, String refreshJti, int tokenVersion) {
        long accessTtlSeconds = resolveAccessTokenTtlSeconds();
        long refreshTtlSeconds = resolveSessionTtlSeconds();
        String accessToken = JwtUtil.createAccessToken(userId, sessionJti, accessJti, tokenVersion, accessTtlSeconds, jwtSecret);
        String refreshToken = JwtUtil.createRefreshToken(userId, sessionJti, refreshJti, tokenVersion, refreshTtlSeconds, jwtSecret);
        return new AuthTokenVO(accessToken, refreshToken, BEARER, accessTtlSeconds);
    }

    private void touchSession(LoginSession session) {
        session.setLastActiveTime(System.currentTimeMillis());
        String key = RedisKeyConstants.loginSession(session.getSessionJti());
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        long targetTtl = ttl == null || ttl <= 0 || shouldRenew(ttl) ? resolveSessionTtlSeconds() : ttl;
        saveSession(session, targetTtl);
    }

    private void saveSession(LoginSession session, long ttlSeconds) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.loginSession(session.getSessionJti()),
                    objectMapper.writeValueAsString(session),
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize login session failed", e);
        }
    }

    private User validateSessionUser(Long userId, Integer tokenVersion, LoginSession session, String sessionJti) {
        if (!userId.equals(session.getUserId()) || !tokenVersion.equals(session.getTokenVersion())) {
            deleteSession(sessionJti);
            throw unauthorized();
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            deleteSession(sessionJti);
            throw unauthorized();
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            deleteSession(sessionJti);
            throw new BusinessException(BusinessException.FORBIDDEN, "账号已被禁用");
        }
        if (resolveTokenVersion(user) != session.getTokenVersion()) {
            deleteSession(sessionJti);
            throw unauthorized();
        }
        return user;
    }

    private void deleteSession(String sessionJti) {
        if (StringUtils.hasText(sessionJti)) {
            stringRedisTemplate.delete(RedisKeyConstants.loginSession(sessionJti));
        }
    }

    private void blacklistAccessJti(String accessJti, long ttlSeconds) {
        if (StringUtils.hasText(accessJti)) {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.accessBlacklist(accessJti),
                    "1",
                    positiveOrDefault(ttlSeconds, resolveBlacklistTtlSeconds()),
                    TimeUnit.SECONDS
            );
        }
    }

    private void blacklistRefreshJti(String refreshJti, long ttlSeconds) {
        if (StringUtils.hasText(refreshJti)) {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.refreshBlacklist(refreshJti),
                    "1",
                    positiveOrDefault(ttlSeconds, resolveSessionTtlSeconds()),
                    TimeUnit.SECONDS
            );
        }
    }

    private boolean isBlacklistedRefreshJti(String refreshJti) {
        return StringUtils.hasText(refreshJti)
                && Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.refreshBlacklist(refreshJti)));
    }

    private Claims parseClaims(String token) {
        try {
            return JwtUtil.parseToken(token, jwtSecret);
        } catch (Exception e) {
            throw unauthorized();
        }
    }

    private void requireTokenType(Claims claims, String tokenType) {
        if (!JwtUtil.isTokenType(claims, tokenType)) {
            throw unauthorized();
        }
    }

    private Long extractUserId(Claims claims) {
        Object userIdClaim = claims.get(JwtUtil.CLAIM_USER_ID);
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            throw unauthorized();
        }
    }

    private String extractClaim(Claims claims, String claimName) {
        String value = claims.get(claimName, String.class);
        if (!StringUtils.hasText(value)) {
            throw unauthorized();
        }
        return value;
    }

    private Integer extractTokenVersion(Claims claims) {
        Integer tokenVersion = claims.get(JwtUtil.CLAIM_TOKEN_VERSION, Integer.class);
        if (tokenVersion == null) {
            throw unauthorized();
        }
        return tokenVersion;
    }

    private BusinessException unauthorized() {
        return new BusinessException(BusinessException.UNAUTHORIZED, "login status expired, please login again");
    }

    private long positiveOrDefault(Long value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private long positiveOrDefault(long value, long defaultValue) {
        return value <= 0 ? defaultValue : value;
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
