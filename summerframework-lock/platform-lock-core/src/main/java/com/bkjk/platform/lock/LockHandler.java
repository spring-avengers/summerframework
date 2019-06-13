package com.bkjk.platform.lock;

import com.bkjk.platform.lock.exception.LockFailedException;

import java.util.concurrent.TimeUnit;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 10:16
 **/
public interface LockHandler {

    /**
     * 获取锁超时
     * @param lockInstance
     *
     * @return null表示最终返回returnObject；返回回任意值表是用该值代替returnObject。
     */
    default Object onLockFailed(LockInstance lockInstance){
        // throw
        throw new LockFailedException(String.format("LockInstance: %s", lockInstance.toString()),lockInstance.getLockFailed());
    };

    /**
     * 加锁
     * @return
     */
    default boolean doLock(LockInstance lockInstance) throws InterruptedException {
      return lockInstance.getLock().tryLock(lockInstance.getTimeoutMillis(), TimeUnit.MILLISECONDS);
    };

    /**
     * 加锁
     * @return
     */
    default boolean doLock(long timeoutMillis,LockInstance lockInstance) throws InterruptedException {
        return lockInstance.getLock().tryLock(timeoutMillis, TimeUnit.MILLISECONDS);
    };

    /**
     * 解锁
     * @return
     */
    default void doUnlock(LockInstance lockInstance){
        lockInstance.getLock().unlock();
    };

}
