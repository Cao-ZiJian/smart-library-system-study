package com.example.library.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.dto.OperationLogPageRequest;
import com.example.library.exception.BusinessException;
import com.example.library.result.Result;
import com.example.library.service.OperationLogService;
import com.example.library.vo.OperationLogVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 操作日志管理接口（管理端）
 */
@Api(tags = "操作日志（管理端）")
@RestController
@RequestMapping("/admin/operation-log")
@Validated
@RequiredArgsConstructor
public class OperationLogAdminController {

    private final OperationLogService operationLogService;

    private void checkAdminRole(String role) {
        if (!"ADMIN".equals(role) && !"LIBRARIAN".equals(role)) {
            throw new BusinessException(403, "只有管理员或馆员可以执行该操作");
        }
    }

    @ApiOperation("操作日志分页查询")
    @GetMapping("/page")
    public Result<Page<OperationLogVO>> page(@RequestAttribute("currentUserRole") String currentUserRole,
                                            @Valid OperationLogPageRequest request) {
        checkAdminRole(currentUserRole);
        Page<OperationLogVO> pageResult = operationLogService.pageLogs(request);
        return Result.success(pageResult);
    }
}
