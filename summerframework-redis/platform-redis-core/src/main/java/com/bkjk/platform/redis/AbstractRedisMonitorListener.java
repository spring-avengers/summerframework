package com.bkjk.platform.redis;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public abstract class AbstractRedisMonitorListener {
    private static final CopyOnWriteArrayList<AbstractRedisMonitorListener> LISTENERS = new CopyOnWriteArrayList<>();

    public static final void addListener(AbstractRedisMonitorListener redisCommandListener) {
        synchronized (LISTENERS) {
            if (!LISTENERS.contains(redisCommandListener)) {
                LISTENERS.add(redisCommandListener);
            }
        }
    }

    public static final void forEachListener(Consumer<AbstractRedisMonitorListener> action) {
        LISTENERS.forEach(action);
    }

    public static final void removeListener(AbstractRedisMonitorListener redisCommandListener) {
        if (LISTENERS.contains(redisCommandListener)) {
            LISTENERS.remove(redisCommandListener);
        }
    }

    public abstract void afterCommand(String hostPort, String command, long costNano, int valueBytesLength,
        Throwable throwable);

}
