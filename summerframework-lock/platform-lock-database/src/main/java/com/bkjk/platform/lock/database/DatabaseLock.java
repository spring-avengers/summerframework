package com.bkjk.platform.lock.database;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/16 19:38
 **/
public class DatabaseLock implements Lock {

    private DataSource dataSource;
    private String tableName;
    /**
     * 要锁定的业务key的表字段名称，合同号、用户号等
     */
    private String keyColumnName;
    /**
     * 备注字段的名称
     */
    private String memoColumnName;
    /**
     * 过期字段的名称
     */
    private String expireColumnName;

    public static final String lock_sql="update %s set %s = %s where %s =%s";

    @Override
    public void lock() {

    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {

    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}
