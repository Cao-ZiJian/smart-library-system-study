package com.example.library.session;

import com.example.library.entity.User;

/**
 * 统一登录会话管理边界：JWT、Redis TTL、黑名单和 tokenVersion 校验均在此收口。
 */
public interface SessionManager {

    String createSession(User user);

    LoginSession getSessionByJti(String jti);

    void removeSession(String accessToken);

    void removeSessionByJti(String jti);

    void renewIfNeededByJti(String jti);

    boolean isBlacklistedJti(String jti);

    AuthenticatedSession authenticate(String accessToken);
}
