package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 座位修改请求参数
 */
@Data
public class SeatUpdateRequest {

    /**
     * 座位ID
     */
    @NotNull(message = "座位ID不能为空")
    private Long id;

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

