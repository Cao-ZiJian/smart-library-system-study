package com.example.library.annotation;

import com.example.library.enums.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明接口所需角色；标注在类上对该 Controller 下全部映射生效，方法上标注则覆盖类级别。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    UserRole[] value();
}
