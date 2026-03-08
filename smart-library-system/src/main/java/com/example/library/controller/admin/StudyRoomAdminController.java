package com.example.library.controller.admin;

import com.example.library.annotation.OperationLog;
import com.example.library.dto.StudyRoomAddRequest;
import com.example.library.dto.StudyRoomUpdateRequest;
import com.example.library.exception.BusinessException;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 自习室管理接口（管理端）
 */
@Api(tags = "自习室管理（管理端）")
@RestController
@RequestMapping("/admin/study-room")
@Validated
@RequiredArgsConstructor
public class StudyRoomAdminController {

    private final StudyRoomService studyRoomService;

    private void checkAdminRole(String role) {
        if (!"ADMIN".equals(role) && !"LIBRARIAN".equals(role)) {
            throw new BusinessException(403, "只有管理员或馆员可以执行该操作");
        }
    }

    @OperationLog("新增自习室")
    @ApiOperation("新增自习室")
    @PostMapping("/add")
    public Result<Void> add(@RequestAttribute("currentUserRole") String currentUserRole,
                            @Valid @RequestBody StudyRoomAddRequest request) {
        checkAdminRole(currentUserRole);
        studyRoomService.addStudyRoom(request);
        return Result.success();
    }

    @OperationLog("修改自习室")
    @ApiOperation("修改自习室")
    @PostMapping("/update")
    public Result<Void> update(@RequestAttribute("currentUserRole") String currentUserRole,
                               @Valid @RequestBody StudyRoomUpdateRequest request) {
        checkAdminRole(currentUserRole);
        studyRoomService.updateStudyRoom(request);
        return Result.success();
    }

    @OperationLog("删除自习室")
    @ApiOperation("删除自习室")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("currentUserRole") String currentUserRole,
                               @PathVariable("id") Long id) {
        checkAdminRole(currentUserRole);
        studyRoomService.deleteStudyRoom(id);
        return Result.success();
    }

    @ApiOperation("自习室列表")
    @GetMapping("/list")
    public Result<List<StudyRoomVO>> list(@RequestAttribute("currentUserRole") String currentUserRole) {
        checkAdminRole(currentUserRole);
        List<StudyRoomVO> list = studyRoomService.listAll();
        return Result.success(list);
    }
}

