package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 预约签到请求参数
 */
@Data
public class ReservationSignInRequest {

    /**
     * 预约ID
     */
    @NotNull(message = "预约ID不能为空")
    private Long reservationId;
}

