package com.example.library.task;

import com.example.library.entity.Reservation;
import com.example.library.enums.ReservationStatusEnum;
import com.example.library.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 预约定时任务：未签到过期、使用中超时自动结束（均为条件更新，可重复执行）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduleTask {

    private final ReservationService reservationService;

    @Scheduled(cron = "0 * * * * ?")
    public void expireNotCheckedInReservations() {
        LocalDateTime now = LocalDateTime.now();
        boolean updated = reservationService.lambdaUpdate()
                .eq(Reservation::getStatus, ReservationStatusEnum.PENDING_CHECK_IN.getCode())
                .lt(Reservation::getStartTime, now)
                .set(Reservation::getStatus, ReservationStatusEnum.EXPIRED.getCode())
                .update();
        log.info("event=reservation_expire_no_checkin now={} batchUpdated={}", now, updated);
    }

    /**
     * 已到结束时间仍在「使用中」→ 自动结束（幂等）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void autoFinishExpiredInUse() {
        LocalDateTime now = LocalDateTime.now();
        boolean updated = reservationService.lambdaUpdate()
                .eq(Reservation::getStatus, ReservationStatusEnum.IN_USE.getCode())
                .lt(Reservation::getEndTime, now)
                .set(Reservation::getStatus, ReservationStatusEnum.FINISHED.getCode())
                .set(Reservation::getFinishTime, now)
                .update();
        log.info("event=reservation_auto_finish now={} batchUpdated={}", now, updated);
    }
}
