package com.example.library.constant;

public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String PREFIX = "lib:v1";

    public static String loginSession(String sessionJti) {
        return PREFIX + ":login:session:" + sessionJti;
    }

    public static String accessBlacklist(String accessJti) {
        return PREFIX + ":login:blacklist:access:" + accessJti;
    }

    public static String refreshBlacklist(String refreshJti) {
        return PREFIX + ":login:blacklist:refresh:" + refreshJti;
    }

    public static String lockRefreshSession(String sessionJti) {
        return PREFIX + ":lock:login:refresh:" + sessionJti;
    }

    public static String loginToken(String jti) {
        return PREFIX + ":login:token:" + jti;
    }

    public static String loginBlacklist(String jti) {
        return PREFIX + ":login:blacklist:" + jti;
    }

    public static String lockReservationSeat(Long seatId) {
        return PREFIX + ":lock:reservation:seat:" + seatId;
    }

    public static String lockReservationUser(Long userId) {
        return PREFIX + ":lock:reservation:user:" + userId;
    }

    public static String lockBorrowApply(Long userId, Long bookId) {
        return PREFIX + ":lock:borrow:apply:user:" + userId + ":book:" + bookId;
    }

    public static String bookDetail(Long bookId) {
        return PREFIX + ":book:detail:" + bookId;
    }
}
