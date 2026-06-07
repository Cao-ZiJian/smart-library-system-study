package com.example.library.session;

import com.example.library.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticatedSession {

    private User user;
    private LoginSession loginSession;
    private String sessionJti;
    private String accessJti;

    public String getJti() {
        return sessionJti;
    }
}
