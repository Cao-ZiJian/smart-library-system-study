package com.example.library.context;

import com.example.library.entity.User;
import com.example.library.exception.BusinessException;

/**
 * 当前登录用户上下文（基于 ThreadLocal）。
 * 生命周期：在请求进入鉴权后写入，在请求完成后清理。
 * 线程隔离：每个请求线程持有独立用户数据，互不影响。
 */
public final class UserContext {

    private static final ThreadLocal<User> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(User user) {
        USER_HOLDER.set(user);
    }

    public static User get() {
        return USER_HOLDER.get();
    }

    public static User getRequiredUser() {
        User user = get();
        if (user == null) {
            throw new BusinessException(BusinessException.UNAUTHORIZED, "未登录或登录已失效");
        }
        return user;
    }

    public static Long getUserId() {
        User user = get();
        return user == null ? null : user.getId();
    }

    public static Long getRequiredUserId() {
        return getRequiredUser().getId();
    }

    public static void remove() {
        clear();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
