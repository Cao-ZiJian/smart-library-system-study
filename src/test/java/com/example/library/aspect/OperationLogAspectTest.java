package com.example.library.aspect;

import com.example.library.annotation.OperationLog;
import com.example.library.context.UserContext;
import com.example.library.entity.User;
import com.example.library.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLogAspectTest {

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private OperationLogAspect operationLogAspect;

    @BeforeEach
    void setUp() {
        operationLogAspect = new OperationLogAspect(operationLogService, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        UserContext.clear();
    }

    @Test
    void around_masksSensitiveFields_andPersistsLog() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/book/add");
        request.setContentType("application/json");
        request.addParameter("token", "secret-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        User user = new User();
        user.setId(7L);
        user.setUsername("librarian");
        UserContext.set(user);

        DemoRequest body = new DemoRequest();
        body.setTitle("clean-code");
        body.setPassword("123456");
        body.setAccessToken("bearer-token");
        body.setRefreshToken("refresh-token");
        body.setNewPassword("new-password");

        when(joinPoint.getArgs()).thenReturn(new Object[]{body});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toLongString()).thenReturn("demoSignature()");
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = operationLogAspect.around(joinPoint, operationLogAnnotation());

        assertEquals("ok", result);

        ArgumentCaptor<com.example.library.entity.OperationLog> captor =
                ArgumentCaptor.forClass(com.example.library.entity.OperationLog.class);
        verify(operationLogService).save(captor.capture());
        com.example.library.entity.OperationLog log = captor.getValue();
        assertEquals("SUCCESS", log.getResult());
        assertEquals(7L, log.getUserId());
        assertTrue(log.getRequestParams().contains("******"));
        assertTrue(log.getRequestParams().contains("password") || log.getRequestParams().contains("token") || log.getRequestParams().contains("accessToken"));
        assertTrue(log.getRequestParams().contains("clean-code"));
        assertTrue(!log.getRequestParams().contains("bearer-token"));
        assertTrue(!log.getRequestParams().contains("refresh-token"));
        assertTrue(!log.getRequestParams().contains("new-password"));
    }

    @Test
    void around_prefersRequestBodyArgOverPathVariable() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/admin/users/3/password");
        request.setContentType("application/json");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        DemoRequest body = new DemoRequest();
        body.setTitle("body-title");
        body.setNewPassword("new-password");

        Method method = ReflectionUtils.findMethod(DemoController.class, "updatePassword", Long.class, DemoRequest.class);
        MethodSignature methodSignature = mock(MethodSignature.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.toLongString()).thenReturn("updatePassword(Long, DemoRequest)");
        when(joinPoint.getArgs()).thenReturn(new Object[]{3L, body});
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = operationLogAspect.around(joinPoint, method.getAnnotation(OperationLog.class));

        assertEquals("ok", result);
        ArgumentCaptor<com.example.library.entity.OperationLog> captor =
                ArgumentCaptor.forClass(com.example.library.entity.OperationLog.class);
        verify(operationLogService).save(captor.capture());
        String requestParams = captor.getValue().getRequestParams();
        assertNotNull(requestParams);
        assertTrue(requestParams.contains("body-title"));
        assertTrue(requestParams.contains("newPassword"));
        assertTrue(requestParams.contains("******"));
        assertTrue(!requestParams.contains("new-password"));
    }

    @Test
    void around_doesNotBreakBusinessWhenLogSaveFails() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/book/add");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(joinPoint.getArgs()).thenReturn(new Object[]{Map.of("title", "x")});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toLongString()).thenReturn("demoSignature()");
        when(joinPoint.proceed()).thenReturn("ok");
        doThrow(new RuntimeException("db down")).when(operationLogService).save(any(com.example.library.entity.OperationLog.class));

        Object result = operationLogAspect.around(joinPoint, operationLogAnnotation());

        assertEquals("ok", result);
    }

    @Test
    void around_recordsFailureAndTruncatesErrorMessage() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/book/add");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(joinPoint.getArgs()).thenReturn(new Object[]{Map.of("title", "x")});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toLongString()).thenReturn("demoSignature()");

        String longMessage = "x".repeat(800);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException(longMessage));

        try {
            operationLogAspect.around(joinPoint, operationLogAnnotation());
        } catch (IllegalStateException ex) {
            assertEquals(longMessage, ex.getMessage());
        }

        ArgumentCaptor<com.example.library.entity.OperationLog> captor =
                ArgumentCaptor.forClass(com.example.library.entity.OperationLog.class);
        verify(operationLogService).save(captor.capture());
        assertEquals("FAIL", captor.getValue().getResult());
        assertEquals(500, captor.getValue().getErrorMsg().length());
    }

    private OperationLog operationLogAnnotation() {
        Method method = ReflectionUtils.findMethod(DemoController.class, "demo");
        return method.getAnnotation(OperationLog.class);
    }

    private static class DemoRequest {
        private String title;
        private String password;
        private String accessToken;
        private String refreshToken;
        private String newPassword;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    private static class DemoController {
        @OperationLog("demo")
        public void demo() {
        }

        @OperationLog("update-password")
        public void updatePassword(@PathVariable Long id, @RequestBody DemoRequest request) {
        }
    }
}

