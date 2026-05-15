package com.example.library.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 座位新增请求参数
 */
@Data
public class SeatAddRequest {

    /**
     * 自习室ID
     */
    @NotNull(message = "自习室ID不能为空")
    private Long studyRoomId;

    /**
     * 座位编号
     */
    @NotBlank(message = "座位编号不能为空")
    @Size(max = 50, message = "座位编号长度不能超过50")
    private String seatNumber;

    /**
     * 状态：1可用 0不可用
     */
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}

