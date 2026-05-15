package com.example.library.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 预约结束使用请求参数
 */
@Data
public class ReservationFinishRequest {

    /**
     * 预约ID
     */
    @NotNull(message = "预约ID不能为空")
    private Long reservationId;
}

