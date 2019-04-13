package com.bkjk.platform.monitor.metric.micrometer.binder.redis;

import java.lang.reflect.Field;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.ReflectionUtils;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

@Slf4j
public class RedisPoolMeterBinder implements MeterBinder {
    Iterable<Tag> tags;
    Pool<Jedis> pool;

    public RedisPoolMeterBinder(Iterable<Tag> tags, JedisConnectionFactory jedisConnectionFactory) {
        this.tags = tags;
        Field poolField = ReflectionUtils.findField(JedisConnectionFactory.class, "pool");
        ReflectionUtils.makeAccessible(poolField);
        try {
            this.pool = (Pool<Jedis>)poolField.get(jedisConnectionFactory);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void bindTo(MeterRegistry registry) {

        if (pool == null) {
            return;
        }
        Field internalPoolField = ReflectionUtils.findField(Pool.class, "internalPool");
        ReflectionUtils.makeAccessible(internalPoolField);
        try {
            GenericObjectPool<Jedis> internalPool = (GenericObjectPool<Jedis>)internalPoolField.get(this.pool);
            Gauge.builder("redis.pool.active", internalPool, GenericObjectPool::getNumActive).tags(tags)
                .description("Active redis connection").register(registry);

            Gauge.builder("redis.pool.total", internalPool, GenericObjectPool::getMaxTotal).tags(tags)
                .description("MaxTotal redis connection").register(registry);

            Gauge.builder("redis.pool.idle", internalPool, GenericObjectPool::getNumIdle).tags(tags)
                .description("Idle redis connection").register(registry);

            Gauge.builder("redis.pool.waiters", internalPool, GenericObjectPool::getNumWaiters).tags(tags)
                .description(
                    "The estimate of the number of threads currently blocked waiting for an object from the pool")
                .register(registry);

            Gauge.builder("redis.pool.borrowed", internalPool, GenericObjectPool::getBorrowedCount).tags(tags)
                .description("The total number of objects successfully borrowed from this pool").register(registry);

            Gauge.builder("redis.pool.created", internalPool, GenericObjectPool::getCreatedCount).tags(tags)
                .description("The total number of objects created for this pool ").register(registry);

            Gauge.builder("redis.pool.destroyed", internalPool, GenericObjectPool::getDestroyedCount).tags(tags)
                .description("The total number of objects destroyed by this pool").register(registry);

        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }

    }
}
