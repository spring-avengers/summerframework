package com.bkjk.platform.redis.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import com.bkjk.platform.redis.DistributedLock;

public class DataRedisDistributedLock implements DistributedLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataRedisDistributedLock.class);

    private static final DelayQueue<RenewLockKeyItem> DELAY_LOCK_KEY = new DelayQueue<RenewLockKeyItem>();

    private static final Map<String, RenewLockKeyItem> CAHCHED_DELAY_LOCK_KEY =
        new ConcurrentHashMap<String, RenewLockKeyItem>();

    private final StringRedisTemplate redisTemplate;

    private final Environment env;

    private DefaultRedisScript<Long> lockScript;

    private DefaultRedisScript<Long> unlockScript;

    private DefaultRedisScript<Long> renewScript;

    private ThreadLocal<String> threadKeyId = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return UUID.randomUUID().toString().replace("-", "");
        }
    };

    public DataRedisDistributedLock(StringRedisTemplate stringRedisTemplate, Environment env) {
        this.redisTemplate = stringRedisTemplate;
        this.env = env;
        this.initLuaScript();
        this.initRenew();
    }

    private void initLuaScript() {
        // lock script
        lockScript = new DefaultRedisScript<Long>();
        lockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lock/lock.lua")));
        lockScript.setResultType(Long.class);
        LOGGER.debug("init lock lua script success:{}", lockScript.getScriptAsString());
        // unlock script
        unlockScript = new DefaultRedisScript<Long>();
        unlockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lock/unlock.lua")));
        unlockScript.setResultType(Long.class);
        LOGGER.debug("init release lua script success:{}", unlockScript.getScriptAsString());
        // renew script
        renewScript = new DefaultRedisScript<Long>();
        renewScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lock/renew.lua")));
        renewScript.setResultType(Long.class);
        LOGGER.debug("init renew lua script success:{}", renewScript.getScriptAsString());
    }

    private void initRenew() {
        Thread renewTask = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        RenewLockKeyItem keyItem = DELAY_LOCK_KEY.take();
                        redisTemplate.execute(renewScript, keyItem.getReidsKeyList(), keyItem.getRedisExpireTime());
                        DELAY_LOCK_KEY.put(keyItem);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        renewTask.setDaemon(true);
        renewTask.setName("REDIS_RENEW_TASK");
        renewTask.start();
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
    public void lock() {
        tryLock();
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
    public void unlock() {
        unlock(getAppLockDefaultKey(env));
    }

    @Override
    public boolean tryLock(String key, long wait, long expire) {
        return this.lockInternal(key, wait, expire);
    }

    @Override
    public void unlock(String key) {
        unlockInternal(key);
    }

    private boolean lockInternal(String key, long waitTime, long expireTime) {
        List<String> keyList = new ArrayList<String>();
        keyList.add(key);
        keyList.add(threadKeyId.get());
        long start = System.currentTimeMillis();
        long duration = 0;
        boolean success = false;
        while (!success && (duration <= waitTime)) {
            if (redisTemplate.execute(lockScript, keyList, String.valueOf(expireTime)) > 0) {
                LOGGER.debug("lock success，lockKey:{}", key);
                success = true;
                return success;
            } else {
                try {
                    Thread.sleep(10, (int)(Math.random() * 500));
                } catch (InterruptedException e) {
                    LOGGER.debug("tryLock occured an exception", e);
                }
            }
            duration = System.currentTimeMillis() - start;
        }
        if (!CAHCHED_DELAY_LOCK_KEY.containsKey(key)) {
            RenewLockKeyItem renewLock = new RenewLockKeyItem(expireTime - 1000, keyList, String.valueOf(expireTime));
            CAHCHED_DELAY_LOCK_KEY.put(key, renewLock);
            DELAY_LOCK_KEY.add(renewLock);
        }
        return success;

    }

    private boolean unlockInternal(String key) {
        List<String> keyList = new ArrayList<String>();
        keyList.add(key);
        keyList.add(threadKeyId.get());
        redisTemplate.execute(unlockScript, keyList);
        LOGGER.debug("unlock，lockKey:{}", key);
        threadKeyId.remove();
        RenewLockKeyItem keyItem = CAHCHED_DELAY_LOCK_KEY.get(key);
        DELAY_LOCK_KEY.remove(keyItem);
        CAHCHED_DELAY_LOCK_KEY.remove(key);
        return true;
    }

    private static class RenewLockKeyItem implements Delayed {

        private final long expire;
        private final List<String> reidsKeyList;
        private final String redisExpireTime;

        public RenewLockKeyItem(long delay, List<String> keyList, String redisExpireTime) {
            this.reidsKeyList = keyList;
            this.expire = System.currentTimeMillis() + delay;
            this.redisExpireTime = redisExpireTime;
        }

        @Override
        public int compareTo(Delayed o) {
            return (int)(this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(this.expire - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        public List<String> getReidsKeyList() {
            return reidsKeyList;
        }

        public String getRedisExpireTime() {
            return redisExpireTime;
        }

    }

}
