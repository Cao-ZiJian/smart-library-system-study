package com.example.library.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录会话在 Redis 中的存储结构（JWT jti 对应的服务端状态）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginSession {

    private Long userId;
    private String username;
    private String role;
    /**
     * 登录时快照，便于审计；鉴权以数据库为准
     */
    private Integer userStatusSnapshot;
    private long loginTimeMillis;
    /**
     * 与 user.token_version 对齐，后台变更可一次性踢掉旧会话
     */
    private int tokenVersion;
}
