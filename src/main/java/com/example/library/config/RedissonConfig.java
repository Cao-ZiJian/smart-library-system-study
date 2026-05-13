package com.example.library.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        String address = String.format("redis://%s:%d", redisProperties.getHost(), redisProperties.getPort());
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase());

        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            config.useSingleServer().setPassword(redisProperties.getPassword());
        }

        return Redisson.create(config);
    }
}
