package com.bkjk.platform.monitor.metric;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.util.StringUtils;

import com.bkjk.platform.monitor.util.RequestUtil;
import com.hazelcast.core.IMap;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.cache.HazelcastCacheMetrics;
import io.micrometer.core.lang.Nullable;

public class MicrometerUtil {

    private static final RequestUtil requestUtil = new RequestUtil();

    private static final Tag EXCEPTION_NONE = tag("exception", "none");

    private static final String METHOD_WITH_URL_COUNT = "method_with_url_count";
    private static final String METHOD_WITH_URL_TIMER = "method_with_url_timer";
    private static final String TRACE_EVENT_COUNT = "trace_event_count";
    public final static String EMPTY_STRING = "none";
    private static ConcurrentHashMap<String, Tag> tagCache;

    public static final Tag TAG_STATUS_KEY_TRUE = tag("status_key", "true");

    public static final Tag TAG_STATUS_KEY_FALSE = tag("status_key", "false");

    public static void eventCount(String event) {
        String url = requestUtil.getCurrentRequestUrlOrDefault("none");
        Metrics.counter(TRACE_EVENT_COUNT, "event", event).increment();
    }

    public static Tag exception(@Nullable Throwable exception) {
        if (exception == null) {
            return EXCEPTION_NONE;
        }
        String simpleName = exception.getClass().getSimpleName();
        return tag("exception", simpleName.isEmpty() ? exception.getClass().getName() : simpleName);
    }

    public static Tags exceptionAndStatusKey(@Nullable Throwable exception) {
        return Tags.of(exception(exception), statusKey(exception));
    }

    public static final long getNanosecondsAfter(long start) {
        return Metrics.globalRegistry.config().clock().monotonicTime() - start;
    }

    public static void methodCount(String methodName, Throwable throwable) {
        String url = requestUtil.getCurrentRequestUrlOrDefault("none");
        Tags tags = Tags.of(Tag.of("method", methodName)).and(MicrometerUtil.exceptionAndStatusKey(throwable));
        Metrics.counter(METHOD_WITH_URL_COUNT, tags).increment();
    }

    public static void methodTimer(String methodName, long time, Throwable throwable) {
        String url = requestUtil.getCurrentRequestUrlOrDefault("none");
        Tags tags = Tags.of(Tag.of("method", methodName)).and(MicrometerUtil.exceptionAndStatusKey(throwable));
        Metrics.timer(METHOD_WITH_URL_TIMER, tags).record(time, TimeUnit.MILLISECONDS);
    }

    public static final void monitor(IMap iMap, String... tags) {
        HazelcastCacheMetrics.monitor(registry(), iMap, tags);
    }

    public static final long monotonicTime() {
        return Metrics.globalRegistry.config().clock().monotonicTime();
    }

    public static final MeterRegistry registry() {
        return Metrics.globalRegistry;
    }

    public static Tag statusKey(@Nullable Throwable exception) {
        return exception == null ? TAG_STATUS_KEY_TRUE : TAG_STATUS_KEY_FALSE;
    }

    public static final Tag tag(String key, String value) {
        if (tagCache == null) {
            tagCache = new ConcurrentHashMap<>();
        }
        if (StringUtils.isEmpty(value)) {
            value = EMPTY_STRING;
        }
        String cacheKey = key + value;
        Tag catchTag = tagCache.get(cacheKey);
        if (null != catchTag) {
            return catchTag;
        }
        Tag tag = Tag.of(key, value);
        tagCache.put(cacheKey, tag);
        return tag;
    }

    public static final Tags tags(String... keyValues) {
        if (keyValues.length == 0) {
            return Tags.empty();
        }
        if (keyValues.length % 2 == 1) {
            throw new IllegalArgumentException("size must be even, it is a set of key=value pairs");
        }
        Tag[] tags = new Tag[keyValues.length / 2];
        for (int i = 0; i < keyValues.length; i += 2) {
            tags[i / 2] = tag(keyValues[i], keyValues[i + 1]);
        }
        return Tags.of(tags);
    }

}
