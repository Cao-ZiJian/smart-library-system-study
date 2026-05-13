package com.example.library.service.oss;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传结果（可序列化给前端）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssUploadResult {

    /**
     * 可直接访问的 URL（预签名或公网地址）
     */
    private String url;

    /**
     * 存储对象键
     */
    private String objectKey;

    private String originalFilename;

    private long size;

    private String contentType;
}
