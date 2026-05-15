package com.example.library.controller.user;

import com.example.library.result.Result;
import com.example.library.service.StudyRoomService;
import com.example.library.vo.StudyRoomVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自习室查询接口，作为座位预约入口。
 */
@Tag(name = "支撑-自习室查询")
@RestController
@RequestMapping("/study-room")
@Validated
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

    @Operation(summary = "可预约自习室列表：仅返回启用资源")
    @GetMapping("/list")
    public Result<List<StudyRoomVO>> list() {
        List<StudyRoomVO> list = studyRoomService.listEnabled();
        return Result.success(list);
    }

    @Operation(summary = "可预约自习室详情：用于选择座位前展示")
    @GetMapping("/{id}")
    public Result<StudyRoomVO> detail(@PathVariable("id") Long id) {
        StudyRoomVO vo = studyRoomService.getEnabledDetail(id);
        return Result.success(vo);
    }
}

