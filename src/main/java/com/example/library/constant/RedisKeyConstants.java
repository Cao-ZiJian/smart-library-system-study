package com.example.library.constant;

/**
 * Redis 业务 key 规范（统一前缀，避免与第三方 key 冲突）
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String PREFIX = "lib:v1";

    /** 登录会话：lib:v1:login:token:{accessToken} */
    public static String loginToken(String jti) {
        return PREFIX + ":login:token:" + jti;
    }

    /** 登出黑名单：lib:v1:login:blacklist:{jti} */
    public static String loginBlacklist(String jti) {
        return PREFIX + ":login:blacklist:" + jti;
    }

    /** 预约创建：座位维度的互斥锁 */
    public static String lockReservationSeat(Long seatId) {
        return PREFIX + ":lock:reservation:seat:" + seatId;
    }

    /** 预约创建：用户维度的互斥锁 */
    public static String lockReservationUser(Long userId) {
        return PREFIX + ":lock:reservation:user:" + userId;
    }

    /** 借阅申请：用户+图书维度的互斥锁 */
    public static String lockBorrowApply(Long userId, Long bookId) {
        return PREFIX + ":lock:borrow:apply:user:" + userId + ":book:" + bookId;
    }

    /** 图书详情缓存（JSON） */
    public static String bookDetail(Long bookId) {
        return PREFIX + ":book:detail:" + bookId;
    }
}
