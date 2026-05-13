package com.example.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书分类对外展示对象
 */
@Data
public class BookCategoryVO {

    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 状态：1 启用 0 禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

