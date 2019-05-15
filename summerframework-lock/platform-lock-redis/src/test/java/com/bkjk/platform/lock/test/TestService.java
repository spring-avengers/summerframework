package com.bkjk.platform.lock.test;

import com.bkjk.platform.lock.LockType;
import com.bkjk.platform.lock.annotation.LockKey;
import com.bkjk.platform.lock.annotation.WithLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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

}
