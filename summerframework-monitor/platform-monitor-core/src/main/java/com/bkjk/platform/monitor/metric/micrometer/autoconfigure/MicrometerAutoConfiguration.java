package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.monitor.metric.micrometer.PlatformTag;
import com.bkjk.platform.monitor.metric.micrometer.binder.micrometer.MicrometerBinder;
import com.bkjk.platform.monitor.util.InetUtils;
import com.bkjk.platform.monitor.util.InetUtilsProperties;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;

public class MicrometerAutoConfiguration {

    @Configuration
    @ConditionalOnClass({HazelcastInstance.class})
    @ConditionalOnBean(IMap.class)
    public static class HazelcastCacheConfiguration {
        @Bean
        public HazelcastCacheMetricsConfiguration hazelcastCacheMetricsConfiguration(Map<String, IMap> iMaps,
            Map<String, HazelcastInstance> hazelcastInstanceMap, MeterRegistry registry, PlatformTag platformTag) {
            return new HazelcastCacheMetricsConfiguration(iMaps, hazelcastInstanceMap, registry, platformTag);
        }

    }

    @Bean
    public HealthMetricsConfiguration healthMetricsConfiguration(HealthAggregator healthAggregator,
        List<HealthIndicator> healthIndicators, MeterRegistry registry, PlatformTag platformTag) {
        return new HealthMetricsConfiguration(healthAggregator, healthIndicators, registry, platformTag);
    }

    @Bean
    public MyRegistryCustomizer meterRegistryCustomizer(PlatformTag platformTag) {
        MyRegistryCustomizer ret = new MyRegistryCustomizer(platformTag);
        return ret;
    }

    @Bean
    public MicrometerBinder micrometerBinder() {
        return new MicrometerBinder();
    }

    @Bean
    public InetUtils monitorPinetUtils(InetUtilsProperties properties) {
        return new InetUtils(properties);
    }

    @Bean
    public InetUtilsProperties monitorProperties() {
        return new InetUtilsProperties();
    }

    @Bean
    public PlatformTag platformTag() {
        return new PlatformTag();
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
