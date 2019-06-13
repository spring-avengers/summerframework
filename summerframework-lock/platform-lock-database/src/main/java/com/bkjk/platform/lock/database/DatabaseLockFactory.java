package com.bkjk.platform.lock.database;

import com.bkjk.platform.lock.LockFactory;
import com.bkjk.platform.lock.LockHandler;
import com.bkjk.platform.lock.LockInstance;
import com.bkjk.platform.lock.LockMonitor;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/16 19:34
 **/
public class DatabaseLockFactory implements LockFactory, LockMonitor {
    private DatabaseLockHandler lockHandler;
    private DataSource dataSource;

    public DatabaseLockFactory(DataSource dataSource,DatabaseLockHandler lockHandler) {
        this.dataSource=dataSource;
        this.lockHandler=lockHandler;
    }

    @Override
    @NonNull
    public Lock getLock(LockInstance lockInstance) {
        return null;
    }

    @Override
    @NonNull
    public ReadWriteLock getReadWriteLock(LockInstance lockInstance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LockHandler getLockHandler(LockInstance lockInstance) {
        return lockHandler;
    }

    @Override
    public Set<String> getLockNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Thread> getLockOwners() {
        throw new UnsupportedOperationException();
    }
}
