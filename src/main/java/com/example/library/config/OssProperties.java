package com.example.library.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 阿里云 OSS 配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /**
     * OSS Endpoint，如 https://oss-cn-hangzhou.aliyuncs.com
     */
    @NotBlank
    private String endpoint;

    @NotBlank
    private String accessKeyId;

    @NotBlank
    private String accessKeySecret;

    @NotBlank
    private String bucket;

    /**
     * 对外访问基址（不要末尾 /）。不填则按「bucket + endpoint 主机名」拼接虚拟主机风格 URL
     */
    private String domain = "";

    /**
     * 单文件最大字节数（默认 5MB）
     */
    private long maxFileSizeBytes = 5 * 1024 * 1024;
}
