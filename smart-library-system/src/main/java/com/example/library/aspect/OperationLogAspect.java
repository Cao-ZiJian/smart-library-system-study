package com.example.library.aspect;

import com.example.library.annotation.OperationLog;
import com.example.library.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 * 拦截带 @OperationLog 注解的方法，记录请求信息与执行结果
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(operationLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLogAnnotation) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        com.example.library.entity.OperationLog logEntity = new com.example.library.entity.OperationLog();
        logEntity.setOperation(operationLogAnnotation.value());
        logEntity.setMethod(joinPoint.getSignature().toLongString());
        if (request != null) {
            logEntity.setRequestUri(request.getRequestURI());
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setIp(getClientIp(request));
            try {
                Long userId = (Long) request.getAttribute("currentUserId");
                String username = (String) request.getAttribute("currentUsername");
                logEntity.setUserId(userId);
                logEntity.setUsername(username);
            } catch (Exception ignored) {
            }
            try {
                Map<String, String[]> paramMap = request.getParameterMap();
                if (!paramMap.isEmpty()) {
                    Map<String, String> simpleMap = new HashMap<>();
                    for (Map.Entry<String, String[]> e : paramMap.entrySet()) {
                        String[] v = e.getValue();
                        simpleMap.put(e.getKey(), v != null && v.length > 0 ? v[0] : "");
                    }
                    logEntity.setRequestParams(objectMapper.writeValueAsString(simpleMap));
                }
                if (request.getContentType() != null && request.getContentType().toLowerCase().contains("application/json")) {
                    Object[] args = joinPoint.getArgs();
                    if (args != null && args.length > 0 && args[0] != null) {
                        try {
                            logEntity.setRequestParams(objectMapper.writeValueAsString(args[0]));
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        logEntity.setCreateTime(LocalDateTime.now());

        Object result;
        try {
            result = joinPoint.proceed();
            logEntity.setResult("SUCCESS");
            operationLogService.save(logEntity);
            return result;
        } catch (Throwable e) {
            logEntity.setResult("FAIL");
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            logEntity.setErrorMsg(errorMsg);
            operationLogService.save(logEntity);
            throw e;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
