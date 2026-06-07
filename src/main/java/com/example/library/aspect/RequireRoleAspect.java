package com.example.library.aspect;

import com.example.library.annotation.RequireRole;
import com.example.library.context.UserContext;
import com.example.library.entity.User;
import com.example.library.enums.UserRole;
import com.example.library.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 统一角色校验切面。
 *
 * 采用显式角色授权策略：接口允许的角色由 @RequireRole 直接声明。
 * 系统不进行角色层级推导，避免隐式权限扩散。
 */
@Aspect
@Component
public class RequireRoleAspect {

    @Around("execution(* com.example.library.controller..*.*(..))")
    public Object aroundController(ProceedingJoinPoint pjp) throws Throwable {
        RequireRole ann = resolveRequireRole(pjp);
        if (ann == null) {
            return pjp.proceed();
        }

        UserRole currentRole = resolveCurrentUserRole();
        if (currentRole == null) {
            throw new BusinessException(BusinessException.FORBIDDEN, "无权访问");
        }
        if (!hasRequiredRole(currentRole, ann)) {
            throw new BusinessException(BusinessException.FORBIDDEN, "权限不足");
        }
        return pjp.proceed();
    }

    private RequireRole resolveRequireRole(ProceedingJoinPoint pjp) {
        RequireRole ann = AnnotationUtils.findAnnotation(
                ((MethodSignature) pjp.getSignature()).getMethod(), RequireRole.class);
        if (ann != null) {
            return ann;
        }
        return AnnotationUtils.findAnnotation(pjp.getTarget().getClass(), RequireRole.class);
    }

    private UserRole resolveCurrentUserRole() {
        User user = UserContext.get();
        String roleCode = user == null ? null : user.getRole();
        return UserRole.fromCode(roleCode);
    }

    private boolean hasRequiredRole(UserRole currentRole, RequireRole ann) {
        Set<UserRole> allowed = new HashSet<>(Arrays.asList(ann.value()));
        return allowed.contains(currentRole);
    }
}
