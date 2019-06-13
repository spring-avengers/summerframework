package com.bkjk.platform.monitor.metric.micrometer.binder.redis;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bkjk.platform.monitor.Monitors;
import com.bkjk.platform.redis.AbstractRedisMonitorListener;

public class MonitorRedisCommandListener extends AbstractRedisMonitorListener {
    private static final Logger logger = LoggerFactory.getLogger(MonitorRedisCommandListener.class);
    private static final String REDIS_COMMAND_COST = "redis.command.cost";
    private static final String REDIS_COMMAND_BYTES_LENGTH = "redis.command.bytes.length";

    private long slowCommandNanosecond = 0;

    public MonitorRedisCommandListener(long slowCommandNanosecond) {
        this.slowCommandNanosecond = slowCommandNanosecond;
    }

    @Override
    public void afterCommand(String hostPort, String command, long costNano, int valueBytesLength,
        Throwable throwable) {
        if (logger.isDebugEnabled() || costNano > slowCommandNanosecond) {

            Monitors.recordTime(REDIS_COMMAND_COST, costNano, TimeUnit.NANOSECONDS, "command", command, "hostPort",
                hostPort, "exception", null == throwable ? "none" : throwable.getClass().getSimpleName());
            Monitors.summary(REDIS_COMMAND_BYTES_LENGTH, valueBytesLength, "command", command, "hostPort", hostPort,
                "exception", null == throwable ? "none" : throwable.getClass().getSimpleName());
        }
    }
}
