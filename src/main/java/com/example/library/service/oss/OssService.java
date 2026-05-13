package com.example.library.service.oss;

import org.springframework.web.multipart.MultipartFile;

/**
 * 通用对象存储上传（当前实现为阿里云 OSS）
 */
public interface OssService {

    /**
     * @param file    上传文件
     * @param folder  仅允许：cover、avatar、study-room
     * @param userId  当前用户 ID，用于生成规范路径 {folder}/{userId}/{uuid}.ext
     */
    OssUploadResult upload(MultipartFile file, String folder, Long userId);
}
