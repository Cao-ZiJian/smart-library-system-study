package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 取消预约请求参数
 */
@Data
public class ReservationCancelRequest {

    /**
     * 预约ID
     */
    @NotNull(message = "预约ID不能为空")
    private Long reservationId;
}

