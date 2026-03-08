package com.example.library.interceptor;

import com.example.library.exception.BusinessException;
import com.example.library.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 *
 * 从请求头中解析 JWT，校验后将当前用户信息放入请求属性，供后续业务使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(401, "未登录或登录凭证缺失");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            // 将当前用户信息放入请求属性，后续可以从 request 中获取
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentUsername", username);
            request.setAttribute("currentUserRole", role);
        } catch (ExpiredJwtException e) {
            log.warn("JWT 过期: {}", e.getMessage());
            throw new BusinessException(401, "登录已过期，请重新登录");
        } catch (Exception e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            throw new BusinessException(401, "登录凭证无效，请重新登录");
        }

        return true;
    }
}










































