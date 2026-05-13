package com.example.library.enums;

/**
 * 预约状态枚举，与 reservation.status 字段保持一致
 */
public enum ReservationStatusEnum {

    PENDING_CHECK_IN("PENDING_CHECK_IN"),
    CANCELED("CANCELED"),
    IN_USE("IN_USE"),
    FINISHED("FINISHED"),
    EXPIRED("EXPIRED");

    private final String code;

    ReservationStatusEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

