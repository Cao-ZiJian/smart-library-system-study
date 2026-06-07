package com.example.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端用户分页查询参数。
 */
@Data
public class AdminUserPageRequest {

    @Min(value = 1, message = "页码必须从 1 开始")
    private Integer pageNo = 1;

    @Min(value = 1, message = "每页数量必须大于 0")
    @Max(value = 100, message = "每页数量不能超过 100")
    private Integer pageSize = 10;

    @Size(max = 100, message = "关键字长度不能超过 100")
    private String keyword;

    @Size(max = 20, message = "角色长度不能超过 20")
    private String role;

    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}
