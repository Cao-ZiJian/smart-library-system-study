package com.example.library.service.oss;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.library.config.OssProperties;
import com.example.library.exception.BusinessException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 阿里云 OSS 上传实现（Controller 仅调用 {@link OssService}，不直接使用本类）
 */
public class AliyunOssServiceImpl implements OssService {

    private static final Set<String> ALLOWED_FOLDERS = Set.of("cover", "avatar", "study-room");

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final OSS ossClient;
    private final OssProperties properties;

    public AliyunOssServiceImpl(OSS ossClient, OssProperties properties) {
        this.ossClient = ossClient;
        this.properties = properties;
    }

    @Override
    public OssUploadResult upload(MultipartFile file, String folder, Long userId) {
        if (userId == null) {
            throw new BusinessException("无法识别当前用户，请先登录后再上传");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        long size = file.getSize();
        if (size <= 0 || size > properties.getMaxFileSizeBytes()) {
            throw new BusinessException("文件大小不合法或超过限制（最大 "
                    + (properties.getMaxFileSizeBytes() / 1024 / 1024) + "MB）");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("仅支持 jpg/png/gif/webp 图片");
        }

        String safeFolder = resolveFolder(folder);
        String ext = resolveExtension(file.getOriginalFilename(), contentType);
        String objectKey = safeFolder + "/" + userId + "/" + UUID.randomUUID() + ext;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType);

        try (InputStream in = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(
                    properties.getBucket(),
                    objectKey,
                    in,
                    metadata
            );
            ossClient.putObject(request);
        } catch (BusinessException e) {
            throw e;
        } catch (OSSException | ClientException e) {
            throw new BusinessException("阿里云 OSS 上传失败（请检查 bucket/endpoint/密钥/权限/网络）：" + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }

        String url = buildPublicUrl(objectKey);
        return new OssUploadResult(url, objectKey, file.getOriginalFilename(), size, contentType);
    }

    private String buildPublicUrl(String objectKey) {
        if (StringUtils.hasText(properties.getDomain())) {
            String base = properties.getDomain().trim().replaceAll("/+$", "");
            return base + "/" + objectKey;
        }
        String ep = properties.getEndpoint().trim();
        if (!ep.startsWith("http://") && !ep.startsWith("https://")) {
            ep = "https://" + ep;
        }
        try {
            URI uri = URI.create(ep);
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                throw new BusinessException("oss.endpoint 格式不正确，无法生成访问 URL，请配置 oss.domain");
            }
            return "https://" + properties.getBucket() + "." + host + "/" + objectKey;
        } catch (Exception e) {
            throw new BusinessException("无法根据 endpoint 生成访问 URL，请配置 oss.domain：" + e.getMessage());
        }
    }

    private static String resolveFolder(String folder) {
        if (!StringUtils.hasText(folder)) {
            throw new BusinessException("folder 不能为空，请传：cover、avatar 或 study-room");
        }
        String f = folder.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_FOLDERS.contains(f)) {
            throw new BusinessException("folder 仅支持：cover、avatar、study-room");
        }
        return f;
    }

    private static String resolveExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
                return normalizeImageExt(ext);
            }
        }
        if (contentType != null) {
            switch (contentType.toLowerCase(Locale.ROOT)) {
                case "image/jpeg":
                    return ".jpg";
                case "image/png":
                    return ".png";
                case "image/gif":
                    return ".gif";
                case "image/webp":
                    return ".webp";
                default:
                    break;
            }
        }
        throw new BusinessException("无法确定文件扩展名，请使用带后缀的文件名上传");
    }

    private static String normalizeImageExt(String ext) {
        if (".jpeg".equals(ext)) {
            return ".jpg";
        }
        return ext;
    }
}
