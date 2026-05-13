package com.example.library.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 配置
 *
 * 配置 RedisTemplate 序列化方式，开启缓存支持，并配置简单缓存（热门图书、自习室列表）。
 */
@Configuration
@EnableCaching
public class RedisConfig {

    public static final String CACHE_BOOK_HOT = "bookHot";
    public static final String CACHE_BOOK_DETAIL = "bookDetail";
    public static final String CACHE_BOOK_CATEGORY_LIST = "bookCategoryList";
    public static final String CACHE_STUDY_ROOM_ENABLED = "studyRoomEnabled";
    public static final String CACHE_STUDY_ROOM_ALL = "studyRoomAll";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 统一缓存 key 前缀，与业务侧 String Redis key（lib:v1:...）风格一致
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("lib:v1::")
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CACHE_BOOK_HOT, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CACHE_BOOK_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CACHE_BOOK_CATEGORY_LIST, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(CACHE_STUDY_ROOM_ENABLED, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CACHE_STUDY_ROOM_ALL, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // key 采用 String 序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // value 采用 JSON 序列化
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
