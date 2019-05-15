package com.bkjk.platform.lock;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.springframework.util.Assert;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/7 19:29
 **/
@Data
@Builder
@Accessors(chain = true)
public class LockInstance{
    @Tolerate
    public LockInstance(){

    }

    boolean fair = false;
    private LockType type = LockType.DEFAULT;
    private volatile String name;
    private long timeoutMillis = Long.MAX_VALUE;
    private long expireTimeMillis = 0;
    private volatile Lock lock;
    private volatile ReadWriteLock readWriteLock;
    /**
     * 当前是否已经获得锁
     */
    private boolean locked;
    /**
     * 加锁抛出的异常
     */
    private Throwable lockFailed;

    /**
     * 解锁抛出的异常
     */
    private Throwable unlockFailed;

    private Consumer<LockInstance> lockedCode;
    private Consumer<LockInstance> lockFailedFallback;
    private Consumer<LockInstance> unLockFailedFallback;

    private LockFactory lockFactory;

    public LockInstance lockFactory(LockFactory lockFactory) {
        this.lockFactory = lockFactory;
        return this;
    }

    public LockInstance setName(String name){
        if(getName()!=null){
            throw new IllegalStateException("Name already exist");
        }
        this.name=name;
        return this;
    }

    public LockInstance setLock(Lock lock){
        if(getLock()!=null){
            throw new IllegalStateException("Lock already exist");
        }
        this.lock=lock;
        return this;
    }

    public LockInstance createLockIfNotExist() {
        if (getLock() != null) {
            return this;
        }
        if (null == getName() && lockedCode != null) {
            setName(lockedCode.getClass().getCanonicalName());
        }
        Assert.notNull(getName(), "Lock name can not be null");
        switch (getType()) {
            case DEFAULT:
                Lock lock = lockFactory.getLock(this);
                this.setLock(lock);
                break;
            case READ:
                ReadWriteLock read = lockFactory.getReadWriteLock(this);
                this.setReadWriteLock(read);
                this.setLock(read.readLock());
                break;
            case WRITE:
                ReadWriteLock write = lockFactory.getReadWriteLock(this);
                this.setReadWriteLock(write);
                this.setLock(write.writeLock());
                break;
            default:
                Assert.notNull(this.getType(), "LockType cant not be null");
                break;
        }
        return this;
    }


    public void lockThen(Consumer<LockInstance> lockedCode){
        lockThen(lockedCode,null,null);
    }

    public void lockThen(Consumer<LockInstance> lockedCode, Consumer<LockInstance> lockFailedFallback){
        lockThen(lockedCode,lockFailedFallback,null);
    }


    public void lockThen(long timeoutMillis, Consumer<LockInstance> lockedCode, Consumer<LockInstance> lockFailedFallback){
        setTimeoutMillis(timeoutMillis)
                .lockThen(lockedCode,lockFailedFallback);
    }

    public void lockThen(long timeoutMillis, Consumer<LockInstance> lockedCode, Consumer<LockInstance> lockFailedFallback, Consumer<LockInstance> unLockFailedFallback){
        setTimeoutMillis(timeoutMillis)
                .lockThen(lockedCode,lockFailedFallback,unLockFailedFallback);
    }

    public void lockThen(Consumer<LockInstance> lockedCode, Consumer<LockInstance> lockFailedFallback, Consumer<LockInstance> unLockFailedFallback) {
        Assert.notNull(lockedCode);
        if(lockFailedFallback==null){
            lockFailedFallback=this.lockFailedFallback;
        }
        if(unLockFailedFallback==null){
            unLockFailedFallback=this.unLockFailedFallback;
        }
        this.lockedCode = lockedCode;
        createLockIfNotExist();
        LockHandler lockHandler = lockFactory.getLockHandler(this);
        try {
            try {
                setLocked(lockHandler.doLock(this));
            } catch (Throwable f) {
                setLockFailed(f);
                setLocked(false);
            }
            if (!isLocked()) {
                // 加锁失败后，如果指定了fallback，则返回fallback
                if (lockFailedFallback != null) {
                    lockFailedFallback.accept(this);
                    return;
                }
                Object ret = lockHandler.onLockFailed(this);
                if (ret != null) {
                    return;
                }
            }
            lockedCode.accept(this);
        } finally {
            try {
                if (isLocked()) {
                    lockHandler.doUnlock(this);
                }
            } catch (Throwable throwable) {
                setUnlockFailed(throwable);
                // 解锁失败后，如果指定了fallback，则调用fallback
                if (unLockFailedFallback != null) {
                    unLockFailedFallback.accept(this);
                }
            }
        }
    }
}
