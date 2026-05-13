package com.example.library.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登录会话（Redis）相关配置
 */
@Data
@ConfigurationProperties(prefix = "auth.session")
public class AuthSessionProperties {

    /**
     * 会话在 Redis 中的存活时间（分钟）
     */
    private int ttlMinutes = 120;

    /**
     * 剩余 TTL 小于该值（秒）时触发滑动续期
     */
    private int renewThresholdSeconds = 600;
}
