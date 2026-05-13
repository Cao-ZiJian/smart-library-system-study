package com.example.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 座位对外展示对象
 */
@Data
public class SeatVO {

    private Long id;

    /**
     * 自习室ID
     */
    private Long studyRoomId;

    /**
     * 座位编号
     */
    private String seatNumber;

    /**
     * 状态：1可用 0不可用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

