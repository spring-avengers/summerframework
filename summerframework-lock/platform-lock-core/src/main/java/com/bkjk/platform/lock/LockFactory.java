package com.bkjk.platform.lock;

import org.springframework.lang.NonNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/5 19:54
 **/
public interface LockFactory {
    /**
     * 获取锁
     * @param lockInstance
     * @return
     */
    @NonNull
    Lock getLock(LockInstance lockInstance);

    /**
     * 获取读写锁
     * @param lockInstance
     * @return
     */
    @NonNull
    ReadWriteLock getReadWriteLock(LockInstance lockInstance);

    /**
     * 获取 LockHandler
     * @param lockInstance
     * @return
     */
    @NonNull
    LockHandler getLockHandler(LockInstance lockInstance);

}
