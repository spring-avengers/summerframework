package com.bkjk.platform.lock;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/5 19:15
 **/
public enum LockType {
    /**
     * @see java.util.concurrent.locks.ReentrantLock
     * @see com.bkjk.platform.lock.annotation.WithLock
     */
    DEFAULT,
    /**
     * @see java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
     * @see com.bkjk.platform.lock.annotation.WithReadLock
     */
    READ,
    /**
     * @see java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock
     * @see com.bkjk.platform.lock.annotation.WithWriteLock
     */
    WRITE,
    ;

    LockType() {
    }
}
