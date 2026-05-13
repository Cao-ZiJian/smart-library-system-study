package com.example.library.task;

import com.example.library.entity.BorrowOrder;
import com.example.library.enums.BorrowStatusEnum;
import com.example.library.service.BorrowOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 借阅相关定时任务：逾期扫描（幂等批量条件更新）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowScheduleTask {

    private final BorrowOrderService borrowOrderService;

    /**
     * LENT 且已过应还时间 → OVERDUE，重复执行仅影响仍为 LENT 且过期的行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void markOverdueBorrowOrders() {
        LocalDateTime now = LocalDateTime.now();
        boolean updated = borrowOrderService.lambdaUpdate()
                .eq(BorrowOrder::getStatus, BorrowStatusEnum.LENT.getCode())
                .lt(BorrowOrder::getDueTime, now)
                .set(BorrowOrder::getStatus, BorrowStatusEnum.OVERDUE.getCode())
                .update();
        log.info("event=borrow_overdue_scan now={} updated={}", now, updated);
    }
}
