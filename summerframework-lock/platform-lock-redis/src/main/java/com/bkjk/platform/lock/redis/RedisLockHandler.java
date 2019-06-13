package com.bkjk.platform.lock.redis;

import com.bkjk.platform.lock.LockHandler;
import com.bkjk.platform.lock.LockInstance;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/7 10:17
 **/
public class RedisLockHandler implements LockHandler {

    @Override
    public boolean doLock(LockInstance lockInstance) throws InterruptedException {
        return ((RedisLock)lockInstance.getLock()).tryLock(lockInstance.getTimeoutMillis(),lockInstance.getExpireTimeMillis());
    }

    @Override
    public boolean doLock(long timeoutMillis,LockInstance lockInstance) throws InterruptedException {
        return ((RedisLock)lockInstance.getLock()).tryLock(timeoutMillis,lockInstance.getExpireTimeMillis());
    }
}
