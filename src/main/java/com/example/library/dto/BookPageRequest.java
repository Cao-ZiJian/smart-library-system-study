package com.example.library.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 图书分页查询请求参数
 */
@Data
public class BookPageRequest {

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
     * 分类ID，可选
     */
    private Long categoryId;

    /**
     * 关键字（按书名模糊搜索），可选
     */
    @Size(max = 100, message = "关键字长度不能超过100")
    private String keyword;

    /**
     * 状态：1 上架 0 下架，可选
     */
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}

