package com.bkjk.platform.monitor.metric.micrometer.binder.db;

import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.bkjk.platform.monitor.Monitors;

@Aspect
public class TransactionUtil {

    private static final Logger logger = LoggerFactory.getLogger("monitor.transaction");

    public static final String MONITOR_KEY = "transaction";

    public static final ThreadLocal<Long> beginTime = new ThreadLocal<>();

    public static void clean() {
        beginTime.remove();
    }

    public static void record(Object transaction, String type, long currentMonotonicTime) {
        long timeElapsedNanos = currentMonotonicTime - beginTime.get();
        logger.info("{} called. Cost = {} nanoseconds . Transaction.hashCode = {}", type, timeElapsedNanos,
            transaction.hashCode());
        Monitors.recordNanoSecond(MONITOR_KEY, timeElapsedNanos, "type", type);
    }

    public static void recordBegin(Object transaction) {
        long now = Monitors.monotonicTime();
        beginTime.set(now);
        record(transaction, "begin", now);
    }

    public static void recordCommit(Object transaction) {
        record(((DefaultTransactionStatus)transaction).getTransaction(), "commit", Monitors.monotonicTime());
        clean();
    }

    public static void recordRollback(Object transaction) {

        record(((DefaultTransactionStatus)transaction).getTransaction(), "rollback", Monitors.monotonicTime());
        clean();
    }

}
