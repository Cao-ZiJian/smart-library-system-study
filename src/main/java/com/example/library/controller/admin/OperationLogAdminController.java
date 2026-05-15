package com.example.library.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.annotation.RequireRole;
import com.example.library.dto.OperationLogPageRequest;
import com.example.library.enums.UserRole;
import com.example.library.result.Result;
import com.example.library.service.OperationLogService;
import com.example.library.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 操作日志查询接口。
 */
@Tag(name = "核心-操作日志")
@RestController
@RequestMapping("/admin/operation-log")
@Validated
@RequiredArgsConstructor
@RequireRole({UserRole.ADMIN, UserRole.LIBRARIAN})
public class OperationLogAdminController {

    private final OperationLogService operationLogService;

    @Operation(summary = "操作日志分页：查看 AOP 记录的关键业务操作")
    @GetMapping("/page")
    public Result<Page<OperationLogVO>> page(@Valid OperationLogPageRequest request) {
        Page<OperationLogVO> pageResult = operationLogService.pageLogs(request);
        return Result.success(pageResult);
    }
}
