package com.example.library.session;

import com.example.library.config.AuthSessionProperties;
import com.example.library.constant.RedisKeyConstants;
import com.example.library.entity.User;
import com.example.library.enums.UserRole;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.UserMapper;
import com.example.library.utils.JwtUtil;
import com.example.library.vo.AuthTokenVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSessionManagerTest {

    private static final String JWT_SECRET = "12345678901234567890123456789012";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock refreshLock;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthSessionProperties authSessionProperties;
    private RedisSessionManager sessionManager;

    @BeforeEach
    void setUp() throws InterruptedException {
        authSessionProperties = new AuthSessionProperties();
        authSessionProperties.setAccessTokenTtlMinutes(30);
        authSessionProperties.setRefreshTokenTtlDays(7);
        authSessionProperties.setRenewThresholdSeconds(600);

        sessionManager = new RedisSessionManager(
                stringRedisTemplate,
                objectMapper,
                authSessionProperties,
                userMapper,
                redissonClient
        );
        ReflectionTestUtils.setField(sessionManager, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(sessionManager, "blacklistTtlSeconds", 7200L);
        lenient().when(redissonClient.getLock(org.mockito.ArgumentMatchers.anyString())).thenReturn(refreshLock);
        lenient().when(refreshLock.tryLock(0, 10, TimeUnit.SECONDS)).thenReturn(true);
        lenient().when(refreshLock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void createSession_storesRedisSessionAndReturnsTokenPair() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthTokenVO tokenVO = sessionManager.createSession(activeUser(3L, "user01", 0));

        assertNotNull(tokenVO.getAccessToken());
        assertNotNull(tokenVO.getRefreshToken());
        assertEquals("Bearer", tokenVO.getTokenType());
        assertEquals(1800L, tokenVO.getExpiresIn());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(604800L), eq(TimeUnit.SECONDS));
        assertTrue(keyCaptor.getValue().startsWith("lib:v1:login:session:"));

        LoginSession session = objectMapper.readValue(valueCaptor.getValue(), LoginSession.class);
        assertEquals(3L, session.getUserId());
        assertEquals("user01", session.getUsername());
        assertNotNull(session.getSessionJti());
        assertNotNull(session.getRefreshJti());

        Claims accessClaims = JwtUtil.parseToken(tokenVO.getAccessToken(), JWT_SECRET);
        Claims refreshClaims = JwtUtil.parseToken(tokenVO.getRefreshToken(), JWT_SECRET);
        assertEquals("access", accessClaims.get(JwtUtil.CLAIM_TOKEN_TYPE));
        assertEquals("refresh", refreshClaims.get(JwtUtil.CLAIM_TOKEN_TYPE));
        assertEquals(session.getSessionJti(), accessClaims.get(JwtUtil.CLAIM_SESSION_JTI));
        assertEquals(session.getRefreshJti(), refreshClaims.get(JwtUtil.CLAIM_REFRESH_JTI));
    }

    @Test
    void authenticate_rejectsRefreshToken() {
        String refreshToken = JwtUtil.createRefreshToken(3L, "session-1", "refresh-1", 0, 604800L, JWT_SECRET);

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionManager.authenticate(refreshToken));

        assertEquals(BusinessException.UNAUTHORIZED, ex.getCode());
    }

    @Test
    void refresh_rejectsAccessToken() {
        String accessToken = JwtUtil.createAccessToken(3L, "session-1", "access-1", 0, 1800L, JWT_SECRET);

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionManager.refresh(accessToken));

        assertEquals(BusinessException.UNAUTHORIZED, ex.getCode());
    }

    @Test
    void refresh_rotatesRefreshJtiAndInvalidatesOldRefresh() throws Exception {
        String refreshToken = JwtUtil.createRefreshToken(3L, "session-rotate", "refresh-old", 0, 604800L, JWT_SECRET);
        LoginSession session = loginSession("session-rotate", "refresh-old", 0);
        when(stringRedisTemplate.hasKey(RedisKeyConstants.refreshBlacklist("refresh-old"))).thenReturn(false);
        when(userMapper.selectById(3L)).thenReturn(activeUser(3L, "user01", 0));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyConstants.loginSession("session-rotate")))
                .thenReturn(objectMapper.writeValueAsString(session));

        AuthTokenVO rotated = sessionManager.refresh(refreshToken);

        Claims rotatedRefresh = JwtUtil.parseToken(rotated.getRefreshToken(), JWT_SECRET);
        assertNotEquals("refresh-old", rotatedRefresh.get(JwtUtil.CLAIM_REFRESH_JTI));
        verify(valueOperations).set(eq(RedisKeyConstants.refreshBlacklist("refresh-old")), eq("1"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void refresh_deletesSessionWhenOldRefreshIsReused() throws Exception {
        String refreshToken = JwtUtil.createRefreshToken(3L, "session-replay", "refresh-old", 0, 604800L, JWT_SECRET);
        LoginSession session = loginSession("session-replay", "refresh-new", 0);
        when(stringRedisTemplate.hasKey(RedisKeyConstants.refreshBlacklist("refresh-old"))).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyConstants.loginSession("session-replay")))
                .thenReturn(objectMapper.writeValueAsString(session));

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionManager.refresh(refreshToken));

        assertEquals(BusinessException.UNAUTHORIZED, ex.getCode());
        verify(stringRedisTemplate).delete(RedisKeyConstants.loginSession("session-replay"));
    }

    @Test
    void authenticate_failsWhenTokenVersionChanged() throws Exception {
        String token = JwtUtil.createAccessToken(3L, "session-version", "access-version", 0, 1800L, JWT_SECRET);
        mockExistingSession("session-version", loginSession("session-version", "refresh-version", 0));
        when(userMapper.selectById(3L)).thenReturn(activeUser(3L, "user01", 1));

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionManager.authenticate(token));

        assertEquals(BusinessException.UNAUTHORIZED, ex.getCode());
        verify(stringRedisTemplate).delete(RedisKeyConstants.loginSession("session-version"));
    }

    @Test
    void authenticate_failsWhenUserDisabled() throws Exception {
        String token = JwtUtil.createAccessToken(3L, "session-disabled", "access-disabled", 0, 1800L, JWT_SECRET);
        User dbUser = activeUser(3L, "user01", 0);
        dbUser.setStatus(0);
        mockExistingSession("session-disabled", loginSession("session-disabled", "refresh-disabled", 0));
        when(userMapper.selectById(3L)).thenReturn(dbUser);

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionManager.authenticate(token));

        assertEquals(BusinessException.FORBIDDEN, ex.getCode());
        verify(stringRedisTemplate).delete(RedisKeyConstants.loginSession("session-disabled"));
    }

    @Test
    void authenticate_successRenewsSessionWhenTtlBelowThreshold() throws Exception {
        String token = JwtUtil.createAccessToken(3L, "session-renew", "access-renew", 0, 1800L, JWT_SECRET);
        mockExistingSession("session-renew", loginSession("session-renew", "refresh-renew", 0));
        when(userMapper.selectById(3L)).thenReturn(activeUser(3L, "user01", 0));
        when(stringRedisTemplate.getExpire(RedisKeyConstants.loginSession("session-renew"), TimeUnit.SECONDS)).thenReturn(300L);

        AuthenticatedSession authenticated = sessionManager.authenticate(token);

        assertEquals(3L, authenticated.getUser().getId());
        assertEquals("session-renew", authenticated.getSessionJti());
        verify(valueOperations).set(eq(RedisKeyConstants.loginSession("session-renew")), org.mockito.ArgumentMatchers.anyString(),
                eq(604800L), eq(TimeUnit.SECONDS));
    }

    @Test
    void removeSession_deletesSessionAndBlacklistsAccessAndRefresh() throws Exception {
        String token = JwtUtil.createAccessToken(3L, "session-logout", "access-logout", 0, 1800L, JWT_SECRET);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyConstants.loginSession("session-logout")))
                .thenReturn(objectMapper.writeValueAsString(loginSession("session-logout", "refresh-logout", 0)));
        when(stringRedisTemplate.getExpire(RedisKeyConstants.loginSession("session-logout"), TimeUnit.SECONDS)).thenReturn(600000L);

        sessionManager.removeSession(token);

        verify(stringRedisTemplate).delete(RedisKeyConstants.loginSession("session-logout"));
        verify(valueOperations).set(eq(RedisKeyConstants.accessBlacklist("access-logout")), eq("1"), anyLong(), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq(RedisKeyConstants.refreshBlacklist("refresh-logout")), eq("1"), eq(600000L), eq(TimeUnit.SECONDS));
    }

    @Test
    void renewIfNeededByJti_refreshesOnlyWhenBelowThreshold() {
        when(stringRedisTemplate.getExpire(RedisKeyConstants.loginSession("session-2"), TimeUnit.SECONDS)).thenReturn(300L);

        sessionManager.renewIfNeededByJti("session-2");

        verify(stringRedisTemplate).expire(RedisKeyConstants.loginSession("session-2"), 604800L, TimeUnit.SECONDS);
    }

    @Test
    void renewIfNeededByJti_skipsRefreshWhenTtlIsEnough() {
        when(stringRedisTemplate.getExpire(RedisKeyConstants.loginSession("session-3"), TimeUnit.SECONDS)).thenReturn(800L);

        sessionManager.renewIfNeededByJti("session-3");

        verify(stringRedisTemplate, never()).expire(eq(RedisKeyConstants.loginSession("session-3")), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void strategyMethods_areStableAndReadable() {
        assertEquals(1800L, sessionManager.resolveAccessTokenTtlSeconds());
        assertEquals(604800L, sessionManager.resolveSessionTtlSeconds());
        assertEquals(7200L, sessionManager.resolveBlacklistTtlSeconds());
        assertTrue(sessionManager.shouldRenew(599L));
        assertFalse(sessionManager.shouldRenew(600L));

        User user = new User();
        user.setTokenVersion(4);
        assertEquals(4, sessionManager.resolveTokenVersion(user));
        assertEquals(0, sessionManager.resolveTokenVersion(null));
    }

    private void mockExistingSession(String sessionJti, LoginSession session) throws Exception {
        when(stringRedisTemplate.hasKey(RedisKeyConstants.accessBlacklist("access-" + sessionJti.substring("session-".length()))))
                .thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyConstants.loginSession(sessionJti))).thenReturn(objectMapper.writeValueAsString(session));
    }

    private static User activeUser(Long id, String username, Integer tokenVersion) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(UserRole.USER.code());
        user.setStatus(1);
        user.setTokenVersion(tokenVersion);
        return user;
    }

    private static LoginSession loginSession(String sessionJti, String refreshJti, int tokenVersion) {
        long now = System.currentTimeMillis();
        return new LoginSession(
                3L,
                "user01",
                UserRole.USER.code(),
                sessionJti,
                refreshJti,
                tokenVersion,
                now,
                now
        );
    }
}
