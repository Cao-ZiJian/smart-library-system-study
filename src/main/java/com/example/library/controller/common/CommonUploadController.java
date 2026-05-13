package com.example.library.controller.common;

import com.example.library.context.UserContext;
import com.example.library.result.Result;
import com.example.library.service.oss.OssService;
import com.example.library.service.oss.OssUploadResult;
import com.example.library.vo.OssUploadResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 基础图片上传能力（需登录），用于头像、图书封面等辅助资源。
 */
@Api(tags = "支撑-基础图片上传")
@RestController
@RequestMapping("/common")
@Validated
@RequiredArgsConstructor
public class CommonUploadController {

    private final OssService ossService;

    @ApiOperation("基础图片上传：返回可访问 URL，用于头像和图书封面")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<OssUploadResultVO> upload(
            @ApiParam("文件") @RequestParam("file") MultipartFile file,
            @ApiParam(value = "业务目录：cover / avatar / study-room（当前主推 cover 与 avatar）")
            @RequestParam(value = "folder", required = false) String folder) {
        Long currentUserId = UserContext.getRequiredUserId();
        OssUploadResult r = ossService.upload(file, folder, currentUserId);
        OssUploadResultVO vo = new OssUploadResultVO(
                r.getUrl(),
                r.getObjectKey(),
                r.getOriginalFilename(),
                r.getSize(),
                r.getContentType()
        );
        return Result.success(vo);
    }
}
