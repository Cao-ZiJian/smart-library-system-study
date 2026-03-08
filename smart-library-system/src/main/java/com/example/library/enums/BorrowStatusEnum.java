package com.example.library.enums;

/**
 * 借阅状态枚举，与 borrow_order.status 字段保持一致
 */
public enum BorrowStatusEnum {

    APPLYING("APPLYING"),
    APPROVED("APPROVED"),
    LENT("LENT"),
    RETURNED("RETURNED"),
    OVERDUE("OVERDUE"),
    REJECTED("REJECTED");

    private final String code;

    BorrowStatusEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

