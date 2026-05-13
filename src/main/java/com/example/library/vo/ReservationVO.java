package com.example.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约记录对外展示对象
 */
@Data
public class ReservationVO {

    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 座位ID
     */
    private Long seatId;

    /**
     * 座位编号
     */
    private String seatNumber;

    /**
     * 自习室ID
     */
    private Long studyRoomId;

    /**
     * 自习室名称
     */
    private String studyRoomName;

    /**
     * 状态
     */
    private String status;

    /**
     * 预约开始时间
     */
    private LocalDateTime startTime;

    /**
     * 预约结束时间
     */
    private LocalDateTime endTime;

    /**
     * 签到时间
     */
    private LocalDateTime checkInTime;

    /**
     * 结束使用时间
     */
    private LocalDateTime finishTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

