package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

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
    private Integer pageSize;

    /**
     * 分类ID，可选
     */
    private Long categoryId;

    /**
     * 关键字（按书名模糊搜索），可选
     */
    private String keyword;

    /**
     * 状态：1 上架 0 下架，可选
     */
    private Integer status;
}

