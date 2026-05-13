package com.example.library.service.reservation;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.library.entity.Reservation;
import com.example.library.enums.ReservationStatusEnum;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 预约冲突校验
 */
@Component
@RequiredArgsConstructor
public class ReservationConflictChecker {

    private final ReservationMapper reservationMapper;

    public void assertNoSeatConflict(Long seatId, LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<Reservation> query = new QueryWrapper<>();
        query.lambda()
                .eq(Reservation::getSeatId, seatId)
                .in(Reservation::getStatus,
                        ReservationStatusEnum.PENDING_CHECK_IN.getCode(),
                        ReservationStatusEnum.IN_USE.getCode())
                .lt(Reservation::getStartTime, endTime)
                .gt(Reservation::getEndTime, startTime);
        if (reservationMapper.selectCount(query) > 0) {
            throw new BusinessException("该时间段内座位已被预约");
        }
    }

    public void assertNoUserConflict(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<Reservation> query = new QueryWrapper<>();
        query.lambda()
                .eq(Reservation::getUserId, userId)
                .in(Reservation::getStatus,
                        ReservationStatusEnum.PENDING_CHECK_IN.getCode(),
                        ReservationStatusEnum.IN_USE.getCode())
                .lt(Reservation::getStartTime, endTime)
                .gt(Reservation::getEndTime, startTime);
        if (reservationMapper.selectCount(query) > 0) {
            throw new BusinessException("该时间段内您已有其他预约");
        }
    }
}
