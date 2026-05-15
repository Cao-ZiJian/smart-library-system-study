package com.example.library.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
    @Max(value = 100, message = "每页数量不能超过 100")
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
    @Size(max = 20, message = "借阅状态长度不能超过20")
    @Pattern(regexp = "APPLYING|APPROVED|LENT|RETURNED|OVERDUE|REJECTED",
            message = "借阅状态不合法")
    private String status;

    /**
     * 用户名（馆员端按用户名筛选）
     */
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;
}

