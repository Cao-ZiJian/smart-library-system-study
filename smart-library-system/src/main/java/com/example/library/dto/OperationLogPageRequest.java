package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

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
    private Integer pageSize;

    /**
     * 操作人用户名，可选模糊
     */
    private String username;

    /**
     * 操作名称，可选模糊
     */
    private String operation;

    /**
     * 结果：SUCCESS/FAIL，可选
     */
    private String result;
}
