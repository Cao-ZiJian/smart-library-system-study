package com.example.library.service.oss;

import com.aliyun.oss.OSS;
import com.example.library.config.OssProperties;
import com.example.library.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 不连真实 OSS：校验类型/大小/folder 及成功路径 URL 形态
 */
@ExtendWith(MockitoExtension.class)
class AliyunOssServiceImplTest {

    @Mock
    private OSS ossClient;

    private OssProperties properties;
    private AliyunOssServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new OssProperties();
        properties.setEndpoint("https://oss-cn-hangzhou.aliyuncs.com");
        properties.setAccessKeyId("unit-test-ak");
        properties.setAccessKeySecret("unit-test-sk");
        properties.setBucket("demo-bucket");
        properties.setMaxFileSizeBytes(1024 * 1024);
        service = new AliyunOssServiceImpl(ossClient, properties);
    }

    @Test
    void rejectsNullUserId() {
        MockMultipartFile file = jpgFile("x".getBytes());
        assertThrows(BusinessException.class, () -> service.upload(file, "cover", null));
    }

    @Test
    void rejectsInvalidFolder() {
        MockMultipartFile file = jpgFile("x".getBytes());
        assertThrows(BusinessException.class, () -> service.upload(file, "common", 1L));
    }

    @Test
    void rejectsBlankFolder() {
        MockMultipartFile file = jpgFile("x".getBytes());
        assertThrows(BusinessException.class, () -> service.upload(file, "", 1L));
    }

    @Test
    void rejectsWrongContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "application/pdf", new byte[]{1});
        assertThrows(BusinessException.class, () -> service.upload(file, "cover", 1L));
    }

    @Test
    void rejectsOversize() {
        byte[] big = new byte[(int) properties.getMaxFileSizeBytes() + 1];
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", big);
        assertThrows(BusinessException.class, () -> service.upload(file, "cover", 1L));
    }

    @Test
    void success_invokesPutObject_andReturnsVirtualHostUrl() throws Exception {
        when(ossClient.putObject(ArgumentMatchers.any())).thenReturn(null);
        MockMultipartFile file = jpgFile(new byte[]{1, 2, 3});
        OssUploadResult r = service.upload(file, "avatar", 99L);
        verify(ossClient).putObject(ArgumentMatchers.any());
        assertTrue(r.getObjectKey().startsWith("avatar/99/"));
        assertTrue(r.getUrl().startsWith("https://demo-bucket.oss-cn-hangzhou.aliyuncs.com/avatar/99/"));
    }

    @Test
    void success_withCustomDomain() throws Exception {
        properties.setDomain("https://cdn.example.com/");
        service = new AliyunOssServiceImpl(ossClient, properties);
        when(ossClient.putObject(ArgumentMatchers.any())).thenReturn(null);
        MockMultipartFile file = jpgFile(new byte[]{1});
        OssUploadResult r = service.upload(file, "study-room", 2L);
        assertTrue(r.getUrl().startsWith("https://cdn.example.com/study-room/2/"));
    }

    private static MockMultipartFile jpgFile(byte[] body) {
        return new MockMultipartFile("file", "test.jpg", "image/jpeg", body);
    }
}
