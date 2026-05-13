package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 座位查询请求参数
 */
@Data
public class SeatQueryRequest {

    /**
     * 自习室ID
     */
    @NotNull(message = "自习室ID不能为空")
    private Long studyRoomId;

    /**
     * 状态：1可用 0不可用，可选
     */
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}

