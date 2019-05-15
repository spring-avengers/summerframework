package com.bkjk.platform.lock;

import com.bkjk.platform.lock.autoconfigure.LockAutoConfiguration;
import com.bkjk.platform.lock.autoconfigure.LockConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/7 17:01
 **/
public class LockOperation  implements ApplicationContextAware {
    private LockFactory defaultLockFactory;
    @Autowired
    private LockConfiguration lockConfiguration;

    private ApplicationContext applicationContext;

    public LockOperation() {
    }

    /**
     * 对于不设置名字的锁，会用 <code>lockedCode.getClass().getCanonicalName()</code> 作为锁的名称
     * @return
     */
    public LockInstance requireLock() {
        checkDefaultLockFactory();
        return new LockInstance().lockFactory(defaultLockFactory).setExpireTimeMillis(lockConfiguration.getExpireTimeMillis());
    }

    /**
     * 获取指定名称的锁，名称只能指定一次，第二次指定会抛出IllegalStateException异常
     * @param name
     * @return
     */
    public LockInstance requireLock(String name) {
        return requireLock(name,Long.MAX_VALUE, lockConfiguration.getExpireTimeMillis(),LockType.DEFAULT,false);
    }

    public LockInstance requireLock(String name,long timeoutMillis) {
        return requireLock(name,timeoutMillis, lockConfiguration.getExpireTimeMillis(),LockType.DEFAULT,false);
    }

    public LockInstance requireLock(String name,long timeoutMillis,long expireTimeMillis,LockType type,boolean fair) {
        checkDefaultLockFactory();
        Assert.notNull(name);
        Assert.notNull(type);
        return new LockInstance()
                .lockFactory(defaultLockFactory)
                .setName(name)
                .setTimeoutMillis(timeoutMillis)
                .setExpireTimeMillis(expireTimeMillis)
                .setType(type)
                .setFair(fair)
                .createLockIfNotExist();
    }

    private void checkDefaultLockFactory(){
        if(defaultLockFactory==null){
            Assert.notNull(applicationContext,"applicationContext should not be null");
            defaultLockFactory=applicationContext.getBean(LockAutoConfiguration.DEFAULT_LOCK_FACTORY_BEAN,LockFactory.class);
            Assert.notNull(defaultLockFactory,"defaultLockFactory not found");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
    }

}
