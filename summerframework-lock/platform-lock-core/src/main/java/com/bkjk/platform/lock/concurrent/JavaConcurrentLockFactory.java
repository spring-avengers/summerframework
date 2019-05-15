package com.bkjk.platform.lock.concurrent;

import com.bkjk.platform.lock.LockFactory;
import com.bkjk.platform.lock.LockHandler;
import com.bkjk.platform.lock.LockInstance;
import com.bkjk.platform.lock.LockMonitor;
import org.springframework.lang.NonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/5 19:59
 **/
public class JavaConcurrentLockFactory implements LockFactory, LockMonitor {

    private ConcurrentHashMap<String, Lock> lockCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ReadWriteLock> readWriteLockCache = new ConcurrentHashMap<>();
    private LockHandler lockHandler = new LockHandler(){};

    @Override
    @NonNull
    public Lock getLock(LockInstance lockInstance) {
        return lockCache.computeIfAbsent(lockInstance.getName(), (key) -> new ReentrantLock(lockInstance.isFair()));
    }

    @Override
    @NonNull
    public ReadWriteLock getReadWriteLock(LockInstance lockInstance) {
        return readWriteLockCache.computeIfAbsent(lockInstance.getName(), (key) -> new ReentrantReadWriteLock(lockInstance.isFair()));
    }

    @Override
    public LockHandler getLockHandler(LockInstance lockInstance) {
        return lockHandler;
    }

    @Override
    public Set<String> getLockNames() {
        HashSet<String> names = new HashSet<>();
        names.addAll(lockCache.keySet());
        names.addAll(readWriteLockCache.keySet());
        return names;
    }

    @Override
    public Map<String, Thread> getLockOwners() {
        throw new UnsupportedOperationException();
    }
}
