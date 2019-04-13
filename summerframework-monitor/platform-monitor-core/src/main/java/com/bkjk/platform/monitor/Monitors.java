package com.bkjk.platform.monitor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.bkjk.platform.monitor.metric.MicrometerUtil;
import com.google.common.collect.ImmutableMap;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.Tracer;

public class Monitors {
    private static final Logger logger = LoggerFactory.getLogger("BizLogger");

    private static final Logger LOGGER = LoggerFactory.getLogger(Monitors.class);

    private static final InheritableThreadLocal<Span> TRANSACTION_CACHE = new InheritableThreadLocal<Span>();

    public static void begin() {
        try {
            Tracer tracer = getTracer();
            Span span = tracer.buildSpan("customerSpan").startManual();
            TRANSACTION_CACHE.set(span);
        } catch (Throwable e) {
            LOGGER.warn(" begin error", e);
        }
    }

    public static final void count(String name, double amount, String... tags) {
        try {
            Metrics.counter(name, MicrometerUtil.tags(tags)).increment(amount);
        } catch (Throwable e) {
            LOGGER.warn(" count error", e);
        }
    }

    public static final void count(String name, String... tags) {
        try {
            Metrics.counter(name, MicrometerUtil.tags(tags)).increment();
        } catch (Throwable e) {
            LOGGER.warn(" count error", e);
        }
    }

    public static void finish() {
        try {
            Span span = TRANSACTION_CACHE.get();
            if (span != null) {
                span.finish();
            }
            TRANSACTION_CACHE.remove();
        } catch (Throwable e) {
            LOGGER.warn(" finish error", e);
        }
    }

    public static final <T extends Number> void gauge(String name, T number, String... tags) {
        try {
            Metrics.gauge(name, MicrometerUtil.tags(tags), number);
        } catch (Throwable e) {
            LOGGER.warn(" gauge error", e);
        }
    }

    public static final <T extends Collection<?>> T gaugeCollectionSize(String name, T collection, String... tags) {
        try {
            return Metrics.gaugeCollectionSize(name, MicrometerUtil.tags(tags), collection);
        } catch (Throwable e) {
            LOGGER.warn(" gaugeCollectionSize error", e);
        }
        return null;
    }

    public static final <T extends Map<?, ?>> T gaugeMapSize(String name, T map, String... tags) {
        try {
            return Metrics.gaugeMapSize(name, MicrometerUtil.tags(tags), map);
        } catch (Throwable e) {
            LOGGER.warn(" gaugeMapSize error", e);
        }
        return null;
    }

    public static final long getNanosecondsAfter(long start) {
        try {
            return registry().config().clock().monotonicTime() - start;
        } catch (Throwable e) {
            LOGGER.warn(" getNanosecondsAfter error", e);
        }
        return System.nanoTime();
    }

    private static Tracer getTracer() {
        Tracer tracer = new org.apache.skywalking.apm.toolkit.opentracing.SkywalkingTracer();
        return tracer;
    }

    public static void logEvent(String type, Object param) {
        try {
            MicrometerUtil.eventCount(type);
            Span span = TRANSACTION_CACHE.get();
            LogEvent event = new LogEvent(type, param);
            logger.info(event.toString());
            if (span != null) {
                ImmutableMap.Builder<String, Object> eventMap = ImmutableMap.builder();
                eventMap.put("eventType", event.getType());
                eventMap.put("eventParam", event.getParam());
                span.log(eventMap.build());
            } else {
                Tracer tracer = getTracer();
                ActiveSpan activeSpan = tracer.buildSpan(type).startActive();
                ImmutableMap.Builder<String, Object> eventMap = ImmutableMap.builder();
                eventMap.put("eventType", event.getType());
                eventMap.put("eventParam", event.getParam());
                activeSpan.log(eventMap.build());
                activeSpan.close();
            }
        } catch (Throwable e) {
            LOGGER.warn("log event error", e);
        }
    }

