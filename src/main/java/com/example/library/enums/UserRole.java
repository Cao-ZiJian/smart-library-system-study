package com.example.library.enums;

/**
 * 与 user.role 字段取值一致
 */
public enum UserRole {

    USER,
    LIBRARIAN,
    ADMIN;

    public String code() {
        return name();
    }

    public static UserRole fromCode(String role) {
        if (role == null) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
