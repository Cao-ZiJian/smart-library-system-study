package com.example.library.task;

import com.example.library.entity.BorrowOrder;
import com.example.library.enums.BorrowStatusEnum;
import com.example.library.mapper.BorrowOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 借阅相关定时任务
 * 自动将已逾期的借阅记录标记为 OVERDUE
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowScheduleTask {

    private final BorrowOrderMapper borrowOrderMapper;

    /**
     * 自动标记借阅逾期：status = LENT 且 当前时间 > due_time 则更新为 OVERDUE
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void markOverdueBorrowOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<BorrowOrder> list = borrowOrderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BorrowOrder>()
                        .eq(BorrowOrder::getStatus, BorrowStatusEnum.LENT.getCode())
                        .lt(BorrowOrder::getDueTime, now)
        );
        if (list.isEmpty()) {
            return;
        }
        for (BorrowOrder order : list) {
            order.setStatus(BorrowStatusEnum.OVERDUE.getCode());
            borrowOrderMapper.updateById(order);
        }
        log.info("借阅定时任务：已将 {} 条借阅记录标记为 OVERDUE", list.size());
    }
}
