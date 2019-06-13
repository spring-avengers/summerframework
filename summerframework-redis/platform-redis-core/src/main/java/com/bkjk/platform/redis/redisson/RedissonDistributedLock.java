package com.bkjk.platform.redis.redisson;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.bkjk.platform.redis.DistributedLock;

public class RedissonDistributedLock implements DistributedLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedissonDistributedLock.class);

    private final RedissonClient redissonClient;

    private final Environment env;

    public RedissonDistributedLock(RedissonClient redissonClient, Environment env) {
        this.redissonClient = redissonClient;
        this.env = env;
    }

    @Override
    public void lock() {
        tryLock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        return tryLock(getAppLockDefaultKey(env), DEFAULT_WAIT_MILLS, DEFAULT_EXPIRE_MILLS);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        Long millis = unit.toMillis(time);
        return tryLock(getAppLockDefaultKey(env), millis, DEFAULT_EXPIRE_MILLS);
    }

    @Override
    public boolean tryLock(String key, long wait, long expire) {
        long start = System.currentTimeMillis();
        long duration = 0;
        boolean success = false;

        while (!success && duration <= wait) {
            try {
                success = redissonClient.getLock(key).tryLock(expire, TimeUnit.MILLISECONDS);
                if (success) {
                    return success;
                } else {
                    LOGGER.warn("try lock fail, will retry lockKey: {}.", key);
                }
            } catch (Exception e) {
                LOGGER.debug("redisson lock error, e : {}", e);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                LOGGER.debug("tryLock occured an exception", e);
            }
            duration = System.currentTimeMillis() - start;
        }
        return success;
    }

    @Override
    public void unlock() {
        unlock(getAppLockDefaultKey(env));
    }

    @Override
    public void unlock(String key) {
        try {
            redissonClient.getLock(key).unlock();
        } catch (Exception e) {
            LOGGER.debug("try to unlock key error, key:{}, error:{}.", key, e);
        }
    }
}
