package com.example.library.dto;

import lombok.Data;

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
    private Integer status;
}

