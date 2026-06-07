package com.example.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端用户角色更新参数。
 */
@Data
public class AdminUserUpdateRoleRequest {

    @NotBlank(message = "角色不能为空")
    @Size(max = 20, message = "角色长度不能超过 20")
    private String role;
}
