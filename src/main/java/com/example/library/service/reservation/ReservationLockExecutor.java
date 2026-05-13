package com.example.library.service.reservation;

import com.example.library.constant.RedisKeyConstants;
import com.example.library.service.lock.LockExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * 预约并发控制，统一封装座位维度与用户维度的组合锁。
 */
@Component
@RequiredArgsConstructor
public class ReservationLockExecutor {

    private static final String BUSY_MESSAGE = "预约繁忙，请稍后重试";

    private final LockExecutor lockExecutor;

    public <T> T executeForCreate(Long userId, Long seatId, Callable<T> action) {
        return lockExecutor.executeWithMultiLock(
                BUSY_MESSAGE,
                action,
                RedisKeyConstants.lockReservationSeat(seatId),
                RedisKeyConstants.lockReservationUser(userId)
        );
    }
}
