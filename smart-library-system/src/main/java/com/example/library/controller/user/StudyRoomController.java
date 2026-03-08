package com.example.library.controller.user;

import com.example.library.result.Result;
import com.example.library.service.StudyRoomService;
import com.example.library.vo.StudyRoomVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自习室接口（用户端）
 */
@Api(tags = "自习室模块（用户端）")
@RestController
@RequestMapping("/study-room")
@Validated
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

    @ApiOperation("自习室列表（仅启用）")
    @GetMapping("/list")
    public Result<List<StudyRoomVO>> list() {
        List<StudyRoomVO> list = studyRoomService.listEnabled();
        return Result.success(list);
    }

    @ApiOperation("自习室详情（仅启用）")
    @GetMapping("/{id}")
    public Result<StudyRoomVO> detail(@PathVariable("id") Long id) {
        StudyRoomVO vo = studyRoomService.getEnabledDetail(id);
        return Result.success(vo);
    }
}

