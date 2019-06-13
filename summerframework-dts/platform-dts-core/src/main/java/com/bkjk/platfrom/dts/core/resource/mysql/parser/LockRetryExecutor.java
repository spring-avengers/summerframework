package com.bkjk.platfrom.dts.core.resource.mysql.parser;

public class LockRetryExecutor {

    private int lockRetryInternal = 1000;
    private int lockRetryTimes = 3;

    public void sleep() {
        if (--lockRetryTimes < 0) {
            throw new RuntimeException("Global lock wait timeout");
        }

        try {
            Thread.sleep(lockRetryInternal);
        } catch (InterruptedException ignore) {
        }
    }
}
