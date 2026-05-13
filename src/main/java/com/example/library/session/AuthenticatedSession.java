package com.example.library.session;

import com.example.library.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 访问令牌校验通过后的登录上下文。
 */
@Data
@AllArgsConstructor
public class AuthenticatedSession {

    private User user;
    private LoginSession loginSession;
    private String jti;
}
