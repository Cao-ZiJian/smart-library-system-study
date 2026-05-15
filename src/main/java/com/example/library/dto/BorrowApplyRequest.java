package com.example.library.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 用户申请借阅请求参数
 */
@Data
public class BorrowApplyRequest {

    /**
     * 图书ID
     */
    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    /**
     * 备注（可选）
     */
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}

