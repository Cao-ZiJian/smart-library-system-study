package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}

