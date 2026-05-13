package com.example.library.vo.dashboard;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudyRoomUtilizationVO {

    private Long studyRoomId;
    private String studyRoomName;
    private Integer capacity;
    /**
     * 当前有效预约占用的不同座位数（待签到+使用中且未过期）
     */
    private Integer occupiedSeats;
    /**
     * 占用座位数 / 容量 * 100，保留两位小数
     */
    private BigDecimal utilizationRate;
}
