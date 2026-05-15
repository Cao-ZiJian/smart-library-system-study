package com.example.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 创建预约请求参数
 */
@Data
public class ReservationCreateRequest {

    /**
     * 座位ID
     */
    @NotNull(message = "座位ID不能为空")
    private Long seatId;

    /**
     * 预约开始时间
     */
    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须是将来时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 预约结束时间
     */
    @NotNull(message = "结束时间不能为空")
    @Future(message = "结束时间必须是将来时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 备注
     */
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}

