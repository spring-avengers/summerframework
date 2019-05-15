package com.bkjk.platform.lock.redisson;

import com.bkjk.platform.lock.LockFactory;
import com.bkjk.platform.lock.LockHandler;
import com.bkjk.platform.lock.LockInstance;
import com.bkjk.platform.lock.LockMonitor;
import org.redisson.api.RedissonClient;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 19:43
 **/
public class RedissonLockFactory implements LockFactory, LockMonitor {

    private RedissonClient redissonClient;
    private LockHandler lockHandler ;

    public RedissonLockFactory(RedissonClient redissonClient,LockHandler lockHandler) {
        this.redissonClient = redissonClient;
        this.lockHandler=lockHandler;
    }


    @Override
    @NonNull
    public Lock getLock(LockInstance lockInstance) {
        return lockInstance.isFair()?redissonClient.getFairLock(lockInstance.getName()):redissonClient.getLock(lockInstance.getName());
    }

    @Override
    @NonNull
    public ReadWriteLock getReadWriteLock(LockInstance lockInstance) {
        // TODO redisson的ReadWriteLock不支持 fair ？
        return redissonClient.getReadWriteLock(lockInstance.getName());
    }

    @Override
    public LockHandler getLockHandler(LockInstance lockInstance) {
        return lockHandler;
    }

    @Override
    public Set<String> getLockNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Thread> getLockOwners() {
        throw new UnsupportedOperationException();
    }
}
