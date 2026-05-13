package com.example.library.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用上传接口返回
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssUploadResultVO {

    private String url;
    private String objectKey;
    private String originalFilename;
    private Long size;
    private String contentType;
}
