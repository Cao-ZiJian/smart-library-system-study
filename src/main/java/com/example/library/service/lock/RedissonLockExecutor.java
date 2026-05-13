package com.example.library.service.lock;

import com.example.library.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于 Redisson 的锁执行器。
 *
 * 多锁场景按 lock key 字符串排序后依次加锁，并按相反顺序释放，避免不同请求以不同顺序持有锁造成死锁。
 */
@Component
@RequiredArgsConstructor
public class RedissonLockExecutor implements LockExecutor {

    private static final long LEASE_SECONDS = 20L;

    private final RedissonClient redissonClient;

    @Override
    public <T> T executeWithLock(String lockKey, String busyMessage, Callable<T> action) {
        return doWithLock(redissonClient.getLock(lockKey), busyMessage, action);
    }

    @Override
    public <T> T executeWithMultiLock(String busyMessage, Callable<T> action, String... lockKeys) {
        Set<String> distinctKeys = new LinkedHashSet<>(Arrays.asList(lockKeys));
        List<RLock> locks = distinctKeys.stream()
                .filter(StringUtils::hasText)
                .sorted()
                .map(redissonClient::getLock)
                .collect(Collectors.toList());
        if (locks.isEmpty()) {
            throw new IllegalArgumentException("lockKeys must not be empty");
        }
        if (locks.size() == 1) {
            return doWithLock(locks.get(0), busyMessage, action);
        }
        return doWithOrderedLocks(locks, busyMessage, action);
    }

    private <T> T doWithLock(RLock lock, String busyMessage, Callable<T> action) {
        boolean locked = false;
        try {
            locked = lock.tryLock(0, LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(busyMessage);
            }
            return action.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(busyMessage);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("execute with distributed lock failed", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private <T> T doWithOrderedLocks(List<RLock> locks, String busyMessage, Callable<T> action) {
        int lockedCount = 0;
        try {
            for (RLock lock : locks) {
                boolean locked = lock.tryLock(0, LEASE_SECONDS, TimeUnit.SECONDS);
                if (!locked) {
                    throw new BusinessException(busyMessage);
                }
                lockedCount++;
            }
            return action.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(busyMessage);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("execute with distributed locks failed", e);
        } finally {
            for (int i = lockedCount - 1; i >= 0; i--) {
                RLock lock = locks.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}
