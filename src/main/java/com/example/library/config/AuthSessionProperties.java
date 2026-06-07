package com.example.library.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "auth.session")
public class AuthSessionProperties {

    private int ttlMinutes = 30;

    private int accessTokenTtlMinutes = 30;

    private int refreshTokenTtlDays = 7;

    private int renewThresholdSeconds = 600;
}
