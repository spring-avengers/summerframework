package com.bkjk.platform.lock.test;

import com.bkjk.platform.lock.LockInstance;
import com.bkjk.platform.lock.LockOperation;
import com.bkjk.platform.lock.LockType;
import com.bkjk.platform.lock.annotation.LockKey;
import com.bkjk.platform.lock.annotation.WithLock;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 13:22
 **/
@Component
@Slf4j
public class TestService {
    private ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    @Autowired
    private LockOperation lockOperation;

    public String format(String mapKey, long i) {
        return mapKey + " last value: " + i;
    }

    private String updateInner(String mapKey, long i) {
        cache.put(mapKey, format(mapKey, i));
        sleepInner(new Random().nextInt(20));
        return cache.get(mapKey);
    }

    @WithLock(value = "updateWithLock", keys = {"#mapKey"})
    public String updateWithLock(String mapKey, long i) {
        return updateInner(mapKey, i);
    }

    public String updateWithoutLock(String mapKey, long i) {
        return updateInner(mapKey, i);
    }

    @WithLock(timeoutMillis = 10)
    public void sleep(@LockKey long i) throws InterruptedException {
        sleepInner(i);
    }

    @WithLock
    public void exception() {
        throw new RuntimeException();
    }

    @WithLock(keys = {"#param['foo']", "#param['bar']"})
    public void multiKey(Map<String, String> param) {
        sleepInner(100);
    }

    @WithLock(name = "readWrite",lockType = LockType.READ)
    public void readLock(int s) {
        sleepInner(s);
    }

    @WithLock(name = "readWrite",lockType = LockType.WRITE)
    public void writeLock(int s) {
        sleepInner(s);
    }

    private void sleepInner(long s) {
        try {
            Thread.sleep(s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @WithLock(timeoutMillis = 10,lockFailedFallback = "fooFallback")
    public void foo(long s){
        sleepInner(s);
    }

    private void fooFallback(long s){
        log.info("fallback "+s);
    }

    @WithLock(timeoutMillis = 10,lockFailedFallback = "barFallback")
    public String bar(long s){
        sleepInner(s);
        return "bar";
    }

    private String barFallback(long s){
        log.info("fallback "+s);
        return "barFallback";
    }

    public String barOperation(long s){
        StringBuilder stringBuilder=new StringBuilder();

        LockInstance lock = lockOperation.requireLock("barOperation",10);
        // 执行业务代码，框架自动加锁和解锁
        lock.lockThen((lockInstance -> {
            sleepInner(s);
            stringBuilder.append("barOperation");
        }));

        return stringBuilder.toString();
    }

    public String barOperationFallback(long s){
        StringBuilder stringBuilder=new StringBuilder();

        LockInstance lock = lockOperation.requireLock("barOperation",10);

        // 执行业务代码，框架自动加锁和解锁
        lock.lockThen((lockInstance -> {
            sleepInner(s);
            stringBuilder.append("barOperation");
        }),lockInstance -> {
            // 如果加锁失败，则执行下面代码
            stringBuilder.append("barOperationFallback");
        });

        return stringBuilder.toString();
    }

    public String barOperation1(long s){
        StringBuilder stringBuilder=new StringBuilder();

        LockInstance lock = lockOperation.requireLock("barOperation1",10);
        // 执行业务代码，框架自动加锁和解锁
        lock.lockThen((lockInstance -> {
            sleepInner(s);
            stringBuilder.append("a");
        }));

        // 执行业务代码，框架自动加锁和解锁
        lock.lockThen((lockInstance -> {
            sleepInner(s);
            stringBuilder.append("b");
        }));

        return stringBuilder.toString();
    }

    public void barManually(){
        Lock lock = lockOperation.requireLock("barManually").getLock();
        Assert.assertNotNull(lock);
        lock.lock();
        try {
            System.out.println("do ...");
        } finally {
            lock.unlock();
        }
    }
}
