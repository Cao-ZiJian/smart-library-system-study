package com.example.library.session;

import com.example.library.config.AuthSessionProperties;
import com.example.library.constant.RedisKeyConstants;
import com.example.library.entity.User;
import com.example.library.enums.UserRole;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.UserMapper;
import com.example.library.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthSessionProperties authSessionProperties;
    private RedisSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        authSessionProperties = new AuthSessionProperties();
        authSessionProperties.setTtlMinutes(120);
        authSessionProperties.setRenewThresholdSeconds(600);

        sessionManager = new RedisSessionManager(
                stringRedisTemplate,
                objectMapper,
                authSessionProperties,
                userMapper
        );
        ReflectionTestUtils.setField(sessionManager, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(sessionManager, "blacklistTtlSeconds", 7200L);
    }

    @Test
    void createSession_usesResolvedTtlAndStoresSessionJson() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        User user = activeUser(3L, "user01", 0);
        user.setTokenVersion(null);

        String token = sessionManager.createSession(user);

        assertNotNull(token);
        org.mockito.ArgumentCaptor<String> keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<String> valueCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(7200L), eq(TimeUnit.SECONDS));
        assertTrue(keyCaptor.getValue().startsWith("lib:v1:login:token:"));

        LoginSession session = objectMapper.readValue(valueCaptor.getValue(), LoginSession.class);
        assertEquals(3L, session.getUserId());
        assertEquals("user01", session.getUsername());
        assertEquals(0, session.getTokenVersion());
    }

    @Test
    void removeSessionByJti_usesMaxOfSessionTtlAndBlacklistTtl() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(sessionManager, "blacklistTtlSeconds", 9000L);

        sessionManager.removeSessionByJti("jti-1");

        verify(stringRedisTemplate).delete(RedisKeyConstants.loginToken("jti-1"));
        verify(valueOperations).set(RedisKeyConstants.loginBlacklist("jti-1"), "1", 9000L, TimeUnit.SECONDS);
    }

    @Test
    void renewIfNeededByJti_refreshesOnlyWhenBelowThreshold() {
        when(stringRedisTemplate.getExpire(RedisKeyConstants.loginToken("jti-2"), TimeUnit.SECONDS)).thenReturn(300L);

        sessionManager.renewIfNeededByJti("jti-2");

        verify(stringRedisTemplate).expire(RedisKeyConstants.loginToken("jti-2"), 7200L, TimeUnit.SECONDS);
    }

    @Test
    void renewIfNeededByJti_skipsRefreshWhenTtlIsEnough() {
        when(stringRedisTemplate.getExpire(RedisKeyConstants.loginToken("jti-3"), TimeUnit.SECONDS)).thenReturn(800L);

        sessionManager.renewIfNeededByJti("jti-3");

        verify(stringRedisTemplate, never()).expire(eq(RedisKeyConstants.loginToken("jti-3")), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void authenticate_failsWhenJtiIsBlacklisted() {
        String token = JwtUtil.createToken(3L, "jti-black", 0, JWT_SECRET);
        when(stringRedisTemplate.hasKey(RedisKeyConstants.loginBlacklist("jti-black"))).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionManager.authenticate(token));

        assertEquals(BusinessException.UNAUTHORIZED, ex.getCode());
    }

    @Test
    void authenticate_failsWhenRedisSessionMissing() {
        String token = JwtUtil.createToken(3L, "jti-missing", 0, JWT_SECRET);
        when(stringRedisTemplate.hasKey(RedisKeyConstants.loginBlacklist("jti-missing"))).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyConstants.loginToken("jti-missing"))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionManager.authenticate(token));

        assertEquals(BusinessException.UNAUTHORIZED, ex.getCode());
    }

    @Test
    void authenticate_failsWhenTokenVersionChanged() throws Exception {
        String token = JwtUtil.createToken(3L, "jti-version", 0, JWT_SECRET);
        LoginSession loginSession = loginSession(3L, "user01", 0);
        User dbUser = activeUser(3L, "user01", 1);

        mockExistingSession("jti-version", loginSession);
        when(userMapper.selectById(3L)).thenReturn(dbUser);

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionManager.authenticate(token));

        assertEquals(BusinessException.UNAUTHORIZED, ex.getCode());
        verify(stringRedisTemplate).delete(RedisKeyConstants.loginToken("jti-version"));
        verify(valueOperations).set(RedisKeyConstants.loginBlacklist("jti-version"), "1", 7200L, TimeUnit.SECONDS);
    }

    @Test
    void authenticate_failsWhenUserDisabled() throws Exception {
        String token = JwtUtil.createToken(3L, "jti-disabled", 0, JWT_SECRET);
        LoginSession loginSession = loginSession(3L, "user01", 0);
        User dbUser = activeUser(3L, "user01", 0);
        dbUser.setStatus(0);

        mockExistingSession("jti-disabled", loginSession);
        when(userMapper.selectById(3L)).thenReturn(dbUser);

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionManager.authenticate(token));

        assertEquals(BusinessException.FORBIDDEN, ex.getCode());
        verify(stringRedisTemplate).delete(RedisKeyConstants.loginToken("jti-disabled"));
        verify(valueOperations).set(RedisKeyConstants.loginBlacklist("jti-disabled"), "1", 7200L, TimeUnit.SECONDS);
    }

    @Test
    void authenticate_successRenewsSessionWhenTtlBelowThreshold() throws Exception {
        String token = JwtUtil.createToken(3L, "jti-renew", 0, JWT_SECRET);
        LoginSession loginSession = loginSession(3L, "user01", 0);
        User dbUser = activeUser(3L, "user01", 0);

        mockExistingSession("jti-renew", loginSession);
        when(userMapper.selectById(3L)).thenReturn(dbUser);
        when(stringRedisTemplate.getExpire(RedisKeyConstants.loginToken("jti-renew"), TimeUnit.SECONDS)).thenReturn(300L);

        AuthenticatedSession authenticated = sessionManager.authenticate(token);

        assertEquals(3L, authenticated.getUser().getId());
        assertEquals("jti-renew", authenticated.getJti());
        verify(stringRedisTemplate).expire(RedisKeyConstants.loginToken("jti-renew"), 7200L, TimeUnit.SECONDS);
    }

    @Test
    void removeSession_blacklistsJtiSoOldTokenFailsAuthentication() {
        String token = JwtUtil.createToken(3L, "jti-logout", 0, JWT_SECRET);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        sessionManager.removeSession(token);

        verify(stringRedisTemplate).delete(RedisKeyConstants.loginToken("jti-logout"));
        verify(valueOperations).set(RedisKeyConstants.loginBlacklist("jti-logout"), "1", 7200L, TimeUnit.SECONDS);

        when(stringRedisTemplate.hasKey(RedisKeyConstants.loginBlacklist("jti-logout"))).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () -> sessionManager.authenticate(token));
        assertEquals(BusinessException.UNAUTHORIZED, ex.getCode());
    }

    @Test
    void strategyMethods_areStableAndReadable() {
        assertEquals(7200L, sessionManager.resolveSessionTtlSeconds());
        assertEquals(7200L, sessionManager.resolveBlacklistTtlSeconds());
        assertTrue(sessionManager.shouldRenew(599L));
        assertFalse(sessionManager.shouldRenew(600L));

        User user = new User();
        user.setTokenVersion(4);
        assertEquals(4, sessionManager.resolveTokenVersion(user));
        assertEquals(0, sessionManager.resolveTokenVersion(null));
    }

    private void mockExistingSession(String jti, LoginSession session) throws Exception {
        when(stringRedisTemplate.hasKey(RedisKeyConstants.loginBlacklist(jti))).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyConstants.loginToken(jti))).thenReturn(objectMapper.writeValueAsString(session));
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

    private static LoginSession loginSession(Long userId, String username, int tokenVersion) {
        return new LoginSession(
                userId,
                username,
                UserRole.USER.code(),
                1,
                System.currentTimeMillis(),
                tokenVersion
        );
    }
}
