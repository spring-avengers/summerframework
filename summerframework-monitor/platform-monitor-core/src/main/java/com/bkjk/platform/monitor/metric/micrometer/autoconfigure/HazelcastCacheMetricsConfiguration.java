package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import java.util.Map;

import com.bkjk.platform.monitor.metric.micrometer.PlatformTag;
import com.bkjk.platform.monitor.metric.micrometer.binder.hazelcast.HazelCastInstanceMetrics;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.HazelcastCacheMetrics;

public class HazelcastCacheMetricsConfiguration {
    public HazelcastCacheMetricsConfiguration(Map<String, IMap> iMaps,
        Map<String, HazelcastInstance> hazelcastInstanceMap, MeterRegistry registry, PlatformTag platformTag) {
        if (iMaps != null) {
            iMaps.forEach((k, v) -> {
                HazelcastCacheMetrics.monitor(registry, v, platformTag.getTags());
            });
        }
        if (hazelcastInstanceMap != null) {
            hazelcastInstanceMap.forEach((k, v) -> {
                new HazelCastInstanceMetrics(v, k, platformTag).bindTo(registry);
            });
        }
    }
}
