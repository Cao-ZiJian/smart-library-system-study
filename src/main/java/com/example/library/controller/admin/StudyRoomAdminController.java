package com.example.library.controller.admin;

import com.example.library.annotation.OperationLog;
import com.example.library.annotation.RequireRole;
import com.example.library.dto.StudyRoomAddRequest;
import com.example.library.dto.StudyRoomUpdateRequest;
import com.example.library.enums.UserRole;
import com.example.library.result.Result;
import com.example.library.service.StudyRoomService;
import com.example.library.vo.StudyRoomVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 自习室基础数据维护接口，作为预约资源支撑。
 */
@Api(tags = "支撑-自习室基础数据")
@RestController
@RequestMapping("/admin/study-room")
@Validated
@RequiredArgsConstructor
@RequireRole({UserRole.ADMIN, UserRole.LIBRARIAN})
public class StudyRoomAdminController {

    private final StudyRoomService studyRoomService;

    @OperationLog("新增自习室")
    @ApiOperation("录入自习室资源：用于座位预约基础数据")
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody StudyRoomAddRequest request) {
        studyRoomService.addStudyRoom(request);
        return Result.success();
    }

    @OperationLog("修改自习室")
    @ApiOperation("维护自习室资源：位置、容量、开放时间和状态")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody StudyRoomUpdateRequest request) {
        studyRoomService.updateStudyRoom(request);
        return Result.success();
    }

    @OperationLog("删除自习室")
    @ApiOperation("清理未绑定座位的自习室资源")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        studyRoomService.deleteStudyRoom(id);
        return Result.success();
    }

    @ApiOperation("自习室资源列表：用于后台核对预约资源")
    @GetMapping("/list")
    public Result<List<StudyRoomVO>> list() {
        List<StudyRoomVO> list = studyRoomService.listAll();
        return Result.success(list);
    }
}
