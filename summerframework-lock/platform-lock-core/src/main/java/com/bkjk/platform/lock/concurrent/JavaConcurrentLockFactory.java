package com.bkjk.platform.lock.concurrent;

import com.bkjk.platform.lock.LockFactory;
import com.bkjk.platform.lock.LockHandler;
import com.bkjk.platform.lock.LockInstance;
import com.bkjk.platform.lock.LockMonitor;
import org.springframework.lang.NonNull;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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


    private final ReferenceQueue<?> queue = new ReferenceQueue<>();
    private ConcurrentHashMap<String, WeakReferenceWithKey<Lock>> lockCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, WeakReferenceWithKey<ReadWriteLock>> readWriteLockCache = new ConcurrentHashMap<>();
    private LockHandler lockHandler = new LockHandler(){};
    private ScheduledExecutorService scheduledExecutor;

    @PostConstruct
    public void init(){
        if(scheduledExecutor!=null&&!scheduledExecutor.isTerminated()){
            scheduledExecutor.shutdown();
        }
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        //noinspection AlibabaThreadPoolCreation
        scheduledExecutor.scheduleWithFixedDelay(()->{
            //noinspection InfiniteLoopStatement
            while (true){
                WeakReferenceWithKey<?> ref = (WeakReferenceWithKey<?>) queue.poll();
                if(ref==null){
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignore) {
                    }
                }else {
                    if(lockCache.containsKey(ref.getKey())){
                        lockCache.remove(ref.getKey(),ref);
                    }
                    if(readWriteLockCache.containsKey(ref.getKey())){
                        readWriteLockCache.remove(ref.getKey(),ref);
                    }
                }
            }
        },0,10, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy(){
        if(null!=scheduledExecutor&&!scheduledExecutor.isTerminated()){
            scheduledExecutor.shutdown();
        }
    }

    @Override
    @NonNull
    public Lock getLock(LockInstance lockInstance) {
        WeakReferenceWithKey<Lock> lockRef = lockCache.computeIfAbsent(lockInstance.getName(), (key) -> new WeakReferenceWithKey<>(new ReentrantLock(lockInstance.isFair()),queue,key));
        Lock lock=lockRef.get();
        if(lock==null){
            ReentrantLock newLock = new ReentrantLock(lockInstance.isFair());
            lockCache.computeIfAbsent(lockInstance.getName(), (key) -> new WeakReferenceWithKey<>(newLock,queue,key));
            lock=newLock;
        }
        return lock;
    }

    @Override
    @NonNull
    public ReadWriteLock getReadWriteLock(LockInstance lockInstance) {

        WeakReferenceWithKey<ReadWriteLock> lockRef = readWriteLockCache.computeIfAbsent(lockInstance.getName(), (key) -> new WeakReferenceWithKey<>(new ReentrantReadWriteLock(lockInstance.isFair()),queue,key));
        ReadWriteLock lock=lockRef.get();
        if(lock==null){
            ReentrantReadWriteLock newLock = new ReentrantReadWriteLock(lockInstance.isFair());
            readWriteLockCache.computeIfAbsent(lockInstance.getName(), (key) -> new WeakReferenceWithKey<>(newLock,queue,key));
            lock=newLock;
        }
        return lock;
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
