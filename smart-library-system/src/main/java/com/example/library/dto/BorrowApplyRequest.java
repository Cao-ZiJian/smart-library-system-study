package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

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
    private String remark;
}

