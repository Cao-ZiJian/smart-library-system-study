package com.example.library.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.example.library.service.oss.AliyunOssServiceImpl;
import com.example.library.service.oss.OssService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 客户端与 {@link OssService} 装配
 */
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssConfiguration {

    @Bean(destroyMethod = "shutdown")
    public OSS aliyunOssClient(OssProperties properties) {
        return new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );
    }

    @Bean
    public OssService aliyunOssService(OSS aliyunOssClient, OssProperties properties) {
        return new AliyunOssServiceImpl(aliyunOssClient, properties);
    }
}
