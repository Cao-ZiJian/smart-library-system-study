package com.example.library.service.reservation;

import com.example.library.entity.Reservation;
import com.example.library.enums.ReservationStatusEnum;
import com.example.library.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 预约状态与基础规则校验
 */
@Component
public class ReservationStateGuard {

    public void requireCancelable(Reservation reservation, Long userId) {
        requireOwner(reservation, userId, "只能取消自己的预约");
        if (!ReservationStatusEnum.PENDING_CHECK_IN.getCode().equals(reservation.getStatus())) {
            throw new BusinessException("当前状态不可取消");
        }
    }

    public void requireSignInable(Reservation reservation, Long userId) {
        requireOwner(reservation, userId, "只能签到自己的预约");
        if (!ReservationStatusEnum.PENDING_CHECK_IN.getCode().equals(reservation.getStatus())) {
            throw new BusinessException("当前状态不可签到");
        }
    }

    public void requireFinishable(Reservation reservation, Long userId) {
        requireOwner(reservation, userId, "只能结束自己的预约");
        if (!ReservationStatusEnum.IN_USE.getCode().equals(reservation.getStatus())) {
            throw new BusinessException("当前状态不可结束使用");
        }
    }

    public void validateCreateTimeRange(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime now) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessException("开始时间必须早于结束时间");
        }
        if (!startTime.isAfter(now)) {
            throw new BusinessException("不能预约过去时间");
        }
    }

    private void requireOwner(Reservation reservation, Long userId, String message) {
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!userId.equals(reservation.getUserId())) {
            throw new BusinessException(message);
        }
    }
}
