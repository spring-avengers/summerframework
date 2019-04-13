package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import java.util.HashSet;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import com.bkjk.platform.monitor.metric.micrometer.PlatformTag;
import com.bkjk.platform.monitor.metric.micrometer.binder.redis.RedisPoolMeterBinder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import redis.clients.jedis.Jedis;

@AutoConfigureAfter(MicrometerAutoConfiguration.class)
public class RedisAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Jedis.class)
    public static class MysqlDatabaseStatusAutoConfiguration {
        @Bean
        public RedisCustomizer redisCustomizer() {
            return new RedisCustomizer();
        }
    }

    public static class RedisCustomizer {
        @Autowired(required = false)
        Map<String, JedisConnectionFactory> jedisConnectionFactoryMap;

        @Autowired
        private MeterRegistry registry;

        @Autowired
        private PlatformTag platformTag;

        @PostConstruct
        public void init() {
            if (jedisConnectionFactoryMap == null) {
                return;
            }
            jedisConnectionFactoryMap.forEach((k, v) -> {
                HashSet<Tag> tags = new HashSet<>();
                tags.addAll(platformTag.getTags());
                tags.add(Tag.of("name", k));
                new RedisPoolMeterBinder(platformTag.getTags(), v).bindTo(registry);
            });
        }

    }
}
