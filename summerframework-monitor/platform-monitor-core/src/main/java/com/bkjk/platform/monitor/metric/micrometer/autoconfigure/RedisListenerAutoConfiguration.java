package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.bkjk.platform.monitor.metric.micrometer.binder.redis.MonitorRedisCommandListener;
import com.bkjk.platform.redis.AbstractRedisMonitorListener;

@ConditionalOnClass(name = "com.bkjk.platform.redis.monitor.RedisCommandListener")
public class RedisListenerAutoConfiguration {
    public static final Logger logger = LoggerFactory.getLogger("RedisListenerAutoConfiguration");

    public static void main(String[] args) {
        System.out.println(TimeUnit.MILLISECONDS.toNanos(1));
    }

    @Value("${monitor.redis.command.slow.nanosecond:1000000}")
    private long slowCommandNanosecond;

    @Bean
    @ConditionalOnProperty(value = "monitor.redis.command.enable", matchIfMissing = true, havingValue = "true")
    public MonitorRedisCommandListener monitorRedisCommandListener() {
        logger.info(
            "Redis performance will be recorded. It is costly sometimes. Disable it by setting monitor.redis.command.enable=false");
        MonitorRedisCommandListener monitorRedisCommandListener =
            new MonitorRedisCommandListener(slowCommandNanosecond);
        AbstractRedisMonitorListener.addListener(monitorRedisCommandListener);
        return monitorRedisCommandListener;
    }
}
