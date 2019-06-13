package com.bkjk.platform.lock.redisson;

import com.bkjk.platform.lock.LockHandler;
import com.bkjk.platform.lock.LockInstance;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/7 10:17
 **/
public class RedissonLockHandler implements LockHandler {

    @Override
    public boolean doLock(LockInstance lockInstance) throws InterruptedException {
        return ((RLock)lockInstance.getLock()).tryLock(lockInstance.getTimeoutMillis(),lockInstance.getExpireTimeMillis()<=0?-1:lockInstance.getExpireTimeMillis(), TimeUnit.MILLISECONDS)
                && !Thread.currentThread().isInterrupted();
    }

    @Override
    public boolean doLock(long timeoutMillis,LockInstance lockInstance) throws InterruptedException {
        return ((RLock)lockInstance.getLock()).tryLock(timeoutMillis,lockInstance.getExpireTimeMillis()<=0?-1:lockInstance.getExpireTimeMillis(), TimeUnit.MILLISECONDS)
                && !Thread.currentThread().isInterrupted();
    }
}
