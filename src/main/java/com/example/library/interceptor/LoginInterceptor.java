package com.example.library.interceptor;

import com.example.library.context.UserContext;
import com.example.library.exception.BusinessException;
import com.example.library.session.AuthenticatedSession;
import com.example.library.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器：只负责提取 Bearer token、调用会话认证边界、写入并清理用户上下文。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionManager sessionManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String accessToken = extractBearerToken(request);
            AuthenticatedSession session = sessionManager.authenticate(accessToken);
            UserContext.set(session.getUser());
            return true;
        } catch (RuntimeException e) {
            UserContext.clear();
            throw e;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(BusinessException.UNAUTHORIZED, "未登录或登录凭证缺失");
        }
        String accessToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(BusinessException.UNAUTHORIZED, "未登录或登录凭证缺失");
        }
        return accessToken;
    }
}
