package com.example.library.service.lock;

import java.util.concurrent.Callable;

/**
 * 分布式锁执行器，屏蔽加锁、解锁和异常转换细节。
 */
public interface LockExecutor {

    <T> T executeWithLock(String lockKey, String busyMessage, Callable<T> action);

    <T> T executeWithMultiLock(String busyMessage, Callable<T> action, String... lockKeys);
}
