package com.example.library.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.annotation.OperationLog;
import com.example.library.annotation.RequireRole;
import com.example.library.dto.AdminResetPasswordRequest;
import com.example.library.dto.AdminUserPageRequest;
import com.example.library.dto.AdminUserUpdateRoleRequest;
import com.example.library.dto.AdminUserUpdateStatusRequest;
import com.example.library.enums.UserRole;
import com.example.library.result.Result;
import com.example.library.service.UserAdminService;
import com.example.library.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户管理接口。
 */
@Tag(name = "管理端-用户管理")
@RestController
@RequestMapping("/admin/users")
@Validated
@RequiredArgsConstructor
@RequireRole({UserRole.ADMIN})
public class UserAdminController {

    private final UserAdminService userAdminService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<Page<AdminUserVO>> page(@Valid AdminUserPageRequest request) {
        return Result.success(userAdminService.pageUsers(request));
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    public Result<AdminUserVO> detail(@PathVariable("id") Long id) {
        return Result.success(userAdminService.getUserDetail(id));
    }

    @OperationLog("更新用户状态")
    @Operation(summary = "启用或禁用用户")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                     @Valid @RequestBody AdminUserUpdateStatusRequest request) {
        userAdminService.updateStatus(id, request);
        return Result.success();
    }

    @OperationLog("更新用户角色")
    @Operation(summary = "修改用户角色")
    @PatchMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable("id") Long id,
                                   @Valid @RequestBody AdminUserUpdateRoleRequest request) {
        userAdminService.updateRole(id, request);
        return Result.success();
    }

    @OperationLog("踢用户下线")
    @Operation(summary = "踢用户下线")
    @PostMapping("/{id}/kick")
    public Result<Void> kick(@PathVariable("id") Long id) {
        userAdminService.kickOut(id);
        return Result.success();
    }

    @OperationLog("重置用户密码")
    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable("id") Long id,
                                      @Valid @RequestBody AdminResetPasswordRequest request) {
        userAdminService.resetPassword(id, request);
        return Result.success();
    }
}
