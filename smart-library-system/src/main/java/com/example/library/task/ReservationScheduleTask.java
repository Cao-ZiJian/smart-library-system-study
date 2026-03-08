package com.example.library.task;

import com.example.library.entity.Reservation;
import com.example.library.enums.ReservationStatusEnum;
import com.example.library.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约相关定时任务
 * 自动将超时未签到的预约标记为 EXPIRED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduleTask {

    private final ReservationMapper reservationMapper;

    /**
     * 自动取消超时未签到预约：status = PENDING_CHECK_IN 且 当前时间 > start_time 则更新为 EXPIRED
     * 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void expireNotCheckedInReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> list = reservationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, ReservationStatusEnum.PENDING_CHECK_IN.getCode())
                        .lt(Reservation::getStartTime, now)
        );
        if (list.isEmpty()) {
            return;
        }
        for (Reservation r : list) {
            r.setStatus(ReservationStatusEnum.EXPIRED.getCode());
            reservationMapper.updateById(r);
        }
        log.info("预约定时任务：已将 {} 条超时未签到预约标记为 EXPIRED", list.size());
    }
}
