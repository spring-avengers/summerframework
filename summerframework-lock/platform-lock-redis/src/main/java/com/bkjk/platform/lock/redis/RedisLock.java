package com.bkjk.platform.lock.redis;

import com.bkjk.platform.lock.exception.LockFailedException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Program: summerframework2
 * @Description: 非重入锁。重复加锁会导致死锁
 * @Author: shaoze.wang
 * @Create: 2019/5/8 14:53
 **/
@Slf4j
public class RedisLock implements Lock {


    public static final String UNLOCK_LUA;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("if redis.call(\"get\",KEYS[1]) == ARGV[1] ");
        sb.append("then ");
        sb.append("    return redis.call(\"del\",KEYS[1]) ");
        sb.append("else ");
        sb.append("    return 0 ");
        sb.append("end ");
        UNLOCK_LUA = sb.toString();
    }


    private static final ConcurrentHashMap<String, RenewEntry> EXPIRATION_RENEWAL_MAP = new ConcurrentHashMap<>();

    public static final String RENEW_LUA="if (redis.call('exists', KEYS[1], ARGV[1]) == 1) then " +
            "redis.call('pexpire', KEYS[1], ARGV[2]); " +
            "return 1; " +
            "end; " +
            "return 0;";

    private RedisTemplate<Object, Object> redisTemplate;

    private final ThreadLocal<String> lockFlag = new ThreadLocal<>();
    private String name;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private long expire;

    public RedisLock(RedisTemplate<Object, Object> redisTemplate,String name,long expire,ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        this.redisTemplate = redisTemplate;
        this.name=name;
        this.expire=expire;
        this.scheduledThreadPoolExecutor=scheduledThreadPoolExecutor;
    }

    @Override
    public void lock() {
        tryLock(Long.MAX_VALUE, 0);
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
        return tryLock(0, 0);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long millis = unit.toMillis(time);
        return tryLock( millis, 0);
    }

    public boolean tryLock(long wait, long expire) {
        if(expire>0){
            this.expire=expire;
        }
        long start = System.currentTimeMillis();
        long duration = 0;
        boolean success = false;
        while (!success && (duration <= wait)) {
            try {
                String result = redisTemplate.execute((RedisCallback<String>)connection -> {
                    JedisCommands commands = (JedisCommands)connection.getNativeConnection();
                    String uuid = UUID.randomUUID().toString();
                    lockFlag.set(uuid);
                    return commands.set(name, uuid, "NX","PX",this.expire);
                });
                if (!StringUtils.isEmpty(result)) {
                    scheduleRenewal(lockFlag.get());
                    success = true;
                    return success;
                } else {
                    log.debug("try lock fail, will retry lockKey: {}.", name);
                    try {
                        // TODO 锁争抢激烈时性能很差，考虑替换为监听key变化。
                        TimeUnit.MILLISECONDS.sleep(new Random().nextInt(10));
                    } catch (InterruptedException e) {
                        log.error("lock occured an exception", e);
                    }
                }
            } catch (Exception e) {
                throw new LockFailedException(e);
            }
            duration = System.currentTimeMillis() - start;
        }
        return success;
    }

    @Override
    public void unlock() {
        cancelRenewal(lockFlag.get());
        try {
            List<String> keys = new ArrayList<>();
            keys.add(name);
            List<String> args = new ArrayList<>();
            args.add(lockFlag.get());
            Long ret=redisTemplate.execute((RedisCallback<Long>) connection -> (Long)eval(connection,UNLOCK_LUA,keys,args));
            if(ret!=1){
                throw new IllegalMonitorStateException("attempt to unlock lock, not locked by current thread: " + Thread.currentThread());
            }
        } catch (Exception e) {
            log.debug("release tryLock occur an exception", e);
        } finally {
            lockFlag.remove();
        }
    }

    private Object eval(RedisConnection connection,String lua,List<String> keys,List<String> args){
        Object nativeConnection = connection.getNativeConnection();
        if (nativeConnection instanceof JedisCluster) {
            return (Long)((JedisCluster)nativeConnection).eval(lua, keys, args);
        } else if (nativeConnection instanceof Jedis) {
            return (Long)((Jedis)nativeConnection).eval(lua, keys, args);
        }
        return 0L;

    }

    public void forceUnlock() {
        cancelRenewal(lockFlag.get());
        try {
            List<String> keys = new ArrayList<>();
            keys.add(name);
            List<String> args = new ArrayList<>();
            args.add(lockFlag.get());
            redisTemplate.execute((RedisCallback<Long>) connection -> {
                Object nativeConnection = connection.getNativeConnection();
                if (nativeConnection instanceof JedisCluster) {
                    return (Long) ((JedisCluster) nativeConnection).del(name);
                } else if (nativeConnection instanceof Jedis) {
                    return (Long) ((Jedis) nativeConnection).del(name);
                }
                return 0L;
            });
        }finally {
            lockFlag.remove();
        }
    }


    private void renew(String lockFlag) {
        List<String> keys = new ArrayList<>();
        keys.add(name);
        List<String> args = new ArrayList<>();
        args.add(lockFlag);
        args.add(String.valueOf(this.expire));
        redisTemplate.execute((RedisCallback) connection -> eval(connection,RENEW_LUA,keys,args));
    }

    private void scheduleRenewal(String lockFlag) {
        if (EXPIRATION_RENEWAL_MAP.containsKey(name)) {
            return;
        }
        ScheduledFuture<?> task = scheduledThreadPoolExecutor.schedule(() -> {
            EXPIRATION_RENEWAL_MAP.remove(name);
            try{
                renew(lockFlag);
            }catch (Throwable t){
                log.error("Error while renew lock {}",name,t);
            }finally {
                scheduleRenewal(lockFlag);
            }
        }, expire / 3, TimeUnit.MILLISECONDS);

        if (EXPIRATION_RENEWAL_MAP.putIfAbsent(name,RenewEntry.builder().task(task).lockFlag(lockFlag).name(name).build()) != null) {
            task.cancel(false);
        }
    }

    void cancelRenewal(String lockFlag) {
        RenewEntry entry= EXPIRATION_RENEWAL_MAP.get(name);
        if (entry != null&&entry.getLockFlag().equals(lockFlag)) {
            EXPIRATION_RENEWAL_MAP.remove(name);
            entry.getTask().cancel(false);
        }
    }

    @Data
    @Builder
    private static class RenewEntry{
        String name;
        String lockFlag;
        ScheduledFuture<?> task;
    }
}
