package com.example.library.service.borrow;

import com.example.library.constant.RedisKeyConstants;
import com.example.library.service.lock.LockExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * 借阅申请并发控制，按 userId + bookId 维度串行化申请动作。
 */
@Component
@RequiredArgsConstructor
public class BorrowApplyLockExecutor {

    private static final String BUSY_MESSAGE = "借阅申请处理中，请勿重复提交";

    private final LockExecutor lockExecutor;

    public <T> T execute(Long userId, Long bookId, Callable<T> action) {
        return lockExecutor.executeWithLock(
                RedisKeyConstants.lockBorrowApply(userId, bookId),
                BUSY_MESSAGE,
                action
        );
    }
}
