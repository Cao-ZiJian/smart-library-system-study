package com.example.library.aspect;

import com.example.library.annotation.OperationLog;
import com.example.library.context.UserContext;
import com.example.library.entity.User;
import com.example.library.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_REQUEST_PARAM_LENGTH = 2000;
    private static final int MAX_ERROR_MSG_LENGTH = 500;
    private static final Set<String> SENSITIVE_KEYS = new LinkedHashSet<>(Arrays.asList(
            "password", "oldPassword", "newPassword", "confirmPassword",
            "accessToken", "token", "authorization", "secret", "accessKeySecret"
    ));

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLogAnnotation) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        com.example.library.entity.OperationLog logEntity = new com.example.library.entity.OperationLog();
        logEntity.setOperation(operationLogAnnotation.value());
        logEntity.setMethod(joinPoint.getSignature().toLongString());
        logEntity.setCreateTime(LocalDateTime.now());
        if (request != null) {
            logEntity.setRequestUri(request.getRequestURI());
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setIp(getClientIp(request));
            fillUserInfo(logEntity);
            logEntity.setRequestParams(buildRequestParams(request, joinPoint.getArgs()));
        }

        long startNano = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            logEntity.setResult("SUCCESS");
            persistLogSafely(logEntity, operationLogAnnotation.value(), request, elapsedMillis(startNano), null);
            return result;
        } catch (Throwable e) {
            logEntity.setResult("FAIL");
            logEntity.setErrorMsg(truncate(e.getMessage(), MAX_ERROR_MSG_LENGTH));
            persistLogSafely(logEntity, operationLogAnnotation.value(), request, elapsedMillis(startNano), e);
            throw e;
        }
    }

    private void fillUserInfo(com.example.library.entity.OperationLog logEntity) {
        try {
            User user = UserContext.get();
            if (user != null) {
                logEntity.setUserId(user.getId());
                logEntity.setUsername(user.getUsername());
            }
        } catch (Exception ignored) {
        }
    }

    private String buildRequestParams(HttpServletRequest request, Object[] args) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();

            Map<String, String[]> paramMap = request.getParameterMap();
            if (paramMap != null && !paramMap.isEmpty()) {
                Map<String, Object> query = new LinkedHashMap<>();
                for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                    String key = entry.getKey();
                    String[] values = entry.getValue();
                    if (values == null || values.length == 0) {
                        query.put(key, "");
                    } else if (values.length == 1) {
                        query.put(key, maskIfSensitive(key, values[0]));
                    } else {
                        query.put(key, Arrays.stream(values)
                                .map(value -> maskIfSensitive(key, value))
                                .collect(Collectors.toList()));
                    }
                }
                payload.put("query", query);
            }

            Object bodyArg = resolveBodyArg(args);
            if (bodyArg != null) {
                payload.put("body", sanitizeObject(objectMapper.convertValue(bodyArg, Object.class)));
            }

            if (payload.isEmpty()) {
                return null;
            }
            return truncate(objectMapper.writeValueAsString(payload), MAX_REQUEST_PARAM_LENGTH);
        } catch (Exception e) {
            log.warn("serialize operation log request params failed", e);
            return null;
        }
    }

    private Object resolveBodyArg(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse || arg instanceof BindingResult) {
                continue;
            }
            return arg;
        }
        return null;
    }

    private Object sanitizeObject(Object value) {
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                String key = String.valueOf(entry.getKey());
                sanitized.put(key, sanitizeValue(key, entry.getValue()));
            }
            return sanitized;
        }
        if (value instanceof Iterable) {
            List<Object> sanitized = new ArrayList<>();
            for (Object element : (Iterable<?>) value) {
                sanitized.add(sanitizeObject(element));
            }
            return sanitized;
        }
        return value;
    }

    private Object sanitizeValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveKey(key)) {
            return "******";
        }
        if (value instanceof Map || value instanceof Iterable) {
            return sanitizeObject(value);
        }
        if (value.getClass().isArray()) {
            if (value instanceof Object[]) {
                return Arrays.stream((Object[]) value)
                        .map(this::sanitizeObject)
                        .collect(Collectors.toList());
            }
            return String.valueOf(value);
        }
        return value;
    }

    private String maskIfSensitive(String key, String value) {
        return isSensitiveKey(key) ? "******" : value;
    }

    private boolean isSensitiveKey(String key) {
        return StringUtils.hasText(key) && SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT));
    }

    private void persistLogSafely(
            com.example.library.entity.OperationLog logEntity,
            String operation,
            HttpServletRequest request,
            long costMs,
            Throwable error
    ) {
        try {
            operationLogService.save(logEntity);
        } catch (Exception saveEx) {
            String uri = request == null ? "N/A" : request.getRequestURI();
            log.warn("save operation log failed, operation={}, uri={}, costMs={}", operation, uri, costMs, saveEx);
        } finally {
            String uri = request == null ? "N/A" : request.getRequestURI();
            if (error == null) {
                log.info("operation finished, operation={}, uri={}, result={}, costMs={}",
                        operation, uri, logEntity.getResult(), costMs);
            } else {
                log.warn("operation finished, operation={}, uri={}, result={}, costMs={}, error={}",
                        operation, uri, logEntity.getResult(), costMs, truncate(error.getMessage(), MAX_ERROR_MSG_LENGTH));
            }
        }
    }

    private long elapsedMillis(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
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
