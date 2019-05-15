package com.bkjk.platform.lock.redis;

import com.bkjk.platform.lock.LockFactory;
import com.bkjk.platform.lock.LockHandler;
import com.bkjk.platform.lock.LockInstance;
import com.bkjk.platform.lock.LockMonitor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 19:43
 **/
public class RedisLockFactory implements LockFactory, LockMonitor {

    private RedisTemplate redisTemplate;
    private LockHandler lockHandler ;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public RedisLockFactory(RedisTemplate redisTemplate,LockHandler lockHandler) {
        this(redisTemplate,lockHandler,Runtime.getRuntime().availableProcessors() * 2);
    }

    public RedisLockFactory(RedisTemplate redisTemplate, LockHandler lockHandler,int scheduledPoolSize) {
        CustomizableThreadFactory threadFactory=new CustomizableThreadFactory("redis-lock");
        threadFactory.setDaemon(true);
        this.redisTemplate = redisTemplate;
        this.lockHandler = lockHandler;
        scheduledThreadPoolExecutor=new ScheduledThreadPoolExecutor(scheduledPoolSize,threadFactory);
    }

    @Override
    @NonNull
    public Lock getLock(LockInstance lockInstance) {
        return new RedisLock(redisTemplate,
                lockInstance.getName(),
                lockInstance.getExpireTimeMillis(),
                scheduledThreadPoolExecutor);
    }

    @Override
    @NonNull
    public ReadWriteLock getReadWriteLock(LockInstance lockInstance) {
        throw new UnsupportedOperationException();
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
