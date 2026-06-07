package com.example.library.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginSession {

    private Long userId;
    private String username;
    private String role;
    private String sessionJti;
    private String refreshJti;
    private int tokenVersion;
    private long createTime;
    private long lastActiveTime;

    public LoginSession(Long userId, String username, String role, Integer userStatusSnapshot,
                        long loginTimeMillis, int tokenVersion) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.tokenVersion = tokenVersion;
        this.createTime = loginTimeMillis;
        this.lastActiveTime = loginTimeMillis;
    }
}