    public static void main(String[] args) {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
        String counterName = "s";
        String[] tag = new String[] {"name", "test"};
        count(counterName, tag);
        count(counterName, 100.2);
        count(counterName, -50);
        Assert.isTrue(Metrics.counter(counterName).count() == 50.2);
        Assert.isTrue(Metrics.counter(counterName, tag).count() == 1);

        String gaugeName = "g";
        gauge(gaugeName, 2);
        gauge(gaugeName, 3, tag);
        Assert.isTrue(Metrics.globalRegistry.get(gaugeName).gauge().value() == 2);
        Assert.isTrue(Metrics.globalRegistry.get(gaugeName).tags(tag).gauge().value() == 3);

        gaugeName = "gl";
        List list = new ArrayList();
        gaugeCollectionSize(gaugeName, list);
        Assert.isTrue(Metrics.globalRegistry.get(gaugeName).gauge().value() == 0);
        list.add("1");
        Assert.isTrue(Metrics.globalRegistry.get(gaugeName).gauge().value() == 1);
        list.clear();
        Assert.isTrue(Metrics.globalRegistry.get(gaugeName).gauge().value() == 0);

        gaugeName = "gm";
        Map m = new HashMap();
        gaugeMapSize(gaugeName, m, tag);
        Assert.isTrue(Metrics.globalRegistry.get(gaugeName).tags(tag).gauge().value() == 0);
        m.put("a", "b");
        Assert.isTrue(Metrics.globalRegistry.get(gaugeName).tags(tag).gauge().value() == 1);
        m.clear();
        Assert.isTrue(Metrics.globalRegistry.get(gaugeName).tags(tag).gauge().value() == 0);

        String timeName = "t";
        recordTime(timeName, 100, TimeUnit.NANOSECONDS);
        Assert.isTrue(Metrics.globalRegistry.get(timeName).timer().count() == 1);
        recordNanoSecond(timeName, 10);
        Assert.isTrue(Metrics.globalRegistry.get(timeName).timer().count() == 2);
        Assert.isTrue(Metrics.globalRegistry.get(timeName).timer().max(TimeUnit.NANOSECONDS) == 100);
        Assert.isTrue(Metrics.globalRegistry.get(timeName).timer().totalTime(TimeUnit.NANOSECONDS) == 110);
        recordNanoSecondAfterStartTime(timeName, monotonicTime());
        Assert.isTrue(Metrics.globalRegistry.get(timeName).timer().totalTime(TimeUnit.NANOSECONDS) > 210);

        String summaryName = "sm";
        summary(summaryName, 10);
        summary(summaryName, 100);
        Assert.isTrue(Metrics.globalRegistry.get(summaryName).summary().count() == 2);
        Assert.isTrue(Metrics.globalRegistry.get(summaryName).summary().max() == 100);
        Assert.isTrue(Metrics.globalRegistry.get(summaryName).summary().totalAmount() == 110);

        summary(summaryName, 10, new double[] {0.9}, tag);
        Assert.isTrue(Metrics.globalRegistry.get(summaryName).tags(tag).summary().count() == 1);
        summary(summaryName, 20, new double[] {0.9}, tag);
        summary(summaryName, 100, new double[] {0.9}, tag);
        Assert.isTrue(Metrics.globalRegistry.get(summaryName).tags(tag).summary().percentile(0.9) > 20);

        double[] percentiles = new double[] {0.5, 0.8, 0.9};
        summary("trade", 10, percentiles, "channel", "wechat");
        summary("trade", 20, percentiles, "channel", "alipay");

    }

    public static final long monotonicTime() {
        try {
            return registry().config().clock().monotonicTime();
        } catch (Throwable e) {
            LOGGER.warn(" monotonicTime error", e);
        }
        return System.nanoTime();
    }

    public static final void recordNanoSecond(String name, long timeElapsedNanos, String... tags) {
        try {
            recordTime(name, timeElapsedNanos, TimeUnit.NANOSECONDS, tags);
        } catch (Throwable e) {
            LOGGER.warn(" recordNanoSecond error", e);
        }
    }

    public static final void recordNanoSecondAfterStartTime(String name, long startTime, String... tags) {
        try {
            recordTime(name, monotonicTime() - startTime, TimeUnit.NANOSECONDS, tags);
        } catch (Throwable e) {
            LOGGER.warn(" recordNanoSecondAfterStartTime error", e);
        }
    }

    public static final void recordTime(String name, long timeElapsed, TimeUnit timeUnit, double[] percentiles,
        Duration[] sla, String... tags) {
        try {
            Timer.builder(name).publishPercentiles(percentiles).sla(sla).publishPercentileHistogram()
                .tags(MicrometerUtil.tags(tags)).register(registry()).record(timeElapsed, timeUnit);
        } catch (Throwable e) {
            LOGGER.warn(" recordTime error", e);
        }
    }

    public static final void recordTime(String name, long timeElapsed, TimeUnit timeUnit, String... tags) {
        try {
            Metrics.timer(name, MicrometerUtil.tags(tags)).record(timeElapsed, timeUnit);
        } catch (Throwable e) {
            LOGGER.warn(" recordTime error", e);
        }
    }

    private static final MeterRegistry registry() {
        return Metrics.globalRegistry;
    }

    public static final void summary(String name, double amount, double[] percentiles, long[] sla, String... tags) {
        try {
            DistributionSummary.builder(name).publishPercentiles(percentiles).publishPercentileHistogram().sla(sla)
                .tags(MicrometerUtil.tags(tags)).register(registry()).record(amount);
        } catch (Throwable e) {
            LOGGER.warn(" summary error", e);
        }
    }

    public static final void summary(String name, double amount, double[] percentiles, String... tags) {
        try {
            DistributionSummary.builder(name).publishPercentiles(percentiles).tags(MicrometerUtil.tags(tags))
                .register(registry()).record(amount);
        } catch (Throwable e) {
            LOGGER.warn(" summary error", e);
        }
    }

    public static final void summary(String name, double amount, String... tags) {
        try {
            Metrics.summary(name, MicrometerUtil.tags(tags)).record(amount);
        } catch (Throwable e) {
            LOGGER.warn(" summary error", e);
        }
    }

}
