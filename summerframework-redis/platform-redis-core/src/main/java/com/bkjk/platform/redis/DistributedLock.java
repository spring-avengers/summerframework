package com.bkjk.platform.redis;

import java.util.concurrent.locks.Lock;

import org.springframework.core.env.Environment;

public interface DistributedLock extends Lock {

    public static final String GLOBAL_LOCK_DEFAULT_KEY = "platform.redis.global.distribute.key";
    public static final String DISTRIBUTED_LOCK_NAME = "distributedLock";

    public static final long DEFAULT_EXPIRE_MILLS = 60000;

    public static final long DEFAULT_WAIT_MILLS = 3000;

    boolean tryLock(String key, long wait, long expire);

    void unlock(String key);

    default String getAppLockDefaultKey(Environment env) {
        return env.getProperty("spring.application.name") + GLOBAL_LOCK_DEFAULT_KEY;
    }

}
