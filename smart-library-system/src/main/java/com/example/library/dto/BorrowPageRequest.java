package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 借阅记录分页查询请求参数
 */
@Data
public class BorrowPageRequest {

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
     * 用户ID（馆员端可用，用户端不需要传）
     */
    private Long userId;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 借阅状态
     */
    private String status;

    /**
     * 用户名（馆员端按用户名筛选）
     */
    private String username;
}

