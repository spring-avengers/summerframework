package com.bkjk.platform.monitor.metric.micrometer.binder.hazelcast;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import com.bkjk.platform.monitor.metric.micrometer.PlatformTag;
import com.hazelcast.core.IMap;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.HazelcastCacheMetrics;

@Aspect
public class HazelCastAspect {
    MeterRegistry registry;
    PlatformTag platformTag;

    public HazelCastAspect(MeterRegistry registry, PlatformTag platformTag) {
        this.registry = registry;
        this.platformTag = platformTag;
    }

    @Around("execution(* com.hazelcast.core.HazelcastInstance.getMap(..))")
    public IMap aroundGetMap(ProceedingJoinPoint pjp) throws Throwable {
        IMap iMap = (IMap)pjp.proceed();
        try {
            HazelcastCacheMetrics.monitor(registry, iMap, platformTag.getTags());
        } catch (Throwable ignore) {
        }
        return iMap;
    }
}
