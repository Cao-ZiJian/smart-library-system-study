package com.example.library.session;

import com.example.library.entity.User;
import com.example.library.vo.AuthTokenVO;

public interface SessionManager {

    AuthTokenVO createSession(User user);

    AuthTokenVO refresh(String refreshToken);

    LoginSession getSessionByJti(String sessionJti);

    void removeSession(String accessToken);

    void removeSessionByJti(String sessionJti);

    void renewIfNeededByJti(String sessionJti);

    boolean isBlacklistedJti(String accessJti);

    AuthenticatedSession authenticate(String accessToken);
}
