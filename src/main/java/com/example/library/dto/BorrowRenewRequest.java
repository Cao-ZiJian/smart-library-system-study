package com.example.library.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 用户续借请求参数
 */
@Data
public class BorrowRenewRequest {

    /**
     * 借阅订单ID
     */
    @NotNull(message = "借阅订单ID不能为空")
    private Long orderId;
}

