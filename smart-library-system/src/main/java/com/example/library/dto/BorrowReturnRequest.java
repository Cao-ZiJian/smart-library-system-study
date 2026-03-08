package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 办理归还请求参数
 */
@Data
public class BorrowReturnRequest {

    /**
     * 借阅订单ID
     */
    @NotNull(message = "借阅订单ID不能为空")
    private Long orderId;

    /**
     * 备注（可选）
     */
    private String remark;
}

