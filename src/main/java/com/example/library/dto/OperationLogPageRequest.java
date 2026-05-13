package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 操作日志分页查询请求参数
 */
@Data
public class OperationLogPageRequest {

    /**
     * 当前页码，从 1 开始
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须从 1 开始")
    private Integer pageNum;

    /**
     * 每页数量
     */
    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于 0")
    @Max(value = 100, message = "每页数量不能超过 100")
    private Integer pageSize;

    /**
     * 操作人用户名，可选模糊
     */
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    /**
     * 操作名称，可选模糊
     */
    @Size(max = 100, message = "操作名称长度不能超过100")
    private String operation;

    /**
     * 结果：SUCCESS/FAIL，可选
     */
    @Size(max = 20, message = "操作结果长度不能超过20")
    @Pattern(regexp = "SUCCESS|FAIL", message = "操作结果只能为SUCCESS或FAIL")
    private String result;
}
