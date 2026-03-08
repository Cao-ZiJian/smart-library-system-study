package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 图书分类新增请求参数
 */
@Data
public class BookCategoryAddRequest {

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50")
    private String name;

    /**
     * 排序值，值越小越靠前
     */
    private Integer sort;

    /**
     * 状态：1 启用 0 禁用
     */
    private Integer status;
}

