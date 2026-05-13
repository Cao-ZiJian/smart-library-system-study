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
 * 统一角色校验，替代 Controller 内手写 if。
 *
 * 当前项目保持轻量显式授权：接口允许哪些角色，就在 @RequireRole 中列出哪些角色。
 * 不做 ADMIN 自动继承 LIBRARIAN/USER 的层级推导，避免实习项目引入过重 RBAC 复杂度。
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
